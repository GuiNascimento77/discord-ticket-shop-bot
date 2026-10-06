package dev.ticketshop.discord;

import dev.ticketshop.config.Config;
import dev.ticketshop.domain.Ticket;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Cuida do canal de texto que representa cada atendimento.
 *
 * <p>O canal nasce trancado: {@code @everyone} nao ve nada, o cliente e a equipe veem.
 * E o que torna o atendimento "privado".
 */
public final class TicketCanal {

    private static final List<Permission> PERMISSOES_DO_CLIENTE = List.of(
            Permission.VIEW_CHANNEL,
            Permission.MESSAGE_SEND,
            Permission.MESSAGE_HISTORY,
            Permission.MESSAGE_ATTACH_FILES,
            Permission.MESSAGE_EMBED_LINKS);

    private static final List<Permission> PERMISSOES_DA_EQUIPE = List.of(
            Permission.VIEW_CHANNEL,
            Permission.MESSAGE_SEND,
            Permission.MESSAGE_HISTORY,
            Permission.MESSAGE_MANAGE,
            Permission.MESSAGE_ATTACH_FILES,
            Permission.MESSAGE_EMBED_LINKS);

    /** Segundos entre a mensagem de despedida e a exclusao do canal. */
    private static final long SEGUNDOS_PARA_APAGAR = 6;

    private final Config config;

    public TicketCanal(Config config) {
        this.config = config;
    }

    /**
     * Monta a acao de criacao do canal, mas <b>nao executa</b>: quem chama decide quando
     * enviar ao Discord com {@code .queue(...)}.
     */
    public ChannelAction<TextChannel> criar(Guild guild, Ticket ticket, Member dono) {
        String nome = nomeDoCanal(ticket, dono);
        Category categoria = config.categoriaId().map(guild::getCategoryById).orElse(null);

        ChannelAction<TextChannel> acao = (categoria == null
                ? guild.createTextChannel(nome)
                : guild.createTextChannel(nome, categoria))
                .setTopic("Atendimento #" + ticket.id() + " - " + dono.getUser().getName())
                // deny = negar; allow = permitir. null significa "nao mexer nessa permissao".
                .addPermissionOverride(guild.getPublicRole(), null, List.of(Permission.VIEW_CHANNEL))
                .addMemberPermissionOverride(dono.getIdLong(), PERMISSOES_DO_CLIENTE, null);

        config.staffRoleId().ifPresent(idDoCargo -> {
            Role cargo = guild.getRoleById(idDoCargo);
            if (cargo != null) {
                acao.addRolePermissionOverride(cargo.getIdLong(), PERMISSOES_DA_EQUIPE, null);
            }
        });

        return acao;
    }

    /** Manda a mensagem de despedida e apaga o canal alguns segundos depois. */
    public void encerrar(TextChannel canal, MessageEmbed despedida) {
        canal.sendMessageEmbeds(despedida).queue(
                mensagem -> canal.delete().queueAfter(SEGUNDOS_PARA_APAGAR, TimeUnit.SECONDS));
    }

    /** Texto que marca a equipe, usado quando um pedido novo precisa de atencao. */
    public String mencaoDaEquipe() {
        return config.staffRoleId().map(id -> "<@&" + id + "> ").orElse("");
    }

    /**
     * Nome do canal no padrao do Discord: minusculo, sem acento e sem espaco.
     * "Joao da Silva" vira {@code pedido-joao-da-silva}.
     */
    static String nomeDoCanal(Ticket ticket, Member dono) {
        String apelido = dono.getUser().getName()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (apelido.isEmpty()) {
            apelido = "cliente";
        }
        if (apelido.length() > 60) {
            apelido = apelido.substring(0, 60);
        }
        return ticket.tipo().prefixoDoCanal() + "-" + apelido;
    }
}

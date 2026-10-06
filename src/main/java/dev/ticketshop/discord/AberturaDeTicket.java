package dev.ticketshop.discord;

import dev.ticketshop.discord.view.Botoes;
import dev.ticketshop.discord.view.Embeds;
import dev.ticketshop.domain.ItemCarrinho;
import dev.ticketshop.domain.ItemPedido;
import dev.ticketshop.domain.Ticket;
import dev.ticketshop.service.TicketService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.util.List;

/**
 * Junta as duas metades: as regras ({@link TicketService}) e o Discord ({@link TicketCanal}).
 *
 * <p>Todo metodo aqui segue o mesmo ritmo:
 * <ol>
 *   <li>grava o ticket no banco;</li>
 *   <li>pede a criacao do canal (assincrono, com {@code queue});</li>
 *   <li>quando o canal fica pronto, salva o id dele e publica a mensagem;</li>
 *   <li>avisa o usuario no canal de origem.</li>
 * </ol>
 *
 * <p>Repare que o canal so e publicado dentro do callback: o Discord pode demorar para
 * criar o canal, e travar a thread esperando seria desperdicio.
 */
public final class AberturaDeTicket {

    private final TicketService tickets;
    private final TicketCanal canais;

    public AberturaDeTicket(TicketService tickets, TicketCanal canais) {
        this.tickets = tickets;
        this.canais = canais;
    }

    /** Abre um atendimento de duvida. */
    public void abrirDuvida(Guild guild, Member dono, String assunto, InteractionHook retorno) {
        Ticket ticket = tickets.abrirDuvida(dono.getId(), assunto);

        canais.criar(guild, ticket, dono).queue(
                canal -> {
                    Ticket vinculado = tickets.vincularCanal(ticket.id(), canal.getId());
                    canal.sendMessageEmbeds(Embeds.ticketDeDuvida(vinculado))
                            .setComponents(Botoes.atendimento(vinculado.id()))
                            .queue();
                    responder(retorno, Embeds.sucesso("Atendimento #" + vinculado.id()
                            + " aberto em " + canal.getAsMention() + "."));
                },
                erro -> responder(retorno, Embeds.erro("Nao consegui criar o canal do atendimento: " + erro.getMessage())));
    }

    /** Abre um atendimento que ja nasce como pedido (compra feita fora de um atendimento). */
    public void abrirPedido(Guild guild, Member dono, List<ItemCarrinho> carrinho, String observacoes,
                            InteractionHook retorno) {
        Ticket ticket = tickets.abrirPedido(dono.getId(), "Pedido pelo catalogo", carrinho);

        canais.criar(guild, ticket, dono).queue(
                canal -> {
                    Ticket vinculado = tickets.vincularCanal(ticket.id(), canal.getId());
                    publicarPedido(canal, vinculado, observacoes);
                    responder(retorno, Embeds.sucesso("Pedido #" + vinculado.id()
                            + " enviado para a equipe em " + canal.getAsMention() + "."));
                },
                erro -> responder(retorno, Embeds.erro("Nao consegui criar o canal do pedido: " + erro.getMessage())));
    }

    /**
     * O fluxo hibrido: o cliente abriu um atendimento por duvida e, na conversa, resolveu
     * comprar. O mesmo canal vira um pedido — o historico nao se perde.
     */
    public void anexarPedidoAoAtendimento(TextChannel canal, Ticket atendimento, List<ItemCarrinho> carrinho,
                                          String observacoes, InteractionHook retorno) {
        Ticket atualizado = tickets.converterEmPedido(atendimento.id(), carrinho);
        publicarPedido(canal, atualizado, observacoes);
        responder(retorno, Embeds.sucesso("Pedido #" + atualizado.id()
                + " registrado neste atendimento. A equipe foi avisada."));
    }

    private void publicarPedido(TextChannel canal, Ticket ticket, String observacoes) {
        List<ItemPedido> itens = tickets.itens(ticket.id());
        MessageEmbed resumo = Embeds.resumoPedido(ticket, itens);

        if (observacoes != null && !observacoes.isBlank()) {
            resumo = new net.dv8tion.jda.api.EmbedBuilder(resumo)
                    .addField("Observacoes do cliente", observacoes, false)
                    .build();
        }

        canal.sendMessage(canais.mencaoDaEquipe() + "novo pedido recebido:")
                .setEmbeds(resumo)
                .setComponents(Botoes.pedido(ticket.id()))
                .queue();
    }

    private static void responder(InteractionHook retorno, MessageEmbed embed) {
        retorno.sendMessageEmbeds(embed).setEphemeral(true).queue();
    }
}

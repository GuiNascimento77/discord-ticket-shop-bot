package dev.ticketshop.discord;

import dev.ticketshop.discord.command.SlashCommand;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * O "indice" de comandos: guarda todos e publica no Discord.
 *
 * <p>Sem isso, cada comando novo exigiria mexer em varios lugares. Aqui, basta criar a
 * classe e adicionar na lista do {@code Bot}.
 */
public final class Comandos {

    private static final Logger LOG = LoggerFactory.getLogger(Comandos.class);

    private final Map<String, SlashCommand> porNome;

    public Comandos(List<SlashCommand> comandos) {
        this.porNome = comandos.stream()
                .collect(Collectors.toUnmodifiableMap(SlashCommand::nome, Function.identity()));
    }

    public Optional<SlashCommand> buscar(String nome) {
        return Optional.ofNullable(porNome.get(nome));
    }

    public List<SlashCommand> todos() {
        return List.copyOf(porNome.values());
    }

    /**
     * Publica os comandos no Discord.
     *
     * <p>Com {@code GUILD_ID} os comandos aparecem no servidor na hora (bom para desenvolver).
     * Sem ele, viram comandos globais, que podem levar ate uma hora para propagar.
     */
    public void registrar(JDA jda, Optional<String> guildId) {
        List<CommandData> dados = todos().stream().map(SlashCommand::dados).collect(Collectors.toList());

        if (guildId.isEmpty()) {
            jda.updateCommands().addCommands(dados).queue(
                    ignorado -> LOG.info("{} comandos globais registrados.", dados.size()),
                    erro -> LOG.error("Falha ao registrar os comandos globais.", erro));
            return;
        }

        Guild guild = jda.getGuildById(guildId.get());
        if (guild == null) {
            LOG.error("Nao encontrei o servidor {}. Confira o GUILD_ID no .env.", guildId.get());
            return;
        }

        guild.updateCommands().addCommands(dados).queue(
                ignorado -> LOG.info("{} comandos registrados no servidor \"{}\".", dados.size(), guild.getName()),
                erro -> LOG.error("Falha ao registrar os comandos no servidor.", erro));
    }
}

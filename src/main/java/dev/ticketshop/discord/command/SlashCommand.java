package dev.ticketshop.discord.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * Um comando de barra (aqueles que aparecem ao digitar {@code /}).
 *
 * <p>Cada comando sabe se declarar ({@link #dados()}) e se executar ({@link #executar}).
 * O registro no Discord acontece em {@code Comandos#registrar}.
 */
public interface SlashCommand {

    /** Nome do comando, sem a barra. Ex.: {@code "carrinho"}. */
    String nome();

    /** Nome, descricao e opcoes que o Discord vai mostrar. */
    SlashCommandData dados();

    /** Executa o comando. */
    void executar(SlashCommandInteractionEvent evento);
}

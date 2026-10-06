package dev.ticketshop.discord.command;

import dev.ticketshop.discord.Autorizacao;
import dev.ticketshop.discord.view.Embeds;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/** {@code /ajuda} — explica o bot. A lista da equipe so aparece para quem e da equipe. */
public final class AjudaCommand implements SlashCommand {

    private final Autorizacao autorizacao;

    public AjudaCommand(Autorizacao autorizacao) {
        this.autorizacao = autorizacao;
    }

    @Override
    public String nome() {
        return "ajuda";
    }

    @Override
    public SlashCommandData dados() {
        return Commands.slash("ajuda", "Mostra o que o bot faz e como usar");
    }

    @Override
    public void executar(SlashCommandInteractionEvent evento) {
        boolean ehEquipe = autorizacao.ehEquipe(evento.getMember());
        evento.replyEmbeds(Embeds.ajuda(ehEquipe)).setEphemeral(true).queue();
    }
}

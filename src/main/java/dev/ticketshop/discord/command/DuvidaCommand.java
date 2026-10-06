package dev.ticketshop.discord.command;

import dev.ticketshop.discord.AberturaDeTicket;
import dev.ticketshop.discord.view.Embeds;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * {@code /duvida} — abre um atendimento privado.
 *
 * <p>Criar canal e uma chamada de rede que pode demorar, e o Discord exige uma resposta em
 * ate 3 segundos. Por isso usamos {@code deferReply()}: avisamos "estou trabalhando nisso"
 * e respondemos depois pelo {@code hook}.
 */
public final class DuvidaCommand implements SlashCommand {

    private final AberturaDeTicket abertura;

    public DuvidaCommand(AberturaDeTicket abertura) {
        this.abertura = abertura;
    }

    @Override
    public String nome() {
        return "duvida";
    }

    @Override
    public SlashCommandData dados() {
        return Commands.slash("duvida", "Abre um atendimento privado com a equipe")
                .addOption(OptionType.STRING, "assunto", "Sobre o que voce quer falar?", true);
    }

    @Override
    public void executar(SlashCommandInteractionEvent evento) {
        if (evento.getGuild() == null || evento.getMember() == null) {
            evento.replyEmbeds(Embeds.erro("Use este comando dentro de um servidor.")).setEphemeral(true).queue();
            return;
        }

        String assunto = evento.getOption("assunto", OptionMapping::getAsString);
        evento.deferReply(true).queue();
        abertura.abrirDuvida(evento.getGuild(), evento.getMember(), assunto, evento.getHook());
    }
}

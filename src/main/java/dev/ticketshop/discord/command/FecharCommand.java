package dev.ticketshop.discord.command;

import dev.ticketshop.discord.Autorizacao;
import dev.ticketshop.discord.TicketCanal;
import dev.ticketshop.discord.view.Embeds;
import dev.ticketshop.domain.Ticket;
import dev.ticketshop.service.TicketService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.Optional;

/**
 * {@code /fechar} — encerra o atendimento do canal onde o comando foi usado.
 *
 * <p>O bot descobre qual e o ticket pelo <b>canal</b>, nao por um numero digitado: menos
 * chance de erro e impossivel fechar o atendimento de outra pessoa sem permissao.
 */
public final class FecharCommand implements SlashCommand {

    private final TicketService tickets;
    private final TicketCanal canais;
    private final Autorizacao autorizacao;

    public FecharCommand(TicketService tickets, TicketCanal canais, Autorizacao autorizacao) {
        this.tickets = tickets;
        this.canais = canais;
        this.autorizacao = autorizacao;
    }

    @Override
    public String nome() {
        return "fechar";
    }

    @Override
    public SlashCommandData dados() {
        return Commands.slash("fechar", "Encerra este atendimento");
    }

    @Override
    public void executar(SlashCommandInteractionEvent evento) {
        if (!(evento.getChannel() instanceof TextChannel canal)) {
            evento.replyEmbeds(Embeds.erro("Use este comando dentro de um atendimento.")).setEphemeral(true).queue();
            return;
        }

        Optional<Ticket> encontrado = tickets.porCanal(canal.getId());
        if (encontrado.isEmpty()) {
            evento.replyEmbeds(Embeds.erro("Este canal nao e um atendimento aberto pelo bot."))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        Ticket ticket = encontrado.get();
        autorizacao.exigirDonoOuEquipe(evento.getMember(), ticket);

        evento.deferReply(true).queue();
        Ticket fechado = tickets.fechar(ticket.id());
        canais.encerrar(canal, Embeds.atendimentoEncerrado(fechado, evento.getUser().getEffectiveName()));
        evento.getHook().sendMessageEmbeds(
                        Embeds.sucesso("Atendimento #" + fechado.id() + " encerrado. Este canal sera apagado em instantes."))
                .setEphemeral(true)
                .queue();
    }
}

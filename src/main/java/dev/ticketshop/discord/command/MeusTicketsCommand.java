package dev.ticketshop.discord.command;

import dev.ticketshop.discord.view.Embeds;
import dev.ticketshop.service.TicketService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/** {@code /meus-tickets} — historico do proprio usuario. */
public final class MeusTicketsCommand implements SlashCommand {

    private final TicketService tickets;

    public MeusTicketsCommand(TicketService tickets) {
        this.tickets = tickets;
    }

    @Override
    public String nome() {
        return "meus-tickets";
    }

    @Override
    public SlashCommandData dados() {
        return Commands.slash("meus-tickets", "Ve os seus atendimentos e pedidos");
    }

    @Override
    public void executar(SlashCommandInteractionEvent evento) {
        evento.replyEmbeds(Embeds.meusTickets(tickets.meusTickets(evento.getUser().getId())))
                .setEphemeral(true)
                .queue();
    }
}

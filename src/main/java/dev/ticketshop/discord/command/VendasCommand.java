package dev.ticketshop.discord.command;

import dev.ticketshop.discord.Autorizacao;
import dev.ticketshop.discord.view.Embeds;
import dev.ticketshop.service.TicketService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/** {@code /vendas} — o painel de numeros, montado por uma consulta agregada no banco. */
public final class VendasCommand implements SlashCommand {

    private final TicketService tickets;
    private final Autorizacao autorizacao;

    public VendasCommand(TicketService tickets, Autorizacao autorizacao) {
        this.tickets = tickets;
        this.autorizacao = autorizacao;
    }

    @Override
    public String nome() {
        return "vendas";
    }

    @Override
    public SlashCommandData dados() {
        return Commands.slash("vendas", "Resumo de vendas da loja")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL));
    }

    @Override
    public void executar(SlashCommandInteractionEvent evento) {
        autorizacao.exigirEquipe(evento.getMember());
        evento.replyEmbeds(Embeds.vendas(tickets.resumo())).setEphemeral(true).queue();
    }
}

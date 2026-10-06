package dev.ticketshop.discord.command;

import dev.ticketshop.discord.view.Botoes;
import dev.ticketshop.discord.view.Embeds;
import dev.ticketshop.domain.ItemCarrinho;
import dev.ticketshop.service.CarrinhoService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.List;

/** {@code /carrinho} — revisa o que ja foi escolhido e oferece o botao de finalizar. */
public final class CarrinhoCommand implements SlashCommand {

    private final CarrinhoService carrinhos;

    public CarrinhoCommand(CarrinhoService carrinhos) {
        this.carrinhos = carrinhos;
    }

    @Override
    public String nome() {
        return "carrinho";
    }

    @Override
    public SlashCommandData dados() {
        return Commands.slash("carrinho", "Ve o seu carrinho e finaliza o pedido");
    }

    @Override
    public void executar(SlashCommandInteractionEvent evento) {
        String usuarioId = evento.getUser().getId();
        List<ItemCarrinho> itens = carrinhos.itens(usuarioId);
        long total = carrinhos.totalCentavos(usuarioId);

        if (itens.isEmpty()) {
            evento.replyEmbeds(Embeds.carrinho(itens, total)).setEphemeral(true).queue();
            return;
        }

        evento.replyEmbeds(Embeds.carrinho(itens, total))
                .setComponents(Botoes.carrinho())
                .setEphemeral(true)
                .queue();
    }
}

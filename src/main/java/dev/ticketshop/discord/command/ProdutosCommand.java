package dev.ticketshop.discord.command;

import dev.ticketshop.discord.view.Embeds;
import dev.ticketshop.discord.view.SeletorProdutos;
import dev.ticketshop.domain.Produto;
import dev.ticketshop.service.LojaService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.List;

/**
 * {@code /produtos} — mostra o catalogo e o menu de escolha.
 *
 * <p>A resposta e <b>efemera</b> ({@code setEphemeral(true)}): so quem digitou o comando
 * ve a mensagem, entao o canal nao fica poluido.
 */
public final class ProdutosCommand implements SlashCommand {

    private final LojaService loja;

    public ProdutosCommand(LojaService loja) {
        this.loja = loja;
    }

    @Override
    public String nome() {
        return "produtos";
    }

    @Override
    public SlashCommandData dados() {
        return Commands.slash("produtos", "Ve o catalogo e escolhe o que colocar no carrinho");
    }

    @Override
    public void executar(SlashCommandInteractionEvent evento) {
        List<Produto> produtos = loja.catalogo();
        if (produtos.isEmpty()) {
            evento.replyEmbeds(Embeds.aviso("A loja ainda nao tem produtos cadastrados."))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        evento.replyEmbeds(Embeds.catalogo(produtos))
                .setComponents(SeletorProdutos.de(produtos))
                .setEphemeral(true)
                .queue();
    }
}

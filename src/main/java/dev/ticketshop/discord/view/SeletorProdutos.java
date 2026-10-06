package dev.ticketshop.discord.view;

import dev.ticketshop.discord.CustomId;
import dev.ticketshop.domain.Produto;
import dev.ticketshop.util.Moeda;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;

import java.util.List;

/** Menu suspenso com o catalogo. */
public final class SeletorProdutos {

    /** O Discord recusa mais de 25 opcoes em um menu. */
    private static final int MAXIMO_DE_OPCOES = 25;

    private SeletorProdutos() {
    }

    public static ActionRow de(List<Produto> produtos) {
        StringSelectMenu.Builder menu = StringSelectMenu.create(CustomId.MENU_PRODUTOS)
                .setPlaceholder("Escolha um produto...")
                .setMinValues(1)
                .setMaxValues(1);

        produtos.stream().limit(MAXIMO_DE_OPCOES).forEach(produto ->
                menu.addOption(produto.nome(), String.valueOf(produto.id()),
                        Moeda.formatar(produto.precoCentavos())));

        return ActionRow.of(menu.build());
    }
}

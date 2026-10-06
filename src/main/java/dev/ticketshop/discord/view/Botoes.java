package dev.ticketshop.discord.view;

import dev.ticketshop.discord.CustomId;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;

/**
 * Botoes que o bot coloca nas mensagens.
 *
 * <p>Cada botao carrega o seu {@code customId}; quando alguem clica, o
 * {@code InteracaoListener} descobre o que fazer a partir desse id.
 */
public final class Botoes {

    private Botoes() {
    }

    /** Botoes do carrinho: finalizar compra ou jogar tudo fora. */
    public static ActionRow carrinho() {
        return ActionRow.of(
                Button.success(CustomId.CARRINHO_FINALIZAR, "Finalizar pedido"),
                Button.danger(CustomId.CARRINHO_LIMPAR, "Esvaziar"));
    }

    /** Botoes dentro do canal de atendimento. */
    public static ActionRow atendimento(long ticketId) {
        return ActionRow.of(
                Button.primary(CustomId.juntar(CustomId.TICKET_COMPRAR, ticketId), "Comprar"),
                Button.danger(CustomId.juntar(CustomId.TICKET_FECHAR, ticketId), "Fechar"));
    }

    /** Botoes da equipe em cima de um pedido. */
    public static ActionRow pedido(long ticketId) {
        return ActionRow.of(
                Button.success(CustomId.juntar(CustomId.PEDIDO_CONFIRMAR, ticketId), "Confirmar pedido"),
                Button.danger(CustomId.juntar(CustomId.PEDIDO_CANCELAR, ticketId), "Cancelar"));
    }
}

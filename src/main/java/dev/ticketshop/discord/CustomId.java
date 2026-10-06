package dev.ticketshop.discord;

import dev.ticketshop.domain.RegraDeNegocioException;

/**
 * Os "custom id" dos botoes, menus e modais do Discord.
 *
 * <p>O Discord nao guarda estado: quando alguem clica num botao, ele devolve apenas o id
 * que voce escolheu. Por isso codificamos o que precisamos saber dentro do proprio id,
 * no formato {@code prefixo:id} — por exemplo {@code pedido:confirmar:12}.
 *
 * <p>Manter isso em um lugar so evita o classico bug de digitar {@code "carrinho:fin"} aqui
 * e {@code "carrinho:finalizar"} la.
 */
public final class CustomId {

    private static final String SEPARADOR = ":";

    // Botoes do carrinho
    public static final String CARRINHO_FINALIZAR = "carrinho:finalizar";
    public static final String CARRINHO_LIMPAR = "carrinho:limpar";

    // Botoes do ticket
    public static final String TICKET_COMPRAR = "ticket:comprar";
    public static final String TICKET_FECHAR = "ticket:fechar";

    // Botoes da equipe
    public static final String PEDIDO_CONFIRMAR = "pedido:confirmar";
    public static final String PEDIDO_CANCELAR = "pedido:cancelar";

    // Menus de selecao
    public static final String MENU_PRODUTOS = "menu:produtos";

    // Modais
    public static final String MODAL_PEDIDO = "modal:pedido";
    public static final String CAMPO_OBSERVACOES = "observacoes";

    private CustomId() {
    }

    /** Monta o id completo: {@code juntar("pedido:confirmar", 12)} vira {@code "pedido:confirmar:12"}. */
    public static String juntar(String prefixo, long id) {
        return prefixo + SEPARADOR + id;
    }

    /**
     * Diz se o id recebido pertence a este prefixo.
     * Compara o id inteiro para nao confundir {@code "ticket:fechar"} com {@code "ticket:fechar-tudo"}.
     */
    public static boolean eh(String customId, String prefixo) {
        return prefixo.equals(customId) || customId.startsWith(prefixo + SEPARADOR);
    }

    /**
     * Le o id numerico no fim do custom id.
     *
     * @throws RegraDeNegocioException se o id vier corrompido (o que nunca deveria acontecer,
     *                                 mas o bot nunca deve quebrar por causa de um clique estranho)
     */
    public static long extrairId(String customId) {
        int ultimoSeparador = customId.lastIndexOf(SEPARADOR);
        if (ultimoSeparador < 0) {
            throw new RegraDeNegocioException("Id de interacao invalido: " + customId);
        }
        try {
            return Long.parseLong(customId.substring(ultimoSeparador + 1));
        } catch (NumberFormatException e) {
            throw new RegraDeNegocioException("Id de interacao invalido: " + customId, e);
        }
    }
}

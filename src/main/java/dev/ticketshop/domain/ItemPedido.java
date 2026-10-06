package dev.ticketshop.domain;

/**
 * Um item ja gravado no banco, ligado a um ticket.
 *
 * <p>Guarda o <b>nome e o preco do momento da compra</b> (snapshot). Se amanha o produto
 * mudar de preco, o pedido antigo continua mostrando o valor que o cliente pagou.
 */
public record ItemPedido(long id, Long produtoId, String nomeProduto, long precoCentavos, int quantidade) {

    /** Item novo, ainda sem id no banco. */
    public static ItemPedido de(long produtoId, String nomeProduto, long precoCentavos, int quantidade) {
        return new ItemPedido(0L, produtoId, nomeProduto, precoCentavos, quantidade);
    }

    public static ItemPedido doCarrinho(ItemCarrinho item) {
        return de(item.produtoId(), item.nome(), item.precoCentavos(), item.quantidade());
    }

    public long subtotalCentavos() {
        return precoCentavos * quantidade;
    }
}

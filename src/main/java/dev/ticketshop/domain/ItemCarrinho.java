package dev.ticketshop.domain;

/**
 * Uma linha do carrinho: quanto do produto X o cliente quer.
 *
 * <p>O carrinho vive apenas em memoria (veja {@code CarrinhoService}) porque e um estado
 * temporario. O que vai para o banco e o {@link ItemPedido}.
 */
public record ItemCarrinho(long produtoId, String nome, long precoCentavos, int quantidade) {

    public ItemCarrinho {
        if (quantidade < 1) {
            throw new RegraDeNegocioException("A quantidade precisa ser pelo menos 1.");
        }
    }

    /** Subtotal deste item, em centavos. */
    public long subtotalCentavos() {
        return precoCentavos * quantidade;
    }

    public ItemCarrinho somarQuantidade(int extra) {
        return new ItemCarrinho(produtoId, nome, precoCentavos, quantidade + extra);
    }
}

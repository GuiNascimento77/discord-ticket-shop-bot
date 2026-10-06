package dev.ticketshop.domain;

/**
 * Um produto da loja.
 *
 * <p>Usamos um {@code record} porque o produto e um dado imutavel: quando algo muda,
 * criamos uma copia com os metodos {@code comXxx(...)} (padrao "wither").
 *
 * <p><b>Preco em centavos.</b> Nunca use {@code double} para dinheiro: 0.1 + 0.2 != 0.3
 * em ponto flutuante. Com {@code long} em centavos, a conta e exata.
 */
public record Produto(long id, String nome, String descricao, long precoCentavos, boolean ativo) {

    /** Produto ainda nao salvo no banco (id 0 = "sem id"). */
    public static Produto novo(String nome, String descricao, long precoCentavos) {
        return new Produto(0L, nome, descricao, precoCentavos, true);
    }

    public Produto comId(long novoId) {
        return new Produto(novoId, nome, descricao, precoCentavos, ativo);
    }

    public Produto desativado() {
        return new Produto(id, nome, descricao, precoCentavos, false);
    }
}

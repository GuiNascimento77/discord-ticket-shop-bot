package dev.ticketshop.domain;

/**
 * Numeros consolidados da loja, usados pelo comando {@code /vendas}.
 *
 * @param pedidosConfirmados  quantos pedidos entraram no caixa
 * @param receitaCentavos     soma de tudo que foi vendido, em centavos
 * @param ticketMedioCentavos receita dividida pelo numero de pedidos (0 se nao houve venda)
 * @param produtoMaisVendido  nome do campeao de vendas, ou uma mensagem de "sem dados"
 */
public record ResumoVendas(long pedidosConfirmados,
                           long receitaCentavos,
                           long ticketMedioCentavos,
                           String produtoMaisVendido) {

    public static ResumoVendas vazio() {
        return new ResumoVendas(0L, 0L, 0L, "Nenhuma venda ainda");
    }
}

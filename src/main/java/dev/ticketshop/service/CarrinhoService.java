package dev.ticketshop.service;

import dev.ticketshop.domain.ItemCarrinho;
import dev.ticketshop.domain.Produto;
import dev.ticketshop.domain.RegraDeNegocioException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Carrinhos de compra, um por usuario.
 *
 * <p>Ficam apenas na memoria: e um estado temporario, e se o bot reiniciar o cliente
 * simplesmente monta o carrinho de novo. O que precisa sobreviver esta no banco (o pedido).
 *
 * <p>{@link ConcurrentHashMap} porque varios usuarios mexem no carrinho ao mesmo tempo,
 * em threads diferentes do JDA.
 */
public final class CarrinhoService {

    private static final int MAXIMO_POR_ITEM = 99;

    private final LojaService loja;
    private final Map<String, List<ItemCarrinho>> carrinhos = new ConcurrentHashMap<>();

    public CarrinhoService(LojaService loja) {
        this.loja = loja;
    }

    /** Adiciona o produto ao carrinho e devolve o carrinho atualizado. */
    public List<ItemCarrinho> adicionar(String usuarioId, long produtoId, int quantidade) {
        if (quantidade < 1) {
            throw new RegraDeNegocioException("A quantidade precisa ser pelo menos 1.");
        }
        Produto produto = loja.buscar(produtoId);
        if (!produto.ativo()) {
            throw new RegraDeNegocioException("O produto \"" + produto.nome() + "\" nao esta mais disponivel.");
        }

        carrinhos.compute(usuarioId, (id, atual) -> {
            List<ItemCarrinho> lista = atual == null ? new ArrayList<>() : new ArrayList<>(atual);
            for (int i = 0; i < lista.size(); i++) {
                ItemCarrinho item = lista.get(i);
                if (item.produtoId() == produtoId) {
                    int novaQuantidade = Math.min(MAXIMO_POR_ITEM, item.quantidade() + quantidade);
                    lista.set(i, new ItemCarrinho(item.produtoId(), item.nome(), item.precoCentavos(), novaQuantidade));
                    return lista;
                }
            }
            lista.add(new ItemCarrinho(produto.id(), produto.nome(), produto.precoCentavos(),
                    Math.min(MAXIMO_POR_ITEM, quantidade)));
            return lista;
        });

        return itens(usuarioId);
    }

    /** Remove um item pelo id do produto. */
    public List<ItemCarrinho> remover(String usuarioId, long produtoId) {
        carrinhos.computeIfPresent(usuarioId, (id, atual) -> {
            List<ItemCarrinho> lista = new ArrayList<>(atual);
            lista.removeIf(item -> item.produtoId() == produtoId);
            return lista;
        });
        return itens(usuarioId);
    }

    /** Copia imutavel do carrinho: ninguem de fora consegue alterar a lista guardada. */
    public List<ItemCarrinho> itens(String usuarioId) {
        return List.copyOf(carrinhos.getOrDefault(usuarioId, List.of()));
    }

    public boolean vazio(String usuarioId) {
        return itens(usuarioId).isEmpty();
    }

    /** Soma dos subtotais, em centavos. */
    public long totalCentavos(String usuarioId) {
        return itens(usuarioId).stream()
                .mapToLong(ItemCarrinho::subtotalCentavos)
                .sum();
    }

    /** Quantidade total de unidades, usada para saber quando agrupar itens iguais. */
    public int totalDeUnidades(String usuarioId) {
        return itens(usuarioId).stream()
                .mapToInt(ItemCarrinho::quantidade)
                .sum();
    }

    public void esvaziar(String usuarioId) {
        carrinhos.remove(usuarioId);
    }
}

package dev.ticketshop.service;

import dev.ticketshop.data.ProdutoRepository;
import dev.ticketshop.domain.Produto;
import dev.ticketshop.domain.RegraDeNegocioException;

import java.util.List;

/**
 * Regras de negocio do catalogo.
 *
 * <p>Esta classe <b>nao conhece o Discord</b>: ela so fala com o repositorio. Isso deixa
 * as regras testaveis sem precisar de token nem de internet, e e o motivo pelo qual
 * {@code LojaServiceTest} roda em milissegundos.
 */
public final class LojaService {

    private static final int TAMANHO_MAXIMO_NOME = 60;

    private final ProdutoRepository produtos;

    public LojaService(ProdutoRepository produtos) {
        this.produtos = produtos;
    }

    /** Produtos que aparecem na vitrine. */
    public List<Produto> catalogo() {
        return produtos.listarAtivos();
    }

    /** Todos os produtos, inclusive os desativados (usado no painel de administracao). */
    public List<Produto> todos() {
        return produtos.listarTodos();
    }

    public Produto cadastrar(String nome, String descricao, long precoCentavos) {
        String nomeLimpo = validarNome(nome);
        if (produtos.buscarPorNome(nomeLimpo).isPresent()) {
            throw new RegraDeNegocioException("Ja existe um produto chamado \"" + nomeLimpo + "\".");
        }
        String descricaoLimpa = descricao == null ? "" : descricao.trim();
        return produtos.inserir(Produto.novo(nomeLimpo, descricaoLimpa, precoCentavos));
    }

    /** Muda o preco de um produto existente. */
    public Produto alterarPreco(long produtoId, long novoPrecoCentavos) {
        Produto produto = buscar(produtoId);
        Produto atualizado = new Produto(produto.id(), produto.nome(), produto.descricao(),
                novoPrecoCentavos, produto.ativo());
        produtos.atualizar(atualizado);
        return atualizado;
    }

    /** Desativa pelo nome (o produto sai da vitrine, mas os pedidos antigos continuam intactos). */
    public Produto desativar(String nome) {
        Produto produto = produtos.buscarPorNome(validarNome(nome))
                .orElseThrow(() -> new RegraDeNegocioException("Nao encontrei o produto \"" + nome + "\"."));
        produtos.desativar(produto.id());
        return produto.desativado();
    }

    public Produto buscar(long produtoId) {
        return produtos.buscarPorId(produtoId)
                .orElseThrow(() -> new RegraDeNegocioException("Produto #" + produtoId + " nao existe."));
    }

    private static String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException("O nome do produto nao pode ficar vazio.");
        }
        String limpo = nome.trim();
        if (limpo.length() > TAMANHO_MAXIMO_NOME) {
            throw new RegraDeNegocioException("O nome do produto pode ter no maximo " + TAMANHO_MAXIMO_NOME + " caracteres.");
        }
        return limpo;
    }
}

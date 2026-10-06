package dev.ticketshop.data;

import dev.ticketshop.domain.Produto;

import java.util.List;
import java.util.Optional;

/**
 * O que o resto do sistema precisa saber sobre guardar produtos.
 *
 * <p>O servico depende desta interface, nao da classe concreta. Vantagens:
 * da para trocar SQLite por Postgres depois sem mexer nas regras de negocio,
 * e da para criar um repositorio falso nos testes.
 */
public interface ProdutoRepository {

    Produto inserir(Produto produto);

    Optional<Produto> buscarPorId(long id);

    Optional<Produto> buscarPorNome(String nome);

    /** Produtos que aparecem no catalogo ({@code ativo = 1}). */
    List<Produto> listarAtivos();

    List<Produto> listarTodos();

    boolean atualizar(Produto produto);

    /** Nao apagamos o produto: apenas o escondemos, para nao quebrar pedidos antigos. */
    boolean desativar(long id);
}

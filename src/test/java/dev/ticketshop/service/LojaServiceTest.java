package dev.ticketshop.service;

import dev.ticketshop.data.Database;
import dev.ticketshop.data.SqliteProdutoRepository;
import dev.ticketshop.domain.Produto;
import dev.ticketshop.domain.RegraDeNegocioException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes das regras do catalogo.
 *
 * <p>Roda com SQLite em memoria: cada teste comeca com um banco limpo e nenhum arquivo
 * fica no seu computador.
 */
class LojaServiceTest {

    private Database banco;
    private LojaService loja;

    @BeforeEach
    void preparar() {
        banco = Database.emMemoria();
        loja = new LojaService(new SqliteProdutoRepository(banco));
    }

    @AfterEach
    void encerrar() {
        banco.close();
    }

    @Test
    @DisplayName("cadastra produto e ele aparece no catalogo")
    void cadastraProduto() {
        Produto produto = loja.cadastrar("Cafe 250g", "Torra media", 3990L);

        assertTrue(produto.id() > 0, "o banco deve devolver um id");
        List<Produto> catalogo = loja.catalogo();
        assertEquals(1, catalogo.size());
        assertEquals("Cafe 250g", catalogo.get(0).nome());
        assertEquals(3990L, catalogo.get(0).precoCentavos());
    }

    @Test
    @DisplayName("nao deixa cadastrar dois produtos com o mesmo nome")
    void naoAceitaNomeRepetido() {
        loja.cadastrar("Caneca", "", 2450L);

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> loja.cadastrar("caneca", "", 3000L));

        assertTrue(erro.getMessage().contains("Ja existe"));
    }

    @Test
    @DisplayName("desativar tira da vitrine mas mantem o produto no banco")
    void desativarProduto() {
        loja.cadastrar("Kit degustacao", "", 5990L);

        loja.desativar("Kit degustacao");

        assertTrue(loja.catalogo().isEmpty(), "a vitrine deve ficar vazia");
        assertEquals(1, loja.todos().size(), "o produto continua registrado");
        assertFalse(loja.todos().get(0).ativo());
    }

    @Test
    @DisplayName("altera o preco de um produto existente")
    void alteraPreco() {
        Produto produto = loja.cadastrar("Cafe 250g", "", 3990L);

        Produto atualizado = loja.alterarPreco(produto.id(), 4290L);

        assertEquals(4290L, atualizado.precoCentavos());
        assertEquals(4290L, loja.buscar(produto.id()).precoCentavos());
    }

    @Test
    @DisplayName("recusa nome vazio")
    void recusaNomeVazio() {
        assertThrows(RegraDeNegocioException.class, () -> loja.cadastrar("   ", "", 1000L));
    }

    @Test
    @DisplayName("buscar produto inexistente explica o erro")
    void produtoInexistente() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class, () -> loja.buscar(999L));
        assertTrue(erro.getMessage().contains("999"));
    }
}

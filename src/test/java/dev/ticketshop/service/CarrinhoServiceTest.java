package dev.ticketshop.service;

import dev.ticketshop.data.Database;
import dev.ticketshop.data.SqliteProdutoRepository;
import dev.ticketshop.domain.ItemCarrinho;
import dev.ticketshop.domain.Produto;
import dev.ticketshop.domain.RegraDeNegocioException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarrinhoServiceTest {

    private static final String CLIENTE = "123456789";

    private Database banco;
    private LojaService loja;
    private CarrinhoService carrinhos;
    private Produto cafe;
    private Produto caneca;

    @BeforeEach
    void preparar() {
        banco = Database.emMemoria();
        loja = new LojaService(new SqliteProdutoRepository(banco));
        carrinhos = new CarrinhoService(loja);

        cafe = loja.cadastrar("Cafe 250g", "", 3990L);
        caneca = loja.cadastrar("Caneca", "", 2450L);
    }

    @AfterEach
    void encerrar() {
        banco.close();
    }

    @Test
    @DisplayName("adicionar o mesmo produto duas vezes soma a quantidade")
    void somaQuantidadeDoMesmoProduto() {
        carrinhos.adicionar(CLIENTE, cafe.id(), 1);
        carrinhos.adicionar(CLIENTE, cafe.id(), 2);

        List<ItemCarrinho> itens = carrinhos.itens(CLIENTE);
        assertEquals(1, itens.size(), "nao deve criar duas linhas para o mesmo produto");
        assertEquals(3, itens.get(0).quantidade());
    }

    @Test
    @DisplayName("total soma os subtotais de todos os itens")
    void totalDoCarrinho() {
        carrinhos.adicionar(CLIENTE, cafe.id(), 2);   // 2 x 39,90 = 79,80
        carrinhos.adicionar(CLIENTE, caneca.id(), 1); // 1 x 24,50

        assertEquals(10430L, carrinhos.totalCentavos(CLIENTE));
        assertEquals(3, carrinhos.totalDeUnidades(CLIENTE));
    }

    @Test
    @DisplayName("carrinhos de clientes diferentes nao se misturam")
    void carrinhosSaoSeparadosPorUsuario() {
        carrinhos.adicionar("cliente-a", cafe.id(), 1);
        carrinhos.adicionar("cliente-b", caneca.id(), 5);

        assertEquals(1, carrinhos.itens("cliente-a").size());
        assertEquals(1, carrinhos.itens("cliente-b").size());
        assertEquals(3990L, carrinhos.totalCentavos("cliente-a"));
        assertEquals(12250L, carrinhos.totalCentavos("cliente-b"));
    }

    @Test
    @DisplayName("produto fora da vitrine nao pode ser comprado")
    void produtoDesativadoNaoEntra() {
        loja.desativar("Caneca");

        assertThrows(RegraDeNegocioException.class, () -> carrinhos.adicionar(CLIENTE, caneca.id(), 1));
    }

    @Test
    @DisplayName("remover item e esvaziar o carrinho")
    void removeEEsvazia() {
        carrinhos.adicionar(CLIENTE, cafe.id(), 1);
        carrinhos.adicionar(CLIENTE, caneca.id(), 1);

        carrinhos.remover(CLIENTE, cafe.id());
        assertEquals(1, carrinhos.itens(CLIENTE).size());

        carrinhos.esvaziar(CLIENTE);
        assertTrue(carrinhos.vazio(CLIENTE));
        assertEquals(0L, carrinhos.totalCentavos(CLIENTE));
    }

    @Test
    @DisplayName("recusa quantidade menor que 1")
    void recusaQuantidadeInvalida() {
        assertThrows(RegraDeNegocioException.class, () -> carrinhos.adicionar(CLIENTE, cafe.id(), 0));
    }

    @Test
    @DisplayName("a lista devolvida nao permite alterar o carrinho por fora")
    void listaEhImutavel() {
        carrinhos.adicionar(CLIENTE, cafe.id(), 1);

        List<ItemCarrinho> itens = carrinhos.itens(CLIENTE);
        assertThrows(UnsupportedOperationException.class,
                () -> itens.add(new ItemCarrinho(caneca.id(), "Caneca", 2450L, 1)));
    }
}

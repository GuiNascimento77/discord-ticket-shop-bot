package dev.ticketshop.service;

import dev.ticketshop.data.Database;
import dev.ticketshop.data.SqliteProdutoRepository;
import dev.ticketshop.data.SqliteTicketRepository;
import dev.ticketshop.domain.ItemCarrinho;
import dev.ticketshop.domain.ItemPedido;
import dev.ticketshop.domain.RegraDeNegocioException;
import dev.ticketshop.domain.ResumoVendas;
import dev.ticketshop.domain.StatusTicket;
import dev.ticketshop.domain.Ticket;
import dev.ticketshop.domain.TipoTicket;
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
 * Testes do ciclo de vida do ticket.
 *
 * <p>Note que <b>nada aqui fala com o Discord</b>: o servico so conhece o repositorio.
 * Se voce precisasse de um token de bot para testar regra de negocio, o desenho estaria errado.
 */
class TicketServiceTest {

    private static final String CLIENTE = "42";

    private Database banco;
    private TicketService atendimentos;
    private CarrinhoService carrinhos;
    private long cafeId;
    private long canecaId;

    @BeforeEach
    void preparar() {
        banco = Database.emMemoria();
        LojaService loja = new LojaService(new SqliteProdutoRepository(banco));
        atendimentos = new TicketService(new SqliteTicketRepository(banco));
        carrinhos = new CarrinhoService(loja);

        cafeId = loja.cadastrar("Cafe 250g", "", 3990L).id();
        canecaId = loja.cadastrar("Caneca", "", 2450L).id();
    }

    @AfterEach
    void encerrar() {
        banco.close();
    }

    private List<ItemCarrinho> carrinhoDe(String usuario) {
        carrinhos.adicionar(usuario, cafeId, 2);
        carrinhos.adicionar(usuario, canecaId, 1);
        return carrinhos.itens(usuario);
    }

    @Test
    @DisplayName("abre um atendimento de duvida, sem pedido")
    void abreDuvida() {
        Ticket ticket = atendimentos.abrirDuvida(CLIENTE, "Voces entregam em Campinas?");

        assertTrue(ticket.id() > 0);
        assertEquals(TipoTicket.DUVIDA, ticket.tipo());
        assertEquals(StatusTicket.ABERTO, ticket.status());
        assertEquals(0L, ticket.totalCentavos());
        assertFalse(ticket.temPedido());
    }

    @Test
    @DisplayName("converte a duvida em pedido mantendo o mesmo ticket")
    void converteDuvidaEmPedido() {
        Ticket duvida = atendimentos.abrirDuvida(CLIENTE, "Quanto custa o cafe?");

        Ticket pedido = atendimentos.converterEmPedido(duvida.id(), carrinhoDe(CLIENTE));

        assertEquals(duvida.id(), pedido.id(), "o atendimento deve continuar sendo o mesmo");
        assertEquals(TipoTicket.PEDIDO, pedido.tipo());
        assertEquals(10430L, pedido.totalCentavos());
        assertEquals(2, atendimentos.itens(pedido.id()).size());
    }

    @Test
    @DisplayName("nao aceita carrinho vazio")
    void recusaCarrinhoVazio() {
        Ticket duvida = atendimentos.abrirDuvida(CLIENTE, "oi");
        assertThrows(RegraDeNegocioException.class, () -> atendimentos.converterEmPedido(duvida.id(), List.of()));
    }

    @Test
    @DisplayName("pedido confirmado entra no resumo de vendas")
    void confirmarPedidoContaComoVenda() {
        Ticket pedido = atendimentos.abrirPedido(CLIENTE, "Pedido pelo catalogo", carrinhoDe(CLIENTE));

        Ticket confirmado = atendimentos.confirmar(pedido.id());

        assertEquals(StatusTicket.CONFIRMADO, confirmado.status());
        ResumoVendas resumo = atendimentos.resumo();
        assertEquals(1L, resumo.pedidosConfirmados());
        assertEquals(10430L, resumo.receitaCentavos());
        assertEquals(10430L, resumo.ticketMedioCentavos());
        assertTrue(resumo.produtoMaisVendido().contains("Cafe"), "o cafe vendeu 2 unidades");
    }

    @Test
    @DisplayName("nao da para confirmar um atendimento sem pedido")
    void naoConfirmaSemPedido() {
        Ticket duvida = atendimentos.abrirDuvida(CLIENTE, "so uma duvida");

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> atendimentos.confirmar(duvida.id()));

        assertTrue(erro.getMessage().contains("nao tem pedido"));
    }

    @Test
    @DisplayName("cancelar tira o pedido do resumo de vendas")
    void cancelarNaoContaComoVenda() {
        Ticket pedido = atendimentos.abrirPedido(CLIENTE, "Pedido", carrinhoDe(CLIENTE));

        atendimentos.cancelar(pedido.id());

        assertEquals(0L, atendimentos.resumo().pedidosConfirmados());
        assertEquals(0L, atendimentos.resumo().receitaCentavos());
    }

    @Test
    @DisplayName("fechar encerra o atendimento e guarda a data")
    void fecharAtendimento() {
        Ticket ticket = atendimentos.abrirDuvida(CLIENTE, "duvida");

        Ticket fechado = atendimentos.fechar(ticket.id());

        assertEquals(StatusTicket.FECHADO, fechado.status());
        assertTrue(fechado.fechadoEm() != null, "a data de fechamento deve ser preenchida");
        assertThrows(RegraDeNegocioException.class, () -> atendimentos.fechar(ticket.id()));
    }

    @Test
    @DisplayName("encontra o ticket pelo canal do Discord")
    void buscaPorCanal() {
        Ticket ticket = atendimentos.abrirDuvida(CLIENTE, "duvida");
        atendimentos.vincularCanal(ticket.id(), "999888777");

        Ticket encontrado = atendimentos.porCanal("999888777").orElseThrow();

        assertEquals(ticket.id(), encontrado.id());
        assertTrue(atendimentos.porCanal("nao-existe").isEmpty());
    }

    @Test
    @DisplayName("lista os atendimentos do cliente")
    void listaTicketsDoCliente() {
        atendimentos.abrirDuvida(CLIENTE, "primeira");
        atendimentos.abrirDuvida(CLIENTE, "segunda");
        atendimentos.abrirDuvida("outro-cliente", "de outra pessoa");

        List<Ticket> meus = atendimentos.meusTickets(CLIENTE);

        assertEquals(2, meus.size());
        assertEquals("segunda", meus.get(0).assunto(), "os mais recentes vem primeiro");
    }

    @Test
    @DisplayName("guarda o preco do momento da compra, mesmo se o produto mudar depois")
    void precoFicaCongeladoNoPedido() {
        Ticket pedido = atendimentos.abrirPedido(CLIENTE, "Pedido", carrinhoDe(CLIENTE));

        LojaService loja = new LojaService(new SqliteProdutoRepository(banco));
        loja.alterarPreco(cafeId, 9990L);

        List<ItemPedido> itens = atendimentos.itens(pedido.id());
        long precoDoCafe = itens.stream()
                .filter(item -> item.nomeProduto().equals("Cafe 250g"))
                .findFirst()
                .orElseThrow()
                .precoCentavos();

        assertEquals(3990L, precoDoCafe, "o pedido antigo deve manter o preco antigo");
    }
}

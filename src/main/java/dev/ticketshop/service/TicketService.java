package dev.ticketshop.service;

import dev.ticketshop.data.TicketRepository;
import dev.ticketshop.domain.ItemCarrinho;
import dev.ticketshop.domain.ItemPedido;
import dev.ticketshop.domain.RegraDeNegocioException;
import dev.ticketshop.domain.ResumoVendas;
import dev.ticketshop.domain.StatusTicket;
import dev.ticketshop.domain.Ticket;
import dev.ticketshop.domain.TipoTicket;

import java.util.List;
import java.util.Optional;

/**
 * Ciclo de vida dos tickets: abrir, virar pedido, confirmar, cancelar e fechar.
 *
 * <p>Assim como {@link LojaService}, nao conhece o JDA. Quem cria o canal no Discord e a
 * camada {@code dev.ticketshop.discord} — aqui cuidamos apenas do que e regra de negocio.
 */
public final class TicketService {

    private static final int TAMANHO_MAXIMO_ASSUNTO = 120;

    private final TicketRepository tickets;

    public TicketService(TicketRepository tickets) {
        this.tickets = tickets;
    }

    /** Abre um ticket de duvida, ainda sem pedido. */
    public Ticket abrirDuvida(String donoId, String assunto) {
        return tickets.inserir(Ticket.novo(donoId, TipoTicket.DUVIDA, validarAssunto(assunto)), List.of());
    }

    /** Abre um ticket que ja nasce como pedido (compra feita fora de um ticket aberto). */
    public Ticket abrirPedido(String donoId, String assunto, List<ItemCarrinho> carrinho) {
        exigirCarrinhoCheio(carrinho);
        List<ItemPedido> itens = converterItens(carrinho);
        Ticket ticket = Ticket.novo(donoId, TipoTicket.PEDIDO, validarAssunto(assunto))
                .comTotal(somar(itens));
        return tickets.inserir(ticket, itens);
    }

    /**
     * O fluxo hibrido: um ticket aberto por duvida recebe uma compra e passa a ser pedido.
     * O historico da conversa continua no mesmo canal.
     */
    public Ticket converterEmPedido(long ticketId, List<ItemCarrinho> carrinho) {
        exigirCarrinhoCheio(carrinho);
        Ticket ticket = buscar(ticketId);
        if (!ticket.estaAberto()) {
            throw new RegraDeNegocioException("O ticket #" + ticketId + " nao esta mais aberto.");
        }

        List<ItemPedido> itens = converterItens(carrinho);
        long total = somar(itens);
        tickets.registrarPedido(ticketId, TipoTicket.PEDIDO, total, itens);
        return buscar(ticketId);
    }

    /** A equipe aprova o pedido: a partir daqui ele entra no relatorio de vendas. */
    public Ticket confirmar(long ticketId) {
        Ticket ticket = buscar(ticketId);
        if (!ticket.estaAberto()) {
            throw new RegraDeNegocioException("O ticket #" + ticketId + " nao esta aberto.");
        }
        if (!ticket.temPedido()) {
            throw new RegraDeNegocioException("O ticket #" + ticketId + " nao tem pedido para confirmar.");
        }
        Ticket confirmado = ticket.comStatus(StatusTicket.CONFIRMADO);
        tickets.atualizar(confirmado);
        return confirmado;
    }

    public Ticket cancelar(long ticketId) {
        Ticket ticket = buscar(ticketId);
        if (!ticket.estaAberto()) {
            throw new RegraDeNegocioException("O ticket #" + ticketId + " nao esta aberto.");
        }
        Ticket cancelado = ticket.comStatus(StatusTicket.CANCELADO);
        tickets.atualizar(cancelado);
        return cancelado;
    }

    /** Encerra o ticket. Um pedido confirmado continua contando como venda. */
    public Ticket fechar(long ticketId) {
        Ticket ticket = buscar(ticketId);
        if (ticket.status() == StatusTicket.FECHADO) {
            throw new RegraDeNegocioException("O ticket #" + ticketId + " ja estava fechado.");
        }
        Ticket fechado = ticket.encerrado();
        tickets.atualizar(fechado);
        return fechado;
    }

    /** Guarda o id do canal do Discord que representa este ticket. */
    public Ticket vincularCanal(long ticketId, String canalId) {
        Ticket ticket = buscar(ticketId).comCanal(canalId);
        tickets.atualizar(ticket);
        return ticket;
    }

    public Ticket buscar(long ticketId) {
        return tickets.buscarPorId(ticketId)
                .orElseThrow(() -> new RegraDeNegocioException("Ticket #" + ticketId + " nao encontrado."));
    }

    /** Descobre de quem e o ticket a partir do canal onde o comando foi usado. */
    public Optional<Ticket> porCanal(String canalId) {
        return tickets.buscarPorCanal(canalId);
    }

    public List<Ticket> meusTickets(String donoId) {
        return tickets.listarPorDono(donoId);
    }

    public List<Ticket> ticketsAbertos() {
        return tickets.listarPorStatus(StatusTicket.ABERTO);
    }

    public List<ItemPedido> itens(long ticketId) {
        return tickets.itensDoTicket(ticketId);
    }

    public ResumoVendas resumo() {
        return tickets.resumoVendas();
    }

    private static void exigirCarrinhoCheio(List<ItemCarrinho> carrinho) {
        if (carrinho == null || carrinho.isEmpty()) {
            throw new RegraDeNegocioException("O carrinho esta vazio. Use /produtos para escolher algo.");
        }
    }

    private static List<ItemPedido> converterItens(List<ItemCarrinho> carrinho) {
        return carrinho.stream().map(ItemPedido::doCarrinho).toList();
    }

    private static long somar(List<ItemPedido> itens) {
        return itens.stream().mapToLong(ItemPedido::subtotalCentavos).sum();
    }

    private static String validarAssunto(String assunto) {
        if (assunto == null || assunto.isBlank()) {
            return "Sem assunto informado";
        }
        String limpo = assunto.trim().replace('\n', ' ');
        return limpo.length() <= TAMANHO_MAXIMO_ASSUNTO ? limpo : limpo.substring(0, TAMANHO_MAXIMO_ASSUNTO);
    }
}

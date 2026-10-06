package dev.ticketshop.data;

import dev.ticketshop.domain.ItemPedido;
import dev.ticketshop.domain.ResumoVendas;
import dev.ticketshop.domain.StatusTicket;
import dev.ticketshop.domain.Ticket;
import dev.ticketshop.domain.TipoTicket;

import java.util.List;
import java.util.Optional;

/** Acesso a tickets e aos itens dos pedidos. */
public interface TicketRepository {

    /** Grava o ticket e, se houver, os itens do pedido — tudo na mesma transacao. */
    Ticket inserir(Ticket ticket, List<ItemPedido> itens);

    boolean atualizar(Ticket ticket);

    /**
     * Converte um ticket de duvida em pedido: troca o tipo, grava o total e substitui
     * os itens. Tudo em uma transacao so.
     */
    boolean registrarPedido(long ticketId, TipoTicket tipo, long totalCentavos, List<ItemPedido> itens);

    Optional<Ticket> buscarPorId(long id);

    Optional<Ticket> buscarPorCanal(String canalId);

    List<Ticket> listarPorDono(String donoId);

    List<Ticket> listarPorStatus(StatusTicket status);

    List<ItemPedido> itensDoTicket(long ticketId);

    ResumoVendas resumoVendas();
}

package dev.ticketshop.domain;

import java.time.Instant;

/**
 * Um ticket: uma conversa privada entre o cliente e a equipe.
 *
 * <p>Um ticket de {@link TipoTicket#DUVIDA} pode virar {@link TipoTicket#PEDIDO}: e assim
 * que o fluxo "duvida que termina em venda" acontece.
 *
 * @param id            chave primaria no banco (0 quando ainda nao foi salvo)
 * @param canalId       id do canal de texto do Discord criado para este ticket
 * @param donoId        id do usuario que abriu o ticket
 * @param tipo          duvida ou pedido
 * @param status        situacao atual
 * @param assunto       texto curto descrevendo o ticket
 * @param totalCentavos soma dos itens do pedido (0 para duvidas sem compra)
 * @param abertoEm      quando o ticket foi aberto
 * @param fechadoEm     quando foi encerrado (null enquanto estiver aberto)
 */
public record Ticket(long id,
                     String canalId,
                     String donoId,
                     TipoTicket tipo,
                     StatusTicket status,
                     String assunto,
                     long totalCentavos,
                     Instant abertoEm,
                     Instant fechadoEm) {

    /** Ticket recem-criado, antes de existir no banco e antes do canal do Discord. */
    public static Ticket novo(String donoId, TipoTicket tipo, String assunto) {
        return new Ticket(0L, null, donoId, tipo, StatusTicket.ABERTO, assunto, 0L, Instant.now(), null);
    }

    public Ticket comId(long novoId) {
        return new Ticket(novoId, canalId, donoId, tipo, status, assunto, totalCentavos, abertoEm, fechadoEm);
    }

    public Ticket comCanal(String novoCanalId) {
        return new Ticket(id, novoCanalId, donoId, tipo, status, assunto, totalCentavos, abertoEm, fechadoEm);
    }

    public Ticket comTipo(TipoTicket novoTipo) {
        return new Ticket(id, canalId, donoId, novoTipo, status, assunto, totalCentavos, abertoEm, fechadoEm);
    }

    public Ticket comStatus(StatusTicket novoStatus) {
        return new Ticket(id, canalId, donoId, tipo, novoStatus, assunto, totalCentavos, abertoEm, fechadoEm);
    }

    public Ticket comTotal(long novoTotalCentavos) {
        return new Ticket(id, canalId, donoId, tipo, status, assunto, novoTotalCentavos, abertoEm, fechadoEm);
    }

    public Ticket comAssunto(String novoAssunto) {
        return new Ticket(id, canalId, donoId, tipo, status, novoAssunto, totalCentavos, abertoEm, fechadoEm);
    }

    /** Encerra o ticket agora. */
    public Ticket encerrado() {
        return new Ticket(id, canalId, donoId, tipo, StatusTicket.FECHADO, assunto, totalCentavos,
                abertoEm, Instant.now());
    }

    public boolean estaAberto() {
        return status.estaAtivo();
    }

    public boolean temPedido() {
        return totalCentavos > 0;
    }
}

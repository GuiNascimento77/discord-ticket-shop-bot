package dev.ticketshop.domain;

/**
 * Ciclo de vida de um ticket.
 *
 * <pre>
 *   ABERTO ──(equipe confirma o pedido)──▶ CONFIRMADO ──(canal encerrado)──▶ FECHADO
 *      │                                          │
 *      └──────────(cliente/equipe cancela)────────┴──────▶ CANCELADO
 * </pre>
 */
public enum StatusTicket {

    ABERTO("Aberto", false),
    CONFIRMADO("Confirmado", true),
    FECHADO("Fechado", true),
    CANCELADO("Cancelado", false);

    private final String rotulo;
    private final boolean contaComoVenda;

    StatusTicket(String rotulo, boolean contaComoVenda) {
        this.rotulo = rotulo;
        this.contaComoVenda = contaComoVenda;
    }

    public String rotulo() {
        return rotulo;
    }

    /**
     * Se este status entra no relatorio de vendas.
     *
     * <p>Repare no {@code switch} sem {@code default}: se um dia alguem adicionar um status
     * novo, o compilador avisa que este metodo ficou desatualizado.
     */
    public boolean contaComoVenda() {
        return switch (this) {
            case CONFIRMADO, FECHADO -> contaComoVenda;
            case ABERTO, CANCELADO -> false;
        };
    }

    public boolean estaAtivo() {
        return this == ABERTO;
    }

    public static StatusTicket deCodigo(String codigo) {
        for (StatusTicket status : values()) {
            if (status.name().equalsIgnoreCase(codigo)) {
                return status;
            }
        }
        throw new RegraDeNegocioException("Status de ticket desconhecido: " + codigo);
    }
}

package dev.ticketshop.domain;

/**
 * Que tipo de atendimento o ticket representa.
 *
 * <p>Enum com campos: cada constante carrega seus proprios dados. Assim evitamos
 * espalhar {@code if (tipo == DUVIDA)} pelo codigo.
 */
public enum TipoTicket {

    DUVIDA("duvida", "Duvida", "❓"),
    PEDIDO("pedido", "Pedido", "🛒");

    private final String codigo;
    private final String rotulo;
    private final String emoji;

    TipoTicket(String codigo, String rotulo, String emoji) {
        this.codigo = codigo;
        this.rotulo = rotulo;
        this.emoji = emoji;
    }

    /** Valor gravado no banco (sem acento, minusculo). */
    public String codigo() {
        return codigo;
    }

    public String rotulo() {
        return rotulo;
    }

    public String emoji() {
        return emoji;
    }

    /** Prefixo do nome do canal do Discord: {@code duvida-joao}, {@code pedido-maria}. */
    public String prefixoDoCanal() {
        return codigo;
    }

    /** Converte o texto do banco de volta para o enum. */
    public static TipoTicket deCodigo(String codigo) {
        for (TipoTicket tipo : values()) {
            if (tipo.codigo.equalsIgnoreCase(codigo)) {
                return tipo;
            }
        }
        throw new RegraDeNegocioException("Tipo de ticket desconhecido: " + codigo);
    }
}

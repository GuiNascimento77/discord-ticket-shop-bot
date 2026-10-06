package dev.ticketshop.data;

/** Erro de banco de dados. Envolve o {@code SQLException} original para nao vazar detalhes. */
public class ErroBancoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ErroBancoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}

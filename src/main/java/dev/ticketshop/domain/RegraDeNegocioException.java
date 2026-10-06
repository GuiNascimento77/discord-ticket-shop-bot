package dev.ticketshop.domain;

/**
 * Erro esperado das regras de negocio (ex.: "produto nao existe", "carrinho vazio").
 *
 * <p>E uma {@code RuntimeException} porque quem chama nao e obrigado a tratar:
 * a camada do Discord captura e mostra a mensagem para o usuario.
 * Erros de programacao de verdade devem continuar sendo excecoes normais.
 */
public class RegraDeNegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }

    public RegraDeNegocioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}

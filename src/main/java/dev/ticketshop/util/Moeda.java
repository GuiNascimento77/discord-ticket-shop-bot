package dev.ticketshop.util;

import dev.ticketshop.domain.RegraDeNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Conversao e formatacao de dinheiro.
 *
 * <p>Regra do projeto: dinheiro circula como {@code long} em <b>centavos</b>.
 * A conversao para texto ("R$ 49,90") e para numero acontece apenas nas bordas:
 * quando o usuario digita e quando o bot mostra na tela.
 */
public final class Moeda {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    /**
     * Espaco inquebravel (U+00A0) que o formatador do pt-BR coloca entre "R$" e o numero.
     *
     * <p>Escrito como escape de proposito: se fosse digitado como caractere literal, o arquivo
     * dependeria da codificacao usada para le-lo e o bug voltaria sem aviso.
     */
    private static final char ESPACO_INQUEBRAVEL = '\u00A0';

    /** Teto de seguranca: evita cadastrar "999999999" por engano. */
    private static final BigDecimal PRECO_MAXIMO = new BigDecimal("1000000");

    private Moeda() {
        // classe utilitaria: nao deve ser instanciada
    }

    /** {@code 4990} vira {@code "R$ 49,90"}. */
    public static String formatar(long centavos) {
        // NumberFormat nao e thread-safe, entao criamos um novo a cada chamada.
        NumberFormat formato = NumberFormat.getCurrencyInstance(PT_BR);
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        // Trocamos o espaco inquebravel por espaco comum para o texto ficar igual
        // no Discord, no terminal e nos logs.
        return formato.format(BigDecimal.valueOf(centavos, 2)).replace(ESPACO_INQUEBRAVEL, ' ');
    }

    /**
     * Le o que o usuario digitou e devolve centavos.
     *
     * <p>Aceita {@code "49,90"}, {@code "49.90"}, {@code "R$ 49,90"} e {@code "1234,5"}.
     * Usa {@link BigDecimal} em vez de {@code double} para nao perder centavos no caminho.
     *
     * @throws RegraDeNegocioException se o texto nao for um valor valido
     */
    public static long paraCentavos(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new RegraDeNegocioException("Informe um preco.");
        }

        String limpo = texto.replace("R$", "")
                .replace("r$", "")
                .replace(ESPACO_INQUEBRAVEL, ' ')
                .replace(" ", "")
                .trim();

        // Se tem virgula, ela e o separador decimal (padrao brasileiro) e o ponto e milhar.
        if (limpo.contains(",")) {
            limpo = limpo.replace(".", "").replace(",", ".");
        }

        BigDecimal valor;
        try {
            valor = new BigDecimal(limpo);
        } catch (NumberFormatException e) {
            throw new RegraDeNegocioException("Preco invalido: \"" + texto + "\". Use algo como 49,90.");
        }

        if (valor.signum() < 0) {
            throw new RegraDeNegocioException("O preco nao pode ser negativo.");
        }
        if (valor.compareTo(PRECO_MAXIMO) > 0) {
            throw new RegraDeNegocioException("Esse preco parece alto demais. Confira o valor.");
        }

        return valor.setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();
    }
}

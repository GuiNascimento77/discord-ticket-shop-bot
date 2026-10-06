package dev.ticketshop.util;

import dev.ticketshop.domain.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoedaTest {

    @Test
    @DisplayName("formata centavos como real brasileiro")
    void formataCentavos() {
        assertTrue(Moeda.formatar(4990).contains("49,90"));
        assertTrue(Moeda.formatar(0).contains("0,00"));
    }

    @Test
    @DisplayName("aceita virgula, ponto, cifrao e separador de milhar")
    void leDiferentesFormatos() {
        assertEquals(4990L, Moeda.paraCentavos("49,90"));
        assertEquals(4990L, Moeda.paraCentavos("49.90"));
        assertEquals(4990L, Moeda.paraCentavos("R$" + (char) 0x00A0 + "49,90"));
        assertEquals(123456L, Moeda.paraCentavos("R$ 1.234,56"));
        assertEquals(5000L, Moeda.paraCentavos("50"));
    }

    @Test
    @DisplayName("arredonda a terceira casa para cima")
    void arredonda() {
        assertEquals(499L, Moeda.paraCentavos("4,985"));
    }

    @Test
    @DisplayName("recusa texto invalido em vez de virar zero silenciosamente")
    void recusaTextoInvalido() {
        assertThrows(RegraDeNegocioException.class, () -> Moeda.paraCentavos("abc"));
        assertThrows(RegraDeNegocioException.class, () -> Moeda.paraCentavos(""));
        assertThrows(RegraDeNegocioException.class, () -> Moeda.paraCentavos(null));
    }

    @Test
    @DisplayName("recusa preco negativo")
    void recusaNegativo() {
        assertThrows(RegraDeNegocioException.class, () -> Moeda.paraCentavos("-10"));
    }

    @Test
    @DisplayName("nao deixa espaco inquebravel na saida (virava 'R$?39,90' no terminal)")
    void semEspacoInquebravel() {
        // O formatador do pt-BR usa U+00A0 entre o cifrao e o numero. Em terminal/Windows
        // isso aparece como "?" e o texto fica feio. Este teste trava a correcao.
        String texto = Moeda.formatar(3990);
        assertEquals("R$ 39,90", texto);
        assertEquals(-1, texto.indexOf((char) 0x00A0), "a saida deve usar espaco comum");
    }

    @Test
    @DisplayName("aceita texto com espaco inquebravel colado do Discord")
    void leEspacoInquebravel() {
        // Se alguem copiar "R$ 49,90" de dentro do Discord, o espaco vem como o caractere U+00A0 (espaco inquebravel).
        assertEquals(4990L, Moeda.paraCentavos("R$" + (char) 0x00A0 + "49,90"));
    }
}

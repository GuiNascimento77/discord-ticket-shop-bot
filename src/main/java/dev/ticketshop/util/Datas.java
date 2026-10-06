package dev.ticketshop.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatacao de datas para exibir no Discord. */
public final class Datas {

    /** Fuso usado nas mensagens. Troque se voce nao estiver no horario de Brasilia. */
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private static final DateTimeFormatter DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'as' HH:mm", Locale.forLanguageTag("pt-BR"));

    private Datas() {
    }

    /** {@code 2026-10-06T18:30:00Z} vira {@code "06/10/2026 as 15:30"}. */
    public static String formatar(Instant momento) {
        return momento == null ? "-" : DATA_HORA.format(momento.atZone(FUSO));
    }
}

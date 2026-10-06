package dev.ticketshop.data;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Uma operacao de leitura no banco que devolve algo.
 *
 * <p>E uma interface funcional: podemos passar uma lambda
 * ({@code db.ler(c -> c.createStatement()...)}). O {@code throws SQLException} evita
 * encher o codigo de {@code try/catch} repetido.
 */
@FunctionalInterface
public interface FuncaoSQL<T> {

    T aplicar(Connection conexao) throws SQLException;
}

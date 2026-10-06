package dev.ticketshop.data;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Uma operacao de escrita no banco que nao precisa devolver nada.
 *
 * <p>Existe separada de {@link FuncaoSQL} para nao obrigar a escrever {@code return null}
 * no fim de cada lambda de escrita.
 */
@FunctionalInterface
public interface AcaoSQL {

    void executar(Connection conexao) throws SQLException;
}

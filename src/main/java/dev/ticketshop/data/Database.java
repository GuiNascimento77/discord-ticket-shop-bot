package dev.ticketshop.data;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Conexao com o SQLite e criacao do esquema.
 *
 * <p>SQLite e um banco de dados em um unico arquivo: nao precisa instalar servidor nenhum.
 * Usamos <b>uma</b> conexao compartilhada, com os metodos de acesso sincronizados, porque
 * os eventos do Discord chegam em varias threads ao mesmo tempo e o SQLite nao gosta de
 * varias escritas simultaneas.
 *
 * <p>Em testes usamos {@link #emMemoria()}: o banco existe so enquanto o processo vive.
 */
public final class Database implements AutoCloseable {

    private static final String ESQUEMA_PRODUTOS = """
            CREATE TABLE IF NOT EXISTS produtos (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                nome            TEXT    NOT NULL UNIQUE COLLATE NOCASE,
                descricao       TEXT    NOT NULL DEFAULT '',
                preco_centavos  INTEGER NOT NULL CHECK (preco_centavos >= 0),
                ativo           INTEGER NOT NULL DEFAULT 1,
                criado_em       TEXT    NOT NULL
            )
            """;

    private static final String ESQUEMA_TICKETS = """
            CREATE TABLE IF NOT EXISTS tickets (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                canal_id        TEXT,
                dono_id         TEXT    NOT NULL,
                tipo            TEXT    NOT NULL,
                status          TEXT    NOT NULL,
                assunto         TEXT    NOT NULL DEFAULT '',
                total_centavos  INTEGER NOT NULL DEFAULT 0,
                aberto_em       TEXT    NOT NULL,
                fechado_em      TEXT
            )
            """;

    private static final String ESQUEMA_ITENS = """
            CREATE TABLE IF NOT EXISTS itens_pedido (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                ticket_id       INTEGER NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
                produto_id      INTEGER REFERENCES produtos(id) ON DELETE SET NULL,
                nome_produto    TEXT    NOT NULL,
                preco_centavos  INTEGER NOT NULL,
                quantidade      INTEGER NOT NULL CHECK (quantidade > 0)
            )
            """;

    private static final String INDICE_CANAL = "CREATE INDEX IF NOT EXISTS idx_tickets_canal ON tickets(canal_id)";
    private static final String INDICE_DONO = "CREATE INDEX IF NOT EXISTS idx_tickets_dono ON tickets(dono_id)";
    private static final String INDICE_ITENS = "CREATE INDEX IF NOT EXISTS idx_itens_ticket ON itens_pedido(ticket_id)";

    /** Usado como trava: garante que apenas uma thread mexe no banco por vez. */
    private final Object trava = new Object();
    private final Connection conexao;

    private Database(Connection conexao) {
        this.conexao = conexao;
    }

    /** Abre (ou cria) o banco em um arquivo. */
    public static Database emArquivo(String caminho) {
        Path destino = Path.of(caminho).toAbsolutePath();
        File pasta = destino.getParent() == null ? null : destino.getParent().toFile();
        if (pasta != null && !pasta.exists() && !pasta.mkdirs()) {
            throw new ErroBancoException("Nao consegui criar a pasta do banco: " + pasta, null);
        }
        return abrir("jdbc:sqlite:" + destino, true);
    }

    /** Banco temporario em memoria RAM, usado pelos testes. */
    public static Database emMemoria() {
        return abrir("jdbc:sqlite::memory:", false);
    }

    private static Database abrir(String url, boolean emDisco) {
        try {
            Connection conexao = DriverManager.getConnection(url);
            Database banco = new Database(conexao);
            banco.configurar(emDisco);
            banco.criarTabelas();
            return banco;
        } catch (SQLException e) {
            throw new ErroBancoException("Falha ao abrir o banco de dados: " + url, e);
        }
    }

    private void configurar(boolean emDisco) throws SQLException {
        try (Statement comando = conexao.createStatement()) {
            // Sem isso, apagar um ticket deixaria os itens dele orfaos.
            comando.execute("PRAGMA foreign_keys = ON");
            if (emDisco) {
                // WAL deixa leitura e escrita mais rapidas e resistentes a travamento.
                comando.execute("PRAGMA journal_mode = WAL");
                comando.execute("PRAGMA busy_timeout = 5000");
            }
        }
    }

    private void criarTabelas() throws SQLException {
        try (Statement comando = conexao.createStatement()) {
            for (String sql : List.of(ESQUEMA_PRODUTOS, ESQUEMA_TICKETS, ESQUEMA_ITENS, INDICE_CANAL, INDICE_DONO, INDICE_ITENS)) {
                comando.execute(sql);
            }
        }
    }

    /** Executa uma consulta. Varias threads podem esperar na fila, uma de cada vez. */
    public <T> T ler(FuncaoSQL<T> consulta) {
        synchronized (trava) {
            try {
                return consulta.aplicar(conexao);
            } catch (SQLException e) {
                throw new ErroBancoException("Falha ao consultar o banco de dados.", e);
            }
        }
    }

    /**
     * Executa uma escrita dentro de uma transacao: ou tudo e gravado, ou nada e gravado.
     * Sem o {@code commit} explicito, uma falha no meio deixaria o pedido pela metade.
     */
    public <T> T escrever(FuncaoSQL<T> operacao) {
        synchronized (trava) {
            try {
                conexao.setAutoCommit(false);
                T resultado = operacao.aplicar(conexao);
                conexao.commit();
                return resultado;
            } catch (SQLException e) {
                desfazer();
                throw new ErroBancoException("Falha ao gravar no banco de dados.", e);
            } finally {
                restaurarAutoCommit();
            }
        }
    }

    /** Versao de {@link #escrever(FuncaoSQL)} para operacoes que nao devolvem nada. */
    public void executar(AcaoSQL operacao) {
        escrever(conexao -> {
            operacao.executar(conexao);
            return null;
        });
    }

    private void desfazer() {
        try {
            conexao.rollback();
        } catch (SQLException ignorado) {
            // Se nem o rollback funcionou, nao ha muito o que fazer aqui.
        }
    }

    private void restaurarAutoCommit() {
        try {
            conexao.setAutoCommit(true);
        } catch (SQLException ignorado) {
            // idem
        }
    }

    @Override
    public void close() {
        synchronized (trava) {
            try {
                conexao.close();
            } catch (SQLException e) {
                throw new ErroBancoException("Falha ao fechar o banco de dados.", e);
            }
        }
    }
}

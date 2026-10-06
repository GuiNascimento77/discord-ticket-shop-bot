package dev.ticketshop.data;

import dev.ticketshop.domain.ItemPedido;
import dev.ticketshop.domain.ResumoVendas;
import dev.ticketshop.domain.StatusTicket;
import dev.ticketshop.domain.Ticket;
import dev.ticketshop.domain.TipoTicket;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Implementacao SQLite de {@link TicketRepository}. */
public final class SqliteTicketRepository implements TicketRepository {

    private static final String COLUNAS =
            "id, canal_id, dono_id, tipo, status, assunto, total_centavos, aberto_em, fechado_em";

    /** Status que entram no relatorio de vendas. Se um status novo aparecer, entra sozinho. */
    private static final List<StatusTicket> STATUS_DE_VENDA =
            Arrays.stream(StatusTicket.values()).filter(StatusTicket::contaComoVenda).toList();

    /** "?, ?" — um espaco reservado por status, montado a partir do enum. */
    private static final String INTERROGACOES =
            String.join(", ", Collections.nCopies(STATUS_DE_VENDA.size(), "?"));

    private final Database banco;

    public SqliteTicketRepository(Database banco) {
        this.banco = banco;
    }

    @Override
    public Ticket inserir(Ticket ticket, List<ItemPedido> itens) {
        return banco.escrever(conexao -> {
            String sql = """
                    INSERT INTO tickets (canal_id, dono_id, tipo, status, assunto, total_centavos, aberto_em, fechado_em)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            long id;
            try (PreparedStatement comando = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                comando.setString(1, ticket.canalId());
                comando.setString(2, ticket.donoId());
                comando.setString(3, ticket.tipo().codigo());
                comando.setString(4, ticket.status().name());
                comando.setString(5, ticket.assunto());
                comando.setLong(6, ticket.totalCentavos());
                comando.setString(7, ticket.abertoEm().toString());
                comando.setString(8, ticket.fechadoEm() == null ? null : ticket.fechadoEm().toString());
                comando.executeUpdate();

                try (ResultSet chaves = comando.getGeneratedKeys()) {
                    if (!chaves.next()) {
                        throw new SQLException("O banco nao devolveu o id do ticket inserido.");
                    }
                    id = chaves.getLong(1);
                }
            }
            inserirItens(conexao, id, itens);
            return ticket.comId(id);
        });
    }

    @Override
    public boolean atualizar(Ticket ticket) {
        return banco.escrever(conexao -> {
            String sql = """
                    UPDATE tickets
                       SET canal_id = ?, tipo = ?, status = ?, assunto = ?, total_centavos = ?, fechado_em = ?
                     WHERE id = ?
                    """;
            try (PreparedStatement comando = conexao.prepareStatement(sql)) {
                comando.setString(1, ticket.canalId());
                comando.setString(2, ticket.tipo().codigo());
                comando.setString(3, ticket.status().name());
                comando.setString(4, ticket.assunto());
                comando.setLong(5, ticket.totalCentavos());
                if (ticket.fechadoEm() == null) {
                    comando.setNull(6, Types.VARCHAR);
                } else {
                    comando.setString(6, ticket.fechadoEm().toString());
                }
                comando.setLong(7, ticket.id());
                return comando.executeUpdate() > 0;
            }
        });
    }

    @Override
    public boolean registrarPedido(long ticketId, TipoTicket tipo, long totalCentavos, List<ItemPedido> itens) {
        // Transacao: apaga os itens antigos, grava os novos e atualiza o ticket.
        // Se algo falhar no meio, o banco volta ao estado anterior.
        return banco.escrever(conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement("DELETE FROM itens_pedido WHERE ticket_id = ?")) {
                comando.setLong(1, ticketId);
                comando.executeUpdate();
            }
            inserirItens(conexao, ticketId, itens);

            String sql = "UPDATE tickets SET tipo = ?, total_centavos = ? WHERE id = ?";
            try (PreparedStatement comando = conexao.prepareStatement(sql)) {
                comando.setString(1, tipo.codigo());
                comando.setLong(2, totalCentavos);
                comando.setLong(3, ticketId);
                return comando.executeUpdate() > 0;
            }
        });
    }

    @Override
    public Optional<Ticket> buscarPorId(long id) {
        return banco.ler(conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement("SELECT " + COLUNAS + " FROM tickets WHERE id = ?")) {
                comando.setLong(1, id);
                try (ResultSet resultado = comando.executeQuery()) {
                    return resultado.next() ? Optional.of(mapear(resultado)) : Optional.<Ticket>empty();
                }
            }
        });
    }

    @Override
    public Optional<Ticket> buscarPorCanal(String canalId) {
        return banco.ler(conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement("SELECT " + COLUNAS + " FROM tickets WHERE canal_id = ?")) {
                comando.setString(1, canalId);
                try (ResultSet resultado = comando.executeQuery()) {
                    return resultado.next() ? Optional.of(mapear(resultado)) : Optional.<Ticket>empty();
                }
            }
        });
    }

    @Override
    public List<Ticket> listarPorDono(String donoId) {
        return banco.ler(conexao -> {
            String sql = "SELECT " + COLUNAS + " FROM tickets WHERE dono_id = ? ORDER BY id DESC";
            try (PreparedStatement comando = conexao.prepareStatement(sql)) {
                comando.setString(1, donoId);
                try (ResultSet resultado = comando.executeQuery()) {
                    return lerTodos(resultado);
                }
            }
        });
    }

    @Override
    public List<Ticket> listarPorStatus(StatusTicket status) {
        return banco.ler(conexao -> {
            String sql = "SELECT " + COLUNAS + " FROM tickets WHERE status = ? ORDER BY id DESC";
            try (PreparedStatement comando = conexao.prepareStatement(sql)) {
                comando.setString(1, status.name());
                try (ResultSet resultado = comando.executeQuery()) {
                    return lerTodos(resultado);
                }
            }
        });
    }

    @Override
    public List<ItemPedido> itensDoTicket(long ticketId) {
        return banco.ler(conexao -> {
            String sql = """
                    SELECT id, produto_id, nome_produto, preco_centavos, quantidade
                      FROM itens_pedido
                     WHERE ticket_id = ?
                     ORDER BY id
                    """;
            List<ItemPedido> itens = new ArrayList<>();
            try (PreparedStatement comando = conexao.prepareStatement(sql)) {
                comando.setLong(1, ticketId);
                try (ResultSet resultado = comando.executeQuery()) {
                    while (resultado.next()) {
                        long produtoId = resultado.getLong("produto_id");
                        itens.add(new ItemPedido(
                                resultado.getLong("id"),
                                resultado.wasNull() ? null : produtoId,
                                resultado.getString("nome_produto"),
                                resultado.getLong("preco_centavos"),
                                resultado.getInt("quantidade")));
                    }
                }
            }
            return itens;
        });
    }

    @Override
    public ResumoVendas resumoVendas() {
        return banco.ler(conexao -> {
            long pedidos = 0;
            long receita = 0;

            String sqlTotais = """
                    SELECT COUNT(*) AS pedidos, COALESCE(SUM(total_centavos), 0) AS receita
                      FROM tickets
                     WHERE status IN (%s) AND total_centavos > 0
                    """.formatted(INTERROGACOES);

            try (PreparedStatement comando = conexao.prepareStatement(sqlTotais)) {
                preencherStatusDeVenda(comando);
                try (ResultSet resultado = comando.executeQuery()) {
                    if (resultado.next()) {
                        pedidos = resultado.getLong("pedidos");
                        receita = resultado.getLong("receita");
                    }
                }
            }

            String produtoCampeao = "Nenhuma venda ainda";
            String sqlCampeao = """
                    SELECT i.nome_produto, SUM(i.quantidade) AS unidades
                      FROM itens_pedido i
                      JOIN tickets t ON t.id = i.ticket_id
                     WHERE t.status IN (%s)
                     GROUP BY i.nome_produto
                     ORDER BY unidades DESC, i.nome_produto
                     LIMIT 1
                    """.formatted(INTERROGACOES);

            try (PreparedStatement comando = conexao.prepareStatement(sqlCampeao)) {
                preencherStatusDeVenda(comando);
                try (ResultSet resultado = comando.executeQuery()) {
                    if (resultado.next()) {
                        produtoCampeao = "%s (%d un.)".formatted(
                                resultado.getString("nome_produto"), resultado.getLong("unidades"));
                    }
                }
            }

            long media = pedidos == 0 ? 0 : receita / pedidos;
            return new ResumoVendas(pedidos, receita, media, produtoCampeao);
        });
    }

    private static void preencherStatusDeVenda(PreparedStatement comando) throws SQLException {
        for (int i = 0; i < STATUS_DE_VENDA.size(); i++) {
            comando.setString(i + 1, STATUS_DE_VENDA.get(i).name());
        }
    }

    private static void inserirItens(Connection conexao, long ticketId, List<ItemPedido> itens) throws SQLException {
        if (itens.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO itens_pedido (ticket_id, produto_id, nome_produto, preco_centavos, quantidade)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            for (ItemPedido item : itens) {
                comando.setLong(1, ticketId);
                if (item.produtoId() == null) {
                    comando.setNull(2, Types.INTEGER);
                } else {
                    comando.setLong(2, item.produtoId());
                }
                comando.setString(3, item.nomeProduto());
                comando.setLong(4, item.precoCentavos());
                comando.setInt(5, item.quantidade());
                comando.addBatch();
            }
            comando.executeBatch();
        }
    }

    private static List<Ticket> lerTodos(ResultSet resultado) throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        while (resultado.next()) {
            tickets.add(mapear(resultado));
        }
        return tickets;
    }

    private static Ticket mapear(ResultSet resultado) throws SQLException {
        String fechadoEm = resultado.getString("fechado_em");
        return new Ticket(
                resultado.getLong("id"),
                resultado.getString("canal_id"),
                resultado.getString("dono_id"),
                TipoTicket.deCodigo(resultado.getString("tipo")),
                StatusTicket.deCodigo(resultado.getString("status")),
                resultado.getString("assunto"),
                resultado.getLong("total_centavos"),
                Instant.parse(resultado.getString("aberto_em")),
                fechadoEm == null ? null : Instant.parse(fechadoEm));
    }
}

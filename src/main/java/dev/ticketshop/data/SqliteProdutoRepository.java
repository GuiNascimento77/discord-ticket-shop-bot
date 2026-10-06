package dev.ticketshop.data;

import dev.ticketshop.domain.Produto;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementacao do catalogo usando SQLite.
 *
 * <p>Todas as consultas usam {@link PreparedStatement} com {@code ?} no lugar dos valores.
 * Isso evita <b>SQL injection</b>: o valor nunca e interpretado como comando.
 */
public final class SqliteProdutoRepository implements ProdutoRepository {

    private static final String COLUNAS = "id, nome, descricao, preco_centavos, ativo";

    private final Database banco;

    public SqliteProdutoRepository(Database banco) {
        this.banco = banco;
    }

    @Override
    public Produto inserir(Produto produto) {
        return banco.escrever(conexao -> {
            String sql = "INSERT INTO produtos (nome, descricao, preco_centavos, ativo, criado_em) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement comando = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                comando.setString(1, produto.nome());
                comando.setString(2, produto.descricao());
                comando.setLong(3, produto.precoCentavos());
                comando.setInt(4, produto.ativo() ? 1 : 0);
                comando.setString(5, Instant.now().toString());
                comando.executeUpdate();

                try (ResultSet chaves = comando.getGeneratedKeys()) {
                    if (chaves.next()) {
                        return produto.comId(chaves.getLong(1));
                    }
                }
            }
            throw new SQLException("O banco nao devolveu o id do produto inserido.");
        });
    }

    @Override
    public Optional<Produto> buscarPorId(long id) {
        return banco.ler(conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement("SELECT " + COLUNAS + " FROM produtos WHERE id = ?")) {
                comando.setLong(1, id);
                try (ResultSet resultado = comando.executeQuery()) {
                    return resultado.next() ? Optional.of(mapear(resultado)) : Optional.<Produto>empty();
                }
            }
        });
    }

    @Override
    public Optional<Produto> buscarPorNome(String nome) {
        return banco.ler(conexao -> {
            // A coluna foi criada com COLLATE NOCASE: "Cafe" e "cafe" sao o mesmo produto.
            try (PreparedStatement comando = conexao.prepareStatement("SELECT " + COLUNAS + " FROM produtos WHERE nome = ?")) {
                comando.setString(1, nome);
                try (ResultSet resultado = comando.executeQuery()) {
                    return resultado.next() ? Optional.of(mapear(resultado)) : Optional.<Produto>empty();
                }
            }
        });
    }

    @Override
    public List<Produto> listarAtivos() {
        return listar("SELECT " + COLUNAS + " FROM produtos WHERE ativo = 1 ORDER BY nome");
    }

    @Override
    public List<Produto> listarTodos() {
        return listar("SELECT " + COLUNAS + " FROM produtos ORDER BY nome");
    }

    @Override
    public boolean atualizar(Produto produto) {
        return banco.escrever(conexao -> {
            String sql = "UPDATE produtos SET nome = ?, descricao = ?, preco_centavos = ?, ativo = ? WHERE id = ?";
            try (PreparedStatement comando = conexao.prepareStatement(sql)) {
                comando.setString(1, produto.nome());
                comando.setString(2, produto.descricao());
                comando.setLong(3, produto.precoCentavos());
                comando.setInt(4, produto.ativo() ? 1 : 0);
                comando.setLong(5, produto.id());
                return comando.executeUpdate() > 0;
            }
        });
    }

    @Override
    public boolean desativar(long id) {
        return banco.escrever(conexao -> {
            try (PreparedStatement comando = conexao.prepareStatement("UPDATE produtos SET ativo = 0 WHERE id = ?")) {
                comando.setLong(1, id);
                return comando.executeUpdate() > 0;
            }
        });
    }

    private List<Produto> listar(String sql) {
        return banco.ler(conexao -> {
            List<Produto> produtos = new ArrayList<>();
            try (PreparedStatement comando = conexao.prepareStatement(sql);
                 ResultSet resultado = comando.executeQuery()) {
                while (resultado.next()) {
                    produtos.add(mapear(resultado));
                }
            }
            return produtos;
        });
    }

    /** Converte a linha atual do resultado em um {@link Produto}. */
    private static Produto mapear(ResultSet resultado) throws SQLException {
        return new Produto(
                resultado.getLong("id"),
                resultado.getString("nome"),
                resultado.getString("descricao"),
                resultado.getLong("preco_centavos"),
                resultado.getInt("ativo") == 1);
    }
}

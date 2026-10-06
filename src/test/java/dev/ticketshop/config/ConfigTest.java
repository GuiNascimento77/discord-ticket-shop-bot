package dev.ticketshop.config;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes da leitura do {@code .env}.
 *
 * <p>Este arquivo existe por causa de um bug de verdade: o bot foi ligado pela primeira vez com
 * um {@code .env} contendo apenas o token e caiu com {@code NullPointerException} antes de
 * conectar. O caminho do banco era lido com {@code getOrDefault} na condicao e {@code get} no
 * ramo do ternario — o primeiro achava o valor padrao, o segundo devolvia {@code null}.
 *
 * <p>Como {@link Config} tambem le as variaveis de ambiente do sistema (e elas vencem o arquivo),
 * os testes que dependem da <b>ausencia</b> de uma variavel usam {@code assumeTrue} para nao
 * quebrar em maquina de CI que tenha a variavel configurada.
 */
class ConfigTest {

    private static final String TOKEN_FALSO = "token-de-teste-123";

    @TempDir
    Path pasta;

    private Path envCom(String conteudo) throws IOException {
        Path arquivo = pasta.resolve(".env");
        Files.writeString(arquivo, conteudo, StandardCharsets.UTF_8);
        return arquivo;
    }

    @Test
    @DisplayName("sem DB_PATH no arquivo usa o caminho padrao (regressao: aqui dava NullPointerException)")
    void usaCaminhoPadraoQuandoNaoTemDbPath() throws IOException {
        Assumptions.assumeTrue(System.getenv("DB_PATH") == null, "DB_PATH definido no ambiente do sistema");

        Config config = Config.carregar(envCom("DISCORD_TOKEN=" + TOKEN_FALSO + "\n"));

        assertEquals("dados/bot.db", config.caminhoBanco());
    }

    @Test
    @DisplayName("DB_PATH vazio tambem cai no caminho padrao")
    void dbPathVazioUsaPadrao() throws IOException {
        Assumptions.assumeTrue(System.getenv("DB_PATH") == null, "DB_PATH definido no ambiente do sistema");

        Config config = Config.carregar(envCom("DISCORD_TOKEN=" + TOKEN_FALSO + "\nDB_PATH=\n"));

        assertEquals("dados/bot.db", config.caminhoBanco());
    }

    @Test
    @DisplayName("DB_PATH preenchido e respeitado")
    void aceitaDbPathPersonalizado() throws IOException {
        Config config = Config.carregar(envCom(
                "DISCORD_TOKEN=" + TOKEN_FALSO + "\n"
                        + "DB_PATH=/var/lib/bot/loja.db\n"));

        assertEquals("/var/lib/bot/loja.db", config.caminhoBanco());
    }

    @Test
    @DisplayName("variaveis opcionais vazias viram Optional.empty")
    void opcionaisVaziosViraramEmpty() throws IOException {
        Assumptions.assumeTrue(System.getenv("GUILD_ID") == null, "GUILD_ID definido no ambiente do sistema");

        Config config = Config.carregar(envCom(
                "DISCORD_TOKEN=" + TOKEN_FALSO + "\n"
                        + "GUILD_ID=\n"
                        + "STAFF_ROLE_ID=   \n"
                        + "CATEGORIA_ID=\n"));

        assertTrue(config.guildId().isEmpty());
        assertTrue(config.staffRoleId().isEmpty());
        assertTrue(config.categoriaId().isEmpty());
    }

    @Test
    @DisplayName("preenche os opcionais quando existem, aceitando aspas")
    void leOpcionais() throws IOException {
        Config config = Config.carregar(envCom("""
                # comentario ignorado
                DISCORD_TOKEN=%s

                GUILD_ID="123456789012345678"
                STAFF_ROLE_ID=987654321098765432
                CATEGORIA_ID='111222333444555666'
                """.formatted(TOKEN_FALSO)));

        assertEquals("123456789012345678", config.guildId().orElseThrow());
        assertEquals("987654321098765432", config.staffRoleId().orElseThrow());
        assertEquals("111222333444555666", config.categoriaId().orElseThrow());
    }

    @Test
    @DisplayName("arquivo sem token falha com mensagem que ensina o que fazer")
    void semTokenFalha() throws IOException {
        Assumptions.assumeTrue(System.getenv("DISCORD_TOKEN") == null, "DISCORD_TOKEN definido no ambiente do sistema");

        Path arquivo = envCom("GUILD_ID=123\n");

        IllegalStateException erro = assertThrows(IllegalStateException.class, () -> Config.carregar(arquivo));
        assertTrue(erro.getMessage().contains("DISCORD_TOKEN nao configurado"));
    }

    @Test
    @DisplayName("token ainda com o texto de exemplo do .env.example e tratado como faltando")
    void tokenPlaceholderFalha() throws IOException {
        Assumptions.assumeTrue(System.getenv("DISCORD_TOKEN") == null, "DISCORD_TOKEN definido no ambiente do sistema");

        Path arquivo = envCom("DISCORD_TOKEN=cole-seu-token-aqui\n");

        assertThrows(IllegalStateException.class, () -> Config.carregar(arquivo));
    }

    @Test
    @DisplayName("arquivo inexistente nao explode: apenas falta o token")
    void arquivoInexistente() {
        Assumptions.assumeTrue(System.getenv("DISCORD_TOKEN") == null, "DISCORD_TOKEN definido no ambiente do sistema");

        Path inexistente = pasta.resolve("nao-existe.env");

        assertThrows(IllegalStateException.class, () -> Config.carregar(inexistente));
    }

    @Test
    @DisplayName("Env le um mapa simples de chave=valor e ignora comentarios e linhas vazias")
    void envLeChaveValor() throws IOException {
        Map<String, String> valores = Env.carregar(envCom("""
                CHAVE_A=1
                CHAVE_B = 2
                # CHAVE_COMENTADA=3

                CHAVE_D="4"
                CHAVE_E='5'
                lixo sem igual
                """));

        assertEquals("1", valores.get("CHAVE_A"));
        assertEquals("2", valores.get("CHAVE_B"));
        assertEquals("4", valores.get("CHAVE_D"), "aspas duplas devem sair do valor");
        assertEquals("5", valores.get("CHAVE_E"), "aspas simples devem sair do valor");
        assertFalse(valores.containsKey("CHAVE_COMENTADA"), "linha com # e comentario");
    }
}

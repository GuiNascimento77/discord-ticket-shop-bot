package dev.ticketshop.config;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Configuracao do bot, carregada do arquivo {@code .env} (ou das variaveis de ambiente).
 *
 * @param token        token do bot — obrigatorio
 * @param guildId      id do servidor; com ele os comandos aparecem na hora (opcional)
 * @param staffRoleId  cargo que atende os tickets (opcional)
 * @param categoriaId  categoria onde os canais de ticket sao criados (opcional)
 * @param caminhoBanco caminho do arquivo do banco de dados
 */
public record Config(String token,
                     Optional<String> guildId,
                     Optional<String> staffRoleId,
                     Optional<String> categoriaId,
                     String caminhoBanco) {

    private static final String PLACEHOLDER = "cole-seu-token-aqui";
    private static final String BANCO_PADRAO = "dados/bot.db";

    /** Carrega o {@code .env} da raiz do projeto. */
    public static Config carregar() {
        return carregar(Path.of(".env"));
    }

    public static Config carregar(Path arquivoEnv) {
        Map<String, String> valores = Env.carregar(arquivoEnv);

        // Lemos o caminho do banco uma vez so. Antes, o getOrDefault ficava na condicao
        // e o get no ramo do operador ternario: quando o .env nao tinha DB_PATH, o
        // primeiro devolvia o padrao (condicao falsa) e o segundo devolvia null -> NPE.
        String caminhoBanco = valores.getOrDefault("DB_PATH", "").trim();

        return new Config(
                exigirToken(valores),
                opcional(valores, "GUILD_ID"),
                opcional(valores, "STAFF_ROLE_ID"),
                opcional(valores, "CATEGORIA_ID"),
                caminhoBanco.isEmpty() ? BANCO_PADRAO : caminhoBanco);
    }

    private static String exigirToken(Map<String, String> valores) {
        String token = valores.getOrDefault("DISCORD_TOKEN", "").trim();
        if (token.isEmpty() || PLACEHOLDER.equals(token)) {
            throw new IllegalStateException("""
                    DISCORD_TOKEN nao configurado.

                    1) Copie o arquivo .env.example para .env
                    2) Cole o token do seu bot em DISCORD_TOKEN=...
                    3) Rode de novo""");
        }
        return token;
    }

    private static Optional<String> opcional(Map<String, String> valores, String chave) {
        String valor = valores.getOrDefault(chave, "").trim();
        return valor.isEmpty() ? Optional.empty() : Optional.of(valor);
    }
}

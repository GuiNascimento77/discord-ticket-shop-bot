package dev.ticketshop.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Leitor simples de arquivo {@code .env}.
 *
 * <p>Formato aceito, uma variavel por linha:
 * <pre>
 *   # comentario
 *   DISCORD_TOKEN=abc123
 *   GUILD_ID="123456"
 * </pre>
 *
 * <p>Variaveis de ambiente do sistema tem prioridade sobre o arquivo, o que e praticamente
 * obrigatorio em hospedagens (Railway, Render, VPS) onde voce nao quer arquivo com segredo.
 */
public final class Env {

    private Env() {
    }

    /** Carrega o arquivo (se existir) e mistura com as variaveis de ambiente do sistema. */
    public static Map<String, String> carregar(Path arquivo) {
        Map<String, String> valores = new LinkedHashMap<>();
        if (Files.isRegularFile(arquivo)) {
            valores.putAll(lerArquivo(arquivo));
        }
        valores.putAll(System.getenv());
        return valores;
    }

    private static Map<String, String> lerArquivo(Path arquivo) {
        Map<String, String> valores = new LinkedHashMap<>();
        List<String> linhas;
        try {
            linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao consegui ler o arquivo " + arquivo, e);
        }

        for (String linha : linhas) {
            String conteudo = linha.trim();
            if (conteudo.isEmpty() || conteudo.startsWith("#")) {
                continue;
            }
            int separador = conteudo.indexOf('=');
            if (separador <= 0) {
                continue;
            }
            String chave = conteudo.substring(0, separador).trim();
            String valor = conteudo.substring(separador + 1).trim();
            valores.put(chave, semAspas(valor));
        }
        return valores;
    }

    private static String semAspas(String valor) {
        boolean entreAspas = valor.length() >= 2
                && ((valor.startsWith("\"") && valor.endsWith("\""))
                 || (valor.startsWith("'") && valor.endsWith("'")));
        return entreAspas ? valor.substring(1, valor.length() - 1) : valor;
    }
}

package dev.ticketshop;

import dev.ticketshop.config.Config;

/**
 * Ponto de entrada.
 *
 * <pre>
 *   java -jar discord-ticket-shop-bot.jar            # liga o bot de verdade
 *   java -jar discord-ticket-shop-bot.jar --demo     # demonstracao sem Discord
 * </pre>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length > 0 && ("--demo".equals(args[0]) || "--offline".equals(args[0]))) {
            Demo.executar();
            return;
        }

        try {
            Bot.iniciar(Config.carregar());
        } catch (IllegalStateException e) {
            // Configuracao faltando: mensagem curta em vez de pilha de erro gigante.
            System.err.println();
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}

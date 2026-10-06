package dev.ticketshop;

import dev.ticketshop.config.Config;
import dev.ticketshop.data.Database;
import dev.ticketshop.data.ProdutoRepository;
import dev.ticketshop.data.SqliteProdutoRepository;
import dev.ticketshop.data.SqliteTicketRepository;
import dev.ticketshop.data.TicketRepository;
import dev.ticketshop.discord.AberturaDeTicket;
import dev.ticketshop.discord.Autorizacao;
import dev.ticketshop.discord.Comandos;
import dev.ticketshop.discord.TicketCanal;
import dev.ticketshop.discord.command.AjudaCommand;
import dev.ticketshop.discord.command.CarrinhoCommand;
import dev.ticketshop.discord.command.DuvidaCommand;
import dev.ticketshop.discord.command.FecharCommand;
import dev.ticketshop.discord.command.MeusTicketsCommand;
import dev.ticketshop.discord.command.ProdutoAdminCommand;
import dev.ticketshop.discord.command.ProdutosCommand;
import dev.ticketshop.discord.command.VendasCommand;
import dev.ticketshop.discord.listener.InteracaoListener;
import dev.ticketshop.service.CarrinhoService;
import dev.ticketshop.service.LojaService;
import dev.ticketshop.service.TicketService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Monta todas as pecas e liga o bot.
 *
 * <p>Este e o unico lugar onde as classes concretas aparecem juntas. Repare que
 * {@code LojaService} recebe um {@code ProdutoRepository} — nao um SQLite qualquer.
 * Trocar o banco por Postgres, por exemplo, mexe aqui e em mais lugar nenhum.
 */
public final class Bot {

    private static final Logger LOG = LoggerFactory.getLogger(Bot.class);

    private Bot() {
    }

    public static void iniciar(Config config) {
        Database banco = Database.emArquivo(config.caminhoBanco());
        // Fecha o banco com calma quando o programa for encerrado (Ctrl+C).
        Runtime.getRuntime().addShutdownHook(new Thread(banco::close, "fechar-banco"));

        ProdutoRepository produtos = new SqliteProdutoRepository(banco);
        TicketRepository tickets = new SqliteTicketRepository(banco);

        LojaService loja = new LojaService(produtos);
        CarrinhoService carrinhos = new CarrinhoService(loja);
        TicketService atendimentos = new TicketService(tickets);

        Autorizacao autorizacao = new Autorizacao(config);
        TicketCanal canais = new TicketCanal(config);
        AberturaDeTicket abertura = new AberturaDeTicket(atendimentos, canais);

        Comandos comandos = new Comandos(List.of(
                new AjudaCommand(autorizacao),
                new ProdutosCommand(loja),
                new CarrinhoCommand(carrinhos),
                new DuvidaCommand(abertura),
                new FecharCommand(atendimentos, canais, autorizacao),
                new MeusTicketsCommand(atendimentos),
                new ProdutoAdminCommand(loja, autorizacao),
                new VendasCommand(atendimentos, autorizacao)));

        InteracaoListener listener = new InteracaoListener(
                comandos, loja, carrinhos, atendimentos, abertura, canais, autorizacao);

        try {
            JDA jda = JDABuilder.createDefault(config.token())
                    .setActivity(Activity.playing("/ajuda"))
                    .addEventListeners(listener)
                    .build();

            // Espera a conexao ficar pronta antes de registrar os comandos.
            jda.awaitReady();
            comandos.registrar(jda, config.guildId());
            LOG.info("Bot conectado como \"{}\". Ja pode usar os comandos no Discord.", jda.getSelfUser().getEffectiveName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("A conexao com o Discord foi interrompida.", e);
        }
    }
}

package dev.ticketshop;

import dev.ticketshop.data.Database;
import dev.ticketshop.data.SqliteProdutoRepository;
import dev.ticketshop.data.SqliteTicketRepository;
import dev.ticketshop.domain.ItemCarrinho;
import dev.ticketshop.domain.ItemPedido;
import dev.ticketshop.domain.Produto;
import dev.ticketshop.domain.ResumoVendas;
import dev.ticketshop.domain.Ticket;
import dev.ticketshop.service.CarrinhoService;
import dev.ticketshop.service.LojaService;
import dev.ticketshop.service.TicketService;
import dev.ticketshop.util.Moeda;

import java.util.List;

/**
 * Demonstracao que roda <b>sem Discord e sem internet</b>.
 *
 * <p>Serve para tres coisas: conferir se o projeto esta funcionando, entender o fluxo de
 * negocio lendo a saida, e mostrar o resultado para quem clonar o repositorio sem ter
 * token de bot.
 *
 * <p>Rode com {@code java -jar target/discord-ticket-shop-bot.jar --demo}.
 * O banco fica em memoria, entao nada e gravado no seu computador.
 */
public final class Demo {

    private Demo() {
    }

    public static void executar() {
        titulo("DEMONSTRACAO OFFLINE - nenhuma conexao com o Discord e necessaria");

        try (Database banco = Database.emMemoria()) {
            LojaService loja = new LojaService(new SqliteProdutoRepository(banco));
            CarrinhoService carrinhos = new CarrinhoService(loja);
            TicketService atendimentos = new TicketService(new SqliteTicketRepository(banco));

            passo1CadastrarProdutos(loja);
            passo2MontarCarrinho(carrinhos);
            passo3AbrirAtendimentoEConverter(atendimentos, carrinhos);
            passo4VerResumo(atendimentos);
        }
    }

    private static void passo1CadastrarProdutos(LojaService loja) {
        titulo("1) A equipe cadastra produtos");
        loja.cadastrar("Cafe especial 250g", "Torra media, graos arabica", Moeda.paraCentavos("39,90"));
        loja.cadastrar("Caneca da loja", "Ceramica 300ml", Moeda.paraCentavos("24,50"));
        loja.cadastrar("Kit degustacao", "3 amostras de 100g", Moeda.paraCentavos("59,90"));

        for (Produto produto : loja.catalogo()) {
            System.out.printf("   #%d  %-22s %s%n", produto.id(), produto.nome(), Moeda.formatar(produto.precoCentavos()));
        }
    }

    private static void passo2MontarCarrinho(CarrinhoService carrinhos) {
        titulo("2) O cliente monta o carrinho (em memoria, nada foi salvo ainda)");
        String cliente = "cliente-42";
        carrinhos.adicionar(cliente, 1, 2);
        carrinhos.adicionar(cliente, 2, 1);
        carrinhos.adicionar(cliente, 1, 1); // mesmo produto de novo: a quantidade deve somar

        for (ItemCarrinho item : carrinhos.itens(cliente)) {
            System.out.printf("   %-22s x%-3d %s%n", item.nome(), item.quantidade(), Moeda.formatar(item.subtotalCentavos()));
        }
        System.out.println("   " + "-".repeat(45));
        System.out.printf("   %-26s %s%n", "TOTAL", Moeda.formatar(carrinhos.totalCentavos(cliente)));
    }

    private static void passo3AbrirAtendimentoEConverter(TicketService atendimentos, CarrinhoService carrinhos) {
        titulo("3) O atendimento nasce como duvida e vira pedido");
        String cliente = "cliente-42";

        Ticket duvida = atendimentos.abrirDuvida(cliente, "Voces entregam em Campinas?");
        System.out.printf("   Ticket #%d aberto (%s) - status: %s%n",
                duvida.id(), duvida.tipo().rotulo(), duvida.status().rotulo());

        Ticket pedido = atendimentos.converterEmPedido(duvida.id(), carrinhos.itens(cliente));
        System.out.printf("   Ticket #%d convertido em %s - total: %s%n",
                pedido.id(), pedido.tipo().rotulo(), Moeda.formatar(pedido.totalCentavos()));

        for (ItemPedido item : atendimentos.itens(pedido.id())) {
            System.out.printf("      %-22s x%-3d %s%n", item.nomeProduto(), item.quantidade(), Moeda.formatar(item.subtotalCentavos()));
        }

        // Carrinho virou pedido: pode ser esvaziado.
        carrinhos.esvaziar(cliente);

        Ticket confirmado = atendimentos.confirmar(pedido.id());
        System.out.printf("   A equipe confirmou o pedido: status agora e %s%n", confirmado.status().rotulo());

        Ticket fechado = atendimentos.fechar(pedido.id());
        System.out.printf("   Canal encerrado: status final %s%n", fechado.status().rotulo());
    }

    private static void passo4VerResumo(TicketService atendimentos) {
        titulo("4) A equipe confere os numeros com /vendas");
        ResumoVendas resumo = atendimentos.resumo();
        System.out.printf("   Pedidos confirmados : %d%n", resumo.pedidosConfirmados());
        System.out.printf("   Receita             : %s%n", Moeda.formatar(resumo.receitaCentavos()));
        System.out.printf("   Ticket medio        : %s%n", Moeda.formatar(resumo.ticketMedioCentavos()));
        System.out.printf("   Produto mais vendido: %s%n", resumo.produtoMaisVendido());
        System.out.println();
        System.out.println("   Tudo isso foi gravado em SQLite de verdade (aqui, em memoria).");
        System.out.println("   No bot real, cada passo corresponde a um clique no Discord.");
    }

    private static void titulo(String texto) {
        System.out.println();
        System.out.println("=".repeat(64));
        System.out.println(" " + texto);
        System.out.println("=".repeat(64));
    }
}

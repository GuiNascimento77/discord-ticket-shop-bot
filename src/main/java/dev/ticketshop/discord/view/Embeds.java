package dev.ticketshop.discord.view;

import dev.ticketshop.domain.ItemCarrinho;
import dev.ticketshop.domain.ItemPedido;
import dev.ticketshop.domain.Produto;
import dev.ticketshop.domain.ResumoVendas;
import dev.ticketshop.domain.Ticket;
import dev.ticketshop.util.Datas;
import dev.ticketshop.util.Moeda;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.Color;
import java.util.List;

/**
 * Todas as "caixas" que o bot mostra no Discord.
 *
 * <p>Juntar isso em uma classe so tem duas vantagens: o visual fica consistente e, quando
 * alguem pedir para mudar as cores, existe exatamente um arquivo para editar.
 */
public final class Embeds {

    private static final Color AZUL = new Color(0x5865F2);
    private static final Color VERDE = new Color(0x57F287);
    private static final Color VERMELHO = new Color(0xED4245);
    private static final Color AMARELO = new Color(0xFEE75C);

    /** O Discord aceita no maximo 25 opcoes por menu de selecao. */
    private static final int MAXIMO_DE_PRODUTOS = 25;

    private Embeds() {
    }

    public static MessageEmbed erro(String mensagem) {
        return new EmbedBuilder()
                .setColor(VERMELHO)
                .setTitle("❌ Nao deu certo")
                .setDescription(mensagem)
                .build();
    }

    public static MessageEmbed sucesso(String mensagem) {
        return new EmbedBuilder()
                .setColor(VERDE)
                .setTitle("✅ Feito")
                .setDescription(mensagem)
                .build();
    }

    public static MessageEmbed aviso(String mensagem) {
        return new EmbedBuilder()
                .setColor(AMARELO)
                .setTitle("⚠️ Atencao")
                .setDescription(mensagem)
                .build();
    }

    public static MessageEmbed ajuda(boolean ehEquipe) {
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(AZUL)
                .setTitle("🤖 Como usar o bot")
                .setDescription("Bot de atendimento e vendas. Use os comandos abaixo — o Discord mostra as opcoes se voce digitar `/`.")
                .addField("🛒 Para comprar",
                        """
                        `/produtos` — ve o catalogo e escolhe o que quer
                        `/carrinho` — revisa o carrinho e finaliza o pedido""", false)
                .addField("💬 Para falar com a equipe",
                        """
                        `/duvida` — abre um atendimento privado
                        `/fechar` — encerra o atendimento deste canal
                        `/meus-tickets` — ve seus atendimentos e pedidos""", false);

        if (ehEquipe) {
            embed.addField("🔧 Para a equipe",
                    """
                    `/produto adicionar` — cadastra um produto
                    `/produto remover` — tira um produto da vitrine
                    `/produto listar` — ve todos os produtos
                    `/vendas` — resumo de vendas""", false);
        }
        return embed.setFooter("Os pedidos aparecem no canal privado criado para o cliente").build();
    }

    public static MessageEmbed catalogo(List<Produto> produtos) {
        EmbedBuilder embed = new EmbedBuilder().setColor(AZUL).setTitle("🛒 Catalogo");
        if (produtos.isEmpty()) {
            return embed.setDescription("Ainda nao ha nenhum produto cadastrado.\nPeça para a equipe usar `/produto adicionar`.")
                    .build();
        }

        StringBuilder texto = new StringBuilder();
        produtos.stream().limit(MAXIMO_DE_PRODUTOS).forEach(produto -> {
            texto.append("**").append(produto.nome()).append("** — ")
                    .append(Moeda.formatar(produto.precoCentavos())).append('\n');
            if (!produto.descricao().isBlank()) {
                texto.append("-# ").append(produto.descricao()).append('\n');
            }
        });

        return embed.setDescription(texto.toString())
                .setFooter("Escolha um produto no menu abaixo para colocar no carrinho")
                .build();
    }

    /** Lista de administracao: mostra tambem o que esta fora da vitrine. */
    public static MessageEmbed listaCompleta(List<Produto> produtos) {
        EmbedBuilder embed = new EmbedBuilder().setColor(AZUL).setTitle("📦 Produtos cadastrados");
        if (produtos.isEmpty()) {
            return embed.setDescription("Nada cadastrado ainda. Use `/produto adicionar`.").build();
        }

        StringBuilder texto = new StringBuilder();
        produtos.stream().limit(MAXIMO_DE_PRODUTOS).forEach(produto -> texto
                .append(produto.ativo() ? "🟢" : "⚫")
                .append(" **#").append(produto.id()).append("** ").append(produto.nome())
                .append(" — ").append(Moeda.formatar(produto.precoCentavos()))
                .append(produto.ativo() ? "" : " *(fora da vitrine)*")
                .append('\n'));

        return embed.setDescription(texto.toString())
                .setFooter("🟢 na vitrine · ⚫ fora da vitrine")
                .build();
    }

    public static MessageEmbed carrinho(List<ItemCarrinho> itens, long totalCentavos) {
        EmbedBuilder embed = new EmbedBuilder().setColor(AZUL).setTitle("🧺 Seu carrinho");
        if (itens.isEmpty()) {
            return embed.setDescription("Seu carrinho esta vazio.\nUse `/produtos` para escolher algo.").build();
        }

        StringBuilder texto = new StringBuilder();
        for (ItemCarrinho item : itens) {
            texto.append("• **").append(item.nome()).append("** x").append(item.quantidade())
                    .append(" — ").append(Moeda.formatar(item.subtotalCentavos())).append('\n');
        }

        return embed.setDescription(texto.toString())
                .addField("Total", "**" + Moeda.formatar(totalCentavos) + "**", false)
                .build();
    }

    public static MessageEmbed resumoPedido(Ticket ticket, List<ItemPedido> itens) {
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(AZUL)
                .setTitle("🛒 Pedido #" + ticket.id())
                .addField("Cliente", "<@" + ticket.donoId() + ">", true)
                .addField("Status", ticket.status().rotulo(), true)
                .addField("Aberto em", Datas.formatar(ticket.abertoEm()), true);

        if (itens.isEmpty()) {
            embed.addField("Itens", "Nenhum item ainda.", false);
        } else {
            StringBuilder lista = new StringBuilder();
            for (ItemPedido item : itens) {
                lista.append("• ").append(item.nomeProduto()).append(" x").append(item.quantidade())
                        .append(" — ").append(Moeda.formatar(item.subtotalCentavos())).append('\n');
            }
            embed.addField("Itens", lista.toString(), false);
        }

        return embed.addField("Total", "**" + Moeda.formatar(ticket.totalCentavos()) + "**", false).build();
    }

    public static MessageEmbed ticketDeDuvida(Ticket ticket) {
        return new EmbedBuilder()
                .setColor(AMARELO)
                .setTitle("❓ Atendimento #" + ticket.id())
                .setDescription("""
                        Ola! Este canal e privado entre voce e a equipe.

                        Escreva sua duvida aqui embaixo. Se a conversa virar uma compra, clique em **Comprar** \
                        que este mesmo atendimento passa a ser um pedido.""")
                .addField("Assunto", ticket.assunto(), false)
                .addField("Aberto em", Datas.formatar(ticket.abertoEm()), true)
                .setFooter("Use o botao Fechar quando terminarmos")
                .build();
    }

    public static MessageEmbed meusTickets(List<Ticket> tickets) {
        EmbedBuilder embed = new EmbedBuilder().setColor(AZUL).setTitle("🎫 Seus atendimentos");
        if (tickets.isEmpty()) {
            return embed.setDescription("Voce ainda nao abriu nenhum atendimento.\nUse `/duvida` para falar com a equipe.")
                    .build();
        }

        StringBuilder texto = new StringBuilder();
        tickets.stream().limit(10).forEach(ticket -> {
            texto.append(ticket.tipo().emoji()).append(" **#").append(ticket.id()).append("** ")
                    .append(ticket.tipo().rotulo()).append(" · ").append(ticket.status().rotulo());
            if (ticket.temPedido()) {
                texto.append(" · ").append(Moeda.formatar(ticket.totalCentavos()));
            }
            if (ticket.canalId() != null) {
                texto.append(" · <#").append(ticket.canalId()).append('>');
            }
            texto.append('\n');
        });

        return embed.setDescription(texto.toString())
                .setFooter("Mostrando os ultimos 10 atendimentos")
                .build();
    }

    public static MessageEmbed vendas(ResumoVendas resumo) {
        return new EmbedBuilder()
                .setColor(VERDE)
                .setTitle("📈 Resumo de vendas")
                .addField("Pedidos confirmados", String.valueOf(resumo.pedidosConfirmados()), true)
                .addField("Receita", Moeda.formatar(resumo.receitaCentavos()), true)
                .addField("Ticket medio", Moeda.formatar(resumo.ticketMedioCentavos()), true)
                .addField("Produto mais vendido", resumo.produtoMaisVendido(), false)
                .setFooter("Conta pedidos confirmados e fechados")
                .build();
    }

    public static MessageEmbed atendimentoEncerrado(Ticket ticket, String quemFechou) {
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(VERMELHO)
                .setTitle("🔒 Atendimento #" + ticket.id() + " encerrado")
                .setDescription("Este canal sera apagado em alguns segundos. Obrigado pelo contato!")
                .addField("Encerrado por", quemFechou, true);

        if (ticket.temPedido()) {
            embed.addField("Total do pedido", Moeda.formatar(ticket.totalCentavos()), true);
        }
        return embed.build();
    }
}

package dev.ticketshop.discord.listener;

import dev.ticketshop.discord.AberturaDeTicket;
import dev.ticketshop.discord.Autorizacao;
import dev.ticketshop.discord.Comandos;
import dev.ticketshop.discord.CustomId;
import dev.ticketshop.discord.TicketCanal;
import dev.ticketshop.discord.view.Botoes;
import dev.ticketshop.discord.view.Embeds;
import dev.ticketshop.discord.view.SeletorProdutos;
import dev.ticketshop.domain.ItemCarrinho;
import dev.ticketshop.domain.Produto;
import dev.ticketshop.domain.RegraDeNegocioException;
import dev.ticketshop.domain.Ticket;
import dev.ticketshop.service.CarrinhoService;
import dev.ticketshop.service.LojaService;
import dev.ticketshop.service.TicketService;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * O unico listener do bot: recebe tudo o que o usuario faz e despacha para o lugar certo.
 *
 * <p>Quatro tipos de interacao chegam aqui:
 * <ul>
 *   <li><b>slash command</b> — {@code /produtos}, {@code /duvida}...;</li>
 *   <li><b>botao</b> — cliques em Finalizar, Confirmar, Fechar;</li>
 *   <li><b>menu de selecao</b> — a escolha do produto;</li>
 *   <li><b>modal</b> — o formulario de observacoes.</li>
 * </ul>
 *
 * <p>Todas passam pelo metodo {@link #proteger}, que evita a pior experiencia possivel:
 * clicar num botao e nao acontecer nada porque o bot quebrou. Se algo falhar, o usuario
 * recebe uma mensagem e o erro completo vai para o log.
 */
public final class InteracaoListener extends ListenerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(InteracaoListener.class);
    private static final int MAXIMO_DE_OBSERVACOES = 500;

    private final Comandos comandos;
    private final LojaService loja;
    private final CarrinhoService carrinhos;
    private final TicketService tickets;
    private final AberturaDeTicket abertura;
    private final TicketCanal canais;
    private final Autorizacao autorizacao;

    public InteracaoListener(Comandos comandos,
                             LojaService loja,
                             CarrinhoService carrinhos,
                             TicketService tickets,
                             AberturaDeTicket abertura,
                             TicketCanal canais,
                             Autorizacao autorizacao) {
        this.comandos = comandos;
        this.loja = loja;
        this.carrinhos = carrinhos;
        this.tickets = tickets;
        this.abertura = abertura;
        this.canais = canais;
        this.autorizacao = autorizacao;
    }

    // ------------------------------------------------------------------ comandos

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent evento) {
        comandos.buscar(evento.getName()).ifPresentOrElse(
                comando -> proteger(() -> comando.executar(evento), evento),
                () -> responderErro(evento, "Comando desconhecido. O bot pode ter sido atualizado agora; tente de novo."));
    }

    // -------------------------------------------------------------------- botoes

    @Override
    public void onButtonInteraction(ButtonInteractionEvent evento) {
        proteger(() -> tratarBotao(evento), evento);
    }

    private void tratarBotao(ButtonInteractionEvent evento) {
        String id = evento.getComponentId();

        if (CustomId.eh(id, CustomId.CARRINHO_FINALIZAR)) {
            abrirModalDePedido(evento);
        } else if (CustomId.eh(id, CustomId.CARRINHO_LIMPAR)) {
            esvaziarCarrinho(evento);
        } else if (CustomId.eh(id, CustomId.TICKET_COMPRAR)) {
            publicarCatalogoNoCanal(evento);
        } else if (CustomId.eh(id, CustomId.TICKET_FECHAR)) {
            fecharAtendimento(evento, CustomId.extrairId(id));
        } else if (CustomId.eh(id, CustomId.PEDIDO_CONFIRMAR)) {
            confirmarPedido(evento, CustomId.extrairId(id));
        } else if (CustomId.eh(id, CustomId.PEDIDO_CANCELAR)) {
            cancelarPedido(evento, CustomId.extrairId(id));
        } else {
            LOG.warn("Clique em um botao sem tratamento: {}", id);
        }
    }

    private void abrirModalDePedido(ButtonInteractionEvent evento) {
        if (carrinhos.vazio(evento.getUser().getId())) {
            responderErro(evento, "Seu carrinho esta vazio. Use `/produtos` para escolher algo.");
            return;
        }

        // Modal = formulario. O texto digitado volta para nos em onModalInteraction.
        Modal modal = Modal.create(CustomId.MODAL_PEDIDO, "Finalizar pedido")
                .addComponents(Label.of("Observacoes (opcional)",
                        TextInput.create(CustomId.CAMPO_OBSERVACOES, TextInputStyle.PARAGRAPH)
                                .setPlaceholder("Forma de pagamento, prazo, endereco de entrega...")
                                .setRequired(false)
                                .setMaxLength(MAXIMO_DE_OBSERVACOES)
                                .build()))
                .build();

        evento.replyModal(modal).queue();
    }

    private void esvaziarCarrinho(ButtonInteractionEvent evento) {
        carrinhos.esvaziar(evento.getUser().getId());
        evento.editMessage(new MessageEditBuilder()
                .setEmbeds(List.of(Embeds.aviso("Carrinho esvaziado. Use `/produtos` para montar de novo.")))
                .setComponents(List.of())
                .build()).queue();
    }

    private void publicarCatalogoNoCanal(ButtonInteractionEvent evento) {
        List<Produto> produtos = loja.catalogo();
        if (produtos.isEmpty()) {
            responderErro(evento, "A loja ainda nao tem produtos cadastrados.");
            return;
        }

        evento.getChannel()
                .sendMessageEmbeds(Embeds.catalogo(produtos))
                .setComponents(SeletorProdutos.de(produtos))
                .queue();

        evento.replyEmbeds(Embeds.sucesso("Catalogo publicado aqui no canal. Escolha um produto no menu."))
                .setEphemeral(true)
                .queue();
    }

    private void fecharAtendimento(ButtonInteractionEvent evento, long ticketId) {
        Ticket ticket = tickets.buscar(ticketId);
        autorizacao.exigirDonoOuEquipe(evento.getMember(), ticket);

        evento.deferReply(true).queue();
        Ticket fechado = tickets.fechar(ticketId);

        if (evento.getChannel() instanceof TextChannel canal) {
            canais.encerrar(canal, Embeds.atendimentoEncerrado(fechado, evento.getUser().getEffectiveName()));
        }
        evento.getHook().sendMessageEmbeds(Embeds.sucesso("Atendimento #" + ticketId + " encerrado."))
                .setEphemeral(true)
                .queue();
    }

    private void confirmarPedido(ButtonInteractionEvent evento, long ticketId) {
        autorizacao.exigirEquipe(evento.getMember());

        Ticket confirmado = tickets.confirmar(ticketId);
        atualizarMensagemDoPedido(evento, confirmado);
        avisarNoCanal(evento, "Pedido #" + ticketId + " confirmado por " + evento.getMember().getAsMention() + ".");
    }

    private void cancelarPedido(ButtonInteractionEvent evento, long ticketId) {
        autorizacao.exigirEquipe(evento.getMember());

        Ticket cancelado = tickets.cancelar(ticketId);
        atualizarMensagemDoPedido(evento, cancelado);
        avisarNoCanal(evento, "Pedido #" + ticketId + " cancelado por " + evento.getMember().getAsMention() + ".");
    }

    /** Reescreve a mensagem do pedido com o status novo e sem os botoes (nao da para clicar duas vezes). */
    private void atualizarMensagemDoPedido(ButtonInteractionEvent evento, Ticket ticket) {
        evento.editMessage(new MessageEditBuilder()
                .setEmbeds(List.of(Embeds.resumoPedido(ticket, tickets.itens(ticket.id()))))
                .setComponents(List.of())
                .build()).queue();
    }

    private static void avisarNoCanal(ButtonInteractionEvent evento, String texto) {
        evento.getChannel().sendMessage(texto).queue();
    }

    // --------------------------------------------------------- menu de selecao

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent evento) {
        proteger(() -> {
            if (!CustomId.eh(evento.getComponentId(), CustomId.MENU_PRODUTOS)) {
                LOG.warn("Menu sem tratamento: {}", evento.getComponentId());
                return;
            }

            String usuarioId = evento.getUser().getId();
            long produtoId = lerIdDoProduto(evento.getValues().get(0));
            carrinhos.adicionar(usuarioId, produtoId, 1);

            List<ItemCarrinho> itens = carrinhos.itens(usuarioId);
            evento.replyEmbeds(Embeds.carrinho(itens, carrinhos.totalCentavos(usuarioId)))
                    .setComponents(Botoes.carrinho())
                    .setEphemeral(true)
                    .queue();
        }, evento);
    }

    private static long lerIdDoProduto(String valor) {
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException e) {
            throw new RegraDeNegocioException("Nao consegui identificar o produto escolhido. Tente de novo.");
        }
    }

    // ------------------------------------------------------------------- modal

    @Override
    public void onModalInteraction(ModalInteractionEvent evento) {
        proteger(() -> {
            if (!CustomId.MODAL_PEDIDO.equals(evento.getModalId())) {
                LOG.warn("Modal sem tratamento: {}", evento.getModalId());
                return;
            }

            String usuarioId = evento.getUser().getId();
            List<ItemCarrinho> itens = carrinhos.itens(usuarioId);
            if (itens.isEmpty()) {
                responderErro(evento, "Seu carrinho esta vazio. Use `/produtos` para escolher algo.");
                return;
            }

            ModalMapping campo = evento.getValue(CustomId.CAMPO_OBSERVACOES);
            String observacoes = campo == null ? "" : campo.getAsString();

            // A criacao do canal demora: avisamos o Discord que estamos trabalhando nisso.
            evento.deferReply(true).queue();

            // Se o cliente clicou em "Finalizar" dentro de um atendimento, o pedido entra
            // nele em vez de criar outro canal. Este e o fluxo "duvida que vira pedido".
            if (evento.getChannel() instanceof TextChannel canal) {
                Optional<Ticket> atendimento = tickets.porCanal(canal.getId());
                if (atendimento.isPresent() && atendimento.get().estaAberto()) {
                    abertura.anexarPedidoAoAtendimento(canal, atendimento.get(), itens, observacoes, evento.getHook());
                    carrinhos.esvaziar(usuarioId);
                    return;
                }
            }

            Guild guild = evento.getGuild();
            if (guild == null || evento.getMember() == null) {
                responderErro(evento, "Use este bot dentro de um servidor.");
                return;
            }

            abertura.abrirPedido(guild, evento.getMember(), itens, observacoes, evento.getHook());
            carrinhos.esvaziar(usuarioId);
        }, evento);
    }

    // ------------------------------------------------------------------ apoio

    /**
     * Executa a acao e garante que qualquer falha vire uma resposta para o usuario.
     *
     * <p>{@link RegraDeNegocioException} e um erro "esperado": mostramos a mensagem exata.
     * Qualquer outra excecao e bug: mostramos um texto generico e guardamos o detalhe no log.
     */
    private void proteger(Runnable acao, IReplyCallback interacao) {
        try {
            acao.run();
        } catch (RegraDeNegocioException e) {
            responderErro(interacao, e.getMessage());
        } catch (Exception e) {
            LOG.error("Erro inesperado ao tratar uma interacao.", e);
            responderErro(interacao, "Algo deu errado do nosso lado. Tente novamente em instantes.");
        }
    }

    /**
     * Responde com um embed de erro, seja qual for o momento da interacao.
     * Se o bot ja tinha respondido, a mensagem vira um aviso extra pelo {@code hook}.
     */
    private static void responderErro(IReplyCallback interacao, String mensagem) {
        MessageEmbed embed = Embeds.erro(mensagem == null ? "Nao consegui completar a acao." : mensagem);
        if (interacao.isAcknowledged()) {
            interacao.getHook().sendMessageEmbeds(embed).setEphemeral(true).queue();
        } else {
            interacao.replyEmbeds(embed).setEphemeral(true).queue();
        }
    }
}

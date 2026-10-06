package dev.ticketshop.discord.command;

import dev.ticketshop.discord.Autorizacao;
import dev.ticketshop.discord.view.Embeds;
import dev.ticketshop.domain.Produto;
import dev.ticketshop.service.LojaService;
import dev.ticketshop.util.Moeda;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.List;

/**
 * {@code /produto} — administracao do catalogo.
 *
 * <p>Duas travas de seguranca:
 * <ol>
 *   <li>{@code setDefaultPermissions} esconde o comando de quem nao pode usar;</li>
 *   <li>{@code exigirEquipe} confere de novo na hora de executar.</li>
 * </ol>
 * A primeira e conveniencia, a segunda e seguranca. Nunca confie so na primeira.
 */
public final class ProdutoAdminCommand implements SlashCommand {

    private final LojaService loja;
    private final Autorizacao autorizacao;

    public ProdutoAdminCommand(LojaService loja, Autorizacao autorizacao) {
        this.loja = loja;
        this.autorizacao = autorizacao;
    }

    @Override
    public String nome() {
        return "produto";
    }

    @Override
    public SlashCommandData dados() {
        return Commands.slash("produto", "Gerencia o catalogo da loja")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL))
                .addSubcommands(
                        new SubcommandData("adicionar", "Cadastra um produto novo")
                                .addOption(OptionType.STRING, "nome", "Nome do produto", true)
                                .addOption(OptionType.STRING, "preco", "Preco, por exemplo 49,90", true)
                                .addOption(OptionType.STRING, "descricao", "Descricao curta (opcional)", false),
                        new SubcommandData("remover", "Tira um produto da vitrine")
                                .addOption(OptionType.STRING, "nome", "Nome do produto", true),
                        new SubcommandData("listar", "Mostra todos os produtos cadastrados"));
    }

    @Override
    public void executar(SlashCommandInteractionEvent evento) {
        autorizacao.exigirEquipe(evento.getMember());

        String subcomando = evento.getSubcommandName();
        if (subcomando == null) {
            evento.replyEmbeds(Embeds.erro("Escolha um subcomando.")).setEphemeral(true).queue();
            return;
        }

        switch (subcomando) {
            case "adicionar" -> adicionar(evento);
            case "remover" -> remover(evento);
            case "listar" -> listar(evento);
            default -> evento.replyEmbeds(Embeds.erro("Subcomando desconhecido: " + subcomando))
                    .setEphemeral(true)
                    .queue();
        }
    }

    private void adicionar(SlashCommandInteractionEvent evento) {
        String nome = evento.getOption("nome", OptionMapping::getAsString);
        String precoTexto = evento.getOption("preco", OptionMapping::getAsString);
        String descricao = evento.getOption("descricao", "", OptionMapping::getAsString);

        Produto produto = loja.cadastrar(nome, descricao, Moeda.paraCentavos(precoTexto));
        evento.replyEmbeds(Embeds.sucesso("Produto **" + produto.nome() + "** cadastrado por "
                        + Moeda.formatar(produto.precoCentavos()) + "."))
                .setEphemeral(true)
                .queue();
    }

    private void remover(SlashCommandInteractionEvent evento) {
        String nome = evento.getOption("nome", OptionMapping::getAsString);
        Produto removido = loja.desativar(nome);
        evento.replyEmbeds(Embeds.sucesso("**" + removido.nome() + "** saiu da vitrine. "
                        + "Os pedidos antigos continuam registrados."))
                .setEphemeral(true)
                .queue();
    }

    private void listar(SlashCommandInteractionEvent evento) {
        List<Produto> produtos = loja.todos();
        evento.replyEmbeds(Embeds.listaCompleta(produtos)).setEphemeral(true).queue();
    }
}

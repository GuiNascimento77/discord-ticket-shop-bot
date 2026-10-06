package dev.ticketshop.discord;

import dev.ticketshop.config.Config;
import dev.ticketshop.domain.RegraDeNegocioException;
import dev.ticketshop.domain.Ticket;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

import java.util.Optional;

/**
 * Quem pode fazer o que.
 *
 * <p>Regra: <b>equipe</b> e quem tem o cargo configurado em {@code STAFF_ROLE_ID} ou a
 * permissao "Gerenciar Canais". Um cliente so mexe no proprio atendimento.
 *
 * <p>Verificacao de permissao sempre do lado do servidor. Nunca confie que o usuario so
 * vai clicar nos botoes que voce mostrou para ele.
 */
public final class Autorizacao {

    private final Optional<String> cargoDaEquipe;

    public Autorizacao(Config config) {
        this.cargoDaEquipe = config.staffRoleId();
    }

    public boolean ehEquipe(Member membro) {
        if (membro == null) {
            return false;
        }
        boolean temCargo = cargoDaEquipe
                .map(id -> membro.getRoles().stream().anyMatch(cargo -> cargo.getId().equals(id)))
                .orElse(false);
        return temCargo || membro.hasPermission(Permission.MANAGE_CHANNEL);
    }

    public boolean ehDono(Member membro, Ticket ticket) {
        return membro != null && membro.getId().equals(ticket.donoId());
    }

    public void exigirEquipe(Member membro) {
        if (!ehEquipe(membro)) {
            throw new RegraDeNegocioException("Esse comando e apenas para a equipe.");
        }
    }

    public void exigirDonoOuEquipe(Member membro, Ticket ticket) {
        if (!ehDono(membro, ticket) && !ehEquipe(membro)) {
            throw new RegraDeNegocioException("Este atendimento nao e seu.");
        }
    }
}

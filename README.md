# 🤖 Bot de Vendas e Tickets para Discord

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9+-blue?logo=apachemaven&logoColor=white)
![JDA](https://img.shields.io/badge/JDA-6.7.0-5865F2?logo=discord&logoColor=white)
![SQLite](https://img.shields.io/badge/SQLite-JDBC-003B57?logo=sqlite&logoColor=white)
![Licença](https://img.shields.io/badge/licen%C3%A7a-MIT-green)

Um bot completo de **atendimento e vendas** para servidores Discord, escrito em **Java 21**.

Ele resolve um problema que qualquer loja pequena no Discord tem: o cliente chama no privado, a
mensagem se perde no meio de outras, ninguém sabe quem já pagou e no fim do mês ninguém sabe
quanto vendeu. Aqui, cada conversa vira um **canal privado** com histórico, cada pedido vira um
**registro no banco** e a equipe tem um comando para ver **quanto vendeu**.

> Feito como projeto de estudo de Java: **orientação a objetos, camadas, JDBC, testes com JUnit,
> tratamento de exceções e integração com API externa (Discord)** — sem framework mágico, tudo
> explícito e comentado em português.

---

## 📑 Índice

- [O que o bot faz](#-o-que-o-bot-faz)
- [Demonstração sem Discord](#-demonstração-sem-discord)
- [Requisitos](#-requisitos)
- [Passo 1 — Instalar o Java 21 e o Maven](#-passo-1--instalar-o-java-21-e-o-maven)
- [Passo 2 — Criar o bot no Discord Developer Portal](#-passo-2--criar-o-bot-no-discord-developer-portal)
- [Passo 3 — Configurar o `.env`](#-passo-3--configurar-o-env)
- [Passo 4 — Pegar os IDs do servidor](#-passo-4--pegar-os-ids-do-servidor)
- [Passo 5 — Compilar e rodar](#-passo-5--compilar-e-rodar)
- [Comandos](#-comandos)
- [Como o fluxo funciona](#-como-o-fluxo-funciona)
- [Arquitetura](#-arquitetura)
- [Banco de dados](#-banco-de-dados)
- [Testes](#-testes)
- [Hospedagem (deixar o bot 24h online)](#-hospedagem-deixar-o-bot-24h-online)
- [Problemas comuns](#-problemas-comuns)
- [Próximos passos](#-próximos-passos)

---

## ✨ O que o bot faz

| Recurso | Como aparece no Discord |
|---|---|
| **Catálogo** | `/produtos` mostra os itens num embed e num menu de seleção |
| **Carrinho** | `/carrinho` mostra itens, subtotal e botões *Finalizar* / *Esvaziar* |
| **Pedido** | Ao finalizar, abre um **formulário (modal)** para observações e cria um canal privado |
| **Ticket de dúvida** | `/duvida assunto:...` abre um atendimento privado com a equipe |
| **Ticket que vira pedido** | Dentro de uma dúvida, o botão **Comprar** publica o catálogo ali mesmo — sem abrir outro canal |
| **Confirmação da equipe** | Botões **Confirmar** / **Cancelar** que só a equipe vê e consegue clicar |
| **Encerramento** | `/fechar` apaga o canal 6 segundos depois de avisar |
| **Histórico** | `/meus-tickets` lista os atendimentos e pedidos do usuário |
| **Painel de vendas** | `/vendas` mostra pedidos confirmados, receita, ticket médio e produto mais vendido |
| **Gestão do catálogo** | `/produto adicionar`, `/produto remover`, `/produto listar` |

**Detalhes que fazem diferença:**

- O canal do ticket nasce **trancado**: `@everyone` não vê nada; só o cliente e o cargo da equipe.
- **Preço congelado**: o item do pedido guarda uma cópia do nome e do preço. Se a equipe mudar o
  preço depois, o pedido antigo continua com o valor de quando foi feito.
- **Dinheiro em centavos (`long`)**, nunca `double` — sem erro de arredondamento.
- Toda permissão é checada **no servidor**. Não adianta forjar um clique em botão que não é seu.
- Se algo der errado, o usuário vê uma mensagem amigável e o erro completo vai para o log — o
  bot **nunca** fica mudo num clique.

---

## 🎬 Demonstração sem Discord

O projeto tem um modo de demonstração que roda **sem token, sem internet e sem Discord**. Ele
executa o fluxo de negócio de verdade (com banco SQLite em memória) e imprime o resultado:

```bash
mvn package
java -jar target/discord-ticket-shop-bot.jar --demo
```

```
================================================================
 DEMONSTRACAO OFFLINE - nenhuma conexao com o Discord e necessaria
================================================================

================================================================
 1) A equipe cadastra produtos
================================================================
   #1  Cafe especial 250g     R$ 39,90
   #2  Caneca da loja         R$ 24,50
   #3  Kit degustacao         R$ 59,90

================================================================
 2) O cliente monta o carrinho (em memoria, nada foi salvo ainda)
================================================================
   Cafe especial 250g     x3   R$ 119,70
   Caneca da loja         x1   R$ 24,50
   ---------------------------------------------
   TOTAL                      R$ 144,20

================================================================
 3) O atendimento nasce como duvida e vira pedido
================================================================
   Ticket #1 aberto (Duvida) - status: Aberto
   Ticket #1 convertido em Pedido - total: R$ 144,20
      Cafe especial 250g     x3   R$ 119,70
      Caneca da loja         x1   R$ 24,50
   A equipe confirmou o pedido: status agora e Confirmado
   Canal encerrado: status final Fechado

================================================================
 4) A equipe confere os numeros com /vendas
================================================================
   Pedidos confirmados : 1
   Receita             : R$ 144,20
   Ticket medio        : R$ 144,20
   Produto mais vendido: Cafe especial 250g (3 un.)
```

É o jeito mais rápido de entender o projeto sem configurar nada — e é exatamente isso que a
integração contínua roda a cada `push` (veja `.github/workflows/ci.yml`).

---

## 📋 Requisitos

| Ferramenta | Versão | Para quê |
|---|---|---|
| **JDK** | 21 (LTS) | compilar e rodar o bot |
| **Maven** | 3.9 ou mais novo | baixar dependências e empacotar |
| **Conta no Discord** | — | criar a aplicação e testar o bot |

> Usa Java 17? Troque `<maven.compiler.release>21</maven.compiler.release>` por `17` no `pom.xml`.
> O JDA 6 roda em Java 8+, mas o projeto usa `record`, `switch` expression e *text blocks*, que
> exigem pelo menos Java 17.

---

## ☕ Passo 1 — Instalar o Java 21 e o Maven

Confira se já tem:

```bash
java -version     # precisa dizer 21.x
mvn -v            # precisa dizer Apache Maven 3.9.x
```

Se algum dos dois der "command not found", siga a sua plataforma.

<details>
<summary><b>Windows</b></summary>

Com o **winget** (vem no Windows 10/11) é uma linha:

```powershell
winget install Microsoft.OpenJDK.21
winget install Apache.Maven
```

Depois **feche e abra o terminal de novo** e confira com `java -version`.

Se preferir instalar na mão:

1. Baixe o **OpenJDK 21** em <https://adoptium.net/temurin/releases/?version=21> (arquivo `.msi`).
2. Baixe o **Maven** em <https://maven.apache.org/download.cgi> (arquivo `-bin.zip`) e
   descompacte em `C:\Programas\apache-maven`.
3. Em *Variáveis de Ambiente* → *Path*, adicione `C:\Programas\apache-maven\bin`.
4. Confira numa janela nova: `java -version` e `mvn -v`.

</details>

<details>
<summary><b>Linux (Debian / Ubuntu / Mint)</b></summary>

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk maven
java -version
mvn -v
```

Fedora / RHEL:

```bash
sudo dnf install -y java-21-openjdk-devel maven
```

</details>

<details>
<summary><b>macOS</b></summary>

Com o [Homebrew](https://brew.sh):

```bash
brew install openjdk@21 maven
```

Se o `java -version` continuar apontando para uma versão antiga, siga as instruções que o próprio
Homebrew imprime ao final da instalação (adicionar o `openjdk@21` ao `PATH` e criar o link
simbólico em `/Library/Java/JavaVirtualMachines`).

</details>

---

## 🔧 Passo 2 — Criar o bot no Discord Developer Portal

Essa parte é toda pelo site. Leva uns 5 minutos.

### 2.1 Criar a aplicação

1. Abra <https://discord.com/developers/applications> e faça login.
2. Clique em **New Application** (canto superior direito).
3. Dê um nome (é o nome que vai aparecer no Discord) e aceite os termos.

### 2.2 Criar o bot e copiar o token

1. No menu da esquerda, clique em **Bot**.
2. Clique em **Add Bot** → **Yes, do it!**.
3. Em **Token**, clique em **Reset Token** → **Copy**.

> ⚠️ **O token é a senha do seu bot.** Quem tem o token controla o bot. Nunca cole ele em
> print, em mensagem, em vídeo ou no GitHub. O `.gitignore` deste projeto já bloqueia o `.env`.
> Se vazar, clique em *Reset Token* de novo — o antigo para de funcionar na hora.

### 2.3 Intents (o que o bot pode "ouvir")

**Este bot não precisa de nenhum intent privilegiado.** Ele vive de *slash commands*, botões,
menus e modais — tudo isso chega pelas interações, que são liberadas por padrão.

Deixe as três opções de **Privileged Gateway Intents** desligadas. Só ligue depois, se você for
adicionar recursos que precisem delas:

| Intent | Quando ligar |
|---|---|
| **Server Members Intent** | se quiser ler a lista de membros ou detectar quem entrou/saiu |
| **Message Content Intent** | se quiser ler o **conteúdo** de mensagens normais (não é o caso aqui) |
| **Presence Intent** | se quiser saber o status/jogo dos membros |

### 2.4 Deixar o bot público (opcional)

Em **Bot → Public Bot**, deixe **ligado** para poder convidar o bot em qualquer servidor seu.
Para um bot de uso pessoal, tanto faz.

### 2.5 Convidar o bot para o seu servidor

Vá em **OAuth2 → URL Generator** e marque:

- **Scopes:** `bot` e `applications.commands`
  *(o segundo é obrigatório para os comandos `/` aparecerem)*
- **Bot Permissions:**
  - ✅ View Channels
  - ✅ Manage Channels — *criar e apagar os canais de ticket*
  - ✅ Send Messages
  - ✅ Embed Links
  - ✅ Attach Files
  - ✅ Read Message History
  - ✅ Manage Messages — *opcional, útil se a equipe quiser limpar o canal*

Ou monte a URL na mão, trocando `SEU_CLIENT_ID` pelo **Application ID** que aparece em
*General Information*:

```
https://discord.com/api/oauth2/authorize?client_id=SEU_CLIENT_ID&permissions=125968&scope=bot%20applications.commands
```

Cole no navegador, escolha o servidor e autorize. O bot vai aparecer offline — normal, ele ainda
não está rodando.

---

## ⚙️ Passo 3 — Configurar o `.env`

Na raiz do projeto:

```bash
cp .env.example .env      # Windows: copy .env.example .env
```

Abra o `.env` e preencha:

| Variável | Obrigatória | O que é |
|---|---|---|
| `DISCORD_TOKEN` | ✅ | o token copiado no passo 2.2 |
| `GUILD_ID` | recomendado | ID do seu servidor. Com ele, os comandos `/` aparecem **na hora** |
| `STAFF_ROLE_ID` | recomendado | cargo que atende os tickets (ex.: `Equipe`). Recebe acesso aos canais |
| `CATEGORIA_ID` | opcional | categoria onde os canais de ticket são criados (mantém o servidor organizado) |
| `DB_PATH` | opcional | caminho do banco. Padrão: `dados/bot.db` |

Sem `GUILD_ID`, os comandos viram **globais** e podem levar até **1 hora** para aparecer.
Durante o desenvolvimento, sempre preencha o `GUILD_ID`.

> **Não quer usar arquivo `.env`?** Sem problema: o bot também lê as **variáveis de ambiente do
> sistema**, e elas têm prioridade sobre o arquivo. É o que você vai usar numa hospedagem.

---

## 🆔 Passo 4 — Pegar os IDs do servidor

### 4.1 Ligar o Modo de desenvolvedor

1. No Discord (app de desktop ou navegador — no celular não tem essa opção), clique na
   **engrenagem ⚙️** no canto inferior esquerdo, ao lado do seu nome.
2. Role até **Avançado** → ligue o **Modo de desenvolvedor**.
3. Feche as configurações.

Agora **clique com o botão direito** em qualquer coisa e vai aparecer **Copiar ID**:

| O que copiar | Onde clicar com o botão direito |
|---|---|
| `GUILD_ID` | no ícone do **servidor** |
| `STAFF_ROLE_ID` | em **Configurações do servidor → Cargos →** no cargo da equipe |
| `CATEGORIA_ID` | na **categoria** onde os tickets devem nascer |

### 4.2 Não achou o Modo de desenvolvedor? Use a URL

Dá para pegar o `GUILD_ID` sem ligar nada, pelo Discord no navegador:

1. Abra <https://discord.com/app> e entre no seu servidor.
2. Olhe a barra de endereços:

```
https://discord.com/channels/123456789012345678/987654321098765432
                            └──── este é o GUILD_ID ────┘
```

O **primeiro** número é o ID do servidor. O segundo é o do canal onde você está.

O mesmo vale para o `CATEGORIA_ID`: clique em um canal que esteja dentro da categoria, e o
segundo número da URL é o ID do canal — não da categoria. Para a categoria, o caminho fácil
continua sendo o botão direito.

> Dica: crie antes uma categoria chamada **Atendimentos** e um cargo **Equipe**. Assim você já
> tem os dois IDs na mão.

### 4.3 Não esqueça de convidar o bot

Os IDs não servem de nada se o bot não estiver **dentro** do servidor. Se ele ainda não aparece
na lista de membros, volte ao [passo 2.5](#25-convidar-o-bot-para-o-seu-servidor).

---

## 🚀 Passo 5 — Compilar e rodar

```bash
# 1. Compila, roda os testes e gera o jar
mvn package

# 2. Liga o bot
java -jar target/discord-ticket-shop-bot.jar
```

Saída esperada:

```
21:40:12 INFO  dev.ticketshop.Bot - 8 comandos registrados no servidor "Meu Servidor".
21:40:12 INFO  dev.ticketshop.Bot - Bot conectado como "Loja". Ja pode usar os comandos no Discord.
```

Vá no Discord e digite `/` — os comandos aparecem. Comece por `/produto adicionar` para cadastrar
alguma coisa e depois `/produtos` para testar como cliente.

Outros comandos úteis:

```bash
java -jar target/discord-ticket-shop-bot.jar --demo    # demonstração offline
mvn test                                               # só os testes
mvn -q package -DskipTests                             # empacotar sem rodar testes
```

> O `target/discord-ticket-shop-bot.jar` tem ~33 MB porque é um **fat jar**: ele carrega o bot
> **e** todas as dependências (JDA, driver do SQLite, logback) num arquivo só. Você copia esse
> único arquivo para qualquer máquina com Java 21 e ele roda.

---

## 🎮 Comandos

### Para todo mundo

| Comando | O que faz |
|---|---|
| `/ajuda` | explica o bot (a parte da equipe só aparece para a equipe) |
| `/produtos` | mostra o catálogo com um menu para adicionar ao carrinho |
| `/carrinho` | mostra o carrinho e os botões de finalizar/esvaziar |
| `/duvida assunto:<texto>` | abre um atendimento privado com a equipe |
| `/fechar` | encerra o atendimento do canal onde foi usado |
| `/meus-tickets` | lista os seus atendimentos e pedidos |

### Para a equipe

| Comando | O que faz |
|---|---|
| `/produto adicionar nome:<texto> preco:<valor> [descricao]` | cadastra um produto (`preco` aceita `49,90`, `49.90`, `R$ 49,90`) |
| `/produto remover nome:<texto>` | tira o produto da vitrine (o histórico de pedidos continua intacto) |
| `/produto listar` | lista tudo o que está cadastrado |
| `/vendas` | pedidos confirmados, receita, ticket médio e produto mais vendido |

**Quem é "equipe"?** Quem tem o cargo configurado em `STAFF_ROLE_ID` **ou** a permissão
*Gerenciar Canais*. Sem `STAFF_ROLE_ID`, o critério passa a ser só a permissão.

---

## 🔄 Como o fluxo funciona

```
CLIENTE                          BOT                            EQUIPE
   │                              │                               │
   ├─ /produtos ─────────────────▶│                               │
   │◀── catálogo + menu ──────────┤                               │
   │                              │                               │
   ├─ escolhe "Café 250g" ───────▶│  guarda no carrinho           │
   ├─ /carrinho ─────────────────▶│                               │
   │◀── itens + TOTAL ────────────┤                               │
   │                              │                               │
   ├─ [Finalizar] ───────────────▶│                               │
   │◀── formulário de observações ┤                               │
   ├─ escreve e envia ───────────▶│                               │
   │                              ├─ cria canal #pedido-cliente ─▶│
   │                              │  (privado: só cliente+equipe) │
   │◀── resumo do pedido ─────────┤                               │
   │                              │◀──── [Confirmar] ─────────────┤
   │                              ├─ status: Confirmado ─────────▶│
   │                              │                               │
   ├─ /fechar ───────────────────▶│                               │
   │◀── "canal apagado em 6s" ────┤                               │
```

E o **fluxo híbrido**, que é o diferencial do projeto:

```
   ├─ /duvida assunto:"tem café?"▶│
   │◀── canal #duvida-cliente ────┤  (ticket nasce como DÚVIDA)
   │                              │
   ├─ [Comprar] ─────────────────▶│  publica o catálogo NO MESMO canal
   ├─ escolhe + finaliza ────────▶│
   │                              ├─ o MESMO ticket vira PEDIDO
   │                              │  (nenhum canal novo é criado)
```

Ou seja: o cliente não precisa abrir um segundo canal no meio da conversa. A dúvida e a venda
acontecem no mesmo lugar, e o histórico fica inteiro.

**Ciclo de vida de um ticket:**

```
ABERTO ──(equipe confirma)──▶ CONFIRMADO ──(/fechar)──▶ FECHADO
   │                                 │
   └──────(equipe cancela)───────────┴──────────────▶ CANCELADO
```

Só `CONFIRMADO` e `FECHADO` contam como venda no `/vendas`.

---

## 🏗️ Arquitetura

O projeto é dividido em camadas, e a regra de ouro é: **as regras de negócio não conhecem o
Discord**. Isso significa que dá para testar tudo sem token, sem internet e sem servidor.

```
Discord (JDA)  ──▶  service  ──▶  data (SQLite/JDBC)
   comandos,           regras        repositórios
   botões,             de           (interfaces +
   modais,             negócio      implementação SQLite)
   embeds
```

```
src/main/java/dev/ticketshop/
├── Main.java                  ponto de entrada (--demo / --offline)
├── Bot.java                   monta as peças e liga o bot (o "wiring" de tudo)
├── Demo.java                  demonstração offline, sem Discord
│
├── config/                    Config.java, Env.java
│                              leitura do .env + validação da configuração
│
├── domain/                    o coração, sem dependência nenhuma
│   ├── Produto.java           record: id, nome, descrição, preço em centavos
│   ├── ItemCarrinho.java      record: produto + quantidade
│   ├── ItemPedido.java        record: cópia do nome/preço no momento da compra
│   ├── Ticket.java            record: atendimento (dúvida ou pedido)
│   ├── TipoTicket.java        enum DUVIDA / PEDIDO (com código, rótulo e emoji)
│   ├── StatusTicket.java      enum ABERTO / CONFIRMADO / FECHADO / CANCELADO
│   ├── ResumoVendas.java      record: números do painel /vendas
│   └── RegraDeNegocioException.java
│                              erro esperado (mostra a mensagem para o usuário)
│
├── data/                      persistência
│   ├── Database.java          conexão JDBC, esquema, transações, trava de thread
│   ├── ProdutoRepository.java      interface
│   ├── TicketRepository.java       interface
│   ├── SqliteProdutoRepository.java   implementação SQLite
│   ├── SqliteTicketRepository.java    implementação SQLite
│   ├── FuncaoSQL.java / AcaoSQL.java  interfaces funcionais (menos try/catch repetido)
│   └── ErroBancoException.java
│
├── service/                   regras de negócio, testáveis sem Discord
│   ├── LojaService.java       catálogo: cadastrar, remover, listar
│   ├── CarrinhoService.java   carrinho em memória por usuário
│   └── TicketService.java     abrir, converter em pedido, confirmar, cancelar, fechar, resumo
│
├── util/                      Moeda.java (centavos ↔ "R$ 49,90"), Datas.java
│
└── discord/                   a única camada que fala com o JDA
    ├── Comandos.java          índice de comandos + registro no Discord
    ├── CustomId.java          os ids de botões/menus/modais, num lugar só
    ├── Autorizacao.java       quem pode o quê
    ├── TicketCanal.java       cria/tranca/apaga os canais de atendimento
    ├── AberturaDeTicket.java  orquestra a abertura e a conversão de um ticket
    ├── command/               um arquivo por comando (padrão Command)
    ├── listener/InteracaoListener.java   recebe tudo e despacha
    └── view/                  Embeds.java, Botoes.java, SeletorProdutos.java
```

### Decisões de projeto (e o porquê)

| Decisão | Motivo |
|---|---|
| **Dinheiro em `long` de centavos** | `double` erra arredondamento. `0.1 + 0.2 != 0.3` e uma loja não pode viver com isso. |
| **`ItemPedido` guarda o preço** | Se o preço mudar amanhã, o pedido de ontem continua com o valor correto. É um *snapshot*. |
| **Repositório é interface** | `LojaService` recebe `ProdutoRepository`, não SQLite. Trocar por Postgres mexe em `Bot.java` e mais nada. |
| **`service` sem JDA** | É o que permite 30 testes rodando em 0,5 s sem token nem internet. |
| **`CustomId` centralizado** | O bug clássico é digitar `"carrinho:fin"` na criação e `"carrinho:finalizar"` no handler. Aqui isso não acontece. |
| **`RegraDeNegocioException` separada** | Erro esperado ("carrinho vazio") mostra a mensagem ao usuário; erro inesperado vira log + mensagem genérica. |
| **Transações no `Database`** | Converter um ticket em pedido grava o ticket **e** os itens: ou grava tudo, ou não grava nada. |

---

## 🗄️ Banco de dados

SQLite em arquivo único (`dados/bot.db` por padrão) — zero instalação, zero servidor. O esquema é
criado automaticamente na primeira execução.

```sql
produtos (id, nome UNIQUE COLLATE NOCASE, descricao, preco_centavos, ativo, criado_em)

tickets (id, canal_id, dono_id, tipo, status, assunto,
         total_centavos, aberto_em, fechado_em)

itens_pedido (id, ticket_id → tickets(id) ON DELETE CASCADE,
              produto_id → produtos(id) ON DELETE SET NULL,
              nome_produto, preco_centavos, quantidade)
```

Dois detalhes que valem nota:

- `ON DELETE CASCADE` nos itens: apagar um ticket apaga os itens junto, sem sobrar lixo.
- `ON DELETE SET NULL` no produto: remover um produto da vitrine **não** apaga o histórico de
  vendas — o item continua com o nome e o preço que foram gravados.

Para inspecionar o banco na mão (opcional):

```bash
sqlite3 dados/bot.db ".tables"
sqlite3 dados/bot.db "SELECT id, nome, preco_centavos FROM produtos;"
```

---

## ✅ Testes

```bash
mvn test
```

```
Tests run: 39, Failures: 0, Errors: 0, Skipped: 0
```

| Arquivo | Testes | Cobre |
|---|---|---|
| `LojaServiceTest` | 6 | cadastro, nome duplicado, preço inválido, remoção, listagem |
| `CarrinhoServiceTest` | 7 | soma de quantidade do mesmo item, limite de 99, esvaziar, total |
| `TicketServiceTest` | 10 | abrir, converter dúvida em pedido, confirmar/cancelar, transições inválidas, resumo |
| `MoedaTest` | 7 | formatação pt-BR, leitura de `49,90`/`49.90`/`R$ 1.234,56`, arredondamento, erros |
| `ConfigTest` | 9 | leitura do `.env`, caminho padrão do banco, opcionais vazios, token faltando |

Os testes usam **SQLite em memória** (`jdbc:sqlite::memory:`), então rodam em menos de um segundo
e não sujam o seu disco. Também servem de documentação: se você quer entender a regra de negócio,
leia os testes antes do código.

> O `ConfigTest` nasceu de um bug de verdade: na primeira vez que o bot foi ligado com um `.env`
> contendo só o token, ele caiu com `NullPointerException` antes de conectar. O caminho do banco
> era lido com `getOrDefault` na condição e `get` no ramo do ternário — o primeiro achava o valor
> padrão, o segundo devolvia `null`. Está em `Config.carregar`. É o tipo de erro que só aparece
> quando você **roda** o programa, não quando você lê o código.

O projeto tem **integração contínua** configurada em `.github/workflows/ci.yml`: a cada `push` ou
*pull request*, o GitHub instala o Java 21, roda `mvn verify` e executa a demonstração offline.

---

## ☁️ Hospedagem (deixar o bot 24h online)

Se o bot roda no seu computador, ele cai quando você desliga o PC. Para ficar online de verdade,
escolha uma das opções:

<details>
<summary><b>Railway / Render (mais fácil, plano gratuito)</b></summary>

1. Suba o projeto para o GitHub.
2. No painel da plataforma, crie um novo serviço apontando para o repositório.
3. Configure o build como `mvn -B package -DskipTests` e o start como
   `java -jar target/discord-ticket-shop-bot.jar`.
4. Em **Variáveis de ambiente**, cadastre `DISCORD_TOKEN`, `GUILD_ID`, `STAFF_ROLE_ID` e
   `CATEGORIA_ID`. **Não** suba o `.env` — o bot lê as variáveis do sistema.
5. Atenção ao **disco**: em planos gratuitos o sistema de arquivos é apagado a cada deploy, o que
   apaga o `dados/bot.db`. Monte um **volume persistente** e aponte `DB_PATH` para dentro dele.

</details>

<details>
<summary><b>VPS Linux (mais controle)</b></summary>

```bash
# 1. Instale o Java na VPS
sudo apt update && sudo apt install -y openjdk-21-jre-headless

# 2. Copie o jar e crie a pasta do banco
sudo mkdir -p /opt/ticket-shop/dados
sudo cp discord-ticket-shop-bot.jar /opt/ticket-shop/

# 3. Teste rodando na mão
cd /opt/ticket-shop
DISCORD_TOKEN="seu-token" java -jar discord-ticket-shop-bot.jar
```

Funcionou? Agora deixe como serviço, para voltar sozinho se cair:

```ini
# /etc/systemd/system/ticket-shop.service
[Unit]
Description=Bot de tickets e vendas
After=network-online.target
Wants=network-online.target

[Service]
User=ticketshop
WorkingDirectory=/opt/ticket-shop
EnvironmentFile=/opt/ticket-shop/.env
ExecStart=/usr/bin/java -jar /opt/ticket-shop/discord-ticket-shop-bot.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now ticket-shop
sudo systemctl status ticket-shop     # ver se subiu
journalctl -u ticket-shop -f          # acompanhar o log
```

Nesse caso o `.env` fica em `/opt/ticket-shop/.env`, com permissão restrita:

```bash
sudo chmod 600 /opt/ticket-shop/.env
```

</details>

---

## 🧯 Problemas comuns

| Sintoma | Causa e solução |
|---|---|
| `DISCORD_TOKEN nao configurado.` | falta o `.env` (ou o token está com o texto de exemplo). Rode `cp .env.example .env` e cole o token. |
| Os comandos `/` não aparecem | faltou o escopo `applications.commands` no convite, **ou** você não preencheu o `GUILD_ID`. Sem ele, comandos globais podem levar até 1 hora. |
| `Missing Permissions` ao abrir ticket | o bot precisa de **Gerenciar Canais**. Reconvide-o com a URL do passo 2.5. |
| `UnsupportedClassVersionError` | você está rodando com Java antigo. Rode `java -version` e instale o JDK 21. |
| Acentos aparecem quebrados no terminal (Windows) | rode `chcp 65001` antes do `java -jar`, ou use o Windows Terminal. |
| O bot cai quando fecho o terminal | é assim mesmo: processo em primeiro plano. Veja [Hospedagem](#-hospedagem-deixar-o-bot-24h-online) ou rode dentro de um `tmux`. |
| Perdi os pedidos depois de reiniciar | confira o `DB_PATH`. Em hospedagens com disco volátil, o banco precisa de um volume persistente. |
| `Bot conectado`, mas ninguém vê o canal | o bot precisa de **Gerenciar Canais** e a categoria configurada precisa permitir a criação. |

Para ver mais detalhes do que está acontecendo, aumente o nível do log em
`src/main/resources/logback.xml` (troque `INFO` por `DEBUG`).

---

## 🔭 Próximos passos

Ideias para evoluir o projeto (boas para praticar):

- [ ] **Pagamento por PIX** — gerar o QR Code e confirmar o pedido sozinho via webhook
- [ ] **Transcript do atendimento** — salvar o histórico do canal num arquivo antes de apagá-lo
- [ ] **Avaliação pós-atendimento** — botão de ⭐ dentro do ticket
- [ ] **Painel web** — um sitezinho para a equipe ver as vendas em gráficos
- [ ] **Categorias de produto** e busca dentro do catálogo
- [ ] **Docker** — `Dockerfile` com build em múltiplos estágios para deploy mais simples
- [ ] **Migração para Postgres** — exercício de trocar a implementação do repositório
- [ ] **Testes da camada Discord** — separar a lógica de `InteracaoListener` para poder testá-la

---

## 📄 Licença

MIT — veja [LICENSE](LICENSE). Use, modifique e publique à vontade; só mantenha o aviso de
copyright.

Se este projeto te ajudou a aprender algo, deixe uma ⭐ no repositório — ajuda outras pessoas a
encontrarem.

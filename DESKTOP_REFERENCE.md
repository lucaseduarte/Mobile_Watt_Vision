# Watt Vision Desktop — referência oficial e comparação

Complementação em 16/11/2026: as limitações de conhecimento dos contratos nas seções históricas abaixo são atualizadas pela seção 13 e por `BACKEND_REFERENCE.md`/`DATABASE_REFERENCE.md`. Ausência de chamada no Desktop não equivale a ausência de endpoint no Backend.

Análise estática de 16/11/2026, revisada após atualização externa do Desktop durante a leitura. Referência final observada: commit `cfa1707` (Ajustes no modo claro). Fonte oficial permanente: `/Users/25203646/Documents/Front_WV`, exclusivamente leitura. Caminhos Desktop neste documento são relativos a essa raiz; caminhos Mobile são relativos a `app/src/main/java/com/example/watt_vision_mobile/`. Regras em `AGENTS.md`; diagnóstico Mobile detalhado em `PROJECT_CONTEXT.md`; execução proposta em `ROADMAP.md`.

## 1. Método e limites

Inventariados os arquivos de `src` (incluindo novos módulos, serviços e assets da atualização), páginas, componentes, serviços, CSS e configurações de projeto. Analisados arquivos-fonte originais; dependências, caches, builds, lockfiles, agregado `repomix-output.xml` e arquivos de credenciais foram excluídos da leitura. Assets raster foram identificados por caminho; não houve renderização visual das telas. Não houve instalação, execução de scripts, build, lint ou alteração no Desktop. Backend não foi acessado nem alterado.

O código frontend comprova chamadas e formatos aceitos pelo cliente, não a existência, autorização, unidade ou resposta efetiva dos endpoints no servidor. “Referência oficial” não significa que toda função Desktop esteja completa ou que dados de exemplo sejam dados de negócio válidos.

## 2. Arquitetura, tecnologias e estrutura

SPA React com componentes funcionais/hooks (`useState`, `useEffect`, `useMemo`, `useCallback`). Estado e comunicação estão concentrados em páginas e alguns componentes; não há store global de negócio, camada domain ou tipagem TypeScript. Tema fica no estado de App e persiste em localStorage. `src/main.jsx` monta `App` em StrictMode e importa CSS. `src/app.jsx` controla autenticação, página ativa, sidebar e modal de configurações em memória; não usa React Router nem navegação real por URL.

| Dependência declarada | Versão/faixa | Uso |
| --- | --- | --- |
| React / React DOM | ^19.2.8 | UI e hooks |
| Vite / plugin React | ^8.3.0 / ^6.1.1 | desenvolvimento/build |
| Tailwind Vite/PostCSS | ^4.3.3 | utilitários visuais |
| Axios | ^1.21.1 | HTTP |
| Lucide React | ^1.58.0 | ícones |
| Recharts | ^3.10.1 | AreaChart/LineChart |
| jsPDF / autotable | ^4.2.1 / ^5.0.9 | PDF local |
| Chart.js | ^4.6.1 | gráficos rasterizados fora da tela no PDF |
| ESLint / @eslint/js | ^10.10.0 / ^10.0.1 | lint; plugins hooks/refresh |

`package.json`: scripts dev, build, lint e preview; sem script de testes. Versões são declarações, não resolução validada. `vite.config.js` aplica React e Tailwind. `src/styles/index.css` importa Tailwind e define classes wv-* e overrides globais para tema claro via html[data-theme=light]. `tailwind.config.js` declara Plus Jakarta Sans e raio card 16px; não há `@config` no CSS: aplicação efetiva dessa configuração legada no pipeline Tailwind 4 deve ser verificada visualmente, sem presumir fonte computada.

```text
Front_WV/
  package.json, vite.config.js, tailwind.config.js, eslint.config.js
  index.html, README.md
  public/{favicon.svg,icons.svg}
  src/
    main.jsx, app.jsx
    assets/{login.png,logoWV.png,Logo_modo_claro.jpeg}
    styles/index.css
    services/{api.js,alarmes.js,auth.js,session.js,relatorioPDF.js}
    pages/{login,dashboard,operacional,medicoes,alarmes,conectividade,relatorios}.jsx
    components/
      layout/{sidebar,topbar,perfilModal}.jsx
      dashboard/{dashboardData.js,dashboardFilters.jsx,statCard.jsx,
                 systemPerformance.jsx,recentAlarms.jsx,
                 indicadoresEnergeticos.jsx,calculoButton.jsx}
      operacional/novoOperacionalModal.jsx
      conectividade/{conectividadeKpiGrid,conectividadeTabela,
                     dispositivoDetalhesModal,dispositivoFormModal}.jsx
      medicoes/{medicaoData.js,cardMedicao.jsx,cardMetaMedicao.jsx,
                filtrosMedicao.jsx,tensaoAoLongoDoTempo.jsx,dadosBrutos.jsx,
                medicoesAlarmesModal.jsx,novoLimiteModal.jsx}
      alarmes/{alarmesKpiGrid,alarmesTabela}.jsx
      configuracoes/{configuracoesSistemaModal,geralSistema,
                     tarifasSistema,usuarioSistema,pontosMedicao}.jsx
```

Existe página completa de Conectividade na revisão final. Não há página consultiva própria de detalhe de local/setor: estes podem abrir formulário de edição. Detalhe de equipamento é um ramo da própria `pages/operacional.jsx`.

## 3. Navegação, autenticação e segurança observadas

- Sidebar expansível/recolhível por clique no logo; Dashboard, Operacional, Medições, Alarmes, Conectividade e Relatórios. Campos path do menu não controlam URLs.
- Topbar pesquisa sem acentos, combina palavras, limita a doze resultados e permite teclado/Ctrl+K. Aceita entidades por props, mas App só fornece páginas; navegação de registros específicos não está conectada. Abre Configurações e Meu Perfil.
- `app.jsx` inicia desautenticado. `pages/login.jsx` continua demonstrativo: submit chama onLoginSuccess sem invocar serviço de autenticação. Meu Perfil também é demonstrativo e identificado como tal, sem ler usuário real.
- Novos `services/auth.js` e `session.js` fornecem POST /auth/login, GET /auth/me e persistência condicional em localStorage (lembrar) ou sessionStorage. Login exige dados.token/dados.usuario; validarSessao retorna null sem token e limpa sessão ao falhar. **Não estão integrados ao login/App atual**; não há restauração automática em App.
- Logout agora chama limparSessao, remove token/usuario dos dois storages, redefine página e booleano.
- Axios usa base `http://localhost:3000/api`, timeout 10 s e interceptor global Bearer com getToken. Serviço de alarmes mantém header explícito também. Não há refresh/interceptor global de resposta identificado.
- Reconhecimento tenta obter ID de payload JWT quando token existe; sem token envia corpo vazio. Decodificar não verifica assinatura/permissão: servidor deve validar sessão e identidade. Nenhuma escrita foi executada na análise.
- Tema dark/light aplica document.documentElement.dataset.theme, persiste em `wattvision_theme` e troca logo em login/sidebar. Esse estado é preferência local, sem escrita de negócio.
- Não portar bypass demonstrativo, perfil fixo ou confiança em identificador enviado pelo cliente como autenticação definitiva Android.

## 4. Design system observado e adaptação

Não há sistema de tokens centralizado completo. Identidade aparece em classes Tailwind, estilos inline e cores literais por componente.

| Elemento | Desktop observado | Mobile atual e direção |
| --- | --- | --- |
| Fundo | escuro #070A15 → #0B132B → #070A15 no CSS; claro #EEF3F8 | fundo sólido #070C15; preservar hierarquia escura, avaliar gradiente sem prejudicar legibilidade |
| Cards | #0C141F, #0D151F, #0D1521; borda #1B2A40 e variantes slate | tokens próximos já presentes; consolidar consistência |
| Inputs | #101827, #070A15, #111722; bordas #17315F/#1D2C42 | #0D1829 e CardBorder; adaptar controles nativos |
| Ação/seleção | blue-600/500, #2C57A8, #356EC5, #24498B | PrimaryBlue #2B66C6; harmonizar tokens sem copiar cada literal |
| Texto | #C0C5C8, #AFB5BA, slate-400/500 | branco e TextMuted #6B7B94; revisar contraste e hierarquia |
| Tons | azul #78B5FF, verde #42C879, vermelho #E47D8B, amarelo #D5B86C; badges ciano/teal/rose | ampla correspondência em Color.kt |
| Fonte | Plus Jakarta Sans 400/500/600/700 solicitada em index.html e config; `font-sans`; PDF usa Helvetica/Arial | FontFamily.Default; divergência a decidir na etapa visual, sem assumir fonte web efetiva |
| Tamanhos | legendas 9–11px, textos 12–14px, títulos 16–24px, valores 25–30px | não transpor px diretamente a sp; garantir leitura e escala de fonte Android |
| Espaçamentos | p-3/4/5/6/8, gaps 2/3/4/6, formulários px-3 py-2.5; base usual Tailwind de 4px | referência conceitual de 8/12/16/20/24; usar dp e áreas de toque adequadas |
| Forma | raios 8/12/16/18/20px, pills e círculos; borda fina, sombra e blur | cards arredondados já presentes; evitar excesso de efeitos/custo de renderização |
| Ícones | Lucide, traço usual 1.8/2, 13–20px; logo raster | Material Icons Extended; equivalência semântica, consistência de peso e descrição |
| Layout | sidebar/topbar, tabelas largas, grid até 4–6 colunas, dialogs com overlay | bottom navigation, listas/cards, detalhes e sheets; não miniaturizar tabelas/forms |

`public/favicon.svg` e `icons.svg` não constituem o principal conjunto de ícones das telas, que importa Lucide; index.html ainda aponta `/vite.svg`, não o favicon local listado. Tema claro/escuro está implementado globalmente no Desktop final via CSS e estado App; o Mobile segue exclusivamente escuro e seu seletor não aplica o tema. Claro usa superfícies #FFFFFF/#F8FBFF, bordas #CBD8E7/#D7E0EB e texto #111827. Overrides por seletores amplos e !important exigem validação visual de tons semânticos; não portar literalmente.

Reuso conceitual: StatCard/CardMedicao, KPIs, badges de severidade/categoria/status, filtros, composição com cobertura, detalhe de alarme, dados brutos, estados vazios/carregamento. Nenhum componente React é reutilizável diretamente em Compose. Cards operacionais, InfoCampo/InfoGrupoCard, AlarmesSecao e seletores Android existentes são bases úteis. Unificar variantes de filtros Android somente dentro do escopo de cada implementação.

## 5. Mapeamento funcional detalhado

### Dashboard

Fontes: `pages/dashboard.jsx`, `components/dashboard/*`.

- Cards: tensão atual, dispositivos ativos, offline e fator de potência. Tensão/FP vêm da última leitura válida da série, não de cálculo consolidado. Ativos incluem status ativo/online; offline incluem offline/inativo.
- Filtros: período Hoje/6 horas/Ontem, local, setor, equipamento e seis grandezas. Opções de entidades vêm de GETs; nomes enviados como parâmetros. Falha dessas listas é convertida em lista vazia sem aviso próprio.
- Polling 5 s com guarda de concorrência e AbortController; `Promise.allSettled` permite resultado parcial. Série temporal usa PZEM 1 fixo; parâmetros dos filtros vão para essa consulta, mas não para contagem de equipamentos ou alarmes. “Ontem” envia 48 h, sem delimitar isoladamente o dia anterior.
- AreaChart: seis grandezas, potência em W, energia acumulada em kWh, frequência em Hz e FP adimensional. Seleciona no máximo 24 pontos por índice uniforme, preservando extremos temporais, mas não necessariamente picos. Domínio Y com margem de 10%, tooltip e estado sem dados.
- Alarmes recentes: ativos/reconhecidos, últimos cinco da ordem recebida em ordem reversa, sem ordenar explicitamente por data/severidade. Trata indisponível e lista vazia sem inventar alarmes.
- Composição: `CalculoButton` soma valores finitos, permite total/cobertura recebidos e calcula percentual arredondado. Página chama `<CalculoButton />` sem dados: usa quatro locais demonstrativos. Não é calculadora tarifária real.
- Seção de indicadores: Consumo Total, Consumo Médio, Pico de Consumo, Maior Consumo, Custo Estimado e Variação. Todos exibem “— / Dados indisponíveis”; não há fórmula/integrador conectado.

### Operacional e detalhe

Fontes: `pages/operacional.jsx`, `components/operacional/novoOperacionalModal.jsx`.

- Abas Equipamentos/Setores/Locais; consultas a cada 5 s e relações equipamento.id_setor → setor.id/id_setor → local.id/id_local por setor.id_local (com fallback eq.id_local). Campos alternativos podem mascarar relação ausente com nomes padrão.
- Busca por nome/código em lowercase, sem normalização de acentos; filtro Ativo agrupa Online e Ativo; Inativo agrupa Offline/Inativo/Atenção. Ao trocar aba, limpa busca/status.
- Tabela exibe nome/código, local/setor ou contagens, potência/consumo e status. Campos de setores/locais não são consistentemente mapeados aos cabeçalhos; há fallbacks numéricos/string. Não usar isso como contrato de contagens/consumo.
- Apenas equipamentos abrem detalhe. Ficha permite edição de nome, código, fabricante, modelo, série, tensão nominal, instalação, local, setor, tipo, status e descrição. Campos ausentes usam exemplos fixos; Online, “há 3 segundos” e três alarmes são fixos.
- Escritas reais: criar equipamento, setor, local e editar equipamento, setor e local. Não portar para Mobile. Local/setor abrem formulário de edição, sem detalhe consultivo próprio Desktop; Mobile já tem telas consultivas, mas simuladas.
- Import `../components/operacional/NovoOperacionalModal` diverge da caixa do arquivo `novoOperacionalModal.jsx`; risco em filesystem case-sensitive. Registrado, sem correção Desktop.

### Medições, limites e dados brutos

Fontes: `pages/medicoes.jsx`, `components/medicoes/*`.

- Série PZEM 1, polling 5 s, query por horas conforme período (24/48/168/720). Só período dispara consulta; seletores de local/setor/equipamento recebem apenas opções padrão da página e não filtram dados.
- Três cards reais: média aritmética de tensão, maior corrente e última potência em W, independentemente da grandeza selecionada. Sem dados usa zeros; alguns campos nulos podem virar zero via Number(null).
- Na revisão final, gráfico recorta Hoje/Ontem para o dia civil local [início, início + 1 dia); demais períodos usam histórico recebido. Cards e dados brutos continuam sobre a série completa retornada (24/48 h), portanto podem divergir do recorte do gráfico. A antiga janela de 60 s foi removida por atualização externa.
- Filtro e gráfico agora configuram as seis grandezas: tensão, corrente, potência, energia, frequência e FP, com aliases. Number(null) ainda pode converter ausência em zero; não reproduzir essa semântica no Mobile.
- Dados brutos exibem data/hora, V, A, W e kWh em ordem decrescente; dez registros por página, até dez páginas (no máximo cem acessíveis). Lista vazia produz totalPaginas=0 e paginaSegura=0; revisar no Mobile. Apesar do nome Modal, componente é renderizado inline.
- Card de meta usa default “Nenhuma meta definida”. “Definir Meta” abre `MedicoesAlarmesModal`, que agora consulta GET /limites, equipamentos, setores e locais. Extrator desse modal só aceita `resposta.data.dados` como array, mais restritivo que outras páginas.
- Limite lido: id, descricao, grandeza, operador, valor, severidade, periodo_inicial/final, ativa e vínculo id_equipamento/id_setor/id_local. Converte ativa numérica em Ativa/Inativa; apresenta entidade/condição/período e filtros.
- Criar/editar/ativar/desativar são escritas reais POST/PUT/PATCH. Ícone de lixeira agora desativa limite via status, sem DELETE: o efeito de negócio continua proibido no Mobile. Consulta de limites pode ser portada com contrato confirmado.
- NovoLimiteModal recebe itemEdicao e listas reais, preenche campos e usa kW/V/A/kWh; validar aliases de período snake_case versus camelCase ao editar. Não é mais formulário mock antigo.
- `medicaoData.js` contém cards fixos antigos, sem import na página atual. Distinguir fixture não usada de dado renderizado.

### Alarmes

Fontes: `pages/alarmes.jsx`, `services/alarmes.js`, `components/alarmes/*`.

- Polling 5 s com cancelamento e prevenção de sobreposição. Erros 401/403 têm mensagens específicas; mantém lista anterior em falha e KPIs exibem “—”.
- Normaliza IDs, títulos, equipamento/local, categoria, severidade e status; preserva campos originais por spread. Crítico/atenção/informativo, Energético/Comunicação e Ativo/Reconhecido/Resolvido. Mobile usa outro conjunto de campos/enums e precisa alinhar aliases com contrato.
- KPIs: ativos inclui reconhecidos; críticos/atenção entre ativos; resolvidos na lista total. Visual dos cartões usa azul fixo, embora dados carreguem propriedade tone.
- Ativos/Histórico, busca por título/equipamento, filtros de severidade/categoria; detalhe mostra contexto e condição. Histórico Hoje/7d/30d/Personalizado apenas muda estado visual, sem filtragem. Paginação é 1/1 fixa. “Resolvido em” usa início, sem timestamp próprio de resolução; “Ver Equipamento” não navega.
- Reconhecer faz PATCH real, atualiza status local após sucesso e mostra erro de ação. **Proibido no Mobile**, que só pode exibir estado de reconhecimento já existente.

### Relatórios

Fontes: `pages/relatorios.jsx` e `services/relatorioPDF.js` (1.079 linhas na revisão final).

- Série temporal PZEM 1, períodos 24/6/48/168/720 h, filtros local/setor por nome, polling 10 s e atualização manual. Sem endpoint dedicado de relatórios.
- Seis grandezas e opção Todas; potência converte W para kW por /1000. Consumo soma deltas não negativos consecutivos por medidor e dia local. Variação usa linha já calculada anterior; não apresenta mais o bug antigo de zero constante.
- Tabela diária de todas as grandezas ou estatística de uma métrica; gráficos Recharts com até 120 amostras, tooltip e escalas automáticas/manuais locais. Código também prepara normalização 0–100 para conjunto de grandezas; os gráficos individuais usam dados reais por unidade. Limites de eixo são visualização, sem alterar medições ou regras operacionais.
- Tarifa estimativa é entrada local persistida em `wattvision_tarifa`, default 0,80; custo = consumo total * entrada. Não é tarifa vigente da API. Mobile pode oferecer simulação local identificada, sem alterar tarifa de negócio.
- PDF A4 paisagem usa jsPDF/autotable e Chart.js em canvas fora da tela: cartões/sparklines, resumo executivo, tabelas estatísticas, tendência, faixas normais fixas, consumo por período, gráficos e rodapé paginado. Impressão continua window.print. Exportação local é permitida em princípio no Mobile.
- Granularidade PDF automática: >48 h por dia, >2 h por hora, restante por minuto. Redução de série até 360 pontos preserva início/mínimo/máximo/fim por bloco, diferente da amostragem uniforme da tela.
- PDF calcula deltas sobre uma sequência global, enquanto tela separa por medidor: pode divergir ao misturar sensores. Faixas “normais” fixas e conclusões do PDF não comprovam limites configurados/validados no servidor. Detalhes na seção 7.

### Configurações

Fontes: `components/configuracoes/*`.

- Modal quatro abas, Escape/overlay para fechar, salvar/cancelar geral apenas em memória. Estados dos filhos não são persistidos pelo salvar global e se perdem ao desmontar/trocar aba.
- Geral: empresa, fusos brasileiros, formato DD/MM/YYYY, MM/DD/YYYY ou YYYY-MM-DD e unidade kWh fixa; aparência alterna claro/escuro efetivamente via estado App e CSS, com persistência local. Preferências não são propagadas globalmente aos formatos das demais telas.
- TarifasSistema inicia **vazio**; cadastro/edição/exclusão locais, valor não negativo, fim >= início; ao ativar uma, desativa as demais. “Vigente” escolhe ativa com início mais recente, sem verificar se data atual pertence à vigência. Modal pai ainda tem dados/handlers de tarifa antigos não conectados à aba usada: não confundir com lista renderizada.
- Usuários: três exemplos, perfil Administrador/Gestor, local, status e último acesso; CRUD local, valida e-mail duplicado, campo de senha não gera autenticação nem persiste senha no item. Não há autorização de tela baseada em usuário real.
- Pontos: três exemplos; dispositivo, nome, escopo Equipamento/Setor/Local, entidade, modelo PZEM-004T V3/PZEM-016/Outro e ativo. CRUD local, sem integração/contrato de API.
- Mobile deve manter consultas de empresa/tarifas/usuários/pontos conforme permissões e preferências locais, removendo gerenciamento. Tarifas e usuários continuam sem endpoints observados; pontos possuem GET em Conectividade, embora a aba de configurações ainda use exemplos. Confirmar contrato e permissões antes de integrar.

### Conectividade

Fontes: `pages/conectividade.jsx`, `components/conectividade/*`.

- Tela implementada na revisão final: KPIs total/online/instáveis/offline, Status Atual, Histórico e Configuração; pesquisa por código/nome/vínculo/local/setor, detalhes de dispositivo e formulários administrativos.
- Consulta GET /dispositivos-iot, /pontos-medicao, /locais, /setores, /equipamentos e /alertas. Polling 5 s e atualização ao focar janela; detalhe consulta GET por ID e resolve relações novamente.
- Normaliza id_dispositivo_iot/id e identificador/codigo_identificacao, nome/modelo/série/MAC/firmware/instalação, data_comunicacao, tx_interval_sec, SSID/IP/broker/port/topic/client_id, wifi/mqtt e vínculos. Status vem do servidor: online → Online; instavel/instável → Instável; demais → Offline. Não calcula offline pelos tempos exibidos na aba Configuração.
- Relações priorizam IDs de raw/ponto; ponto ativo preferido. Equipamento define setor atual e setor define local atual; na ausência de vínculo no banco, usa cache local `wattvision_vinculos_dispositivos`. Mobile não deve criar vínculo fictício a partir de cache Desktop.
- Há defaults fixos de modelo/firmware/rede/MQTT e detalhe com ponto/nomes de exemplo. Exibir ausente no Mobile em vez de apresentá-los como dados reais.
- Histórico deriva de alertas categoria comunicacao, id_tipo_alerta=2 ou mensagem contendo conex; status Resolvido vira Reconectado, demais Desconectado. Data usa criado_em com fallback Hoje. Lista só é atualizada se há eventos, podendo manter histórico antigo quando resposta fica vazia.
- Paginação do histórico com dez itens; seletores Hoje/7/30/Personalizado e datas **não filtram** eventos na função de paginação. Erros de consulta principal ficam em console; auxiliares viram vazio, podendo ocultar falhas.
- Escritas reais: criar/editar/excluir dispositivo, alterar status Wi-Fi/MQTT (PATCH com fallback PUT), criar ponto de medição associado e salvar configuração global. Mobile deve remover todas essas ações da UI e serviços; preservar somente consulta de status/histórico/detalhes permitidos.
- Configuração usa defaults 5/2/3 minutos e regras booleanas; não há GET de configuração global identificado. PUT falho é engolido, mas UI pode indicar sucesso. Defaults não são regra comprovada de conectividade.

## 6. Endpoints e contratos observados

Base Desktop: `http://localhost:3000/api`. Caminhos completos abaixo, não executados. Mobile usa outra base HTTP local e prefixo api/ em Retrofit; não duplicar o prefixo. Todos os serviços Axios podem receber Bearer pelo interceptor.

| Método/rota | Entradas e formato lido | Destino Mobile |
| --- | --- | --- |
| POST `/api/auth/login` | email, senha → dados.token e dados.usuario; serviço auth não chamado pelo Login atual | sessão permitida; estrutura já existe |
| GET `/api/auth/me` | dados com usuário; serviço não ligado à restauração App | sessão final |
| GET `/api/equipamentos` | array ou dados/equipamentos; id/id_equipamento, nome, codigo, id_setor/id_local, ficha e status | GET existe; DTO e uso operacional incompletos |
| GET `/api/setores` | array ou dados/setores; id/id_setor, nome, codigo, id_local, status, responsavel, descricao | consulta pendente |
| GET `/api/locais` | array ou dados/locais; id/id_local, nome, codigo, endereco, responsavel, status | consulta pendente |
| GET `/api/medicoes/serie-temporal` | id_pzem=1, horas; Dashboard local/setor/equipamento/grandeza; Relatórios local/setor; array ou dados (Relatórios também medicoes) | existe; filtros e aliases a confirmar |
| GET `/api/alertas` | array ou dados/alertas; IDs/status/categoria/severidade/datas/vínculos | declarado, tela completa pendente |
| GET `/api/limites` | modal lê apenas dados[]; id, descricao, grandeza, operador, valor, severidade, periodo_inicial/final, ativa e IDs de vínculo | consulta permitida; sem escrita |
| GET `/api/dispositivos-iot` | array ou dados[]; identificação/hardware/rede/status/timestamp e vínculos | consulta pendente |
| GET `/api/dispositivos-iot/{id}` | objeto em dados ou direto; se array usa primeiro | detalhe consultivo pendente |
| GET `/api/pontos-medicao` | array ou dados[]; id_dispositivo_iot, status, id_equipamento/id_setor/id_local; demais campos não garantidos | consulta permitida; contrato de aba Configurações a completar |

Escritas de negócio identificadas, **não portar**:

| Método/rota | Corpo observado / efeito |
| --- | --- |
| POST `/api/equipamentos` | nome, codigo, id_setor, status, potencia_nominal |
| PUT `/api/equipamentos/{id}` | ficha técnica/identificação/status/descricao e id_setor, payload selecionado |
| POST `/api/setores`, PUT `/api/setores/{id}` | nome, codigo, id_local, responsavel, descricao, status |
| POST `/api/locais`, PUT `/api/locais/{id}` | nome, codigo, endereco, responsavel, status, id_empresa |
| PATCH `/api/alertas/{id}/reconhecer` | id_usuario quando token permite decodificação; sem token envia {} |
| POST `/api/limites`, PUT `/api/limites/{id}` | descricao, grandeza, operador, valor, severidade, periodo_inicial/final, ativa, IDs de vínculo |
| PATCH `/api/limites/{id}/status` | ativa Boolean; também usado para desativar pelo ícone de lixeira |
| POST `/api/dispositivos-iot`, PUT `/api/dispositivos-iot/{id}` | id_empresa, nome, identificador, hardware/firmware/instalação, rede/MQTT, intervalo, wifi/mqtt/status |
| PATCH `/api/dispositivos-iot/{id}/status`, fallback PUT mesma rota | status, wifi, mqtt; altera estado operacional |
| DELETE `/api/dispositivos-iot/{id}` | remove dispositivo; limpa cache local |
| POST `/api/pontos-medicao` | nome, descricao, data_instalacao, status, IDs local/setor/equipamento/dispositivo; criação associada a dispositivo |
| PUT `/api/configuracoes/conectividade` | tempo_offline_min, tempo_instavel_min, tempo_reconexao_min, alarme_offline, alarme_reconectar, registrar_historico |

Corpos das escritas não são necessários à consulta Mobile. Não foram identificados GET de configuração global de conectividade ou serviços de listagem de tarifas/usuários. Configurações corporativas locais continuam administrativas mesmo sem HTTP.

Alarmes aceitam id_alerta/id, titulo/tipo_alerta/mensagem, equipamento/local aninhados ou String, categoria_alerta/categoria, severidade e status, data_hora/data_criacao/inicio, reconhecido_por/id_usuario_reconhecimento, id_dispositivo_iot e condição/valores detectado/limite. Desktop defaulta categoria desconhecida para Energético e severidade desconhecida para Informativo; confirmar semântica antes de portar. Mobile usa id_equipamento/tipo/resolvido e perde parte do contexto.

Endpoints Mobile sem chamada correspondente Desktop: GET resumo/ultima/consumo de medições e alertas/ativos. Ausência no frontend não prova inexistência no backend. Nenhum cliente fornece endpoint refresh.

## 7. Fórmulas existentes e lacunas a resolver

| Indicador | Implementação final Desktop | Limitação para adaptação |
| --- | --- | --- |
| Média tensão / máximo corrente | soma/amostras finitas; max corrente | média não ponderada; Number(null) pode virar zero |
| Potência | Medições/Dashboard em W; Relatórios transforma /1000 para kW | Mobile rotula kW sem conversão; confirmar unidade servida |
| Composição | soma de valores finitos e cobertura percentual arredondada | Dashboard usa exemplos; não comprova agregação sem duplicidade |
| Consumo na tela de Relatórios | agrupa por id_pzem/pzem_id/id_medidor (fallback único), ordena, soma deltas >=0; deltas negativos ignorados; primeiro valor referência | delta alocado no dia local da leitura final; não cobre consumo anterior à primeira amostra nem resolve aliases/duplicatas sozinho |
| Consumo PDF | soma deltas >=0 numa sequência global ordenada; conta quedas | não separa medidores: possível divergência de tela em dados multissensor |
| Média relatório | consumo total / dias com linha; demais média aritmética | dias sem dados não entram; sem ponderação temporal |
| Pico relatório | maior consumo diário; demais maior amostra | não é demanda máxima nem pico instantâneo de consumo |
| Custo | consumo total * tarifa local editável; default 0,80 | simulação/estimativa, não tarifa vigente comprovada |
| Variação diária | (valor atual − anterior) / abs(anterior) *100; sem anterior/zero retorna 0 | ausência de base não é variação real zero; explicitar no Mobile |
| Tendência PDF | médias das duas metades de amostras; percentual sobre abs(primeira); estável se abs <5% | exige >=24h e >=4 valores; regra local do PDF, não indicador operacional validado |
| Faixas “normais” PDF | tensão 198–242 V, frequência 59,9–60,1 Hz, FP mínimo 0,92 | constantes de exemplo de instalação; não confundir com limites reais da API |
| Data de grupo | tela e PDF usam calendário local | fuso servidor/empresa ainda precisa ser confirmado |

PDF calcula percentuais fora da faixa e resumo executivo com limites fixos; isso é inferência local de relatório, não histórico de alarmes do backend. Consultar limites operacionais reais via contrato pode ser necessário antes de atribuir normalidade. Alterar escala de gráfico ou tarifa simulada local não altera dados de negócio e pode ser permitido, com identificação adequada.

Não há fórmulas reais conectadas para os seis indicadores energéticos do Dashboard, demanda máxima ou horas de funcionamento. Confirmar seleção de medidor, unidade, janela versus dia civil, fuso, reset e cobertura; não inferir um indicador empresarial consolidado a partir de um sensor fixo.

## 8. Estados, formatação e comportamentos a preservar/adaptar

- Números/moeda usam pt-BR, Intl/toLocaleString, BRL com duas casas nas tarifas; composição com até uma casa. Algumas telas usam toFixed com ponto, logo não há uniformidade completa.
- Datas em DD/MM/YYYY na exibição, ISO em inputs; parse frequentemente substitui espaço por T e usa fuso do navegador. Preferência de formato de data geral não está conectada a esses formatadores; tema agora é global. Definir política única Android quando contratos forem confirmados.
- Dashboard separa falha parcial de alarmes/medições; alarmes têm skeleton, mensagem e vazio. Relatórios têm loading/erro/vazio. Medições mantém dados antigos em erro, mas não os identifica uniformemente como desatualizados. Operacional converte falha das três consultas em vazio, sem loading/erro explícito suficiente.
- Pesquisa, filtros, expansão de sidebar/modal, tooltip e exportação são locais. Polling deve respeitar visibilidade/ciclo de vida Android; não portar timers React literalmente.
- Consultas por IDs e relações reais são preferíveis a nomes duplicáveis, mas migração depende do contrato existente; não inventar query por ID onde servidor só foi observado recebendo nome.
- Não portar subtítulos ou ações de estado vazio que mandem cadastrar registros para o Mobile.

## 9. Matriz Desktop × Mobile e plano por módulo

“Parcial” descreve presença de interface/fluxo, não comprova integração funcional nem validação em emulador. Nenhum módulo foi declarado integralmente correto por mera semelhança de UI.

| Módulo | Desktop | Mobile atual | Preservar / corrigir / implementar |
| --- | --- | --- | --- |
| Navegação/tema | sidebar/topbar, busca de páginas/perfil demonstrativo, claro/escuro persistido | barra inferior/rotas por String, drawer não conectado | preservar navegação nativa; garantir acesso a todos os módulos/voltar/restauração; padronizar tokens |
| Development Mode | não é referência segura: login simulado | entrada autenticada fixa em todas variantes | substituir futuramente por modo só Debug; Release sem bypass |
| Dashboard | quatro KPIs, seis grandezas, indicadores ainda vazios, composição exemplo | seis KPIs diferentes, quatro grandezas, indicadores não renderizados, fallback fictício | alinhar informação/semântica, remover falsos dados, ligar filtros, acrescentar Hz/FP e composição real conforme contrato |
| Operacional | três listas via API; cria/edita; relações parciais | três abas/cards/filtros nativos com mocks aleatórios | preservar interação; GET reais, IDs e estados; não portar escrita |
| Equipamento | ficha editável, status/alarmes fixos | ficha consultiva, estados stale/erro e cópia; tudo mock; ID inicial incorreto | preservar componentes; corrigir seleção, integrar campos/alarmes e conexão reais disponíveis |
| Local/setor | sem detalhe próprio | detalhes consultivos mock | preservar telas; preencher dados/vínculos reais, não inventar endpoint de detalhe |
| Medições | série real, seis grandezas, gráfico por dia e dados brutos; limites via API | placeholder | construir tela nativa sem definir/editar metas/limites; não reproduzir bugs de grandeza/zero |
| Alarmes | consulta real, detalhe/filtros e PATCH reconhecimento | lista parcial no Dashboard/detalhe; rota placeholder | criar consulta completa/normalização, histórico funcional conforme contrato, sem PATCH |
| Conectividade | dispositivos/pontos via API, KPIs, status/histórico/detalhe e escrita administrativa | placeholder | adaptar GETs e campos reais, remover configuração/controle/CRUD e defaults; confirmar regra do servidor |
| Relatórios | série real, deltas por medidor, seis grandezas, escala/tarifa locais, PDF avançado | placeholder | relatório nativo com fórmulas/unidades confirmadas e estados claros; exportação local a definir no módulo |
| Configurações | quatro abas em memória, tarifas renderizadas vazias, usuários/pontos exemplos | quatro abas com exemplos e CRUD em memória | somente consulta autorizada + preferências locais; corrigir diferenças de data/tema; aguardam APIs |
| Sessão | serviços reais não conectados ao Login/App, Bearer global, logout limpa storage | login HTTP existe, mas bypass global e logout solto | preservar estrutura Mobile, finalizar entre últimas etapas com contrato real |

O Mobile já adapta adequadamente vários conceitos de interação (Pager, cards, sheets, busca sem acentos, cópia e descrição expansível). Isso deve ser preservado, corrigindo origem e semântica dos dados. Não há comprovação funcional integral sem API e emulador.

## 10. Segurança somente leitura e lacunas externas

No Mobile atual não foram encontrados endpoints Retrofit de escrita de negócio; CRUD de Configurações é local. No Desktop há POST/PUT operacionais, PATCH de reconhecimento, POST/PUT/PATCH de limites, CRUD/status de dispositivos, POST de ponto e PUT de configuração de conectividade. Ao adaptar, excluir essas operações também dos serviços/repositórios Mobile, não apenas os botões.

**Necessidades do backend a registrar, sem modificar:** autorização efetiva de consultas por usuário/empresa/local, restrição de escrita para a sessão/perfil destinado ao Mobile, proteção contra acesso por IDs de outra empresa e identidade derivada de token validado. Distinguir o app pelo User-Agent ou confiar em esconder ações não resolve autorização. Um JWT de desenvolvimento com poder de administrador continua tendo esse poder no servidor; bypass Debug não reduz nem amplia suas permissões.

Pendências para cada módulo: respostas reais e paginação de locais/setores/equipamentos, detalhe e relações PZEM/ponto/dispositivo/equipamento (parcialmente evidenciadas em Conectividade); unidades; política de datas/fuso; consumo e cobertura; fonte de tarifa/vigência; endpoints de usuários/tarifas/configurações; confirmação dos campos de pontos, limites e dispositivos e suas permissões; histórico/períodos de alarmes; refresh/expiração/revogação de sessão. Esses contratos não estão completos nos frontends; devem ser fornecidos/confirmados quando necessários, sem bloquear trabalho independente de apresentação e sem substituição silenciosa por mocks.

## 11. Controle da revisão analisada

Durante a revisão final por hashes, fontes Desktop foram atualizadas externamente (sem comandos de escrita deste agente). O mapeamento foi refeito sobre o estado observado no commit `cfa1707`, incluindo Conectividade, limites por API, autenticação desacoplada, tema claro e Relatórios/PDF novos. A versão inicialmente lida não é usada como estado final. O status Desktop já continha `repomix-output.xml` não rastreado; foi ignorado e preservado. Nenhum build/teste Desktop foi executado. Verificação final compara hashes do estado revisado e baseline Mobile; alterações externas anteriores são distinguidas da atuação deste agente.

## 12. Atualização Mobile — Development Mode

O Desktop não foi alterado nesta implementação. A matriz anterior descreve a baseline comparativa; após aprovação, Mobile passou a ter acesso direto exclusivo Debug, login obrigatório Release, demonstração explícita Debug para telas mock e ausência de fallbacks fictícios no Dashboard. Controle por BuildConfig.DEBUG/AppAccessPolicy e source sets debug/release, sem nova integração de módulos. Detalhes atuais em PROJECT_CONTEXT.md, seção 13, e ROADMAP.md. Login/serviços Desktop continuam referência de código, sem adoção do bypass demonstrativo como autenticação Android.

## 13. Comparação complementada pelo Backend e pelo script

Em 16/11/2026, Desktop HEAD confirmado `cfa17077f68cee4adae2388f7625a20d634b4433`, Backend `db9d479414ffd0e0ad427ea560ab0b0d9bd3c955`. Código Desktop preservado; análise anterior reutilizada com consulta pontual a Dashboard/Medições/Relatórios. Script oficial: `/Users/25203646/Documents/BD/Script Completo.rtf`, extraído somente para análise. Referências técnicas detalhadas: BACKEND_REFERENCE.md e DATABASE_REFERENCE.md.

| Referência Desktop | Contrato/estrutura agora identificados | Orientação Mobile |
| --- | --- | --- |
| GET série com nomes de entidades/grandeza | Backend só lê id_pzem/horas | resolver cadeia PZEM→ponto→escopo por IDs; não atribuir efeito a filtros ignorados |
| 30d=720h em telas Desktop | serviço permite até 168h, SQL limita a 10000 amostras ASC | não reproduzir período não suportado nem truncar silenciosamente |
| Resumo/última não usados pelo Desktop | existem no Backend, mas retornam uma leitura global | não rotular como consolidação da empresa |
| Detalhe usa registro listado | GETs de local/setor/equipamento por ID existem | integrar detalhe com endpoint comprovado, campos e permissão adequados |
| Usuários/tarifas/empresa sem consumo API identificado | rotas GET existem, exigem Administrador | GET compatível com consulta, condicionado ao acesso real; sem CRUD |
| Reconhecido/Resolvido na interface | alerta SQL/serviço aceita ativo/reconhecido; não há resolvido | exibir status existente; nenhuma ação de reconhecimento/resolução |
| Consumo ignora deltas negativos | endpoint retorna 422 para reset ou troca de ponto | não reproduzir política divergente como consumo oficial |
| Tarifa simulada, PDF com cálculo local | API consumo exige tarifa ativa única cobrindo todo período | distinguir simulação e estimativa calculada na API; tratar ausência/conflito |
| Conectividade mostra estado, parâmetros e eventos | API retorna status armazenado; view usa idade 120s; tabelas de configuração/eventos sem rota | distinguir fontes; sem histórico ou status inferido silenciosamente |
| PUT configuração/fallback PUT status | rotas não montadas no servidor analisado | não portar; ambas seriam escrita de negócio |
| Edição descricao de equipamento | coluna existe; service/repository de escrita não a persistem | ficha consultiva pode ler valor disponível, sem portar edição |

Backend não consome as 12 views do script nos repositórios lidos. Filtros e cálculos Desktop são evidência de intenção da interface, não contrato do servidor. A diferença entre views, service de consumo e código de relatório está documentada com fontes; nenhuma harmonização foi implementada. Fonte primária de contrato é a rota/controller/service/repository correspondente, ainda dependente de validação contra ambiente autorizado e schema implantado.

JWT e perfil são efetivos em parte do servidor, com filtragem por local especificamente no módulo Locais. Outros grupos têm proteção incompleta ou listas globais. Adiar sessão definitiva Android não autoriza explorar essa abertura nem relaxar o servidor. Desktop continua exclusivamente referência de visual/experiência; não foi compilado, executado ou modificado nesta etapa.

## 14. Atualização Mobile — adequação consultiva

Development Mode aprovado manualmente pelo usuário. Etapa 09 solicitada (adequação consultiva, etapa 3 do roadmap) implementada e aguardando validação no emulador. Removidos CRUD local, formulários e handlers administrativos de tarifas/usuários/pontos do Mobile; empresa e fuso passam a somente leitura. Preservadas abas/cards de consulta e preferências pessoais separadas, sem nova integração ou redesenho Desktop. Nenhuma mudança no Desktop.

A existência de interfaces administrativas na referência não autoriza o Mobile a oferecê-las, inclusive em demonstração. Configurações Mobile segue consulta demonstrativa apenas com opt-in Debug; fora disso, integração indisponível explícita. Formato de data pessoal continua limitado às tarifas na sessão e seleção de tema não altera o tema global, com aviso na interface. Evolução dessas preferências permanece no módulo correspondente. Evidências atuais em PROJECT_CONTEXT.md, seção 15, e ROADMAP.md, seção 8; matrizes anteriores permanecem histórico.

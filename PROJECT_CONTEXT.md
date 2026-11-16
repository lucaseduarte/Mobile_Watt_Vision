# Watt Vision Mobile — contexto validado

Estado atual: Development Mode aprovado manualmente pelo usuário. Adequação exclusivamente consultiva implementada, aguardando validação no emulador (seção 15). Essa seção prevalece sobre os achados históricos de CRUD e edição corporativa.

Atualização documental do ecossistema em 16/11/2026: contratos do Backend e estrutura do script agora mapeados em `BACKEND_REFERENCE.md` e `DATABASE_REFERENCE.md`. A seção 14 complementa/supera lacunas históricas sobre inexistência de contratos; a seção 13 continua sendo a fonte do comportamento Mobile implementado. Nenhuma implementação adicional foi feita.

Análise inicial em 16/11/2026, diretamente no repositório. O relato do agente anterior foi tratado como hipótese e corrigido abaixo. Este documento descreve o estado existente, inclusive comportamentos incompatíveis com o produto desejado; não os autoriza. Regras permanentes em `AGENTS.md`.

Atualização de continuidade em 16/11/2026: análise comparativa do Desktop concluída, sem implementação. Fonte oficial: `/Users/25203646/Documents/Front_WV`, exclusivamente leitura. Mapeamento completo em `DESKTOP_REFERENCE.md`; prioridades, dependências e aceite incremental em `ROADMAP.md`. As seções 1–11 preservam o diagnóstico Mobile inicial; a seção 12 registra decisões/evidências comparativas. A seção 13 descreve o Development Mode implementado e prevalece sobre os achados históricos que foram corrigidos.

## 1. Finalidade e limites da análise

Android nativo do TCC Watt Vision, destinado à consulta e monitoramento energético industrial e comercial em campo. Gestão, administração e configuração corporativa pertencem ao desktop. Mobile permite consultas de indicadores, medições, alarmes, equipamentos, locais, setores, tarifas, usuários e pontos conforme permissões, conectividade e relatórios; filtros, pesquisa, ordenação, simulações/cálculos sem escrita, sessão própria e preferências locais.

Na análise inicial foram inspecionados a estrutura do repositório, configuração Gradle, manifesto, recursos, serviços, repositórios, modelos, ViewModels, estados, navegação, telas, componentes, tema, utilitários e testes existentes. O Desktop foi disponibilizado e analisado na continuidade (seção 12). Não foram consultados servidores ou credenciais; não há validação de respostas reais, unidades ou permissões do servidor. Inspeção estática não equivale a validação visual em dispositivo.

## 2. Stack e configuração declaradas

| Item | Estado no repositório |
| --- | --- |
| Projeto/módulo | `Watt_Vision_Mobile`, único módulo `:app` |
| Namespace/applicationId | `com.example.watt_vision_mobile` |
| Linguagem/UI | Kotlin, Jetpack Compose, Material 3 |
| Organização | MVVM com ViewModel/AndroidViewModel, StateFlow e Coroutines |
| Build | AGP 9.5.1, Gradle Wrapper 9.6.0, plugin Kotlin Compose 2.2.10 |
| Compose BOM | 2026.02.01 |
| Android | minSdk 26, compileSdk/targetSdk 37, versão 1.0 (código 1) |
| JVM | daemon JDK 25 via `gradle-daemon-jvm.properties`; source/target Java 11 |
| Rede | Retrofit 2.11.0, Gson, OkHttp 4.12.0 |
| Coroutines | 1.7.3 |
| AndroidX declarados | Core 1.10.1, Lifecycle runtime 2.6.1, Activity Compose 1.8.0 |
| Testes | JUnit 4.13.2, AndroidX JUnit 1.1.5, Espresso 3.5.1, Compose UI Test |

Versões acima são as declarações locais, não garantia de resolução ou compatibilidade. Não há camada domain/use cases, DI com Hilt/Koin, Navigation Compose, Room, DataStore, WebSocket ou serviço de monitoramento em background implementados nas fontes analisadas. Dependências são criadas diretamente pelos ViewModels/repositórios.

## 3. Estrutura e responsabilidades

Raiz contém scripts Gradle, wrapper, catálogo `gradle/libs.versions.toml`, `.gitignore` e configuração local de SDK não versionada. Diretórios `build`, `.gradle`, `.kotlin` e `.idea` contêm saídas/configurações locais, não funcionalidades de produto.

Base Kotlin: `app/src/main/java/com/example/watt_vision_mobile/`.

```text
MainActivity.kt
data/
  TokenManager.kt
  api/{ApiClient,AuthApiService,AuthInterceptor,WattVisionApiService}.kt
  repository/{AuthRepository,DashboardRepository,
              OperacionalRepository,EquipamentoDetalheRepository}.kt
model/
  ApiResponse.kt, LoginRequest.kt, LoginResponse.kt, UsuarioDto.kt, User.kt
  MedicaoDto.kt, ConsumoDto.kt, TarifaDto.kt, EquipamentoDto.kt, AlertaDto.kt
ui/
  components/
    WattVisionBottomBar.kt, WattVisionDrawer.kt
    filtros/{FiltroLinhaSeletor,FiltroSeletorBusca}.kt
  screens/
    login/{LoginScreen,LoginViewModel}.kt
    dashboard/{DashboardScreen,DashboardViewModel,DashboardUiState}.kt
      components/{DashboardHeaderAndFilters,FiltroSeletorBusca,
                  StatCardsSection,SystemPerformanceChart,RecentAlarmsSection,
                  IndicadoresEnergeticosSection,CalculoButtonAndModal}.kt
    operacional/{OperacionalScreen,OperacionalViewModel,OperacionalUiState,
                 LocalDetalheScreen,SetorDetalheScreen}.kt
      components/{OperacionalCards,OperacionalFilterSheet}.kt
    equipamento_detalhe/{EquipamentoDetalheScreen,
                         EquipamentoDetalheViewModel,EquipamentoDetalheUiState}.kt
      components/EquipamentoDetalheComponents.kt
    configuracoes/{ConfiguracoesScreen,ConfiguracoesViewModel,ConfiguracoesUiState}.kt
    placeholder/PlaceholderScreen.kt
  theme/{Color,Theme,Type}.kt
util/{DateFormatter,StringNormalization}.kt
```

`User.kt` contém apenas a declaração de pacote, sem classe User. Estados também declaram modelos de apresentação: itens operacionais, alarmes, filtros, cards e configurações. Interfaces dos repositórios mock dependem de modelos em `ui/screens`, indicando acoplamento entre dados e apresentação.

Recursos: `res/drawable` inclui logo, fundo de login, imagem de dashboard e ícones launcher; `mipmap-*` contém ícones por densidade. `values` contém nome do app, cores XML de template e tema Android sem ActionBar. `xml` contém regras padrão de backup/extração. Grande parte dos textos está diretamente nos composables.

## 4. Navegação, sessão e fluxo de dados

Single Activity com `setContent`, `WattVisionTheme` e `Scaffold`. Rotas são Strings controladas por `remember`; seleção do ID de equipamento usa `rememberSaveable`, enquanto local e setor selecionados ficam em memória. A barra inferior contém Dashboard, Operacional, Medições, Conectividade e Relatórios. Dashboard e Operacional têm atalhos para outras rotas; Configurações tem implementação própria. Alarmes, Medições, Conectividade e Relatórios caem no `PlaceholderScreen`.

`WattVisionDrawerContent` existe, inclusive callback Sair, mas não é conectado à árvore da MainActivity. Não existe pilha de navegação central. Detalhe de equipamento trata voltar do sistema via `BackHandler`; os demais detalhes não possuem o mesmo tratamento explícito.

Fluxo geral: Composable → ViewModel → Repository → Retrofit/OkHttp → DTO → estado StateFlow → Composable. Nas áreas simuladas, o repositório gera modelos da UI diretamente, sem API. ViewModels são mantidos no escopo da Activity. Dashboard e detalhe iniciam polling no `init`; não há pausa explícita desses jobs ao trocar de tela.

### Autenticação

- `LoginScreen`: e-mail, senha, visibilidade de senha, lembrar de mim, carregamento e erro. Recuperação de senha recebe callback vazio por padrão e não é conectada na Activity.
- `LoginViewModel`: valida campos não vazios, chama AuthRepository e sinaliza sucesso. Lembrar de mim só altera estado da UI, sem condicionar persistência.
- `AuthRepository`: login em IO, armazena token quando presente; logout remove token local; `isLoggedIn` testa apenas presença de token. Esses dois últimos métodos não estão integrados ao fluxo da Activity.
- `MainActivity` inicia `isLoggedIn = true`, portanto exibe o Dashboard diretamente. Não chama `/me` nem valida token na inicialização.
- `TokenManager`: SharedPreferences padrão, sem criptografia. Não há renovação de token, verificação de expiração ou tratamento central de 401. Login com `sucesso=true` e token ausente ainda pode retornar sucesso.

## 5. API e modelos

`ApiClient` mantém singleton Retrofit, URL HTTP fixa de rede local, Gson, AuthInterceptor e logging BODY. Timeouts de conexão/leitura/escrita: 5 segundos. O host não é reproduzido aqui; consultar configuração existente quando necessário. Manifesto permite Internet e tráfego cleartext.

Contratos **declarados no cliente**, ainda sem confirmação no servidor:

| Método e rota | Parâmetros/retorno declarado | Uso atual |
| --- | --- | --- |
| POST `api/auth/login` | corpo LoginRequest(email, senha); LoginResponse | AuthRepository |
| GET `api/auth/me` | ApiResponse<UsuarioDto> | declarado, sem chamada |
| GET `api/medicoes/resumo` | ApiResponse<MedicaoDto> | Dashboard |
| GET `api/medicoes/ultima` | ApiResponse<MedicaoDto> | Dashboard |
| GET `api/medicoes/serie-temporal` | id_pzem, horas; lista MedicaoDto | gráfico Dashboard |
| GET `api/medicoes/consumo` | id_pzem, inicio, fim; ConsumoDto | método no repositório, sem uso na UI |
| GET `api/equipamentos` | lista EquipamentoDto | Dashboard; Operacional usa mock |
| GET `api/alertas` | lista AlertaDto | declarado, sem chamada |
| GET `api/alertas/ativos` | lista AlertaDto | Dashboard |

Não há endpoints declarados para locais, setores, CRUD, listagem administrativa de usuários/tarifas/pontos ou detalhe de equipamento. Não inferir contratos para substituir mocks.

- ApiResponse: sucesso, mensagem e dados genéricos. LoginResponse contém AuthData(token, tipo, usuario).
- UsuarioDto: IDs de usuário/empresa, nome, e-mail, perfil e status.
- EquipamentoDto: IDs, nome, tipo, status e localização; não contém toda a ficha exibida pelo detalhe mock.
- MedicaoDto: IDs de medição/PZEM/ponto, data_hora e grandezas elétricas; aceita aliases Gson e valores Number/String por campos `Any?`, convertidos para Double. Conversão de String usa `toDoubleOrNull`.
- ConsumoDto: período, quantidade e extremos das medições, consumo_kwh, tarifa, custo_estimado e moeda. TarifaDto contém id, descricao e valor_kwh.
- AlertaDto: IDs, tipo, mensagem, severidade, data_hora e resolvido.
- Repositórios de Dashboard usam HTTP/body como critério de sucesso sem validar consistentemente o campo `sucesso`. Capturam Exception genericamente, inclusive possíveis cancelamentos de coroutine.

## 6. Telas e origem das informações

| Área | O que existe | Origem/limitações |
| --- | --- | --- |
| Dashboard | cabeçalho, filtros, composição, cards, gráfico Canvas interativo e alarmes recentes | API misturada a fallbacks fictícios |
| Operacional | Pager com equipamentos/setores/locais, busca normalizada, status, filtros em bottom sheet, estados vazio/carregamento | MockOperacionalRepository; valores e contagens aleatórios |
| Local e setor | resumo, identificação, contagens, consumo e alarmes | item mock recebido; alarmes e responsáveis fixos; endereço local fixo; não listam efetivamente entidades filhas |
| Detalhe equipamento | status cadastral e de conexão, última comunicação, alarmes, ficha técnica, vínculos e descrição | MockEquipamentoDetalheRepository; não há telemetria instantânea real nesta tela |
| Configurações | abas Geral, Tarifas, Usuários e Pontos de Medição; formulários e ações de gestão | listas iniciais fixas, alterações apenas em StateFlow/memória |
| Demais rotas | mensagem de módulo em desenvolvimento | placeholder |

### Dashboard

Polling executa chamadas sequenciais e espera 3 segundos ao final do ciclo; não é streaming nem intervalo garantido de três segundos entre respostas. Seleciona PZEM pela última medição/resumo e assume ID 1 se ausente. Cards atuais: consumo total, dispositivos ativos, potência ativa, tensão atual, corrente média e frequência.

Gráfico oferece tensão, consumo, potência e corrente, com períodos 1h, 6h, 24h, 7d e 30d. Usa série temporal da API, sem geração de pontos quando vazia. Modal “Ver composição” soma valores e apresenta cobertura de entidades; **não é calculadora tarifária**. `IndicadoresEnergeticosSection` existe e o ViewModel produz indicadores, mas DashboardScreen não chama essa seção.

### Operacional e detalhes

Filtros operacionais aplicam busca por nome/código sem acentos, status e relações por nomes; abas/filtros são escritos em SavedStateHandle. Dados gerados em chamadas separadas podem divergir, inclusive contagens e vínculos. Há campo equipmentFilter no estado, sem aplicação nas funções de filtragem.

Detalhe faz polling inicialmente a cada 5 segundos com backoff até 30 segundos em falhas e cronômetro local para idade da comunicação. Estados INITIAL_LOADING, UPDATED, STALE e TOTAL_ERROR. Mock simula online/offline, falhas periódicas, campos vazios e novo alarme crítico; isso não comprova conectividade real. Componentes ordenam alarmes por severidade e timestamp, exibem inicialmente três, permitem expansão, cópia local de identificadores e expansão da descrição.

### Configurações

Geral permite editar nome da empresa, fuso, formato de data e tema; salvar/cancelar geral só copia estado em memória. DataFormatter é usado nas vigências das tarifas; preferência não é global. Escolha Claro/Escuro não altera WattVisionTheme, que sempre usa esquema escuro. Salvar/Cancelar geral não implementa transação das listas: operações de tarifas/usuários/pontos atualizam suas listas imediatamente em memória.

## 7. Violações da regra de consulta

Não foram encontrados PUT/PATCH/DELETE ou POST de negócio nas interfaces Retrofit. Isso limita o efeito atual das ações administrativas à memória, mas não torna sua experiência compatível com o escopo.

| Ação incompatível | Evidência | Efeito atual e orientação futura |
| --- | --- | --- |
| Alterar nome da empresa e fuso como configuração corporativa | ConfiguracoesScreen: aba Geral; ConfiguracoesViewModel.updateNomeEmpresa/updateFusoHorario | local; remover edição corporativa. Fuso só pode ser preferência pessoal se definido assim |
| Criar/editar/excluir/ativar tarifas | FormTarifaCard, abrirFormTarifa, salvarTarifa, excluirTarifa | local; preservar consulta de tarifas/histórico com dados reais |
| Gerenciar usuários, perfis e status | FormUsuarioCard, salvarUsuario, excluirUsuario | local; retirar gestão do mobile |
| Criar/editar/excluir/configurar pontos, escopo, medidor e status | FormPontoCard, salvarPonto, excluirPonto | local; preservar apenas consulta de pontos |

Nenhuma correção foi aplicada nesta etapa. Preferências pessoais de aparência/data, pesquisa, filtros, atualização de consulta, expansão e cópia local são compatíveis com o escopo quando implementadas como tal.

## 8. Diagnóstico técnico priorizado

Prioridades sugeridas para próximas instruções, sem autorização implícita de implementação:

1. **Escopo do produto:** retirar fluxos administrativos descritos acima, preservando consultas pertinentes.
2. **Confiabilidade dos dados:** Dashboard cria alarmes fictícios inclusive quando a API retorna lista vazia legítima; composição usa locais fixos quando vazia; indicadores assumem valores elétricos fixos ausentes e frequência padrão. Ausência de conexão também vira zeros em alguns cards. Não apresentar isso como dado real.
3. **Sessão:** substituir primeiro a entrada autenticada fixa por Development Mode exclusivo Debug, com Release sem bypass. Validação/restauração/logout definitivos, expiração e renovação ficam entre as últimas etapas conforme decisão de continuidade e contratos reais.
4. **Exposição de dados:** BODY logging está ativo sem restrição explícita a debug. Redação de Authorization/Cookie não protege senha no corpo de login nem token no corpo da resposta. HTTP sem TLS, SharedPreferences sem criptografia e backups habilitados sem exclusão explícita do token merecem revisão antes de uso real. Não se afirma exploração nem exposição observada.
5. **Seleção do equipamento:** `EquipamentoDetalheViewModel.equipamentoId` é `val` inicializada uma vez. `setEquipamentoId` altera SavedStateHandle e reinicia loops, mas fetchData usa a propriedade antiga; pode continuar exibindo equipamento inicial ao selecionar outro.
6. **Filtros de Dashboard:** filtros de período/local/setor/equipamento mudam estado e reiniciam polling, mas não são usados para limitar as chamadas/dados. Opções dos seletores são fixas. “30d” consulta 168 horas, igual a “7d”.
7. **Semântica dos indicadores:** “Dispositivos Ativos/Conectados” conta toda a lista sem filtrar status; energia de uma medição é rotulada como consumo total do sistema. Potência é rotulada em kW sem conversão explícita; unidade real precisa ser confirmada. Equipamentos da API recebem valor zero na composição sem cálculo de consumo.
8. **Atualização e erros:** polling fica no escopo da Activity, sem pausa explícita na saída da tela. Dashboard pode reter série antiga após falha e ainda sinalizar “Ao vivo”, sem indicador consistente de dado desatualizado. Exceções podem ser ocultadas; erros de autenticação não são centralizados.
9. **Restauração de estado:** DashboardFilterState e TabFilterState são data classes simples, não Parcelable/Serializable; escritas diretas no SavedStateHandle exigem verificação, com risco de falha em filtros/restauração. Parte da navegação usa apenas remember e não sobrevive a recriação.
10. **Reuso e manutenção:** FiltroSeletorBusca/FiltroItemModel e FiltroLinhaSeletor possuem variantes no pacote dashboard e em components/filtros. ConfiguracoesScreen concentra 1.236 linhas. Registrar duplicação sem fazer refatoração arquitetural agora.
11. **Dependências/testes:** imports de APIs Compose de Lifecycle (ex.: collectAsStateWithLifecycle) não têm dependência lifecycle-runtime-compose explicitamente declarada; o build atual resolveu essas APIs e passou, portanto não foi constatado erro de compilação por isso. Só existem testes de template, sem cobertura de negócio, sessão, filtros, ViewModels ou contratos.

## 9. Identidade visual e componentes reutilizáveis

Tema exclusivamente escuro em `ui/theme/Theme.kt`. Paleta validada em Color.kt: DarkBackground #070C15, InputBackground #0D1829, CardBackground #0C141F, CardBorder #1B2A40, SurfaceDark #0D151F, PrimaryBlue #2B66C6, ToneCyanText #22D3EE, ToneGreenText #42C879, ToneRedText #E47D8B e ToneYellowText #D5B86C. Tipografia base com FontFamily.Default; bodyLarge de 16sp. Não há integração do seletor Claro/Escuro com o tema global.

Reusar componentes de cards, badges, campos/grupos de informação, alarmes expansíveis e seletores com busca/debounce de 200 ms. Há uso de LazyColumn, HorizontalPager, ModalBottomSheet, Canvas, estados vazios e algumas descrições/semânticas de acessibilidade. A consistência completa e o comportamento em diferentes dispositivos ainda precisam de avaliação visual.

## 10. Correções do contexto anterior

- O objetivo mobile não inclui controle nem gestão administrativa.
- Operacional/detalhe não são integrações reais; configurações não persistem na API.
- AuthApiService não possui renovação de token; apenas login e me.
- TokenManager usa SharedPreferences comum, não EncryptedSharedPreferences.
- Drawer existe como componente, mas não está conectado à Activity.
- Não há classe User implementada; User.kt está vazio além do pacote.
- Dashboard renderiza seis cards diferentes dos quatro descritos anteriormente; gráfico oferece 1h/6h/24h/7d/30d, não 12m.
- CalculoButtonAndModal é composição/soma, não simulação de custo por tarifa vigente.
- IndicadoresEnergeticosSection não é renderizada no Dashboard atual; detalhe não apresenta medições instantâneas reais.
- Preferências de tema/data não estão aplicadas globalmente.

## 11. Validação e próximos passos

Somente `AGENTS.md` e `PROJECT_CONTEXT.md` foram criados nesta etapa. Sem chamadas ao backend, sem alterações de fontes, recursos ou configuração. A execução do Gradle gerou saídas locais de build e relatórios ignorados pelo Git.

Comando concluído com **BUILD SUCCESSFUL**:

```bash
bash ./gradlew --no-configuration-cache :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

- APK debug compilado; Gradle reaproveitou tarefas de compilação Kotlin já atualizadas.
- Teste unitário existente: 1 executado, zero falhas/erros; valida apenas `2 + 2 = 4`.
- Lint: zero erros e 35 avisos. Incluem dependências com versões mais novas, ordem de Modifier, recursos não usados, bitmaps sem densidade, extensão PNG com conteúdo JPEG e convenções Gradle/KTX.
- Limitação do lint: checks `UnsafeLifecycleWhenUsage` e `RepeatOnLifecycleWrongUsage` foram ignorados por incompatibilidade do lint customizado da biblioteca Lifecycle. Sucesso da tarefa não significa cobertura integral desses checks.
- Relatórios: `app/build/reports/lint-results-debug.html`, `app/build/reports/lint-results-debug.txt` e `app/build/reports/tests/testDebugUnitTest/index.html`.
- Primeira tentativa no sandbox foi bloqueada pelo lock do cache Gradle fora do workspace. Após permissão, modo offline parou por ausência de material-icons-extended-desktop no cache e erro associado de configuration cache. A execução acima, com resolução de dependências e sem configuration cache, concluiu sem modificar configuração do projeto.
- Testes instrumentados não executados; exigem dispositivo/emulador. Não foi realizada validação visual ou funcional em dispositivo, nem teste de integração com API.
- `git diff --check` sem problemas; status final contém somente os dois novos documentos.

Referências Desktop já recebidas e analisadas na continuidade abaixo. Para substituir mocks, confirmar contratos reais e envelopes observados de locais/setores/equipamentos e obter os ainda ausentes de detalhes/tarifas/pontos/usuários/conectividade; confirmar relações PZEM/equipamento, unidades elétricas, agregações, autorização e política de sessão. Não inventar esses dados a partir da interface desktop.

## 12. Continuidade — comparação com Desktop e decisões permanentes

### Fonte oficial, revisão e escopo

`/Users/25203646/Documents/Front_WV` é referência permanente visual, funcional e de regras de negócio, exclusivamente leitura. Implementações futuras só no Mobile e com autorização incremental. Ignorar dependências, caches, arquivos gerados/agregados e credenciais. Backend não foi acessado/modificado.

A verificação por hashes detectou atualização externa do Desktop durante a análise. O mapeamento foi revisto sobre o commit observado `cfa1707` (Ajustes no modo claro), incorporando Conectividade, limites por API, auth/session, tema claro e Relatórios/PDF novos. As diferenças de conteúdo não foram causadas por este agente. `repomix-output.xml` já aparecia não rastreado e foi ignorado/preservado. Revalidar a revisão ao iniciar cada módulo.

Desktop usa React/Vite/Tailwind, Axios, Lucide, Recharts e PDF local com jsPDF/Chart.js. Estado local de componentes; App controla navegação e tema, sem router/store de negócio global. Detalhes, versões e evidências em `DESKTOP_REFERENCE.md`.

### Matriz comparativa atual

| Área | Desktop final | Mobile atual / direção |
| --- | --- | --- |
| Navegação/visual | sidebar/topbar, busca de páginas e perfil demonstrativo; tema claro/escuro persistido; Plus Jakarta Sans declarada/Lucide | barra inferior/rotas locais, fonte padrão/Material Icons, só escuro; preservar UX nativa e padronizar |
| Dashboard | quatro KPIs tensão/ativos/offline/FP, seis grandezas; indicadores indisponíveis e composição exemplo | seis KPIs diferentes, quatro grandezas, indicadores não renderizados, falsos fallbacks; parcial |
| Operacional | GET listas/relações por IDs; criação/edição de equipamentos, setores e locais | Pager/cards/busca/sheets consultivos com mocks; preservar interação e integrar fontes reais |
| Detalhes | equipamento editável com status/alarmes fixos; setor/local abrem formulário | detalhes consultivos dos três com mocks; corrigir ID e fonte de dados |
| Medições | série real, seis grandezas, gráfico por dia, cards/dados brutos; limites/metas consultados e alterados via API | placeholder; construir consulta sem configurar regras |
| Alarmes | lista/KPIs/detalhe/filtros via API e PATCH reconhecimento | componentes parciais no Dashboard/detalhe; rota placeholder; não portar PATCH |
| Conectividade | GET dispositivos/pontos/alertas, KPIs/status/histórico/detalhes e gestão administrativa | placeholder; adaptar somente consultas; confirmar regra servidor |
| Relatórios | deltas por medidor/dia, seis grandezas, conversão de potência, escalas/tarifa locais, PDF avançado | placeholder; confirmar semântica e divergência PDF multimedidor |
| Configurações | listas locais: tarifas vazias, usuários/pontos exemplos; tema global efetivo | listas exemplo/CRUD local; tornar consultivo e obter dados/permissões |
| Sessão | serviços login/me e Bearer global; Login/App ainda demonstrativos; logout limpa storage | login HTTP e token existentes, entrada inicial fixa; Dev Mode pendente, sessão definitiva final |

Elementos Mobile aproveitáveis: cards, Pager, filtros com busca sem acentos/debounce, sheets, badges, InfoCampo/InfoGrupoCard, cópia e descrição/alarmes expansíveis. Nenhum módulo foi considerado integralmente validado em emulador/API pela inspeção.

### Contratos e escritas: evidências novas

- GET de locais/setores/equipamentos aceita array ou envelopes dados/nome plural em parte das páginas; IDs id/id_local/id_setor/id_equipamento precisam normalização confirmada. Equipamento → setor → local por IDs reais; DTO Mobile atual é insuficiente para toda a ficha.
- Conectividade consulta GET `/api/dispositivos-iot`, detalhe `/api/dispositivos-iot/{id}`, `/api/pontos-medicao`, listas operacionais e alertas. Relaciona ponto.id_dispositivo_iot ao dispositivo e IDs de equipamento/setor/local; prioriza vínculo do banco e hierarquia atual, com cache local de fallback que não deve ser tratado como verdade do servidor.
- Status de dispositivo vem de status textual (Online/Instável/Offline) e data_comunicacao; tempos 5/2/3 minutos são defaults de configuração, sem GET de configuração observado. Não comprovam regra backend de disponibilidade.
- GET `/api/limites` agora existe no frontend: modal lê dados[] com id, descricao, grandeza, operador, valor, severidade, periodo_inicial/final, ativa e IDs de vínculo. Só consulta pode ser portada.
- Serviços POST `/api/auth/login` e GET `/api/auth/me` validam formatos e persistem token/usuário conforme lembrar; não são chamados pelo Login/App atual. Axios envia Bearer globalmente. Refresh continua sem endpoint identificado.
- Alarmes Desktop normaliza id_alerta/id, status textual, categoria, equipamento/local aninhados ou String e reconhecimento; Mobile usa DTO menor (incluindo resolvido Boolean). Confirmar aliases e resposta real.
- Tarifas/usuários e leitura de configuração global ainda não têm serviços identificados. GET pontos em Conectividade não garante todos os campos da aba Configurações. Cadeia PZEM/ponto/dispositivo/equipamento permanece incompleta.

Escritas Desktop que não devem ser portadas: POST/PUT operacionais; PATCH reconhecimento; POST/PUT de limites e PATCH de ativação/desativação (inclusive pelo ícone de lixeira); POST/PUT/DELETE de dispositivos e PATCH/PUT de status Wi-Fi/MQTT; POST ponto associado e PUT configuração global. CRUD de configurações locais também é incompatível com a experiência consultiva.

Mobile não possui escrita de negócio Retrofit identificada; ConfiguracoesViewModel altera listas locais. Adequação deve alcançar UI, ViewModels e camada de dados. Registrar necessidade de autorização somente leitura e isolamento por usuário/empresa/local no backend; não modificar servidor sem autorização nem alegar segurança comprovada por ocultar botões.

### Cálculos, dados fictícios e limitações

- Desktop final converte potência de W para kW por /1000 em Relatórios. Mobile usa kW sem conversão: confirmar unidade servida e alinhar.
- Tela Relatórios soma deltas não negativos consecutivos por medidor/dia local; primeiro valor é base, reset não gera consumo. Pico é maior consumo diário. Variação usa linha calculada anterior. Os problemas antigos de max−min, variação sempre zero e agrupamento UTC foram corrigidos externamente e não representam a revisão final.
- PDF ainda calcula deltas numa sequência global sem separar medidores, podendo divergir da tela. Usa faixas fixas 198–242 V, 59,9–60,1 Hz e FP >=0,92 para inferências locais, sem comprovar limites operacionais reais.
- Tarifa é entrada simulada local persistida, default 0,80, não tarifa vigente de API. Simulação explicitamente identificada pode ser permitida no Mobile; edição de tarifa corporativa não.
- Medições suporta seis grandezas e recorta Hoje/Ontem por dia civil no gráfico; cards/dados brutos usam a série completa de 24/48 h. Nulos/vazios ainda podem virar zeros.
- Dashboard mantém composição de exemplo e indicadores indisponíveis. Detalhe operacional contém status/alarmes fixos. Conectividade tem defaults de modelo/rede/firmware e histórico de datas visualmente selecionáveis sem filtro real; falhas podem virar vazio ou ficar ocultas.
- Tema claro/escuro Desktop agora é efetivo/persistido; formato de data geral não é aplicado globalmente. Mobile continua com seletor inoperante e variantes de filtros duplicadas.

### Development Mode e ordem de trabalho

Planejado, não implementado: abrir Dashboard diretamente apenas Debug, preservar login/estrutura e navegação por todos os módulos. Release sem sessão abre login, sem bypass disponível. Consultas protegidas exigem sessão válida; sem credenciais/tokens fixos ou relaxamento do servidor. Demonstração só para teste visual Debug, explicitamente escolhida e identificada, sem fallback por falha/vazio/401/403.

Autenticação definitiva, validação, refresh se houver contrato, expiração, armazenamento seguro e logout ficam entre últimas etapas. Exposição de dados por logging deve ser corrigida antes. A atual entrada isLoggedIn=true não satisfaz Development Mode.

Sequência: Dev Mode → escopo consultivo → Dashboard → Operacional → detalhes → Medições → Alarmes → Conectividade → Relatórios → Configurações consultivas → revisão API → visual/reuso → autenticação → segurança → validação final. Integrar/testar em cada módulo; revisão transversal não adia dados reais. Critérios em `ROADMAP.md`.

### Resultado desta etapa

Somente os quatro documentos solicitados no Mobile foram criados/atualizados. Não houve implementação, alteração de fontes/recursos/configurações por este agente, execução de build/teste Desktop nem chamada ao backend. Verificação documental: 128 arquivos do baseline Mobile e estado Desktop revisto comparados por hashes, sem mudança posterior à revisão; referências entre documentos existentes, cercas Markdown balanceadas e ausência de espaços finais. Status Mobile contém apenas os quatro documentos não rastreados; status Desktop mantém o agregado não rastreado preexistente. Não foram repetidos builds/testes de aplicação por mudança apenas documental; resultado da seção 11 permanece histórico.

Aguardar autorização para a próxima etapa; usuário valida cada interface no emulador antes do avanço.

## 13. Development Mode — implementação autorizada

Estado: implementado, aguardando validação do usuário no emulador. Apenas esta etapa foi iniciada; não houve nova integração de módulo, CRUD remoto ou alteração Desktop/backend.

### Acesso e variantes

- Gradle gera BuildConfig. MainActivity inicia isLoggedIn=false; AppAccessPolicy só permite entrada sem login quando BuildConfig.DEBUG é verdadeiro e o usuário não pediu para ver o login. Não há flag de intent, preferência ou endpoint que habilite o bypass em Release.
- Debug inicia no Dashboard com barra “Desenvolvimento · Debug”. Menu Módulos permite alcançar Dashboard, Operacional, Medições, Alarmes, Conectividade, Configurações e Relatórios; rotas sem implementação seguem placeholders.
- “Entrar na API” abre a tela de login existente; “Voltar ao desenvolvimento sem login” ou voltar Android retorna ao conteúdo Debug. Um login real obtém token pela API existente, utilizado pelo interceptor; não foi inserida sessão/usuário/credencial fixa. Token permanece no mecanismo existente; armazenamento seguro, refresh, expiração e logout definitivos continuam posteriores.
- Release tem BuildConfig.DEBUG=false, abre login e não contém os controles Debug; src/release fornece composable vazio compatível com a assinatura de src/debug. Sucesso de login agora exige sucesso=true e token não vazio, evitando que resposta sem token libere o conteúdo. Não há restauração automática de sessão implementada nesta etapa: Release volta a exigir login ao recriar a Activity.

### Dados e limites de escopo

- Dashboard continua consultando os endpoints existentes, sem implementar o redesign/contrato completo. Fallbacks fictícios de alarmes, composição, indicadores e frequência foram retirados; falha/ausência vira erro, indisponível, lista vazia ou N/A. Série falha não reaproveita silenciosamente a anterior; sem ID de medidor retornado não consulta PZEM 1 arbitrário.
- Repositório Dashboard valida sucesso do envelope e preserva código HTTP para diferenciar 401/403; cancelamento é propagado. Mensagens não incluem corpos/respostas sensíveis. Se backend exigir login, abrir a UI Debug não elimina essa exigência.
- Composição permanece sem fonte real e seu botão fica oculto enquanto vazia, em vez de exibir valores inventados. Alarmes distinguem indisponibilidade de resposta vazia.
- Filtros por entidade ainda usam opções demonstrativas preexistentes e não filtram API: aviso explícito no Dashboard; implementação efetiva está pendente. Cálculos/unidades/KPIs, período 30d, seleção de equipamento e restauração de filtros permanecem problemas dos módulos futuros.
- Operacional, seus detalhes e Configurações continuam com mocks originais. Só podem ser abertos em Debug após ligar “Liberar telas de demonstração”; há aviso permanente “DEMONSTRAÇÃO · Dados simulados, sem conexão com a API”. O seletor não muda Dashboard para mock. Desligado ou em Release, esses módulos mostram “Consulta à API ainda não integrada”, sem carregar/renderizar mocks. Não houve criação de novos fixtures.
- Formulários administrativos locais existentes não foram reimplementados/removidos; estão restritos à demonstração explícita Debug e aguardam etapa de adequação consultiva. Navegação não significa módulo concluído ou permissão concedida pelo servidor.
- Removido HttpLoggingInterceptor do cliente, incluindo BODY que poderia registrar senha/token. Não foi alterada base URL, contrato, autenticação servidor, persistência ou política de transporte.
- Ajuste necessário à compilação Release: fundo_login.png era JPEG; renomeado para fundo_login.jpg sem mudar bytes nem R.drawable.fundo_login/aparência.

### Arquivos desta implementação

- app/build.gradle.kts: geração de BuildConfig.
- MainActivity.kt e AppAccessPolicy.kt: decisão de acesso por build, login opcional Debug e proteção de demonstração.
- ui/components/DevelopmentModeBar.kt em src/debug e src/release: controles de desenvolvimento isolados por variante.
- ui/components/UnavailableDataScreen.kt: aviso de consulta ainda não integrada.
- data/api/ApiClient.kt: remoção de logging HTTP.
- data/repository/AuthRepository.kt e model/LoginResponse.kt: login sem token não autentica.
- data/repository/DashboardRepository.kt, DashboardViewModel.kt, DashboardUiState.kt, DashboardScreen.kt e components/RecentAlarmsSection.kt: ausência/falha de dados sem simulação silenciosa.
- src/test/.../AppAccessPolicyTest.kt: testes de isolamento Release/Debug, seleção explícita e resposta de login sem token. ExampleUnitTest preservado.
- res/drawable/fundo_login.png → fundo_login.jpg: correção apenas de extensão.
- Os quatro documentos permanentes foram atualizados para registrar estado e limites.

### Validação no emulador pelo usuário

1. Sincronizar Gradle e selecionar a variante debug do módulo app no Android Studio.
2. Executar no emulador: deve abrir Dashboard sem login e exibir barra Debug.
3. Testar Módulos e barra inferior. Sem demonstração, Operacional/Configurações mostram consulta não integrada; rotas futuras mostram placeholder.
4. Ligar “Liberar telas de demonstração”, abrir Operacional/detalhes/Configurações e conferir aviso persistente de dados simulados; desligar e confirmar retorno ao estado indisponível.
5. API indisponível ou sem sessão não deve gerar alarmes/medições fictícios; respostas 401/403 geram mensagem apropriada. Não é preciso entrar para navegar na UI Debug.
6. Se necessário consultar API protegida, usar “Entrar na API” e credenciais reais autorizadas, nunca valores fixos no código; voltar ao desenvolvimento não faz login no backend.
7. Release deve abrir login, sem barra, seletor ou botão de retorno Debug. Instalação Release depende de assinatura apropriada; o APK gerado pela verificação pode ser unsigned.
8. Validar visualmente e informar o resultado antes de avançar à etapa seguinte.

Resultado automatizado: Debug e Release compilados, seis testes passando, lint de ambas variantes com zero erros/34 avisos. Bytecode Release usa DEBUG=false e não chama controles Debug; APK Release sem textos/controles de desenvolvimento. Evidências e limitações em ROADMAP.md, seção 6. Não foram executados testes funcionais contra backend nem declarada validação visual em emulador nesta implementação.

## 14. Complementação técnica — Backend e banco de dados

### Referências e método

- Desktop `/Users/25203646/Documents/Front_WV`: HEAD `cfa17077f68cee4adae2388f7625a20d634b4433`; análise anterior reutilizada, fontes consultadas pontualmente.
- Backend `/Users/25203646/Documents/Back-End/back-end_wattVision`: HEAD `db9d479414ffd0e0ad427ea560ab0b0d9bd3c955`; leitura de rotas, controllers, services, repositories, middlewares, entrada HTTP e ingestão MQTT.
- Banco `/Users/25203646/Documents/BD/Script Completo.rtf`: texto extraído via textutil stdout, original preservado. Inventário: 19 tabelas, 12 views, uma procedure e um trigger, sem stored function. Seeds/registros não foram transcritos. SHA-256 e dicionário em DATABASE_REFERENCE.md.
- Mobile HEAD `7b023e9ce82413814440cea53c550265761dd4af`, com alterações locais preexistentes do Development Mode preservadas. HEAD não representa sozinho essas mudanças. Comparações pontuais em DTOs, Retrofit e DashboardViewModel.
- Fontes representam implementações locais e script, não ambiente implantado. Nenhuma requisição, execução SQL, servidor, migração, instalação ou teste de aplicação nesta etapa documental. Configurações potencialmente sensíveis não foram abertas para analisar valores.

### Arquitetura e contratos identificados

Node.js/Express → routes → controllers → services → repositories mysql2/promise → MySQL. MQTT recebe JSON e reutiliza medicaoService; SQL armazena ponto histórico junto à leitura. Trigger, se instalado, atualiza data_comunicacao; não atualiza status nem cria alarme. API entrega envelopes sucesso/dados/quantidade e a UI Mobile segue Retrofit/Repository/ViewModel/StateFlow/Compose. Android nunca conecta diretamente ao banco.

Existem GETs de detalhes de local/setor/equipamento, listagens por hierarquia, PZEMs, pontos por dispositivo, última leitura por ponto, empresas, tarifas, usuários e suas autorizações. Essas são evidências novas do Backend, não novas integrações Mobile. Locais são filtrados por empresa e autorizações do Gestor; empresas/tarifas/usuários exigem Administrador; alertas/limites exigem Administrador ou Gestor. Medições/setores/equipamentos/dispositivos/pontos/PZEMs não têm middleware de autenticação nas rotas lidas, inclusive para escrita. Falta proteção uniforme; GETs globais de vários módulos protegidos também não limitam empresa/local.

### Correções das hipóteses anteriores

| Hipótese/lacuna histórica | Evidência atual e impacto |
| --- | --- |
| Tarifas/usuários/empresa sem API conhecida | existem listagens e detalhes, restritos a Administrador; não liberar por suposição a Gestor |
| Detalhes sem endpoint comprovado | GET por ID de locais/setores/equipamentos existe; usar após validação de integração/permissão |
| 30d pode ser corrigido só trocando 168 por 720 | série suporta 1–168h e LIMIT 10000 ASC; precisa contrato adicional ou limitação explícita de produto |
| Filtros Desktop comprovam filtros servidor | controller da série só lê id_pzem/horas; nomes de local/setor/equipamento/grandeza são ignorados |
| Resumo é agregado do sistema | resumo/ultima usam a última leitura global; não representam soma ou média empresarial |
| Reset pode ser ignorado para consumo real | endpoint rejeita queda/mudança de ponto com 422; Desktop/views usam outra política |
| Conectividade segue defaults 5/2/3 minutos | API devolve status armazenado; view usa 120s; parâmetros não estão aplicados em rotina identificada |
| Alertas resolvidos disponíveis | script e service têm ativo/reconhecido, sem resolução; DTO resolvido Boolean não corresponde |
| Views e tabelas implicam funcionalidades expostas | eventos/configurações/relatórios persistidos não têm rotas identificadas; sem acesso direto Android |

Potência bruta é tratada como W pela conversão da view/relatório para kW; ingestão não converte escala. Mobile ainda rotula bruto como kW. Energia usada como kWh pelo consumo; firmware e escala real não validados. Não foram encontrados cálculos de potência reativa/aparente ou demanda. API de consumo soma diferenças entre leituras internas ao período civil, rejeita reset e exige tarifa ativa única que cubra todo intervalo; custo é consumo × tarifa, arredondado a duas casas. Menos de duas leituras retorna nulos e mensagem dentro de dados.

DTO de equipamento não cobre tipo_equipamento, id_setor e aliases nome_local/nome_setor; DTO de alerta não cobre status/tipo_alerta e referências de contexto. Medição/alerta BIGINT exigem revisão de Int. UsuarioDto.status não vem em /me; consumo não modela mensagem interna de insuficiência. Nulos, tipo decimal/data do driver e fuso precisam validação autorizada, sem registros sensíveis na documentação.

### Segurança, lacunas e estado preservado

Backend possui JWT HS256 com usuário ativo reconsultado, bcrypt e perfil, mas não autorização uniforme somente leitura/empresa/local. Reconhecimento aceita usuário do body; proibido no Mobile. Não houve alteração nem teste exploratório no servidor. Tabelas têm FKs/CHECK/UNIQUE, mas não garantem isolamento de empresas ou geração de alertas; há diferenças de status/faixas entre serviços e script. O banco efetivamente implantado permanece desconhecido.

Development Mode permanece implementado e aguardando validação no emulador. Nenhum módulo foi integrado, nenhum código alterado, nenhum novo build executado. Próxima etapa continua dependendo de autorização: validar modo Debug/Release e então adequar escopo consultivo incrementalmente, consultando as novas referências por módulo. Recomendações externas de segurança não autorizam modificar Backend/Desktop/banco.

AGENTS.md existe na raiz `/Users/25203646/Documents/Mobile_Watt_Vision/AGENTS.md`; suas instruções foram fornecidas no contexto desta sessão e lidas do arquivo. Isso confirma disponibilidade das diretrizes aqui, mas não comprova o mecanismo interno de descoberta do cliente. O relato anterior de `/status` com “Agents.md: none” não foi reproduzido nem explicado; não se atribui causa sem evidência e não foram alteradas configurações do Codex.

## 15. Etapa 09 solicitada — adequação exclusivamente consultiva

Aprovação recebida: Development Mode validado manualmente no emulador, todos os itens aprovados. Autorizada apenas adequação consultiva (etapa 3 do roadmap existente). Estado desta entrega: implementada, aguardando validação manual do usuário; sem avanço para módulo seguinte.

### Alterações de escopo

- ConfiguracoesScreen preserva Geral, Tarifas, Usuários e Pontos de Medição e os componentes/card/badges consultivos. Removidos botões de adicionar/editar/excluir, três formulários completos e seus campos/seletores administrativos. Nome da empresa e fuso corporativo agora são Text, sem callback de escrita. Título passa a “Consultas e preferências”. Barra Salvar/Cancelar aparece apenas em Geral e trata preferências locais.
- ConfiguracoesUiState remove seis campos de controle de formulário/ID em edição. ConfiguracoesGeralState mantém somente dados corporativos consultivos; novo ConfiguracoesPreferenciasState separa formato de data e seleção pessoal de tema. Exemplos de consulta existentes foram preservados, sem novos fixtures, sob opt-in Debug e identificação já existentes na MainActivity.
- ConfiguracoesViewModel remove abrir/fechar/salvar/excluir de tarifas/usuários/pontos, alteração de empresa/fuso e salvamento geral misturando preferências e negócio. Restam seleção de aba, atualização validada de formato/tema e salvar/cancelar exclusivamente preferências pessoais em memória.
- Abas consultivas recebem apenas uiState, sem ViewModel administrativo. Tarifas mantêm valores, descrição e vigência; usuários mantêm identidade/perfil/status/último acesso; pontos mantêm dispositivo/modelo/escopo/status. Badge de tarifa diz Ativa, sem afirmar vigência calculada; sem data final não implica “Atual”.
- Formato de data continua aplicado às tarifas nesta sessão; seleção de tema ainda não altera o tema global e agora informa essa limitação na tela. Persistência e integração global continuam pendentes da etapa própria, sem redesenho nesta entrega. Fechar pelo X mantém o comportamento anterior de sair sem transação; Cancelar explícito restaura preferências salvas em memória.

### Auditoria das demais camadas e preservações

Varredura de Kotlin em main/debug/release e revisão dos callbacks: Operacional/detalhes permitem consulta, filtros, atualização, expansão e cópia, sem CRUD de negócio. Dashboard, estados de erro, gráficos, login, estruturas de sessão e rotas/placeholder preservados. Não foram modificados MainActivity, AppAccessPolicy, controles Debug/Release, API ou repositories nesta etapa; alterações anteriores nesses arquivos continuam presentes e preservadas.

Retrofit não possuía chamadas de escrita de negócio a remover. Único POST é auth/login (permitido pelo efeito de sessão); GETs existentes foram mantidos. Nenhum endpoint, DTO de integração ou caminho alternativo sem autenticação adicionado. Métodos administrativos removidos eram mutações locais do ConfiguracoesViewModel, não operações HTTP. Sem comunicação direta Android/banco.

Consultas sem integração continuam com estado indisponível fora da demonstração explícita; nenhuma informação consultiva foi eliminada por ausência de API. Não há integração nova de Configurações. Backend mantém dependências de autenticação/isolamento documentadas: GET de tarifa/usuário/empresa exige Administrador; autorização somente leitura no servidor e escopo uniforme não são garantidos pelo cliente. Nenhuma referência externa alterada ou operação contra API/banco realizada.

### Arquivos desta etapa

- app/src/main/java/com/example/watt_vision_mobile/ui/screens/configuracoes/ConfiguracoesScreen.kt
- app/src/main/java/com/example/watt_vision_mobile/ui/screens/configuracoes/ConfiguracoesUiState.kt
- app/src/main/java/com/example/watt_vision_mobile/ui/screens/configuracoes/ConfiguracoesViewModel.kt
- app/src/test/java/com/example/watt_vision_mobile/ConsultativeScopeTest.kt
- AGENTS.md, PROJECT_CONTEXT.md, DESKTOP_REFERENCE.md, BACKEND_REFERENCE.md e ROADMAP.md.

ConsultativeScopeTest cobre preservação dos registros ao navegar/alterar preferências, cancelamento somente de preferências, allowlist de consultas Retrofit e login como única exceção POST. Não instancia cliente HTTP nem acessa banco. Testes existentes de política de acesso Debug/Release preservados. Resultado final: BUILD SUCCESSFUL para assembleDebug/testDebugUnitTest/lintDebug/assembleRelease/lintRelease; 10 testes sem falhas/erros, zero erros e 34 avisos Lint por variante. Inspeção de APKs sem marcadores administrativos e Release sem controles Debug; BuildConfig Release=false e bytecode passa false às políticas. Houve correção de teste que inicialmente contava método sintético Kotlin na allowlist; detalhes, limitações e roteiro de emulador em ROADMAP.md, seção 8. Nenhum teste de API ou instrumentado executado nesta etapa.

### Ajuste solicitado — datas fixas em Configurações

Por solicitação do usuário, datas das tarifas fixadas em DD/MM/AAAA, usando o padrão brasileiro já existente em DateFormatter. Removidos seletor de formato, campo de formato no estado pessoal, métodos de alteração/salvar/cancelar e barra inferior de ações. Empresa/fuso e listas consultivas preservados; seleção local demonstrativa de tema permanece com aviso de integração pendente, sem operação de salvamento. Testes de preferências ajustados para navegação/tema sem mutação de negócio e formatação brasileira fixa. Este registro prevalece sobre as descrições anteriores de salvar/cancelar formato.

Validar no Debug com demonstração explicitamente ativada: Geral exibe formato fixo, sem seletor ou barra de salvar/cancelar; Tarifas exibe DD/MM/AAAA. Development Mode e autenticação Release preservados. Adequação consultiva continua aguardando validação manual, sem avanço de módulo.

Verificação do ajuste: `bash ./gradlew --no-configuration-cache :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease :app:lintRelease` concluiu com BUILD SUCCESSFUL. Dez testes sem falhas/erros; Lint sem erros e 34 avisos por variante. `git diff --check` sem problemas. Validação visual no emulador permanece pendente.

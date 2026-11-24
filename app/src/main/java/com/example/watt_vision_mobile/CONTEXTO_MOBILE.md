# Watt Vision — Contexto Mobile

## Tecnologias
- Kotlin
- Android Studio
- API REST
- JSON

## Objetivo
Consultar e visualizar medições energéticas e cadastros.

## Integração
- Back-end: Node.js + Express
- Banco: MySQL 8
- Comunicação IoT: MQTT
- Endpoints: documentados na coleção do Postman

## Funcionalidades
- [ ] Login
- [ ] Consulta de locais
- [ ] Consulta de setores
- [ ] Consulta de equipamentos
- [ ] Visualização de medições
- [ ] Filtros do protótipo
- [ ] Atualização periódica

## Decisões confirmadas
- O Android consome a API do back-end.
- O Android não acessa diretamente o MySQL.
- Registros inativados não devem ser excluídos fisicamente.

## Pendências
- Confirmar o contrato de autenticação.
- Mapear os endpoints necessários para cada tela.
- Confirmar o intervalo de atualização das medições.
- Confirmar os endpoints de filtros e dashboard.

## Estado atual
Atualizar conforme o desenvolvimento.

## Próxima tarefa
Definir após o diagnóstico do projeto.

## Alarmes — interface implementada em 17/11/2026

Situação: implementada, aguardando validação manual do usuário no emulador. Não avançar para Conectividade ou outras telas sem autorização. Integração completa com o Backend permanece para a fase posterior.

### Escopo e isolamento

- Branch de trabalho: `teste-prototipo`; HEAD de referência: `a30af6e`, com alterações locais anteriores do Dashboard preservadas.
- Apenas a rota existente `Alarmes` foi associada à nova tela em `MainActivity.kt`. Menus, Sidebar, demais destinos e autenticação não foram alterados.
- Dashboard, Operacional, detalhes e Configurações foram preservados byte a byte durante esta implementação, conforme comparação de hashes dos arquivos existentes.
- Tela Kotlin/Compose/Material 3 com ViewModel e StateFlow; componentes e modelos próprios, sem mudanças em componentes compartilhados.
- Nenhum cadastro, edição, exclusão, reconhecimento, resolução ou alteração de limites. Nenhuma chamada de escrita de negócio adicionada.

### Interface e regras

- Cabeçalho, atualização manual, pesquisa, filtros por status/severidade/categoria, lista vertical com LazyColumn, cards e detalhes roláveis somente leitura.
- Indicadores sobre a lista recebida, antes dos filtros: Ativos (`ativo`), Críticos (ativos com `critico`), Atenção (ativos com `atencao`), Reconhecidos (`reconhecido`). Reconhecidos não integram os indicadores de ativos.
- Categorias: `energetico` e `comunicacao`; severidades: `critico`, `atencao`, `informativo`. Valores ausentes/desconhecidos são explícitos e não recebem classificação automática válida.
- Pesquisa local sem distinção de maiúsculas ou acentos, sobre mensagem, tipo, identificador e outros campos disponíveis. Filtros locais combináveis, sem presumir query parameters do servidor.
- ID do dispositivo IoT é identificado como tal. Não é tratado como equipamento ou PZEM. Nomes e relacionamentos não retornados não são inventados.
- Detalhes mostram valores detectado/limite sem presumir unidade, além dos IDs e dos metadados de reconhecimento existentes. Não disponibilizam ação de reconhecer.
- Datas DD/MM/AAAA e horários de 24 horas. Offset explícito é preservado; horários sem offset permanecem civis conforme recebidos, sem inventar fuso. Datas inválidas/ausentes não usam o horário atual como fallback.
- Zero real é distinto de valor ausente. Indicadores sem snapshot usam traço, e resposta vazia válida permite zero.
- Estados: carregamento, conteúdo, vazio, pesquisa sem resultado, erro de comunicação, sessão ausente, acesso negado e indisponibilidade. Falha após sucesso identifica dados antigos como desatualizados; 401/403 limpam o snapshot.

### Consulta e limitações

- `AlarmesApiService` declara somente `GET /api/alertas`; a lista geral já traz `tipo_alerta` e `categoria_alerta`, necessários aos detalhes. O modal usa o snapshot, sem depender do GET de detalhe que não traz os mesmos joins.
- Antes de ler alertas, o repositório exige token existente e valida a sessão em `GET /api/auth/me`. Não realiza login automático. Cancelamento e troca de sessão não reaproveitam resultados obsoletos.
- Contratos baseados nos arquivos Desktop `0b4b7bd` e Backend `2be8c51` consultados na análise; não equivalem a integração validada contra o servidor.
- GETs de alertas sem proteção e sem isolamento por empresa no Backend continuam uma dependência externa. A validação no cliente não substitui autorização do servidor. Backend não foi alterado.
- Não há paginação/filtros de servidor identificados para alertas. Volumes grandes exigem avaliação na futura integração.
- Não há resolução de alertas no contrato analisado. Nenhum módulo Histórico foi iniciado.
- Limitação preexistente fora do escopo: `MainActivity` inicia `isLoggedIn = true` sem distinção de variante. Não foi alterada; não considerar a autenticação global Debug/Release definitivamente validada. O módulo novo não concede sessão nem implementa bypass.

### Demonstração e validação manual

- Somente em Debug: acessar Alarmes pelo botão existente no Dashboard e tocar em `Ver demonstração de Alarmes`.
- Ativação explícita, aviso persistente `DEBUG · DADOS SIMULADOS`, fonte separada da real e botão `Voltar aos dados da API`. Erros, listas vazias e ausência de sessão nunca ativam demonstração.
- Release não contém gerador de amostras nem seletor demonstrativo de Alarmes.
- Dez exemplos fictícios: seis ativos, dois críticos ativos, dois de atenção ativos, três reconhecidos e um registro sem classificação. O registro sem classificação não é contado como ativo/reconhecido.
- Conferir pesquisa por `comunicacao`, filtros combinados, limpar pesquisa/filtros, contagens, modal rolável, fechamento, atualização manual, teclado e larguras pequenas. Em demonstração, combinar Ativo + Atenção + Energético retorna dois registros.
- Validação visual e integração real pendentes. Nenhuma operação contra API ou banco foi executada nesta implementação.

### Arquivos desta implementação

Modificados:
- `app/src/main/java/com/example/watt_vision_mobile/MainActivity.kt`
- `app/src/main/java/com/example/watt_vision_mobile/CONTEXTO_MOBILE.md`

Criados:
- `app/src/main/java/com/example/watt_vision_mobile/model/alarmes/Alarme.kt`
- `app/src/main/java/com/example/watt_vision_mobile/model/alarmes/AlarmeDto.kt`
- `app/src/main/java/com/example/watt_vision_mobile/data/api/AlarmesApiService.kt`
- `app/src/main/java/com/example/watt_vision_mobile/data/repository/AlarmesRepository.kt`
- `app/src/main/java/com/example/watt_vision_mobile/ui/screens/alarmes/AlarmesScreen.kt`
- `app/src/main/java/com/example/watt_vision_mobile/ui/screens/alarmes/AlarmesViewModel.kt`
- `app/src/main/java/com/example/watt_vision_mobile/ui/screens/alarmes/AlarmesUiState.kt`
- `app/src/main/java/com/example/watt_vision_mobile/ui/screens/alarmes/components/AlarmeCard.kt`
- `app/src/main/java/com/example/watt_vision_mobile/ui/screens/alarmes/components/AlarmesFilters.kt`
- `app/src/main/java/com/example/watt_vision_mobile/ui/screens/alarmes/components/AlarmesIndicators.kt`
- `app/src/debug/java/com/example/watt_vision_mobile/ui/screens/alarmes/AlarmesSourceHost.kt`
- `app/src/debug/java/com/example/watt_vision_mobile/ui/screens/alarmes/AlarmesDemoData.kt`
- `app/src/release/java/com/example/watt_vision_mobile/ui/screens/alarmes/AlarmesSourceHost.kt`
- `app/src/test/java/com/example/watt_vision_mobile/ui/screens/alarmes/AlarmesRulesTest.kt`
- `app/src/test/java/com/example/watt_vision_mobile/ui/screens/alarmes/AlarmesRepositoryTest.kt`
- `app/src/testDebug/java/com/example/watt_vision_mobile/ui/screens/alarmes/AlarmesDemoDataTest.kt`

### Verificações realizadas

Comando aprovado:

```bash
bash ./gradlew --no-configuration-cache :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileReleaseKotlin :app:lintRelease
```

- Debug compilado. 47 testes unitários passaram (19 novos de Alarmes), incluindo filtros, indicadores, DTO, datas, campos desconhecidos, estados, autenticação simulada, erros HTTP, cancelamento e demonstração.
- Lint Debug e Release: zero erros e 35 avisos por variante.
- Compilação Kotlin Release aprovada. Gerador de demonstração de Alarmes ausente dos arquivos compilados Release.
- `git diff --check` aprovado. Auditoria não encontrou métodos de escrita no serviço de Alarmes.
- `bash ./gradlew --no-configuration-cache :app:assembleRelease` falhou em `mergeReleaseResources`: `fundo_login.png` contém JPEG e AAPT não o compila. Recurso preexistente preservado, sem correção fora do escopo.
- Testes usaram respostas controladas; não comprovaram integração real. Não foram executados testes instrumentados desta tela.
- Nenhum commit criado; demais telas não foram iniciadas.

## Relatórios

- A rota existente **Relatórios** abre `RelatoriosScreen`. O destino usa Kotlin, Compose e Material 3, com `RelatoriosViewModel` e estado imutável em `StateFlow`.
- Um botão Filtros abre o painel de período, local, setor, equipamento e grandeza. Os seletores têm busca, e local e setor restringem as opções de equipamento. A tarifa em R$/kWh é somente para consulta, e o custo estimado usa a tarifa configurada e o consumo do período selecionado.
- Quatro indicadores fixos de consumo e custo. Energia por dia usa os deltas não negativos do contador de cada medidor. A primeira leitura é referência. Indicadores sem valor exibem traço.
- A aba **Gráficos** aplica o filtro de grandeza, limita a amostra a 120 pontos e oferece escala Y por gráfico e consulta por toque ou arraste. **Medições por dia** usa todas as leituras disponíveis, mantém as seis grandezas e mostra o custo por dia.
- Períodos reutilizam as janelas de Medições. **Hoje** começa à meia-noite, **Ontem** termina antes da meia-noite de hoje e **6 horas**, **Últimos 7 dias** e **Últimos 30 dias** são janelas móveis.
- Em Debug, **Ver demonstração de Relatórios** ativa a fonte local do source set `debug`. O aviso **DEBUG · DADOS SIMULADOS** identifica a tela e cada página do PDF. Release permanece em estado vazio nesta etapa, com a função de fonte retornando ausência de dados.
- Atualizar recarrega a fonte local. Exportar PDF usa o seletor de arquivos do Android, e Imprimir abre o serviço de impressão do sistema. Ambos usam o gerador de PDF do Front_WV em A4 paisagem, com as leituras filtradas antes da amostragem da tela.
- O gerador Desktop roda offline em um WebView isolado. Para atualizar o bundle e as licenças a partir do Front_WV local, execute `node scripts/gerar-relatorios-pdf.mjs ../Front_WV` na raiz do Mobile. O script usa as dependências já instaladas no Desktop.
- A integração com a API pertence à fase posterior. Os contratos encontrados são `GET /api/locais`, `GET /api/setores` e `GET /api/medicoes/serie-temporal` por PZEM, com limite de 10.000 registros por consulta. A cobertura de múltiplos medidores e a completude dos períodos precisam ser definidas nessa fase.
- A suíte de validação usa `:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:lintDebug` e `:app:compileReleaseKotlin`. Os testes de Relatórios verificam reinício do contador, isolamento por medidor, limite do dia anterior, janela de seis horas, amostragem e ausência de consumo sem uma segunda leitura.

## Integração — Entrega 1: ambiente e segurança de rede

Configure as URLs como propriedades Gradle no arquivo pessoal `%USERPROFILE%\.gradle\gradle.properties` (ou no diretório definido em `GRADLE_USER_HOME`). Na CI, use as variáveis `ORG_GRADLE_PROJECT_wattVisionDebugApiUrl` e `ORG_GRADLE_PROJECT_wattVisionReleaseApiUrl` conforme a variante.

| Propriedade | Configuração |
|---|---|
| `wattVisionDebugApiUrl` | Opcional. Informe a URL absoluta HTTP ou HTTPS do servidor de desenvolvimento. Sem a propriedade, o aplicativo abre e o cliente bloqueia as chamadas antes de acessar a rede. Somente uma URL HTTP explícita habilita cleartext em Debug. |
| `wattVisionReleaseApiUrl` | Obrigatória para gerar o Release. Informe a URL absoluta HTTPS aprovada para o ambiente de distribuição. O Gradle interrompe a geração se o valor estiver ausente ou inválido. |

Use somente o endereço base. O app acrescenta a barra final quando necessário, e os serviços Retrofit já incluem o prefixo `api/`. A URL fica visível no APK. Nunca inclua usuário, senha, JWT, segredos, parâmetros de consulta ou fragmentos. Use dados e contas fictícias no servidor de desenvolvimento.

O cliente HTTP não registra requisições nem respostas. As regras de backup e transferência excluem `watt_vision_prefs.xml`. O JWT continua nas SharedPreferences legadas até a Entrega 2. O aplicativo ainda não está liberado para credenciais reais.

As consultas do Dashboard estão suspensas em ambas as variantes, inclusive após atualização manual, filtros e tentativas do gráfico. A suspensão cobre alertas ativos, equipamentos, última medição, resumo, série temporal e o carregamento dos catálogos de locais, setores, equipamentos, pontos de medição e PZEM. A validação de `/api/auth/me` que fazia parte do polling do gráfico também fica suspensa. Vincule as consultas a uma sessão validada e à autorização do servidor antes de reativá-las em `DashboardViewModel`.

## Integração — Entrega 2: autenticação e sessão global

O `AuthRepository` publica o estado de sessão via `StateFlow`. A navegação protegida só é criada após `GET /api/auth/me` confirmar a identidade, tanto no login quanto na restauração. Configurações usa a identidade validada em memória. A ação **Sair da conta** no Meu perfil limpa as credenciais e encerra os ViewModels da sessão.

O cliente só permite o transporte de `POST /api/auth/login` e `GET /api/auth/me` nesta entrega. Redirecionamentos ficam desabilitados. O Dashboard mantém a suspensão da Entrega 1. A abertura de Alarmes pode pedir `/auth/me`; as consultas de alertas, catálogos e cadastro são bloqueadas antes de acessar a rede. O app segue sem autorização para usar credenciais corporativas reais.

**Lembrar acesso** persiste o JWT somente após a confirmação do servidor. O Android Keystore mantém uma chave AES de 256 bits não exportável. O Android fornece o IV aleatório para AES-GCM, e o conteúdo autenticado fica no arquivo de preferências já excluído de backup e transferência. A variante e a Base URL fazem parte dos dados autenticados da cifra. Sem **Lembrar acesso**, a credencial existe apenas durante o processo.

A migração remove apenas a chave legada `jwt_token` e solicita novo login. Chave ausente ou invalidada e conteúdo que falha na verificação criptográfica descartam o token salvo. Falha temporária de acesso ao armazenamento impede a entrada e permite nova tentativa. HTTP 401 invalida a sessão correspondente, enquanto 403 informa falta de acesso. Timeout e falha de servidor deixam a restauração pendente e preservam a credencial para nova tentativa. Respostas antigas são descartadas pela geração da sessão, mesmo quando dois logins recebem o mesmo JWT.

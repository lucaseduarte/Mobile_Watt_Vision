# Watt Vision Mobile — roadmap incremental

Atualizado em 16/11/2026. Referência Desktop: `/Users/25203646/Documents/Front_WV`, somente leitura. Regras em `AGENTS.md`; diagnóstico em `PROJECT_CONTEXT.md`; evidências comparativas e contratos em `DESKTOP_REFERENCE.md`.

Complementação documental concluída: Backend `/Users/25203646/Documents/Back-End/back-end_wattVision` (HEAD `db9d479414ffd0e0ad427ea560ab0b0d9bd3c955`) e banco `/Users/25203646/Documents/BD/Script Completo.rtf`. Consultar `BACKEND_REFERENCE.md` para contratos implementados e `DATABASE_REFERENCE.md` para estrutura e divergências. A análise documental não iniciou implementação. Posteriormente, usuário aprovou todos os itens do Development Mode e autorizou adequação consultiva, agora implementada e aguardando validação manual (seção 8).

## 1. Estado e governança

- **Concluído:** análise estática inicial Mobile; análise Desktop × Mobile; mapeamento de contratos, regras e divergências; documentação inicial dos quatro arquivos.
- **Implementação atual:** Development Mode validado manualmente pelo usuário, todos os itens aprovados. Adequação consultiva (etapa 3; denominada Etapa 09 na solicitação) implementada e aguardando validação manual. Sem nova integração de módulos.
- **Validação anterior:** assembleDebug, teste unitário de template e lintDebug passaram na análise Mobile (35 avisos; dois checks Lifecycle não executados). Isso não comprova fluxos reais nem validação em emulador.
- **Escopo atual:** adequação estrutural ao escopo consultivo, preservando Development Mode. Não executar build Desktop, instalar pacotes nele, alterá-lo ou mudar o backend. Compilação/testes/lint de ambas as variantes Mobile são registrados abaixo.
- **Próximo passo:** aguardar validação da adequação consultiva no emulador. Dashboard e demais módulos dependem de nova autorização.
- Preservar a arquitetura existente quando adequada. Mudanças significativas (nova navegação, DI, camadas/módulos, mecanismo amplo de persistência) precisam de aprovação prévia; não são pré-requisitos presumidos deste plano.
- Trabalhar módulo por módulo. Ao terminar cada interface, apresentar resultado, verificações e pendências; o usuário valida no emulador e fornece instrução para avançar. Nenhuma implementação recebe status concluído só porque compila.

Estados usados: **Concluído documental**, **Parcial existente**, **Pendente**, **Aguardando contrato**, **Aguardando validação no emulador**. Não atribuir percentuais artificiais de conclusão.

## 2. Sequência e dependências

| Etapa | Entrega | Prioridade / estado | Dependências principais |
| --- | --- | --- | --- |
| 1 | análise e mapeamento Desktop × Mobile | concluído documental | fontes de ambas aplicações |
| 2 | Development Mode só Debug + exposição de logs | validado manualmente pelo usuário | todos os itens aprovados |
| 3 | adequação exclusivamente consultiva (Etapa 09 solicitada) | implementada / aguardando validação manual | etapa 2 aprovada; seção 8 |
| 4 | Dashboard | alta / parcial existente | 2–3; unidades, filtros e dados disponíveis |
| 5 | Operacional | alta / parcial com mocks | 2–3; contratos de listas e relações |
| 6 | detalhes de locais, setores e equipamentos | alta / parcial com mocks | 5; seleção por ID e campos reais |
| 7 | Medições | alta / placeholder | série/unidades/relacionamento de medidor |
| 8 | Alarmes | alta / placeholder com componentes parciais | contrato de alertas e permissões |
| 9 | Conectividade | média / Mobile placeholder | GETs dispositivos/pontos/alertas observados; confirmar respostas/regras |
| 10 | Relatórios | média / placeholder | 7; fórmulas, fuso e tarifa confirmados |
| 11 | Configurações consultivas | média / parcial incompatível | 3; GET pontos/tarifas/usuários/empresas identificados; perfis e isolamento a validar |
| 12 | revisão transversal das integrações | alta / pendente | contratos implementados nos módulos anteriores |
| 13 | refinamento visual e componentes | média / pendente | interfaces funcionais e validações por módulo |
| 14 | autenticação e sessão definitivas | alta, final / parcial estrutural | contratos de sessão/refresh/revogação e acesso |
| 15 | revisão final de segurança | alta, final / pendente | 14; auditoria Release e permissões servidor |
| 16 | testes e validação final | alta, final / pendente | demais etapas e aceite em emulador |

Integração e testes básicos ocorrem em **cada módulo**; etapa 12 é auditoria transversal, não adiamento de dados reais. Correções de vazamento ocorrem antes, sem esperar 14–15. Se contrato externo impedir um módulo, registrar a parte pendente e combinar o próximo passo; não declarar concluído nem preencher com dados inventados.

## 3. Entregas por etapa e critérios de conclusão

### 2. Development Mode e proteção antecipada de dados

Implementado após autorização, conforme registro da seção 6; itens abaixo mantêm os critérios de escopo e aceite:

- Substituir a entrada autenticada global por condição exclusiva Debug. Avaliar BuildConfig e/ou isolamento em `src/debug` conforme estrutura Gradle existente; mudanças necessárias serão feitas apenas quando autorizadas.
- Abrir Dashboard diretamente em Debug e permitir navegação por todos os módulos. Preservar login e estrutura de autenticação para acesso opcional a uma sessão válida; Release sem sessão apresenta login, sem alternativa de bypass.
- Separar acesso local à UI de acesso à API: bypass não cria JWT. Consultas protegidas usam sessão de desenvolvimento válida obtida por fluxo autorizado. Sem sessão, mostrar autenticação necessária; servidor e permissões permanecem intactos.
- Não inserir credenciais/tokens fixos. Não imprimir corpos de autenticação nem dados sensíveis. Rever logging BODY agora; criptografia/persistência definitivas continuam em etapa final, sem justificar vazamentos durante desenvolvimento.
- Se necessário para testes visuais, dados demonstrativos explicitamente selecionados, disponíveis somente Debug e com identificação persistente da origem nas telas. Nunca fallback automático por falha/401/403/vazio. Não misturar alarmes/indicadores reais com exemplos sem separação clara.
- Antes da implementação, decidir o mecanismo mínimo de sessão opcional/demonstração dentro da arquitetura atual; não pressupor que toda API aceite requisição sem token.

**Aceite:** Debug abre Dashboard repetidamente sem exigir login e preserva navegação; Release não permite entrada direta nem habilitação do modo por preferência/intent; login preservado; API protegida continua exigindo token válido; ausência de credenciais fixas e logs sensíveis. Compilar Debug e Release, verificar artefato/condições de bypass, testar ausência de sessão e origem de dados, validar no emulador. Caso Release esteja impedida por configuração existente, registrar o bloqueio e não declarar garantia demonstrada.

### 3. Adequação ao escopo consultivo

- Remover formulários e handlers Mobile de criação/edição/exclusão de tarifas, usuários e pontos, ativação/configuração e edição corporativa. Preservar informações de consulta pertinentes e preferências pessoais.
- Auditar chamadas e caminhos de ação em UI, ViewModels, repositories, serviços e navegação. Não portar POST/PUT operacionais, PATCH reconhecimento/limites, CRUD/status de dispositivos, POST ponto ou PUT configuração do Desktop.
- Metas/limites: não definir, editar, ativar ou desativar. GET /limites foi observado no Desktop final; confirmar resposta/permissões para consulta.
- Registrar dependência de autorização somente leitura no servidor; não alterar backend. Consultar usuários é permitido conforme acesso; gerenciamento é proibido.

**Aceite:** navegação não oferece escrita de negócio e camada de dados não a expõe; login/sessão e preferências locais continuam possíveis; teste de fluxo e auditoria estática de verbos/efeitos; consultas existentes preservadas. Remover botões isoladamente não basta.

### 4. Dashboard

- Preservar cards/sheets/gráfico nativos, adaptar os quatro KPIs Desktop (tensão, ativos, offline, FP) e definir informação adicional só se baseada em dados reais.
- Remover alarmes fictícios, composição de locais fixa, indicadores e frequência padrão. Lista vazia legítima não vira alerta; falha não vira zero medido.
- Fazer filtros efetivos com opções reais e seleção de PZEM via ponto/escopo. Backend lê apenas id_pzem/horas, aceita 1–168h e limita a 10000 amostras ASC. Não enviar nomes como filtros presumidos. 30d=168h está incorreto, mas trocar para 720h também falha: representar período não suportado ou obter novo contrato mediante autorização externa. Diferenciar janela móvel de dia civil; Desktop “Ontem=48h” não é especificação suficiente.
- Confirmar unidade de potência; suportar seis grandezas disponíveis no contrato, incluindo Hz e FP, com rótulos corretos. Definir seleção real do medidor em vez de ID arbitrário.
- Composição/cobertura e indicadores energéticos dependem de fonte/fórmula confirmada. Desktop contém exemplos/valores indisponíveis; não usar isso como base para inventar agregação.
- Polling com ciclo de vida, falha parcial e dados desatualizados explícitos; separar última consulta da data da medição.

**Aceite:** valores rastreáveis à resposta e ao filtro; sem mistura silenciosa de origens; zero/vazio/erro/stale distintos; períodos e unidades corretos; gráfico legível com uma/muitas/nenhuma amostra. Validar cenário parcial e filtros no emulador; cálculos/filtros relevantes com testes comportamentais.

### 5. Operacional

- Integrar GET de equipamentos, setores e locais já observados no Desktop, após confirmar formatos de resposta e permissões. Estender DTOs só com evidência suficiente.
- Substituir MockOperacionalRepository e números aleatórios por fontes reais; preservar Pager, busca sem acentos, filtros e cards existentes.
- Relacionar IDs reais equipamento → setor → local; confirmar aliases de IDs e contagens. Não inventar consumo agregado ausente.
- Separar status cadastral de conectividade; não agrupar Ativo/Online como equivalentes sem regra confirmada.
- Corrigir persistência/restauração de filtros SavedStateHandle e aplicação dos campos efetivamente disponíveis; evitar nomes como identidade de entidades.

**Aceite:** todas as abas usam fontes reais, busca/filtros refletem dados, nenhuma contagem muda por randomização e detalhes recebem ID correto; vazio/erro/loading separados; filtros/scroll/aba preservados ao voltar; validação no emulador.

### 6. Detalhamento de locais, setores e equipamentos

- Corrigir ID inicial imutável no ViewModel de equipamento e evitar dados do equipamento anterior após nova seleção.
- Preservar ficha consultiva, cópia de identificadores, descrição expansível, ordenação de alarmes e estados loading/stale/erro.
- Substituir mock e alarmes, endereço, responsáveis e timestamps fixos. Somente mostrar campos disponíveis e marcar ausentes sem inventar.
- GETs de equipamento/local/setor por ID foram identificados no Backend. Usar envelopes/campos catalogados, validando permissões e dados em ambiente autorizado; locais possuem checagem explícita de escopo, demais entidades não têm proteção equivalente identificada. Não inventar telemetria/alarme agregado em detalhe cadastral.
- Locais/setores: vínculos, contagens e consumo reais quando disponíveis; Desktop não possui detalhes prontos desses dois módulos.
- Conexão e última comunicação dependem de dados reais/regra da etapa 9; não simular online como informação real.

**Aceite:** selecionar A/B exibe suas próprias informações, voltar mantém contexto, campos ausentes e alarmes vazios são corretos, polling não continua desnecessariamente em tela oculta. Validar rotação/voltar e falha com dado anterior.

### 7. Medições

- Implementar consulta por medidor/ponto/equipamento conforme vínculo real; período e seis grandezas; gráfico nativo com unidades e datas corretas.
- Cards de média, máximo e atual com definição explícita; Desktop final suporta frequência/FP, mas cards/dados brutos e gráfico podem usar recortes temporais distintos. Confirmar se KPIs seguem grandeza ou a semântica fixa Desktop.
- Adaptar dados brutos para lista/detalhe ou tabela mobile acessível; ordenar por timestamp, suportar histórico sem limite silencioso de cem registros e definir paginação conforme API real.
- Confirmar o GET /limites observado no Desktop para consultar metas/limites; remover “Definir Meta”, “Novo Limite” e alterações de status de regra.
- Contrato de limites agora identificado: grandezas tensao/corrente/potencia/energia, flag ativa e vigência; ativo não implica vigente. Série não oferece paginação/cursor e pode cortar registros após 10000; 30d e histórico completo não são comprovados. Revisar IDs BIGINT e datas/decimais antes de implementar.

**Aceite:** valores e estatísticas coerentes com amostras, unidades/filtros reais, sem zeros artificiais, boa leitura/touch/scroll. Testar nulos, janela temporal, ordenação e um/nenhum registro; validar no emulador.

### 8. Alarmes

- Implementar lista, KPIs, ativos/histórico, pesquisa/filtros e detalhe somente leitura. Reusar badges e componentes nativos quando adequados.
- Confirmar campos/aliases de id_alerta/id, categoria, severidade, status e equipamento/local. Não reclassificar severidade desconhecida silenciosamente como informativa.
- Ordenar por timestamp/severidade conforme objetivo acordado; fazer períodos e paginação funcionarem, diferentemente dos controles decorativos Desktop.
- Exibir status Reconhecido/Resolvido existente, sem reconhecer/resolver. Navegação para equipamento somente com vínculo real.
- Evidência Backend/script: somente ativo/reconhecido, sem estado/rota de resolução. A orientação anterior só se aplica a estado comprovado em contrato futuro; não apresentar Resolvido agora. GET ativos exclui reconhecidos; lista global traz categoria/tipo, detalhe/por dispositivo têm menos joins. Não existe avaliação automática de limites identificada na ingestão.

**Aceite:** sem PATCH ou outra escrita; KPIs coerentes com os registros/filtros definidos; 401/403/vazio/falha distinguidos; condição, data e origem corretas; validação no emulador.

### 9. Conectividade

- Desktop final implementa GET /dispositivos-iot, detalhe por ID, GET /pontos-medicao e histórico de /alertas. Mobile é placeholder. Confirmar respostas/permissões, relação PZEM/ponto/dispositivo, data_comunicacao e regra servidor de Online/Instável/Offline.
- Backend identificado: GET dispositivo retorna status armazenado, service aceita online/offline; script também admite instavel/disponivel. View de idade usa 120s e não é consumida pela API; parâmetros 5/2/3min não têm rotina identificada. Tabela de eventos existe, mas não há endpoint/geração de eventos comprovados. Histórico de reconexão permanece dependência externa, sem inferir a partir de reconhecimento de alerta.
- Adaptar KPIs/lista/detalhe/histórico consultivos. Remover CRUD, alteração Wi-Fi/MQTT/status, criação de ponto e configuração global. Não portar cache de vínculos ou defaults de firmware/rede como dados reais; períodos do histórico devem filtrar efetivamente.

**Aceite:** estado deriva de fonte/regra confirmada, nenhum status fixo, reconexão/perda de dados/idade bem representados; navegação consultiva e validação no emulador. Sem contrato, só protótipo explicitamente demonstrativo pode ser validado visualmente; integração permanece pendente.

### 10. Relatórios

- Consultar série disponível; filtros e consolidação por período/local/setor conforme contrato; definir fuso/calendário e separação por medidor.
- Referência final usa deltas não negativos por medidor, primeira leitura como base, reset ignorado, dia local, variação entre linhas e W/1000. Validar semântica e divergência do PDF, que não separa medidores; problemas da revisão anterior não são o estado atual.
- Desktop tem tarifa simulada local editável (default 0,80); adaptar com identificação explícita, sem alterar tarifa de negócio. Tarifa vigente exige contrato real; faixas fixas de normalidade do PDF não comprovam limites operacionais.
- Usar o contrato agora identificado de consumo com datas civis inclusivas, no mínimo duas leituras, tarifa ativa única cobrindo todo intervalo e erro 422 para reset/troca de ponto. Desktop/views ignoram reset e views têm outra fronteira temporal: não declarar resultados equivalentes. API não fornece relatório/PDF; 30d da série está bloqueado pelo teto 168h, embora consumo aceite intervalo de datas sem esse teto. Nunca contornar via listagem irrestrita de todo histórico para prometer cobertura sem avaliação de volume/segurança.
- Adaptar resumo e linhas diárias para cards/detalhes; definir exportação/compartilhamento/impressão local no escopo do módulo, sem escrita de negócio.

**Aceite:** resultados reproduzíveis com casos de referência e testes de cálculo, custo rastreável, exportação (se incluída) consistente com filtros e dados da tela; vazio/erro distintos; emulador.

### 11. Configurações exclusivamente consultivas

- Preservar informações de empresa, tarifas/vigências, usuários/perfis/status/último acesso e pontos/dispositivos/escopos/modelos conforme permissões. Não usar exemplos do Desktop como respostas reais.
- GETs de pontos, tarifas, usuários e empresas foram identificados no Backend. Tarifas/usuários/empresas exigem Administrador inclusive em consulta; não contornar perfil nem presumir isolamento corporativo nas listas globais. Minimizar dados pessoais/rede. Configurações globais têm tabelas no script, mas nenhum endpoint identificado.
- Empresa/fuso corporativo somente leitura. Aparência/formato/fuso de visualização podem ser preferências locais claramente separadas e efetivas globalmente.
- Alinhar formato YYYY-MM-DD Desktop versus YYYY/MM/DD Mobile quando houver decisão de produto; Desktop final aplica/persiste claro/escuro; Mobile precisa tornar sua preferência efetiva, sem seletor inoperante.

**Aceite:** nenhuma ação administrativa, permissões respeitadas, API real onde disponível, preferência local persiste/aplica sem alterar dados corporativos; validar no emulador.

### 12. Auditoria transversal da API

- Revisar todos os endpoints usados, DTOs/envelopes/aliases, parâmetros, URLs/prefixos e seleção de medidores, sem inventar contrato.
- Separar HTTP, campo sucesso=false, payload inválido, nulos, lista vazia, 401, 403, timeout e falha de rede; preservar cancelamento de coroutines.
- Evitar requisições duplicadas e polling invisível; impedir resposta antiga de sobrescrever seleção nova; revisar cache/última atualização.
- Confirmar inexistência de caminhos de escrita e registrar restrições necessárias no backend para sessões Mobile.

**Aceite:** matriz de contratos atualizada e cenários de integração representativos verificados com sessão autorizada, sem escrever dados de negócio. Lacunas externas identificadas sem alegar segurança do servidor não auditado.

### 13. Refinamento visual e reuso

- Consolidar cores, tipografia, espaçamentos, cards, filtros, estados e unidades entre módulos; decidir fonte nativa versus referência Plus Jakarta Sans.
- Tratar textos longos, contraste, escala de fonte, TalkBack, áreas de toque e tamanhos de tela; evitar tabelas desktop comprimidas e controles sem função.
- Reduzir duplicação de seletores gradualmente, preservando contratos e comportamento; nenhuma reestruturação ampla automática.

**Aceite:** validação de todas as telas em emulador, alinhamento de identidade e consistência com evidências visuais, sem regressão funcional.

### 14. Autenticação definitiva

- Integrar login preservado, restauração e validação de sessão, expiração, logout e proteção de rotas; armazenamento seguro de tokens e regras de backup.
- Renovação JWT **somente** após contrato real de refresh; os clientes analisados não fornecem endpoint. Definir reautenticação quando refresh não existir.
- Revisar lembrar acesso, permissões de usuário/empresa, 401/403, revogação e limpeza de dados de sessão; Desktop possui auth/session/Bearer global, mas Login/App ainda não chamam entrar/validarSessao; não portar bypass demonstrativo.
- Preservar conveniência Debug sem oferecer bypass Release e sem contornar autenticação da API.

**Aceite:** fluxos novo login/sessão válida/expirada/revogada/logout/reabertura verificados; sem senha persistida; Release protegida; contratos e testes de sessão documentados; emulador.

### 15. Revisão final de segurança

- Reauditar Release: nenhum bypass/demonstração habilitável, logs ou credenciais expostos; validar armazenamento/backup e configuração de transporte para ambiente de destino.
- Revalidar somente leitura na camada de dados e autorização efetiva no servidor mediante contrato/ambiente autorizado; não testar escritas destrutivas por iniciativa própria.
- Separar recomendações de backend das alterações Mobile. TLS e mudanças de infraestrutura/backend dependem de autorização e ambiente; documentar limites reais, sem declarar garantia só pelo cliente.

**Aceite:** problemas materiais resolvidos ou explicitamente registrados com dependência/impacto; nenhum vazamento conhecido deixado para publicação; documentação reflete verificação executada.

### 16. Testes e validação final

- Compilar variantes pertinentes, executar testes de comportamento/cálculos/sessão e lint; testes instrumentados de fluxos centrais quando houver emulador disponível.
- Percorrer todos os módulos, voltar, filtros, rotação, ausência/queda de rede, erros/parcial, permissões, grandes listas, fonte ampliada e desempenho de polling/gráficos.
- Atualizar quatro documentos com estado final, contratos confirmados, limitações e evidências. Não confundir o teste de template atual com cobertura de produto.

**Aceite:** usuário valida as interfaces no emulador, checks pertinentes passam com avisos analisados, Release sem bypass, dados reais rastreáveis, escrita de negócio ausente e pendências externas transparentes.

## 4. Registro de problemas e acompanhamento

| ID | Problema atual | Etapa de tratamento | Situação |
| --- | --- | --- | --- |
| P01 | bypass global `isLoggedIn=true` | 2, 14–15 | corrigido: Debug condicionado; Release exige login; sessão definitiva posterior |
| P02 | logging BODY pode expor senha/token | 2 (antecipado), 15 | corrigido: interceptor de logging removido |
| P03 | CRUD/configuração corporativa na UI Mobile | 3 | removido de UI/estado/ViewModel; consulta preservada; aguarda emulador |
| P04 | mocks/fallbacks de alarmes/indicadores/composição | 2, 4–6, 11 | Dashboard sem fallback fictício; mocks de telas só demonstração Debug explícita; integração real pendente |
| P05 | filtros Dashboard inefetivos; 30d=7d | 4 | identificado |
| P06 | semântica consumo/ativos; W versus kW | 4, 7, 10 | depende de contrato/unidades |
| P07 | equipamento selecionado usa ID inicial | 6 | identificado |
| P08 | SavedStateHandle com filtros não suportados; navegação efêmera | 4–6, 13 | identificado por inspeção; validar reprodução |
| P09 | polling em Activity; cancelamento/erros e stale inconsistentes | 4–8, 12 | identificado |
| P10 | DTOs/envelopes/aliases insuficientes | 4–8, 12 | confirmar respostas reais |
| P11 | Mobile Conectividade vazio; Desktop com API, defaults e histórico parcial | 9 | confirmar contratos/regras; adaptar consulta |
| P12 | GETs usuários/tarifas/empresas/pontos/limites não integrados | 7, 11 | contratos identificados no Backend; restrições de perfil/escopo e execução ainda a validar |
| P13 | PDF não separa medidores e tem faixas fixas; tarifa simulada | 10 | revisão atual corrigiu consumo/unidade/variação; validar semântica |
| P14 | tema/data inoperantes globalmente e seletores duplicados | 11, 13 | identificado |
| P15 | sessão incompleta, HTTP, token sem criptografia/backup | 14–15; exposição antecipada em 2 | identificado |
| P16 | testes de template; lint com checks Lifecycle ignorados | por módulo, 16 | baseline anterior registrado |
| P17 | Desktop Login/perfil demonstrativos, detalhe operacional fixo, import com caixa divergente | referência | registrar apenas; não modificar Desktop |
| P18 | Backend com autenticação parcial e escopo corporativo incompleto | dependência externa, por módulo | sem mudança autorizada; não usar abertura como permissão; segurança do cliente não resolve |
| P19 | série limitada a 168h/10000 ASC, sem cursor e filtros de entidade | 4, 7, 10 | 30d/histórico integral dependem de contrato adicional ou limitação explícita |
| P20 | reset, tarifa e fronteiras divergentes entre API/views/Desktop | 7, 10 | API rejeita reset/troca de ponto; exige tarifa única; definir semântica e testes |
| P21 | status/conectividade e histórico com fontes distintas | 9 | view 120s não exposta; evento/configuração sem rota/rotina identificada |
| P22 | schema fornecido pode diferir do implantado; faixas/nulos/status divergentes | dependência externa, 12 | validar schema/respostas em ambiente autorizado; sem executar script |

Ao concluir cada etapa autorizada, registrar aqui data, arquivos/módulos alterados, verificações, aceite no emulador e pendências reais. Estado atualizado: etapa 2 validada manualmente; etapa 3 implementada e aguardando validação manual, conforme seção 8; demais módulos permanecem pendentes.

## 5. Revisão da referência

O Desktop mudou externamente durante a análise. O plano final incorpora o commit observado `cfa1707`: Conectividade, limites por API, tema claro persistido, serviços auth/session ainda desacoplados de Login/App e Relatórios/PDF novos. Revalidar as fontes ao iniciar cada módulo. Nenhuma mudança Desktop foi feita por este agente.

## 6. Registro de entrega — Development Mode

Data: 16/11/2026. Escopo autorizado: apenas etapa 2. Estado: implementado, **aguardando validação do usuário no emulador**.

### Entrega

- BuildConfig gerado por variante e política de acesso local com flag de build; Debug abre Dashboard, Release abre login. Resposta de login sem token não concede acesso.
- Barra de desenvolvimento/menu de todos os módulos, login real opcional e retorno sem login isolados em src/debug; src/release contém funções vazias. Sem credenciais, usuários ou tokens fixos de desenvolvimento.
- Seletor inicialmente desligado para abrir telas mock preexistentes, com aviso permanente de demonstração. Operacional/detalhes e Configurações só renderizam esses exemplos em Debug após opt-in; Release ou opt-out mostra consulta ainda não integrada. Dashboard continua somente API, não é convertido em mock pelo seletor.
- Removidos fallbacks fictícios do Dashboard, composição inventada e zeros de ausência; mensagens para falta de dados/401/403/falha parcial. Repositório verifica sucesso do envelope e propaga cancelamento. Filtros demonstrativos ainda não integrados são informados explicitamente.
- Logging HTTP removido para não imprimir corpos de autenticação/dados sensíveis.
- Correção de build preexistente: fundo_login.png tinha conteúdo JPEG; renomeado para .jpg, mantendo bytes e referência R.drawable.fundo_login.
- Lista completa dos arquivos alterados e roteiro de emulador em PROJECT_CONTEXT.md, seção 13.

### Verificações

Comando de aceite automatizado:

```bash
bash ./gradlew --no-configuration-cache :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease :app:lintRelease
```

Testes incluem ExampleUnitTest preservado e AppAccessPolicyTest (matriz de acesso Debug/Release, demonstração só com opt-in e login sem token). Nenhum teste chama backend. Resultado final: **BUILD SUCCESSFUL**. Debug e Release compilados; **6 testes unitários**, zero falhas/erros (1 existente + 5 novos). Lint Debug e Release: **zero erros e 34 avisos em cada variante**. Permanecem avisos preexistentes de dependências/recursos/convenções e incompatibilidade de lint Lifecycle: UnsafeLifecycleWhenUsage/RepeatOnLifecycleWrongUsage não foram executados. Dois ícones antigos de login também geram avisos Kotlin de depreciação; não alterados nesta etapa.

Evidências adicionais:

- BuildConfig Release gerado com DEBUG=false; Debug gera DEBUG=true.
- Bytecode de MainActivity Release passa false à política de acesso/demonstração e não chama controles de desenvolvimento.
- APK Debug contém os três marcadores da UI Debug; APK Release não contém “Desenvolvimento · Debug”, “Liberar telas de demonstração” ou “Voltar ao desenvolvimento sem login”. Controles também isolados por source sets.
- `app/build/reports/development-mode-verification.json` registra a inspeção de artefatos e identidade dos bytes do JPEG renomeado.
- Relatórios: app/build/reports/tests/testDebugUnitTest/index.html e lint-results-debug.html/lint-results-release.html.
- APKs: app/build/outputs/apk/debug/app-debug.apk e app/build/outputs/apk/release/app-release-unsigned.apk. Release não foi instalada/executada no emulador nem assinada para produção.
- git diff --check e referências documentais verificados sem problemas. Validação de interface/fluxos no emulador continua pendente do usuário.

Houve tentativa inicial com testReleaseUnitTest, mas essa tarefa não existe na configuração AGP atual; removida da invocação, sem alteração de configuração para criá-la. Testes de política cobrem explicitamente o caso Release, complementados por inspeção do bytecode/artefato Release. Falha inicial de compilação por Context no novo Column foi corrigida com this@MainActivity. Build Release também revelou o JPEG com extensão PNG, corrigido sem alteração visual.

### Limitações e pendências preservadas

- Validação visual no emulador pelo usuário ainda não realizada. Nenhuma chamada de teste ao backend, alteração Desktop/backend ou assinatura de produção efetuada.
- Sessão definitiva, criptografia, refresh/expiração/logout e validação do perfil/escopo servidor continuam nas etapas finais; Release não restaura automaticamente sessão, exigindo login ao recriar Activity.
- Operacional/Configurações continuam demonstrações, não consultas integradas; formulários locais administrativos continuam pendentes de remoção na etapa 3. Nenhum endpoint de escrita de negócio foi adicionado.
- Unidades/KPIs, filtros/30d, seleção de equipamento, SavedStateHandle e polling fora da tela permanecem pendências dos módulos, sem alegação de correção nesta etapa.
- Usuário deve validar debug, banner/origem dos dados, navegação, opt-in/out de demonstração e login opcional. Aguardar seu retorno antes de qualquer próximo módulo.

## 7. Entrega documental — ecossistema completo

Em 16/11/2026, autorização restrita a documentação. Criados BACKEND_REFERENCE.md (arquitetura, catálogo GET/escritas, parâmetros/envelopes, validações, segurança e regras por módulo) e DATABASE_REFERENCE.md (19 tabelas com colunas/tipos, FKs/CHECK/índices, 12 views, procedure, trigger e Mermaid). AGENTS.md, PROJECT_CONTEXT.md, DESKTOP_REFERENCE.md e este roadmap complementados, preservando diagnóstico e evidências históricas.

Referências técnicas: Backend HEAD `db9d479414ffd0e0ad427ea560ab0b0d9bd3c955`; Desktop HEAD `cfa17077f68cee4adae2388f7625a20d634b4433`; Mobile HEAD `7b023e9ce82413814440cea53c550265761dd4af`, com alterações locais anteriores preservadas. RTF identificado por SHA-256 em DATABASE_REFERENCE.md; não é prova do banco implantado. Nenhum código-fonte ou original externo modificado; nenhuma execução de API, MQTT, banco, SQL, migração ou servidor. Sem novos builds/testes de aplicação por alteração exclusivamente documental; verificações da seção 6 permanecem resultados anteriores.

Impactos: reduzidas lacunas sobre endpoints/relacionamentos, preservadas dependências de autorização/fuso/escala real. Acrescentados limites efetivos de série, diferenças de consumo, cobertura tarifária e ausência de API de eventos/configurações/relatórios. Autenticação definitiva Android permanece no final; vulnerabilidades de autorização do servidor são dependências externas imediatas para uso seguro, sem autorizar mudança Backend nesta etapa. Nenhum módulo recebe aceite com base nesta análise.

Verificação documental: `git diff --check` sem problemas; seis documentos com cercas Markdown balanceadas, sem espaços finais e links locais existentes. Inventário das 19 tabelas comparado à extração DDL. Comparação por SHA-256 de 200 arquivos de fontes/referências selecionados e do RTF, entre a captura de integridade desta análise e a revisão documental, sem alterações. Essa comparação não é auditoria completa de arquivos excluídos/configuração sensível nem prova do ambiente implantado. Backend permaneceu com árvore Git limpa na consulta de status. Os resultados de build/testes da seção 6 não foram repetidos nesta etapa.

Próxima ação recomendada: usuário validar o Development Mode no emulador; após instrução, executar adequação consultiva (etapa 3) e avançar por módulo usando BACKEND_REFERENCE.md como índice técnico. Combinar tratamento das dependências externas de segurança com responsável autorizado antes de integração sensível. Aguardar autorização; não iniciar implementação automaticamente.

## 8. Entrega — Etapa 09 solicitada / etapa 3 do roadmap

Development Mode: validação manual concluída pelo usuário, todos os itens aprovados. Adequação exclusivamente consultiva: implementada, aguardando validação manual no emulador. A numeração da solicitação não inicia Conectividade (etapa 9 histórica); nenhuma implementação de outro módulo autorizada.

Removidos três formulários administrativos e todos os botões/handlers de cadastro/edição/exclusão/ativação de tarifas, usuários e pontos; removidos seis campos de formulário/ID em edição. Empresa/fuso corporativo são somente leitura, sem métodos de alteração. Preferências locais separadas em estado próprio; salvar/cancelar restritos a elas. Consulta em quatro abas preservada, incluindo listas, valores, vigência, perfis, status e vínculos, ainda demonstrativa com opt-in Debug. Abas de consulta recebem apenas estado. Nenhuma chamada administrativa HTTP existia no Mobile: nenhuma rota/repository de API precisou ser removida ou criada. POST de login preservado, demais operações de rede são GET.

Fontes alteradas nesta etapa: ConfiguracoesScreen.kt, ConfiguracoesUiState.kt e ConfiguracoesViewModel.kt, no pacote ui/screens/configuracoes, além do novo src/test/.../ConsultativeScopeTest.kt. Documentos atualizados: AGENTS.md, PROJECT_CONTEXT.md, DESKTOP_REFERENCE.md, BACKEND_REFERENCE.md e este roadmap. Arquivos de Development Mode, login, API e Dashboard não foram editados nesta etapa; mudanças anteriores preservadas.

Limitações: preferências continuam em memória, formato de data aplicado apenas às tarifas; seleção de tema continua demonstrativa e informa que tema global permanece escuro. Integração/persistência global fica para etapa própria. Configurações reais permanecem indisponíveis fora de demonstração; nenhuma credencial/token fixo ou novo fixture. Segurança Backend continua dependência externa (autenticação uniforme, isolamento por empresa/local e restrição de escrita por sessão/perfil); não modificada nem contornada.

### Verificação técnica

Comando solicitado, sem chamadas ao Backend/banco:

```bash
bash ./gradlew --no-configuration-cache :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease :app:lintRelease
```

A primeira tentativa no sandbox parou no lock do cache Gradle externo; execução autorizada fora do sandbox. Primeira compilação passou; repetição para incluir teste novo encontrou falha no teste de allowlist por incluir getSerieTemporal$default, método sintético gerado pelo compilador Kotlin. Teste corrigido para inspecionar métodos reais e tratar nulidade das anotações; não houve alteração da API para satisfazê-lo. Resultado final: **BUILD SUCCESSFUL**, Debug e Release compilados; **10 testes**, zero falhas/erros (seis existentes + quatro de ConsultativeScopeTest). Lint Debug/Release: **zero erros e 34 avisos em cada variante**. Limitações preexistentes dos checks Lifecycle continuam registradas na seção 6. Nenhum teste chama API/banco. Após a execução, removida apenas linha vazia extra no EOF da tela; nenhuma alteração semântica após os checks.

Auditoria estática: nenhuma ocorrência dos handlers/estados/formulários administrativos removidos ou anotações de escrita de negócio nas fontes Kotlin; único POST permanece auth/login. APKs Debug e Release inspecionados: nenhum marcador dos três formulários, dos métodos de mutação removidos ou dos botões de adicionar; controles de desenvolvimento presentes no Debug e ausentes no Release. BuildConfig Release DEBUG=false e bytecode MainActivity passa false para canOpenContent/canShowDemo. Testes existentes cobrem Release sem sessão e demonstração exclusiva Debug com opt-in. Evidência de marcadores em app/build/reports/consultative-scope-verification.json; inspeção de marcadores complementa, sem substituir, auditoria de fontes e política.

Relatórios: app/build/reports/tests/testDebugUnitTest/index.html, lint-results-debug.html e lint-results-release.html. APK Debug em app/build/outputs/apk/debug/app-debug.apk; Release unsigned em app/build/outputs/apk/release/app-release-unsigned.apk. `git diff --check` final sem problemas. Fontes externas e fluxo Development Mode não editados nesta etapa. Validação manual desta nova interface pelo usuário permanece pendente.

### Roteiro de validação manual

1. Executar Debug: Dashboard abre diretamente, barra de desenvolvimento e navegação disponíveis.
2. Sem demonstração, Configurações/Operacional mantêm mensagem de consulta não integrada, sem dados simulados silenciosos.
3. Ativar demonstração explicitamente e abrir Configurações: aviso persistente de dados simulados; quatro abas acessíveis.
4. Geral: empresa/fuso não abrem teclado e não podem ser editados. Salvar/Cancelar tratam apenas preferências locais. Alterar formato e conferir datas das tarifas; cancelar restaura última preferência salva. Seleção de tema informa limitação e não muda configuração global.
5. Tarifas/Usuários/Pontos: conferir cards, valores, status, perfis, vigências e vínculos. Não deve existir adicionar/editar/excluir/ativar, formulário ou ação administrativa. Barra de salvar geral não aparece nessas abas.
6. Operacional/detalhes: conferir navegação, pesquisa/filtros, expansão e cópia existentes; nenhuma ação de negócio.
7. Desativar demonstração: consulta indisponível novamente. Release sem sessão abre login e não oferece barra/seletor/bypass; APK Release gerado é unsigned, instalação exige assinatura adequada.
8. Informar resultado para aceite desta etapa. Não avançar ao Dashboard ou a outro módulo sem instrução.

### Ajuste solicitado — datas fixas em Configurações

Por solicitação do usuário, datas das tarifas fixadas em DD/MM/AAAA, usando o padrão brasileiro já existente em DateFormatter. Removidos seletor de formato, campo de formato no estado pessoal, métodos de alteração/salvar/cancelar e barra inferior de ações. Empresa/fuso e listas consultivas preservados; seleção local demonstrativa de tema permanece com aviso de integração pendente, sem operação de salvamento. Testes de preferências ajustados para navegação/tema sem mutação de negócio e formatação brasileira fixa. Este registro prevalece sobre as descrições anteriores de salvar/cancelar formato.

Validar no Debug com demonstração explicitamente ativada: Geral exibe formato fixo, sem seletor ou barra de salvar/cancelar; Tarifas exibe DD/MM/AAAA. Development Mode e autenticação Release preservados. Adequação consultiva continua aguardando validação manual, sem avanço de módulo.

Verificação do ajuste: `bash ./gradlew --no-configuration-cache :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease :app:lintRelease` concluiu com BUILD SUCCESSFUL. Dez testes sem falhas/erros; Lint sem erros e 34 avisos por variante. `git diff --check` sem problemas. Validação visual no emulador permanece pendente.

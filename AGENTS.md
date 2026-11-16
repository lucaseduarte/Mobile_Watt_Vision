# Watt Vision Mobile — instruções permanentes

## Produto e escopo

Aplicativo Android do TCC Watt Vision para consulta e monitoramento energético de ambientes industriais e comerciais por profissionais em campo. A versão desktop concentra gestão, administração e configuração. Leia `PROJECT_CONTEXT.md` para o inventário e diagnóstico validados no código.

## Referência oficial Desktop

- Caminho permanente: `/Users/25203646/Documents/Front_WV`.
- Usar exclusivamente para leitura e análise. Nunca criar, modificar ou excluir arquivos nesse diretório, inclusive saídas de build, instalação de dependências ou formatação automática.
- Ignorar dependências, caches, arquivos gerados, agregados como `repomix-output.xml` e credenciais (`.env`, tokens, chaves e configurações sensíveis).
- Toda implementação ocorre em `/Users/25203646/Documents/Mobile_Watt_Vision`. Backend permanece fora do escopo sem autorização.
- Consultar `DESKTOP_REFERENCE.md` para fontes, contratos observados, comparação e lacunas; `ROADMAP.md` para sequência e critérios de entrega. O código de cada aplicação é fonte primária do próprio estado atual.
- A referência oficial contém partes simuladas e inconsistências: preservar intenção funcional e identidade visual, sem reproduzir dados fictícios, bugs ou regras não confirmadas.

## Regra fundamental: somente consulta

- Permitir visualização de indicadores, medições, gráficos, históricos, alarmes, locais, setores, equipamentos, tarifas, usuários e pontos de medição conforme permissões; conectividade e relatórios; pesquisa, filtros e ordenação; simulações/cálculos sem escrita; autenticação e gerenciamento da própria sessão.
- Permitir interações locais sem alteração de dados de negócio, como expansão de cards, navegação, cópia de identificadores e preferências pessoais de apresentação.
- Não implementar cadastro, edição, exclusão, alteração de parâmetros operacionais, gerenciamento de usuários, alteração de tarifas, configuração ou controle remoto de equipamentos, nem qualquer escrita de dados de negócio.
- Ao adaptar referências desktop, remover ações administrativas e preservar informações úteis à consulta. A existência de CRUD simulado no código atual não autoriza sua manutenção ou expansão como funcionalidade do produto.
- Distinguir preferências pessoais locais de configurações corporativas: nome da empresa e parâmetros operacionais não são preferências pessoais.
- POST de autenticação é permitido; o critério é o efeito da operação, não apenas o verbo HTTP.
- Reconhecer/resolver alarmes e criar/editar/ativar limites ou metas operacionais são alterações de negócio e não pertencem ao Mobile. Exibir o status/limite existente pode ser permitido por contrato e autorização.
- Remover ações de escrita da apresentação e da camada de dados, quando existentes; ocultar botões não garante segurança. Registrar requisitos de autorização somente leitura no backend, sem alterá-lo por iniciativa própria. Não confiar apenas em perfil ou identificadores enviados pelo cliente.

## Referências técnicas oficiais — Backend e banco

- Backend: `/Users/25203646/Documents/Back-End/back-end_wattVision`, somente leitura. Revisão analisada em 16/11/2026: `db9d479414ffd0e0ad427ea560ab0b0d9bd3c955` (arquivos locais); consultar `BACKEND_REFERENCE.md` por módulo.
- Banco: `/Users/25203646/Documents/BD/Script Completo.rtf`, somente leitura. Extrair texto sem alterar o RTF; consultar `DATABASE_REFERENCE.md`. Não executar SQL, migrações, seeds ou operações no banco sem autorização específica. O script não comprova o schema implantado.
- Desktop orienta visual/experiência; Backend orienta contratos e processamento implementados; script complementa estrutura/integridade. Investigar divergências, sem assumir que uma fonte descreve integralmente as demais. Contrato em código não equivale a resposta validada em execução.
- Android acessa dados exclusivamente pela API Watt Vision; nunca estabelecer conexão direta ao banco ou usar tabelas/views como endpoints presumidos.
- Contratos agora identificados: GETs de detalhes, tarifas, empresas, usuários, PZEMs e pontos. Permissões reais variam; alguns GETs exigem Administrador. Rotas abertas não autorizam acesso indevido; isolamento por empresa/local e restrição de escrita no Backend continuam dependências externas.
- Série temporal aceita apenas id_pzem e horas (1–168), com até 10000 registros ASC. Não substituir 30d=168h por 720h sem novo contrato. Filtros de entidade enviados pelo Desktop não são aplicados pelo controller.
- Consumo da API rejeita reset e mudança de ponto com 422, exige tarifa única cobrindo o período; não reproduzir descarte silencioso de deltas negativos do Desktop/views como regra equivalente.
- Conectividade calculada por view, status armazenado e parâmetros corporativos são fontes diferentes; não inventar regra de online/offline ou geração automática de alarmes/eventos.
- Ignorar credenciais, segredos, configurações sensíveis, dependências e gerados também no Backend. Não iniciar servidor/subscriber MQTT ou testes que conectem ao banco durante análise documental.

## Development Mode — implementado e validado manualmente pelo usuário

- Autenticação definitiva é uma das últimas etapas; isso não adia correções de exposição de senha, token ou outros dados sensíveis.
- Acesso direto ao Dashboard exclusivamente em Debug via BuildConfig.DEBUG e AppAccessPolicy. Login, repositório, interceptor e estrutura de sessão preservados. Release inicia no login e só concede acesso após resposta de login com sucesso e token não vazio.
- Release nunca pode oferecer bypass, seletor de demonstração ou rota alternativa que conceda sessão autenticada. Sem sessão, deve manter o fluxo de login. Isolar código/recursos exclusivos em variantes Debug quando aplicável e verificar também Release.
- Permitir navegação por todos os módulos; distinguir acesso de navegação de autorização do servidor e de funcionalidade implementada. Placeholder não é módulo concluído.
- Bypass local não autoriza consultas protegidas nem modifica autenticação do backend. Para dados reais protegidos, usar sessão de desenvolvimento válida obtida por fluxo autorizado, sem credenciais/tokens fixos no código, recursos ou documentação.
- Somente para teste visual, permitir dados de demonstração explicitamente selecionados e identificados de forma persistente. Separar a origem demonstrativa da real; nunca ativar demonstração silenciosamente após erro, 401/403 ou lista vazia da API.
- Tratar falta de sessão/permissão/dados com estados claros. Não exigir login a cada abertura Debug; definir posteriormente retenção segura da sessão conforme contrato, sem inventar refresh endpoint.
- Implementação atual: barra de desenvolvimento e seletor explícito de demonstração em src/debug; src/release fornece implementação vazia. Dashboard sempre consulta API; Operacional/detalhes e Configurações simulados só são renderizados em Debug após seleção explícita. Sem isso, mostram consulta ainda não integrada. Sem logging HTTP de corpos.
- Critérios de aceite: Debug abre diretamente o Dashboard e navega; Release sem sessão abre login e não dispõe de bypass; nenhuma credencial fixa nem escrita de negócio; modo/origem dos dados visíveis; verificação de ambas as variantes e validação no emulador.
- Renovação JWT, expiração, validação definitiva, armazenamento seguro, logout e fluxos completos ficam para etapa final conforme `ROADMAP.md`, dependentes dos contratos reais.

## Desenvolvimento

- Comunicar e documentar em português brasileiro.
- Preservar Kotlin, Jetpack Compose, Material 3, MVVM, StateFlow, Coroutines e a organização existente. Não realizar mudanças arquiteturais significativas sem aprovação.
- Usar o desktop como referência funcional e visual, adaptando a experiência para uso mobile nativo; não simplesmente reduzir a tela desktop.
- Preservar a identidade visual e priorizar componentes reutilizáveis, evitando duplicação.
- Usar dados reais da API. Não inventar endpoints, DTOs, relações, unidades, regras de agregação ou regras de negócio. Os contratos declarados no cliente não comprovam o comportamento do servidor.
- Não apresentar dados simulados, valores fixos ou zeros como medições reais ou resposta a falhas da API. Usar estados de carregamento, vazio, erro e dados desatualizados de forma explícita.
- Não modificar backend ou desktop sem autorização.
- Não expor credenciais, tokens, senhas, dados sensíveis ou conteúdo de autenticação em código, documentação, logs ou respostas.
- Após implementações, executar compilação e testes pertinentes; registrar comandos, resultados e limitações. Não afirmar que passaram quando houve bloqueio anterior à execução.
- Preservar alterações do usuário e evitar alterações fora do escopo solicitado.
- Manter `PROJECT_CONTEXT.md` coerente com mudanças relevantes, separando comportamento existente, problemas e propostas.
- Desenvolver incrementalmente por módulo. Após cada implementação, executar verificações pertinentes, apresentar a interface para validação do usuário no emulador e aguardar instrução antes de avançar ao módulo seguinte.

## Verificação local

O wrapper não possui permissão executável no estado analisado; usar `bash ./gradlew` sem alterar permissões desnecessariamente.

```bash
bash ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

O daemon está configurado para JDK 25; compatibilidade de código Java é 11. SDK mínimo 26 e compile/target 37. Consultar os arquivos Gradle para versões atuais. Testes instrumentados exigem dispositivo/emulador.

## Etapa atual acordada

Development Mode validado manualmente pelo usuário: todos os itens aprovados. Etapa 09 solicitada pelo usuário corresponde à etapa 3 do roadmap (adequação consultiva), não à etapa 9 de Conectividade. Adequação consultiva implementada, aguardando validação manual no emulador. Não avançar para Dashboard, integração ou implementação de outro módulo sem nova autorização.

Formulários, estados e métodos de criação/edição/exclusão/ativação de tarifas, usuários e pontos foram removidos. Empresa e fuso corporativo são somente leitura. Formato de data fixo DD/MM/AAAA, sem seletor nem botões Salvar/Cancelar. Seleção local de tema continua demonstrativa em memória, com limitação explícita, sem alterar tema global. Nenhuma escrita de negócio é permitida mesmo em demonstração Debug. Login e estruturas de consulta permanecem.

Referências Desktop/Backend/banco permanecem intocadas. Complementação documental do ecossistema concluída em 16/11/2026; BACKEND_REFERENCE.md/DATABASE_REFERENCE.md seguem como índices técnicos. Resultados e roteiro desta implementação em PROJECT_CONTEXT.md, seção 15, e ROADMAP.md, seção 8.

# Watt Vision — referência técnica do Backend

Análise estática em 16/11/2026. Fonte: `/Users/25203646/Documents/Back-End/back-end_wattVision`, HEAD `db9d479414ffd0e0ad427ea560ab0b0d9bd3c955`. Desktop: `/Users/25203646/Documents/Front_WV`, HEAD `cfa17077f68cee4adae2388f7625a20d634b4433`. Mobile: `/Users/25203646/Documents/Mobile_Watt_Vision`, HEAD `7b023e9ce82413814440cea53c550265761dd4af`, com alterações locais preexistentes do Development Mode. HEAD identifica a revisão; as conclusões são sobre os arquivos locais lidos, sem garantia de equivalência ao servidor implantado.

Somente documentação autorizada. Nenhum servidor iniciado, dependência instalada, teste conectado, requisição HTTP/MQTT, SQL ou migração executados. Arquivos de credenciais e configurações potencialmente sensíveis não foram abertos para análise de valores. Consultar [DATABASE_REFERENCE.md](DATABASE_REFERENCE.md) para DDL e [ROADMAP.md](ROADMAP.md) para dependências. Contrato identificado no código não é resposta observada de uma API em execução.

## 1. Arquitetura e fontes por responsabilidade

Node.js com módulos ES (`type: module`), Express 5.2.1, mysql2 3.24.5, mqtt 5.16.0, bcrypt 6.0.0, jsonwebtoken 9.0.3, cors 2.8.6 e dotenv 18.0.4; nodemon 3.1.14 em desenvolvimento (faixas declaradas com `^`, não versões instaladas verificadas). `package.json` oferece start/dev, db:test e teste apenas informativo, sem suíte automatizada configurada. Não executar `db:test` nesta etapa.

| Caminho relativo ao Backend | Responsabilidade |
| --- | --- |
| `server.js` | inicia HTTP, porta de ambiente ou 3000, importa subscriber MQTT com efeitos de conexão |
| `src/app.js` | CORS aberto, parser JSON, GET `/` de diagnóstico e montagem `/api` |
| `src/routes/index.js` e `*Routes.js` | catálogo efetivamente montado e controles de acesso |
| `src/controllers/*Controller.js` | parâmetros HTTP, envelopes, status e erros |
| `src/services/*Service.js` | validações, vínculos, cálculos; alguns serviços também executam SQL |
| `src/database/monitoramento/*Repository.js` | consultas parametrizadas mysql2, tabelas `tbl_*` |
| `src/middlewares/{authMiddleware,perfilMiddleware}.js` | JWT, usuário ativo e perfil |
| `src/mqtt/{mqttClient,medicaoSubscriber}.js` | conexão/reconexão, assinatura e ingestão das leituras |
| `src/config/{database,mqttConfig,testDatabase}.js` | pontos de configuração; valores de ambiente/infraestrutura não auditados |
| `src/docs/swagger.js` | arquivo vazio na leitura; não há OpenAPI efetivo montado em app.js |

Não há Models/ORM ou camada de migrations encontrada no inventário de fontes. Repositórios importam conexão compartilhada de `config/database.js`; uso de `mysql2/promise` e `mysql.createPool` identificado estruturalmente. Não foram inspecionados host, usuário, senha, TLS, timezone ou opções de conversão da conexão. `alertaService` e `limiteService` consultam banco diretamente: separação de camadas é parcial. Não foi identificado handler global de erros/404 JSON em app.js; falhas de parser/rotas desconhecidas não têm garantia do envelope dos controllers. Não há WebSocket/SSE ou agendador de conectividade identificado nos pontos de entrada.

## 2. Autenticação e autorização efetivamente montadas

`POST /api/auth/login`: recebe `{email: string, senha: string}`, normaliza e-mail para minúsculas, exige campos não vazios, compara bcrypt, exige usuário `Ativo`, assina HS256 (expiração de ambiente ou 8h). Configuração JWT exige segredo com pelo menos 32 caracteres; nenhum valor foi consultado ou reproduzido. Sucesso atualiza `ultimo_acesso` no banco: é efeito da própria sessão, permitido ao Mobile. Não há refresh, logout remoto, revogação de token ou recuperação de senha identificados nas rotas.

Resposta de login: `{sucesso:true, mensagem:string, dados:{token:string,tipo:"Bearer",usuario:{id,id_empresa,nome,email,perfil,status}}}`. Token é descrito por tipo, nunca por exemplo real. `GET /api/auth/me` verifica Bearer, assinatura/expiração HS256, busca novamente usuário no banco e exige status ativo. Retorna `{sucesso:true,mensagem:string,dados:{id,id_empresa,nome,email,perfil}}`; **não retorna status**, diferentemente do login. Usuário inexistente/token ausente/inválido/expirado: 401; inativo/perfil negado: 403; falha de configuração/banco: 500. Perfil vem do banco após verificação, não apenas do claim.

| Grupo `/api` | Proteção identificada |
| --- | --- |
| `auth/login` | público, inicia sessão |
| `auth/me` | JWT e usuário ativo |
| `locais` | JWT em todas as rotas; GET lista por empresa e, para Gestor, por autorização `tbl_usuario_local`; GET por ID verifica permissão; escritas exigem Administrador |
| `empresas`, `tarifas` | JWT + Administrador em todo o grupo, inclusive GET |
| `limites`, `alertas` | JWT + Administrador ou Gestor em todo o grupo |
| `usuarios` | JWT + Administrador em cada operação, inclusive GET de autorizações |
| `medicoes`, `setores`, `equipamentos`, `pontos-medicao`, `dispositivos-iot`, `pzems` | sem middleware de autenticação/perfil montado nas rotas lidas, inclusive escritas |

**Limitação material:** autenticação não está aplicada de forma uniforme. As consultas globais de alertas, limites, empresas, tarifas e usuários não recebem escopo do usuário autenticado nos respectivos serviços/repositórios. A proteção por perfil não equivale a isolamento por empresa/local. Locais possuem filtragem explícita; não generalizar essa proteção às outras entidades. Escritas de locais verificam perfil, mas não repetem a autorização do GET por ID. Reconhecimento de alerta usa `req.body.id_usuario`, não `req.usuario.id`; o serviço verifica existência do usuário informado, sem vinculá-lo à identidade autenticada.

Esses achados são inspeção estática, sem teste de exploração. Não usar rotas abertas como autorização para consultar dados de terceiros. Não criar perfil/token administrador fixo para contornar acesso. Restrições de sessão Mobile somente leitura e isolamento no servidor são dependências externas a tratar com autorização específica; nenhuma correção Backend foi feita.

## 3. Convenções de contrato

GET de lista normalmente responde HTTP 200 com `{sucesso:true,quantidade:number,dados:[...]}`; `quantidade` conta o resultado, **não é paginação**. Detalhe retorna `{sucesso:true,dados:{...}}`. Lista vazia é 200 com `dados:[]`. Detalhe ausente geralmente 404; última/resumo sem medição também 404. Há exceções: GET de autorizações retorna mensagem e dados sem quantidade; `/auth/me` tem usuário resumido.

Erros dos controllers geralmente são `{sucesso:false,mensagem:string}`: 400 validação, 401/403 autenticação/perfil, 404 inexistente, 409 duplicidade/conflito e 422 inconsistência energética; 500 usa mensagem genérica. Empresa/usuário e alguns serviços tratam duplicidade explicitamente; não presumir mapeamento uniforme de todos os erros SQL. Controllers de autorização de local para usuário retornam `erro.message` inclusive em erro inesperado. Vários controllers registram objeto de erro completo no servidor; subscriber imprime payload de medição. Sem inspeção de logs reais, registrar risco de exposição de SQL/dados e não replicar esse logging no Mobile.

Consultas SQL retornam números decimais e datas conforme configuração mysql2 não auditada. Resumo converte grandezas com `Number`; consumo converte resultados explicitamente. Listas de medições retornam valores do driver: Android deve preservar nulos e aceitar o tipo efetivamente recebido após validação de integração. BIGINT de medição/alerta não cabe necessariamente em Kotlin Int; planejar Long e avaliar precisão do transporte JS/driver. Não há filtro genérico por texto/status/data, paginação ou ordenação recebida do cliente fora dos parâmetros explicitamente catalogados abaixo.

## 4. Catálogo de consultas para o Mobile

Todos os caminhos abaixo têm prefixo `/api`. São operações de leitura pelo efeito identificado. Adequação funcional não garante autorização do usuário; aplicar a matriz de proteção acima. IDs de caminho são inteiros positivos validados pelos serviços. Sem body nos GETs. Query não listada não implementa filtro, mesmo que Desktop a envie.

| GET relativo | Parâmetros efetivos | Dados e ordenação/fontes |
| --- | --- | --- |
| `/medicoes` | nenhum | todas as medições, data/hora e ID decrescentes; sem paginação |
| `/medicoes/ultima` | nenhum | última leitura **global** por data/hora DESC, id DESC; objeto com id, id_pzem, id_ponto_medicao, data_hora e seis grandezas |
| `/medicoes/resumo` | nenhum | mesma última leitura global; alias id_medicao e grandezas convertidas; não é agregado de instalação |
| `/medicoes/serie-temporal` | query id_pzem obrigatório; horas inteiro 1–168, default 24 | filtro `data_hora >= NOW()-horas`, ASC data/hora e ID, LIMIT 10000; sem limite superior temporal; não valida existência do PZEM como o consumo |
| `/medicoes/ponto/:id/ultima` | id do ponto | última leitura de cada PZEM no ponto histórico; ASC id_pzem; envelope inclui id_ponto_medicao; ponto sem leituras/inexistente pode retornar [] |
| `/medicoes/consumo` | query id_pzem, inicio, fim (YYYY-MM-DD) | objeto de consumo/custo; regras na seção 6 |
| `/locais`, `/locais/:id` | ID opcional no caminho | local: id,id_empresa,codigo,nome,endereco,responsavel,status; lista autorizada usa l.* e inclui descricao; lista ASC id, detalhe com campos explícitos sem descricao |
| `/setores`, `/setores/:id`, `/setores/local/:idLocal` | ID/setor ou local | id,id_local,codigo,nome,responsavel,descricao,status; lista/detalhe incluem nome_local; lista por local não; ASC nome |
| `/equipamentos`, `/equipamentos/:id`, `/equipamentos/setor/:idSetor` | ID/equipamento ou setor | e.*; lista/detalhe incluem nome_setor e nome_local, sem id_local agregado; por setor retorna tabela; ASC nome |
| `/pontos-medicao`, `/pontos-medicao/:id`, `/pontos-medicao/dispositivo/:idDispositivo` | ID/ponto ou dispositivo | pm.*; lista/detalhe incluem nome_local/nome_setor/nome_equipamento dos vínculos diretos; por dispositivo não; ASC nome |
| `/dispositivos-iot`, `/dispositivos-iot/:id` | ID opcional | todas as colunas do dispositivo, lista ASC identificador; status armazenado, sem cálculo de idade |
| `/pzems`, `/pzems/:id` | ID opcional | p.* + nome_ponto_medicao; lista ASC id |
| `/empresas`, `/empresas/:id` | ID opcional | identificação/contato/status; lista campos explícitos, ASC razao_social |
| `/tarifas`, `/tarifas/:id`, `/tarifas/empresa/:idEmpresa` | ID/tarifa ou empresa | tarifa.*; lista global inclui empresa, DESC id; por empresa DESC data_inicial |
| `/limites`, `/limites/:id`, `/limites/ativos` | ID opcional | todas as colunas; listas DESC id; ativos somente ativa=1, sem verificar vigência |
| `/alertas`, `/alertas/:id`, `/alertas/ativos`, `/alertas/dispositivo/:idDispositivo` | ID/alerta ou dispositivo | a.*; lista global inclui tipo_alerta,categoria_alerta; ativos inclui tipo_alerta, somente status='ativo'; detalhe/por dispositivo sem joins; listas DESC data_hora,id |
| `/usuarios`, `/usuarios/:id`, `/usuarios/empresa/:idEmpresa` | ID/usuário ou empresa | campos públicos do cadastro (sem senha), data_cadastro,ultimo_acesso; lista global inclui empresa DESC id, por empresa ASC nome |
| `/usuarios/:idUsuario/locais` | ID usuário | lista de objetos {id_local}, ASC id_local; exige Administrador, não é rota de autoescopo do Gestor |
| `/auth/me` | Bearer | usuário autenticado; seção 2 |

GET `/` (fora de /api) retorna `{sistema,status}` de funcionamento, sem testar integridade do banco. Não há rota dedicada de relatório, demanda, eventos de conexão, configuração global, consumo agregado por local/setor ou exposição direta das views do script. `PUT /api/configuracoes/conectividade` e fallback `PUT /api/dispositivos-iot/:id/status` usados pelo Desktop não estão montados no Backend analisado.

## 5. Escritas catalogadas — não consumir no Mobile

POST de login é a única exceção de sessão. As operações a seguir alteram dados de negócio mesmo quando usam PATCH de status ou apenas concedem autorização. PUT é validado como atualização completa nos serviços, não assumir merge parcial.

| Grupo | Operações relativas a /api | Corpo efetivamente tratado (nomes de campos, sem valores reais) |
| --- | --- | --- |
| medições | POST `/medicoes` | id_pzem,data_hora,tensao,corrente,potencia_ativa,energia_acumulada,frequencia,fator_potencia; ponto resolvido no servidor |
| locais | POST `/locais`; PUT `/locais/:id`; PATCH `/locais/:id/status` | id_empresa,codigo,nome,endereco,responsavel,status; PATCH status |
| setores | POST `/setores`; PUT `/setores/:id`; PATCH `/setores/:id/status` | id_local,codigo,nome,responsavel,descricao,status |
| equipamentos | POST `/equipamentos`; PUT `/equipamentos/:id`; PATCH `/equipamentos/:id/status` | id_setor,codigo,nome,tipo_equipamento,fabricante,modelo,numero_serie,potencia_nominal,tensao_nominal,data_instalacao,status; descricao não é persistida por esse service |
| pontos | POST `/pontos-medicao`; PUT `/pontos-medicao/:id`; PATCH `/pontos-medicao/:id/status` | nome,descricao,data_instalacao,status,id_dispositivo_iot e exatamente um de id_local/id_setor/id_equipamento |
| dispositivos | POST `/dispositivos-iot`; PUT `/dispositivos-iot/:id`; PATCH `/dispositivos-iot/:id/status`; DELETE `/dispositivos-iot/:id` | id_empresa,nome,identificador,mac_address,modelo,numero_serie,ip,versao_firmware,data_instalacao,ssid,broker,port,topic,client_id,tx_interval_sec,wifi,mqtt,status,data_comunicacao; PATCH trata somente status |
| PZEMs | POST `/pzems`; PUT `/pzems/:id`; PATCH `/pzems/:id/status`; DELETE `/pzems/:id` | id_ponto_medicao,numero_serie,modelo,versao,data_instalacao,status |
| empresas | POST `/empresas`; PUT `/empresas/:id`; PATCH `/empresas/:id/status` | razao_social,nome_fantasia,cnpj,email,telefone,status |
| tarifas | POST `/tarifas`; PUT `/tarifas/:id`; PATCH `/tarifas/:id/status` | id_empresa,descricao,valor_kwh,data_inicial,data_final,status |
| limites | POST `/limites`; PUT `/limites/:id`; PATCH `/limites/:id/status` | descricao,grandeza,operador,valor,severidade,periodo_inicial,periodo_final,ativa e exatamente um vínculo; PATCH ativa |
| alertas | POST `/alertas`; PATCH `/alertas/:id/reconhecer` | id_tipo_alerta,id_limite,id_dispositivo_iot,id_medicao,valor_detectado,valor_limite,severidade,mensagem; reconhecer recebe id_usuario |
| usuários | POST `/usuarios`; PUT `/usuarios/:id`; PATCH `/usuarios/:id/status`; POST `/usuarios/:idUsuario/locais/:idLocal` | cadastro id_empresa,nome,email,senha,perfil,status; PUT id_empresa,nome,email,perfil; PATCH status; concessão usa IDs do caminho |

Criações retornam 201; medições/locais/setores/equipamentos/pontos/dispositivos/PZEMs retornam ID específico no envelope; empresas/tarifas/limites/alertas/usuários retornam objeto em dados. Atualizações/status retornam 200 com dados; exclusões retornam 200 com mensagem. Autorização de local retorna 201 com {id_usuario,id_local,autorizado:true}.

Validações identificadas: códigos únicos e textos limitados; Ativo/Inativo para entidades; empresa Ativa/Inativa; equipamento ativo exige setor/local/empresa ativos; setor ativo exige local ativo; ponto ativo exige entidade e ancestrais ativos; PZEM ativo exige ponto ativo. Ponto não verifica correspondência entre empresa do dispositivo e empresa da entidade selecionada. Exclusão de dispositivo com pontos e de PZEM com medições gera 409. Dispositivo valida MAC, IPv4, porta 1–65535, intervalo 1–86400, indicadores 0/1/boolean; serviço aceita só online/offline. PZEM aceita Ativo/Inativo. Usuário aceita Administrador/Gestor, e-mail único e bcrypt custo 12; senha de cadastro 8–72 caracteres e até 72 bytes. Tarifas exigem valor >0 e período válido, mas não impedem sobreposição nem desativam outras tarifas. Há verificações prévias separadas do INSERT, sem transação abrangente identificada nesses fluxos.

## 6. Medições, cálculos e ingestão

Fluxo: dispositivo publica JSON → `mqttClient` → `medicaoSubscriber` (tópico exato de configuração, QoS 0) → `medicaoService.criar` → `medicaoRepository.criar` → tbl_medicao. POST HTTP reutiliza o mesmo serviço. O subscriber captura erro de JSON e de persistência, registra falha e não implementa fila/reprocessamento. Não foi identificado vínculo validado entre identidade do publicador MQTT e id_pzem do payload; credenciais/ACL do broker não foram auditadas. O cliente passa clientId/reconnectPeriod/connectTimeout, sem opções explícitas de autenticação nesse arquivo; infraestrutura externa pode ter controles não observados.

Criação exige data `YYYY-MM-DD HH:mm:ss` válida, preservada sem conversão de fuso, ID positivo, PZEM e ponto `Ativo`. Consulta status do dispositivo, mas não o usa como bloqueio. Ponto é obtido do cadastro atual do PZEM, não do corpo. Grandezas obrigatórias, finitas, sem boolean/vazio: tensão 0–1000; corrente 0–1000; potência ativa 0–1000000; energia acumulada 0–1000000000; frequência 0–1000; FP 0–1. Esses intervalos são validações de software, não especificação física nem limites de alarme. Futuro timestamp não é rejeitado pelo serviço; duplicata (PZEM,data_hora) encontra UNIQUE do script, sem tratamento específico de idempotência no serviço.

Unidades sustentadas pelas referências: potência bruta em W (view divide por 1000 para kW), energia acumulada usada como kWh pelo cálculo, tensão V, corrente A, frequência Hz e FP adimensional. Ingestão grava números recebidos sem conversão de escala; firmware/dispositivo real não foi fornecido, portanto não há verificação metrológica ponta a ponta. Não foram identificados potência reativa/aparente, demanda por janela, energia reativa, tarifa horária, impostos/bandeiras ou previsão implementados. Não derivar essas grandezas por suposição.

Consumo (`medicaoService.calcularConsumo` + consultas correspondentes):

1. Valida datas civis reais, inicio <= fim; busca PZEM com vínculos atuais.
2. Consulta leituras do sensor em `[inicio, fim + 1 dia)`, ordenadas por data_hora ASC,id ASC; não inclui leitura imediatamente anterior ao início.
3. Menos de duas: 200 com consumo_kwh/custo_estimado nulos, quantidade e mensagem interna em dados; não consulta tarifa nesse ramo.
4. Soma diferenças consecutivas de energia acumulada. Valores inválidos/negativos, queda do contador ou mudança de id_ponto_medicao entre leituras: 422, sem estimativa parcial.
5. Busca tarifa pela empresa do **vínculo atual** do PZEM; exige exatamente uma tarifa `Ativa` cobrindo todo o período (inicial <= inicio, final nula ou >= fim). Zero ou múltiplas tarifas elegíveis: 422, inclusive sem disponibilizar consumo isolado no resultado final.
6. Retorna consumo arredondado a seis casas, tarifa {id,descricao,valor_kwh}, custo=consumo*valor_kwh arredondado a duas, moeda BRL, datas e quantidade. Não rateia tarifas ou calcula consumo anterior à primeira amostra.

Limitações: transferência de PZEM pode usar empresa atual para leituras antigas; comparação dos pontos é estrita JS; Number(null) pode virar 0 em histórico legado, embora nova ingestão rejeite nulo. DATETIME não carrega offset; NOW()/serialização do driver/calendário Android exigem validação de fuso. Faixas do service excedem capacidade de algumas colunas (frequência DECIMAL(5,2) e extremo de energia); banco pode rejeitar valores aceitos pela aplicação.

## 7. Alarmes, limites e conectividade

Limites: tensao/corrente/potencia/energia; operadores >,<,>=,<=; severidade meta/informativo/atencao/critico; valor >=0; datas opcionais ordenadas; exatamente um vínculo de local/setor/equipamento. Consulta de ativos filtra flag, não data. Hz e FP não são grandezas de limite aceitas pelo serviço/script.

Alertas: tipo deve existir em tbl_tipo_alerta; categoria vem do tipo (energetico/comunicacao); referências opcionais verificadas por existência, sem comprovar que pertencem ao mesmo contexto/empresa. Severidade é informativo/atencao/critico e mensagem obrigatória. Criação grava NOW e status ativo; reconhecimento troca para reconhecido, informa usuário/data e impede duplicação com 409. **Não há estado resolvido no contrato de escrita/script nem rota de resolução.** Não foi encontrada chamada ao avaliador de limites a partir da ingestão: cadastro de limites/alertas não comprova geração automática de alarmes.

Conectividade via API retorna status persistido e data_comunicacao; PATCH modifica só status (wifi/mqtt extras são ignorados). Não há rotina identificada para atualizar status automaticamente pela idade, gravar eventos ou aplicar tbl_config_comunicacao. O trigger do script atualiza data_comunicacao após medição, sem alterar status. A view SQL calcula ONLINE até 120s, OFFLINE após isso, SEM COMUNICACAO para nulo e HORARIO INVALIDO para futuro; **essa view não é consultada pelos repositórios**. Script admite instavel/disponivel e PZEM Defeito, mas os serviços de escrita não os aceitam. Mobile deve distinguir status cadastrado, idade da comunicação e eventual estado calculado autorizado.

## 8. Divergências e dependências de integração

| ID | Evidência e causa identificada | Consequência para Mobile |
| --- | --- | --- |
| E01 | Desktop envia local/setor/equipamento/grandeza na série; controller só lê id_pzem/horas | filtros enviados são ignorados; resolver IDs via PZEM/ponto/entidades, sem inventar query |
| E02 | Desktop usa 720h; Backend aceita até 168; Mobile rotula 30d mas envia 168 | não trocar apenas para 720; 30 dias exige contrato adicional autorizado ou estado indisponível explícito |
| E03 | limite 10000 ASC e sem metadados de corte/cursor | pode perder medições mais recentes; não declarar histórico completo nem estatísticas integrais |
| E04 | resumo/ultima são uma leitura global | não representam consumo total nem grandezas agregadas da empresa |
| E05 | EquipamentoDto espera tipo/localizacao/id_empresa, resposta tem tipo_equipamento,nome_local,nome_setor,id_setor | enriquecer DTO com contrato e resolver hierarquia por IDs; não inventar id_local retornado |
| E06 | AlertaDto usa tipo/resolvido/id_equipamento; API usa tipo_alerta/status e referências de limite/medição/dispositivo | preservar status textual e contexto; ausência de resolvido não significa ativo; sem vínculo direto garantido ao equipamento |
| E07 | API ativos só inclui ativo; Desktop inclui reconhecido no KPI ativo | definir semântica de KPI/aba explicitamente, sem alterar estado no Mobile |
| E08 | consumo API rejeita reset; Desktop e view diária ignoram deltas negativos | não chamar ambos de mesma medida; confirmar política e preferir cálculo da API com erro explícito |
| E09 | view diária usa LAG sobre todo histórico antes de agrupar; API usa apenas amostras do intervalo | divergência na fronteira do primeiro dia; view também não separa ponto histórico e usa vínculos atuais nas relações |
| E10 | view mensal de custo seleciona tarifa ativa no primeiro dia do mês; API exige cobertura de todo período | não usar view como contrato do endpoint; sobreposição pode duplicar linhas da view |
| E11 | script contém configurações/eventos/relatórios; não há rotas nem repositórios correspondentes | tabelas/views não autorizam acesso Android direto ou promessa de funcionalidade |
| E12 | Desktop edita descricao de equipamento, service/repository não a escrevem; detalhe GET e.* pode retorná-la | ler campo disponível; não inferir suporte de edição (proibida no Mobile) |
| E13 | Mobile rotula potência bruta kW; view/relatório Desktop convertem /1000 | padronizar W/kW conforme módulo, com teste futuro e confirmação de escala de origem |
| E14 | autenticação parcial e escopo corporativo incompleto | planejar integração autorizada por módulo; não contornar 401/403 nem tratar rotas abertas como seguras |
| E15 | schema permite grandezas nulas, serviço de ingestão exige todas | manter ausente distinto de zero; resumo Number(null) exige cuidado ao validar dados legados |

DTO de consumo Mobile cobre a resposta normal, mas omite mensagem de insuficiência dentro de dados; UsuarioDto.status pode ser nulo no /me. IDs de medição/alerta devem considerar BIGINT. Datas e decimais precisam de amostra autorizada de integração, não de registros copiados para a documentação.

Consultas já disponíveis mas não integradas no Mobile: locais/setores e detalhes, equipamento por ID/setor, PZEMs/pontos/dispositivos e vínculos, últimas por ponto, tarifas/empresas/usuários, limites e contexto completo de alertas. Retrofit Mobile já declara consumo e lista geral de alertas, porém telas correspondentes não os consomem integralmente. Dashboard usa resumo/ultima/série/equipamentos/ativos; demonstrações operacionais não são integrações.

## 9. Roteiro de consulta por módulo e limites da entrega

| Módulo | Fontes prioritárias | Dependências restantes |
| --- | --- | --- |
| Dashboard/Medições | medicao Routes/Controller/Service/Repository; pzem e pontoMedicao Repositories | seleção de sensor, fuso, escala real, cobertura/limite temporal; sem agregação empresarial inventada |
| Operacional/detalhes | local/setor/equipamento e usuarioService/Repository | autorização uniforme externa; IDs e campos ausentes; correções Mobile de seleção/restauração |
| Alarmes/limites | alerta/limite e tabelas correspondentes | status e relações; geração automática não implementada; somente GET |
| Conectividade | dispositivoIot/pontoMedicao/pzem; view/trigger no script | regra de status divergente e histórico sem endpoint; não simular reconexão |
| Relatórios/tarifas | medicaoService.calcularConsumo, tarifaRepository e views | resets, janela e vigência; 30d/série não suportados; API não exporta PDF |
| Configurações consultivas | empresa/tarifa/usuario/pontoMedicao | GETs administrativos exigem perfil; minimizar dados pessoais e de rede; sem configuração global API |
| Sessão | auth Routes/Controller/Service e middlewares | armazenamento Android/expiração/logout local; sem refresh identificado |

Recomendação: preservar Development Mode e sua validação pendente; após nova autorização, adequar escopo consultivo e integrar incrementalmente as consultas com contratos identificados. Falhas de autorização/isolamento do Backend precisam de decisão e tratamento externo autorizado antes de uso sensível; não são resolvidas por login tardio ou por ocultar botões. Nenhuma implementação Mobile foi iniciada nesta análise.

## 10. Atualização Mobile — adequação consultiva implementada

Development Mode aprovado manualmente pelo usuário. Na Etapa 09 solicitada (etapa 3 do roadmap), foram removidas mutações locais administrativas em ConfiguracoesViewModel, seus estados e formulários, preservando consultas de tarifas/usuários/pontos e informação corporativa somente leitura. Não existiam chamadas Retrofit de negócio POST/PUT/PATCH/DELETE a excluir; catálogo Mobile permanece sete GETs de monitoramento e GET auth/me, além de POST auth/login permitido pela sessão. Teste local verifica a allowlist das interfaces existentes sem requisições.

Nenhuma API/repository Backend foi alterada; nenhuma nova consulta ou escrita remota implementada. As limitações de autenticação, perfis e isolamento descritas nas seções 2 e 8 permanecem dependências externas; retirar ações do Android não altera permissões do token no servidor. Implementação consultiva aguarda validação manual no emulador. A seleção de tema e formato em memória é preferência pessoal; não altera tbl_config_sistema nem dados corporativos. Detalhes em PROJECT_CONTEXT.md, seção 15.

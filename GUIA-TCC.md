# Guia de apresentação do Watt Vision

## Pré-requisitos e revisões

- Windows com Android Studio compatível com AGP 9.5.1, JDK 25 para o Gradle, Android SDK Platform 37, Platform Tools e virtualização. Crie um AVD API 37. Use o Gradle Wrapper 9.6.0 deste repositório.
- Node.js 24.13.0 e npm 11.6.2 são as versões usadas no ensaio. MySQL 8.0.46 foi a versão de referência. Prepare **uma instância MySQL descartável**, separada de qualquer base existente, com escuta em loopback.
- Use a revisão Android que contém este guia, o SQL, as fontes do Debug e os assets de PDF. A revisão antiga com URL HTTP fixa não faz parte da entrega.
- Use o Backend no commit **`2be8c5193bb4d808e75436377d77f01faa0e0e51`**. A ponta de `main` do Backend inclui recuperação de senha e SMTP e ficou fora da revisão ensaiada.

Em uma pasta nova, clone a revisão Android de entrega depois de ela ser publicada. Clone também o Backend e fixe o commit ensaiado:

```powershell
$wvRefAndroid = 'substitua-pela-branch-ou-tag-de-entrega'
git clone --branch $wvRefAndroid https://github.com/lucaseduarte/Mobile_Watt_Vision.git Mobile_Watt_Vision
git clone https://github.com/DiegodelPadua/beck-end_wattVision.git back-end_wattVision
git -C back-end_wattVision switch --detach 2be8c5193bb4d808e75436377d77f01faa0e0e51
Set-Location back-end_wattVision
npm.cmd ci
Set-Location ..
```

Reserve conexão para baixar o SDK, a imagem do AVD e as dependências. O Android Studio pode gerar seu próprio `local.properties`. O pacote PDF já está no Android, e sua compilação dispensa o repositório React.

## MySQL descartável e conta de demonstração

1. Inicie apenas o serviço MySQL da instância descartável pelo painel **Serviços** do Windows. Confirme no cliente MySQL a instância, a porta e o diretório de dados antes de importar. Use `127.0.0.1:3306` somente se essa porta for da instância descartável. Confirme o relógio do banco com `SELECT NOW(), @@session.time_zone, TIMEDIFF(NOW(), UTC_TIMESTAMP());`. O app interpreta os horários da série em `America/Sao_Paulo` (UTC−03:00 no ensaio). Com outra porta local, substitua `3306` nestas instruções e no `.env`.
2. Importe [scripts/tcc-demonstracao.sql](scripts/tcc-demonstracao.sql) por um cliente MySQL em modo lote **sem `--force`**. O script cria `db_watt_vision` e falha se esse nome já existir. Ele inclui apenas o esquema de tabelas e uma massa sintética para o TCC, com PZEM 1 e quatro leituras próximas da hora da importação. O script original com `DROP DATABASE`, contas e material de autenticação fica fora desse procedimento.
3. Exemplo com o cliente MySQL no PATH e um perfil administrativo local. O comando de configuração pede a senha no terminal e guarda o perfil no armazenamento local do cliente. Substitua o usuário e a porta conforme sua instância:

   ```powershell
   Set-Location Mobile_Watt_Vision
   mysql_config_editor.exe set --login-path=watt-tcc-admin --host=127.0.0.1 --port=3306 --user=root --password
   cmd.exe /d /c 'mysql.exe --login-path=watt-tcc-admin --protocol=TCP --default-character-set=utf8mb4 < scripts\tcc-demonstracao.sql'
   ```

4. Com autorização para a conta do ensaio, provisione um Gestor fictício vinculado à empresa e ao local da amostra. O script valida a identificação da base sintética vazia antes de inserir o usuário. Ele lê os segredos por entrada padrão, gera bcrypt localmente e mostra apenas o identificador fictício `operador.tcc@example.invalid`. Execute da raiz do Android com Node e as dependências do Backend instaladas:

   ```powershell
   $wvAdmin = Read-Host 'Usuário administrativo da instância descartável'
   $wvSenhaAdmin = Read-Host 'Senha administrativa local' -AsSecureString
   $wvSenhaOperador = Read-Host 'Nova senha do operador de demonstração' -AsSecureString
   $wvEncoding = $OutputEncoding
   try {
       $OutputEncoding = [System.Text.UTF8Encoding]::new($false)
       @{
           admin = $wvAdmin
           senhaAdmin = [pscredential]::new('admin', $wvSenhaAdmin).GetNetworkCredential().Password
           senhaOperador = [pscredential]::new('operador', $wvSenhaOperador).GetNetworkCredential().Password
       } | ConvertTo-Json -Compress | node .\scripts\tcc-provisionar-conta.mjs ..\back-end_wattVision 3306
   } finally {
       $OutputEncoding = $wvEncoding
       Remove-Variable wvSenhaAdmin,wvSenhaOperador,wvAdmin,wvEncoding
   }
   ```

5. Configure uma conta de serviço MySQL dedicada ao Backend, com senha local, restrita ao host do Backend. Para login, `/me` e série temporal desta revisão, conceda `SELECT` em `db_watt_vision.*` e `UPDATE (ultimo_acesso)` em `db_watt_vision.tbl_usuario`. O login atual registra o acesso; uma conta com apenas `SELECT` recebe erro no login. O aplicativo permanece de consulta nos fluxos de negócio. Guarde as senhas e o JWT localmente, fora do Git e dos logs.

## Backend e rede

Crie o `.env` privado na raiz do Backend a partir de `.env.example`. Use estes valores de referência e preencha os segredos localmente:

| Variável | Configuração do ensaio |
|---|---|
| `PORT` | `3000` |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | `127.0.0.1`, `3306` ou porta descartável escolhida, `db_watt_vision` |
| `DB_USER`, `DB_PASSWORD` | Conta de serviço local com as permissões acima |
| `JWT_SECRET`, `JWT_EXPIRES_IN` | Segredo aleatório exclusivo com pelo menos 32 caracteres e validade `8h` |
| `DEMO_MODE` | `false` |
| `MQTT_BROKER_URL`, `MQTT_TOPIC_MEDICOES` | Broker isolado autorizado, ou `mqtt://127.0.0.1:1` e tópico `watt-tcc-isolado` para ensaiar apenas os dados importados. A conexão MQTT poderá registrar recusas e novas tentativas nesse segundo caso. |

O Node abre a porta em endereço geral nesta revisão. Antes de iniciá-lo, confirme que a política de rede aprovada bloqueia a entrada da LAN nas portas 3000 e MySQL, preservando o acesso local e o caminho do AVD. O emulador usa `10.0.2.2:3000` para chegar ao host. Só o Backend se conecta ao MySQL.

Com o MySQL da apresentação já ativo, rode `npm.cmd start` na raiz do Backend. Confirme que `http://localhost:3000/` responde com o serviço Watt Vision.

## Android e roteiro de apresentação

1. Abra a raiz Android no Android Studio, sincronize e escolha **Debug** no AVD. O Release exige HTTPS autorizado e fica fora deste roteiro.
2. Configure a URL Debug local para autenticação. Use a opção local do Gradle indicada abaixo ou o arquivo **pessoal** de propriedades Gradle. Mantenha a série real desabilitada na primeira execução.
3. Compile e instale. Em PowerShell na raiz do Android, com JDK/SDK selecionados e AVD ativo:

   ```powershell
   .\gradlew.bat :app:installDebug -PwattVisionDebugApiUrl=http://10.0.2.2:3000/ -PwattVisionDebugLocalSeries=false
   ```

4. Abra o aplicativo, entre com a conta autorizada e use **Ver demonstração do gráfico** no Dashboard. Navegue para Medições, Alarmes, Conectividade, Operacional e Relatórios. Use os controles de demonstração quando disponíveis e mantenha visíveis os rótulos de dados simulados. Gere um PDF de teste em Relatórios antes da apresentação. O modo demonstrativo usa dados locais após a autenticação; uma instalação limpa precisa do Backend para entrar.
5. **Consulta real opcional.** Depois de confirmar isolamento, sessão e autorização, reinstale Debug trocando apenas `wattVisionDebugLocalSeries` para `true` no comando acima. No Dashboard, selecione **Dados reais do Backend**, escolha 6, 24, 48 ou 168 horas e toque em **Atualizar**. Somente o GET da série do PZEM 1 é liberado. A origem é exatamente `http://10.0.2.2:3000/`. Uma resposta dessa base está rotulada pelo app como proveniente do Backend; explique na apresentação que o banco contém **leituras sintéticas**. Com as quatro linhas ainda na janela, a última mostra 221,10 V, 2,10 kW e 15,400 kWh no contador. Medições reaproveita a mesma consulta. Volte ao modo demonstrativo, encerre a sessão e reinstale com a flag `false` após o ensaio.

## Diagnóstico rápido

| Sintoma | Verificação |
|---|---|
| Gradle falha antes de compilar | Conferir JDK 25, SDK API 37, configuração Gradle JDK no IDE e rede para o primeiro download. |
| APK abre no login e não conecta | Conferir Backend em `localhost:3000`, URL Debug `10.0.2.2:3000` e AVD. A URL LAN do host fica fora da política da série. |
| Login retorna 500 | Conferir banco selecionado, segredo JWT e permissão de atualizar `tbl_usuario.ultimo_acesso`. |
| Login retorna 401 ou 403 | Conferir conta, senha e status com o responsável pelo ensaio. Evitar registrar os valores em relatórios. |
| A interface real não aparece | Conferir variante Debug, flag explícita, URL exata e sessão validada. |
| Série vazia | Conferir a janela relativa ao relógio do MySQL e a idade da importação. Use a demonstração identificada ou prepare nova instância descartável para outro ensaio. |
| MQTT registra conexão recusada | É esperado quando foi escolhido o destino local sem broker. Confira separadamente a rota raiz e o login. |
| PDF indisponível | Confira se o asset acompanhado das licenças está versionado e se o relatório de demonstração permite exportar na seleção atual. |

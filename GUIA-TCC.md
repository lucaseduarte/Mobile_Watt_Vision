# Guia de apresentação do Watt Vision

## Pré-requisitos e revisões

- Windows com Android Studio compatível com AGP 9.5.1, JDK 25 para o Gradle, Android SDK Platform 37, Platform Tools e virtualização. Crie um AVD API 37. Use o Gradle Wrapper 9.6.0 deste repositório.
- Node.js 24.13.0 e npm 11.6.2 são as versões usadas no ensaio. MySQL 8.0.46 foi a versão de referência. Prepare **uma instância MySQL descartável**, separada de qualquer base existente, com escuta em loopback.
- Use a revisão Android que contém este guia, o SQL, as fontes do Debug e os assets de PDF. A revisão antiga com URL HTTP fixa não faz parte da entrega.
- Use o Backend no commit **`2be8c5193bb4d808e75436377d77f01faa0e0e51`**. A ponta de `main` do Backend inclui recuperação de senha e SMTP e ficou fora da revisão ensaiada.

Em uma pasta nova, clone a revisão Android de entrega depois de ela ser publicada. Clone também o Backend e fixe o commit ensaiado:

```powershell
$wvRefAndroid = 'teste-prototipo'
git clone --branch $wvRefAndroid https://github.com/lucaseduarte/Mobile_Watt_Vision.git Mobile_Watt_Vision
git clone https://github.com/DiegodelPadua/beck-end_wattVision.git back-end_wattVision
git -C back-end_wattVision switch --detach 2be8c5193bb4d808e75436377d77f01faa0e0e51
Set-Location back-end_wattVision
npm.cmd ci
Set-Location ..
```

Reserve conexão para baixar o SDK, a imagem do AVD e as dependências. O Android Studio pode gerar seu próprio `local.properties`. O pacote PDF já está no Android, e sua compilação dispensa o repositório React.

## MySQL descartável e conta de demonstração

1. Inicie apenas o serviço MySQL da instância descartável pelo painel **Serviços** do Windows. Confirme no cliente MySQL a instância, a porta e o diretório de dados antes de importar. Use `127.0.0.1:3306` somente se essa porta for da instância descartável. Confirme o relógio do banco com `SELECT NOW(), @@session.time_zone, TIMEDIFF(NOW(), UTC_TIMESTAMP());`. O app interpreta os horários da série em `America/Sao_Paulo` (UTC−03:00 no ensaio). Com outra porta local, substitua `3306` nestas instruções e em `DB_PORT`.
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

Exporte as variáveis abaixo no processo que vai iniciar o Backend. Use os valores de referência e preencha os segredos localmente. O lançador exige que o Node receba as configurações da base descartável pelo ambiente.

| Variável | Configuração do ensaio |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | `127.0.0.1`, `3306` ou porta descartável escolhida, `db_watt_vision` |
| `DB_USER`, `DB_PASSWORD` | Conta de serviço local com as permissões acima |
| `JWT_SECRET`, `JWT_EXPIRES_IN` | Segredo aleatório exclusivo com pelo menos 32 caracteres e validade `8h` |
| `DEMO_MODE` | `false` |

Na raiz do Android, execute o lançador local com o caminho da cópia do Backend no commit validado:

```powershell
$env:DB_HOST = '127.0.0.1'
$env:DB_PORT = Read-Host 'Porta da instância MySQL descartável'
$env:DB_NAME = 'db_watt_vision'
$env:DB_USER = Read-Host 'Conta de serviço MySQL'
$wvSenhaServico = Read-Host 'Senha da conta de serviço' -AsSecureString
$env:DB_PASSWORD = [pscredential]::new($env:DB_USER, $wvSenhaServico).GetNetworkCredential().Password
Remove-Variable wvSenhaServico
$env:JWT_SECRET = node -p "require('node:crypto').randomBytes(32).toString('hex')"
$env:JWT_EXPIRES_IN = '8h'
$env:DEMO_MODE = 'false'
$env:TZ = 'America/Sao_Paulo'
try {
    node .\scripts\tcc-backend-local.mjs ..\back-end_wattVision
} finally {
    Remove-Item Env:DB_PASSWORD, Env:JWT_SECRET
}
```

O lançador usa os controllers do Backend validado, escuta em `127.0.0.1:13000` e verifica a identificação da base sintética e do operador. Ele exige JWT para as consultas de negócio e restringe o acesso às rotas de leitura que o aplicativo usa. Confirme que o MySQL também escuta apenas em loopback e que a porta MySQL corresponde ao diretório descartável. O lançador dispensa um broker MQTT. Confirme a resposta de `http://127.0.0.1:13000/` no host.

## Android e roteiro de apresentação

1. Abra a raiz Android no Android Studio, sincronize e escolha **Debug** no AVD. O Release continua exigindo HTTPS.
2. Confirme o isolamento da rede e encaminhe a porta local do Backend pelo ADB:

   ```powershell
   adb reverse tcp:13000 tcp:13000
   .\gradlew.bat :app:installDebug -PwattVisionDebugApiUrl=http://127.0.0.1:13000/ -PwattVisionDebugLocalSeries=true
   ```

3. Entre com a conta autorizada. Confira Dashboard, Medições, Alarmes, Conectividade, Operacional e Relatórios. Use **Hoje** para comparar com a massa recém-importada. Em Relatórios, gere e abra um PDF de teste. Os registros são sintéticos e a origem é o MySQL.
4. Para a base recém-importada, a última leitura contém 221,10 V, 2,10 kW e 15,400 kWh no contador. O status de Conectividade é o valor persistido. A falta de leitura recente não altera esse status.
5. Encerre a sessão e pare os serviços de ensaio. Remova o encaminhamento com `adb reverse --remove tcp:13000`. Consultas de negócio ficam desabilitadas em builds sem a flag explícita.

## Diagnóstico rápido

| Sintoma | Verificação |
|---|---|
| Gradle falha antes de compilar | Conferir JDK 25, SDK API 37, configuração Gradle JDK no IDE e rede para o primeiro download. |
| APK abre no login e não conecta | Confira o Backend em `127.0.0.1:13000`, a URL do Debug, o AVD selecionado e `adb reverse --list`. |
| Login retorna 500 | Conferir banco selecionado, segredo JWT e permissão de atualizar `tbl_usuario.ultimo_acesso`. |
| Login retorna 401 ou 403 | Conferir conta, senha e status com o responsável pelo ensaio. Evitar registrar os valores em relatórios. |
| As consultas estão suspensas | Confira variante Debug, flag explícita, a URL exata e a sessão validada. |
| Série vazia | Conferir a janela relativa ao relógio do MySQL e a idade da importação. Ajuste o período ou prepare nova instância descartável para outro ensaio. |
| PDF indisponível | Confira se o asset acompanhado das licenças está versionado e se o relatório permite exportar na seleção atual. |

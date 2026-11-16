# Teste de ponta a ponta da administracao. Rode com a aplicacao ligada:
#   powershell -ExecutionPolicy Bypass -File .\testar-admin.ps1
# Usa so letras sem acento de proposito. Rodar duas vezes mostra erros 409 (ja existe): e esperado.

$base = "http://localhost:8080"

function Chamar($metodo, $caminho, $corpo = $null, $token = $null) {
    $cabecalhos = @{}
    if ($token) { $cabecalhos["Authorization"] = "Bearer $token" }
    $p = @{ Method = $metodo; Uri = "$base$caminho"; Headers = $cabecalhos; ContentType = "application/json; charset=utf-8" }
    if ($corpo) { $p["Body"] = ConvertTo-Json -InputObject $corpo -Depth 5 }
    try { return Invoke-RestMethod @p }
    catch {
        $resposta = $_.Exception.Response
        $codigo = if ($resposta) { [int]$resposta.StatusCode } else { "sem resposta" }
        $detalhe = $_.ErrorDetails.Message
        if (-not $detalhe -and $resposta) {
            try { $detalhe = (New-Object System.IO.StreamReader($resposta.GetResponseStream())).ReadToEnd() } catch { }
        }
        if (-not $detalhe) { $detalhe = $_.Exception.Message }
        Write-Host "   -> HTTP $codigo $detalhe" -ForegroundColor Yellow
        return $null
    }
}
function Passo($texto) { Write-Host ""; Write-Host "== $texto" -ForegroundColor Cyan }

$cnpjA = "11222333000181"     # Hospital A (CNPJ numerico valido)
$cnpjB = "12ABC34501DE35"     # Clinica B (CNPJ alfanumerico valido)

Passo "1. Login como administrador do sistema (resposta bruta, para diagnostico)"
try {
    $bruta = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$base/api/auth/login" -ContentType "application/json" `
        -Body '{"identificador":"admin@prontuarios.local","senha":"Admin@12345"}'
    "HTTP $($bruta.StatusCode)"
    $bruta.Content
    $admin = $bruta.Content | ConvertFrom-Json
}
catch {
    Write-Host "O login do admin falhou, entao os outros passos nao fazem sentido." -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    if ($_.ErrorDetails.Message) { Write-Host $_.ErrorDetails.Message -ForegroundColor Red }
    Write-Host "Veja tambem o log da aplicacao (terminal do mvnw)." -ForegroundColor Red
    exit 1
}
$tAdmin = $admin.token
"roles: $($admin.usuario.roles -join ', ')"
"deveTrocarSenha existe na resposta: $($admin.usuario.PSObject.Properties.Name -contains 'deveTrocarSenha')"

Passo "2. Cadastrar duas instituicoes (com e sem mascara)"
Chamar Post "/api/instituicoes" @{ nome = "Hospital A"; cnpj = "11.222.333/0001-81"; tipo = "Hospital"; endereco = "Rua 1, Pelotas" } $tAdmin | Out-Host
Chamar Post "/api/instituicoes" @{ nome = "Clinica B"; cnpj = $cnpjB; tipo = "Clinica" } $tAdmin | Out-Host

Passo "3. CNPJ invalido deve dar 400"
Chamar Post "/api/instituicoes" @{ nome = "Falsa"; cnpj = "11222333000182" } $tAdmin | Out-Null

Passo "4. Criar departamento no Hospital A"
$dep = Chamar Post "/api/instituicoes/$cnpjA/departamentos" @{ nome = "Clinica Medica" } $tAdmin
$dep | Out-Host

Passo "5. Cadastrar medico (senha temporaria aparece uma unica vez) e um admin da instituicao"
$medico = Chamar Post "/api/instituicoes/$cnpjA/usuarios" @{
    nome = "Dr Joao"; email = "joao@teste.com"; papeis = @("MEDICO"); departamentoId = $dep.id
    numeroConselho = "12345"; ufConselho = "RS"; especialidade = "Clinica Geral" } $tAdmin
$medico | ConvertTo-Json -Depth 5 | Out-Host
$adminInst = Chamar Post "/api/instituicoes/$cnpjA/usuarios" @{
    nome = "Rita Admin"; email = "rita@teste.com"; papeis = @("ADMIN_INSTITUICAO") } $tAdmin
"senha temporaria da Rita: $($adminInst.senhaTemporaria)"

Passo "6. Medico sem numero do conselho deve dar 400"
Chamar Post "/api/instituicoes/$cnpjA/usuarios" @{ nome = "Sem CRM"; email = "semcrm@teste.com"; papeis = @("MEDICO") } $tAdmin | Out-Null

Passo "7. Listar usuarios do Hospital A"
Chamar Get "/api/instituicoes/$cnpjA/usuarios" $null $tAdmin | Format-Table id, nome, email, ativo, departamentoNome, numeroConselho -AutoSize | Out-Host

Passo "8. Medico entra com a senha temporaria: precisa trocar, e nao tem permissao nenhuma ainda"
$j = Chamar Post "/api/auth/login" @{ identificador = "joao@teste.com"; senha = $medico.senhaTemporaria }
"deveTrocarSenha: $($j.usuario.deveTrocarSenha) | roles: [$($j.usuario.roles -join ', ')]"
Chamar Get "/api/instituicoes/$cnpjA/usuarios" $null $j.token | Out-Null

Passo "9. Medico troca a senha e recebe token novo com o papel MEDICO"
$j2 = Chamar Post "/api/auth/trocar-senha" @{ senhaAtual = $medico.senhaTemporaria; novaSenha = "NovaSenha123" } $j.token
"deveTrocarSenha: $($j2.usuario.deveTrocarSenha) | roles: $($j2.usuario.roles -join ', ')"
$j2.usuario.instituicoes | ConvertTo-Json -Depth 5 | Out-Host
"medico tentando listar usuarios (deve ser 403):"
Chamar Get "/api/instituicoes/$cnpjA/usuarios" $null $j2.token | Out-Null

Passo "10. Rita (admin do Hospital A) troca a senha, lista o Hospital A e tenta a Clinica B"
$r = Chamar Post "/api/auth/login" @{ identificador = "rita@teste.com"; senha = $adminInst.senhaTemporaria }
$r2 = Chamar Post "/api/auth/trocar-senha" @{ senhaAtual = $adminInst.senhaTemporaria; novaSenha = "NovaSenha123" } $r.token
"Hospital A (deve listar):"
(Chamar Get "/api/instituicoes/$cnpjA/usuarios" $null $r2.token).Count
"Clinica B (deve ser 403):"
Chamar Get "/api/instituicoes/$cnpjB/usuarios" $null $r2.token | Out-Null
"Rita criando instituicao (deve ser 403):"
Chamar Post "/api/instituicoes" @{ nome = "X"; cnpj = "00000000000191" } $r2.token | Out-Null

Passo "11. Rita altera o papel do Dr Joao e depois inativa o vinculo dele"
Chamar Put "/api/instituicoes/$cnpjA/usuarios/$($medico.usuario.id)/papeis" @{
    papeis = @("MEDICO", "RECEPCIONISTA"); departamentoId = $dep.id } $r2.token | ConvertTo-Json -Depth 5 | Out-Host
Chamar Delete "/api/instituicoes/$cnpjA/usuarios/$($medico.usuario.id)" $null $r2.token | Out-Host
"Rita tentando se inativar (deve ser 409):"
Chamar Delete "/api/instituicoes/$cnpjA/usuarios/$($adminInst.usuario.id)" $null $r2.token | Out-Null

Passo "12. Dr Joao entra de novo: sem instituicoes ativas, entao sem papeis"
$j3 = Chamar Post "/api/auth/login" @{ identificador = "joao@teste.com"; senha = "NovaSenha123" }
"roles: [$($j3.usuario.roles -join ', ')] | instituicoes: $($j3.usuario.instituicoes.Count)"

Write-Host ""; Write-Host "Fim. Linhas amarelas 'HTTP 4xx' sao os erros esperados dos passos 3, 6, 8, 9, 10, 11." -ForegroundColor Green
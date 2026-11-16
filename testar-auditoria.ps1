# Teste de ponta a ponta da auditoria.
# Rode com a aplicacao ligada:
#   powershell -ExecutionPolicy Bypass -File .\testar-auditoria.ps1
# Pode rodar quantas vezes quiser: cada execucao cria usuarios e pacientes novos.
# Usa so letras sem acento de proposito.

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

function NovoCpf {
    $d = @()
    1..9 | ForEach-Object { $d += (Get-Random -Minimum 0 -Maximum 10) }
    foreach ($n in 10, 11) {
        $soma = 0
        for ($i = 0; $i -lt ($n - 1); $i++) { $soma += $d[$i] * ($n - $i) }
        $resto = ($soma * 10) % 11
        if ($resto -eq 10) { $resto = 0 }
        $d += $resto
    }
    return ($d -join "")
}

function EntrarComo($cnpj, $nome, $email, $papeis, $tAdmin, $extra = @{}) {
    $corpo = @{ nome = $nome; email = $email; papeis = $papeis }
    foreach ($k in $extra.Keys) { $corpo[$k] = $extra[$k] }
    $criado = Chamar Post "/api/instituicoes/$cnpj/usuarios" $corpo $tAdmin
    if (-not $criado) { return $null }
    $login = Chamar Post "/api/auth/login" @{ identificador = $email; senha = $criado.senhaTemporaria }
    $troca = Chamar Post "/api/auth/trocar-senha" @{ senhaAtual = $criado.senhaTemporaria; novaSenha = "NovaSenha123" } $login.token
    return $troca.token
}

function NovoPaciente($cnpj, $nome, $email, $tRecepcao) {
    $cpf = NovoCpf
    $novo = Chamar Post "/api/instituicoes/$cnpj/pacientes" @{ nome = $nome; cpf = $cpf; email = $email; dataNascimento = "1990-05-20" } $tRecepcao
    if (-not $novo) { return $null }
    $login = Chamar Post "/api/auth/login" @{ identificador = $cpf; senha = $novo.senhaTemporaria }
    $troca = Chamar Post "/api/auth/trocar-senha" @{ senhaAtual = $novo.senhaTemporaria; novaSenha = "NovaSenha123" } $login.token
    return @{ cpf = $cpf; token = $troca.token }
}

function Tabela($logs) {
    $logs | ForEach-Object {
        $c = $_.chaveRecurso | ConvertFrom-Json
        [pscustomobject]@{ id = $_.id; usuario = $_.nomeUsuario; acao = $_.acao; status = $c.status; cnpj = $c.cnpj; cpf = $c.cpf; entre = $c.entreInstituicoes; ip = $c.ip }
    } | Format-Table -AutoSize | Out-Host
}

$cnpjA = "11222333000181"     # Hospital A
$cnpjB = "12ABC34501DE35"     # Clinica B
$s = Get-Date -Format "yyyyMMddHHmmss"

Passo "1. Preparacao"
$admin = Chamar Post "/api/auth/login" @{ identificador = "admin@prontuarios.local"; senha = "Admin@12345" }
if (-not $admin) { Write-Host "Login do admin falhou." -ForegroundColor Red; exit 1 }
$tAdmin = $admin.token
Chamar Post "/api/instituicoes" @{ nome = "Hospital A"; cnpj = $cnpjA; tipo = "Hospital" } $tAdmin | Out-Null
Chamar Post "/api/instituicoes" @{ nome = "Clinica B"; cnpj = $cnpjB; tipo = "Clinica" } $tAdmin | Out-Null
$tAdmA = EntrarComo $cnpjA "Rita Admin A" "admina$s@teste.com" @("ADMIN_INSTITUICAO") $tAdmin
$tRecA = EntrarComo $cnpjA "Maria Recepcao" "recepcaoa$s@teste.com" @("RECEPCIONISTA") $tAdmin
$tMedA = EntrarComo $cnpjA "Dr Carlos" "medicoa$s@teste.com" @("MEDICO") $tAdmin @{ numeroConselho = "11111"; ufConselho = "RS" }
$tRecB = EntrarComo $cnpjB "Joana Recepcao B" "recepcaob$s@teste.com" @("RECEPCIONISTA") $tAdmin
$tMedB = EntrarComo $cnpjB "Dra Lucia" "medicob$s@teste.com" @("MEDICO") $tAdmin @{ numeroConselho = "33333"; ufConselho = "SC" }
$ana = NovoPaciente $cnpjA "Ana Souza Teste" "ana$s@teste.com" $tRecA
if (-not ($tAdmA -and $tRecA -and $tMedA -and $tRecB -and $tMedB -and $ana)) { Write-Host "Preparacao falhou." -ForegroundColor Red; exit 1 }
$inicio = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
$de = [uri]::EscapeDataString($inicio)
"tudo pronto. Ana: CPF $($ana.cpf). Os eventos abaixo sao so os que acontecem a partir de agora ($inicio)."

Passo "2. Atividade no Hospital A: abrir, iniciar, diagnosticar, consultar"
$at = Chamar Post "/api/instituicoes/$cnpjA/atendimentos" @{ cpfPaciente = $ana.cpf; motivo = "Dor de garganta" } $tRecA
Chamar Post "/api/instituicoes/$cnpjA/atendimentos/$($at.id)/iniciar" $null $tMedA | Out-Null
Chamar Post "/api/instituicoes/$cnpjA/atendimentos/$($at.id)/diagnosticos" @{ descricao = "Faringite"; cid = "J02.9" } $tMedA | Out-Null
Chamar Get "/api/instituicoes/$cnpjA/atendimentos/$($at.id)/prontuario" $null $tMedA | Out-Null
Chamar Get "/api/instituicoes/$cnpjA/pacientes/$($ana.cpf)/historico" $null $tMedA | Out-Null

Passo "3. Dra Lucia (Clinica B) tenta, e e barrada; depois ganha acesso aprovado, consulta, e perde o acesso"
Chamar Get "/api/instituicoes/$cnpjB/atendimentos/$($at.id)/prontuario" $null $tMedB | Out-Null
$sol = Chamar Post "/api/instituicoes/$cnpjB/solicitacoes-acesso" @{ cpfPaciente = $ana.cpf; finalidade = "Avaliacao cardiologica" } $tRecB
Chamar Post "/api/paciente/solicitacoes-acesso/$($sol.id)/aprovar" $null $ana.token | Out-Null
Chamar Get "/api/instituicoes/$cnpjB/pacientes/$($ana.cpf)/historico" $null $tMedB | Out-Null
Chamar Get "/api/instituicoes/$cnpjB/atendimentos/$($at.id)/prontuario" $null $tMedB | Out-Null
Chamar Post "/api/paciente/solicitacoes-acesso/$($sol.id)/revogar" $null $ana.token | Out-Null
Chamar Get "/api/instituicoes/$cnpjB/atendimentos/$($at.id)/prontuario" $null $tMedB | Out-Null

Passo "4. Tentativa de login com senha errada na conta da Ana (401)"
Chamar Post "/api/auth/login" @{ identificador = $ana.cpf; senha = "SenhaErrada999" } | Out-Null

Passo "5. A Ana ve quem acessou os dados dela (somente acoes de OUTRAS pessoas)"
$acessos = Chamar Get "/api/paciente/acessos" $null $ana.token
$acessos | Format-Table instante, acao, ator, instituicao, status, entreInstituicoes -AutoSize | Out-Host
"total: $($acessos.Count) (esperado: 11 acoes de outras pessoas, contando as 2 tentativas negadas; as aprovacoes e revogacoes da propria Ana nao entram)"

Passo "6. Admin do sistema: tudo o que aconteceu com o CPF da Ana"
$tudo = Chamar Get "/api/auditoria?cpf=$($ana.cpf)&de=$de&limite=200" $null $tAdmin
Tabela $tudo
"total: $($tudo.Count) (esperado: 12 desde o inicio do teste, ja incluindo as acoes da propria Ana)"

Passo "7. Admin do sistema: so as consultas de prontuario; so as da Clinica B; so os logins (de agora em diante)"
"consultas de prontuario:"
Tabela (Chamar Get "/api/auditoria?acao=CONSULTAR_PRONTUARIO&cpf=$($ana.cpf)&de=$de" $null $tAdmin)
"eventos da Clinica B (acesso entre instituicoes aparece com entre = True):"
Tabela (Chamar Get "/api/auditoria?cnpj=$cnpjB&cpf=$($ana.cpf)&de=$de" $null $tAdmin)
"logins (status 200 = ok, 401 = falha):"
Tabela (Chamar Get "/api/auditoria?acao=LOGIN&de=$de&limite=20" $null $tAdmin)

Passo "8. Admin do Hospital A ve so os eventos do Hospital A"
$logsA = Chamar Get "/api/instituicoes/$cnpjA/auditoria?de=$de&limite=200" $null $tAdmA
$cnpjsVistos = $logsA | ForEach-Object { ($_.chaveRecurso | ConvertFrom-Json).cnpj } | Sort-Object -Unique
"eventos: $($logsA.Count) | instituicoes presentes: $($cnpjsVistos -join ', ') (so deve aparecer $cnpjA)"

Passo "9. Permissoes da auditoria: todos os 403 abaixo sao esperados"
"admin do Hospital A olhando a Clinica B:"
Chamar Get "/api/instituicoes/$cnpjB/auditoria" $null $tAdmA | Out-Null
"admin do Hospital A olhando o log global:"
Chamar Get "/api/auditoria" $null $tAdmA | Out-Null
"medico olhando o log global:"
Chamar Get "/api/auditoria" $null $tMedA | Out-Null
"paciente olhando o log global:"
Chamar Get "/api/auditoria" $null $ana.token | Out-Null
"medico olhando 'quem acessou meus dados' (area do paciente):"
Chamar Get "/api/paciente/acessos" $null $tMedA | Out-Null

Passo "10. A consulta ao log tambem e auditada (quem auditou)"
Tabela (Chamar Get "/api/auditoria?acao=CONSULTAR_AUDITORIA&de=$de&limite=5" $null $tAdmin)

Write-Host ""; Write-Host "Fim. Linhas amarelas 'HTTP 4xx' sao os erros esperados dos passos 3, 4 e 9." -ForegroundColor Green

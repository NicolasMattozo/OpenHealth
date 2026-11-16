# Teste de ponta a ponta da area do paciente (casos de uso 15, 16 e 17 + revogacao).
# Rode com a aplicacao ligada:
#   powershell -ExecutionPolicy Bypass -File .\testar-paciente.ps1
# Pode rodar quantas vezes quiser: cada execucao cria recepcionistas e pacientes novos.
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

function EntrarComo($cnpj, $nome, $email, $papeis, $tAdmin) {
    $criado = Chamar Post "/api/instituicoes/$cnpj/usuarios" @{ nome = $nome; email = $email; papeis = $papeis } $tAdmin
    if (-not $criado) { return $null }
    $login = Chamar Post "/api/auth/login" @{ identificador = $email; senha = $criado.senhaTemporaria }
    $troca = Chamar Post "/api/auth/trocar-senha" @{ senhaAtual = $criado.senhaTemporaria; novaSenha = "NovaSenha123" } $login.token
    return $troca.token
}

# Cadastra um paciente pela recepcao e devolve o token dele (login pelo CPF + troca de senha)
function NovoPaciente($cnpj, $nome, $email, $tRecepcao) {
    $cpf = NovoCpf
    $novo = Chamar Post "/api/instituicoes/$cnpj/pacientes" @{ nome = $nome; cpf = $cpf; email = $email; dataNascimento = "1990-05-20" } $tRecepcao
    if (-not $novo) { return $null }
    $login = Chamar Post "/api/auth/login" @{ identificador = $cpf; senha = $novo.senhaTemporaria }
    $troca = Chamar Post "/api/auth/trocar-senha" @{ senhaAtual = $novo.senhaTemporaria; novaSenha = "NovaSenha123" } $login.token
    return @{ cpf = $cpf; token = $troca.token }
}

function Resumo($lista) {
    $lista | Format-Table id, nomeInstituicao, status, finalidade -AutoSize | Out-Host
}

$cnpjA = "11222333000181"     # Hospital A
$cnpjB = "12ABC34501DE35"     # Clinica B (CNPJ alfanumerico)
$sufixo = Get-Date -Format "yyyyMMddHHmmss"

Passo "1. Preparacao (409 = instituicao ja existe, ok)"
$admin = Chamar Post "/api/auth/login" @{ identificador = "admin@prontuarios.local"; senha = "Admin@12345" }
if (-not $admin) { Write-Host "Login do admin falhou." -ForegroundColor Red; exit 1 }
$tAdmin = $admin.token
Chamar Post "/api/instituicoes" @{ nome = "Hospital A"; cnpj = $cnpjA; tipo = "Hospital" } $tAdmin | Out-Null
Chamar Post "/api/instituicoes" @{ nome = "Clinica B"; cnpj = $cnpjB; tipo = "Clinica" } $tAdmin | Out-Null
$tRecA = EntrarComo $cnpjA "Maria Recepcao A" "recepcaoa$sufixo@teste.com" @("RECEPCIONISTA") $tAdmin
$tRecB = EntrarComo $cnpjB "Joana Recepcao B" "recepcaob$sufixo@teste.com" @("RECEPCIONISTA") $tAdmin
$ana = NovoPaciente $cnpjA "Ana Souza Teste" "ana$sufixo@teste.com" $tRecA
$bia = NovoPaciente $cnpjA "Bia Lima Teste" "bia$sufixo@teste.com" $tRecA
if (-not $tRecA -or -not $tRecB -or -not $ana -or -not $bia) { Write-Host "Preparacao falhou." -ForegroundColor Red; exit 1 }
"pacientes Ana (CPF $($ana.cpf)) e Bia (CPF $($bia.cpf)) prontas"

Passo "2. Hospital A e Clinica B solicitam acesso aos dados da Ana"
$sA = Chamar Post "/api/instituicoes/$cnpjA/solicitacoes-acesso" @{ cpfPaciente = $ana.cpf; finalidade = "Acompanhamento no Hospital A" } $tRecA
$sB = Chamar Post "/api/instituicoes/$cnpjB/solicitacoes-acesso" @{ cpfPaciente = $ana.cpf; finalidade = "Continuidade do tratamento" } $tRecB

Passo "3. Ana lista as solicitacoes (duas PENDENTE)"
Resumo (Chamar Get "/api/paciente/solicitacoes-acesso" $null $ana.token)

Passo "4. Ana aprova a da Clinica B; aprovar de novo deve dar 409"
Chamar Post "/api/paciente/solicitacoes-acesso/$($sB.id)/aprovar" $null $ana.token | Out-Host
Chamar Post "/api/paciente/solicitacoes-acesso/$($sB.id)/aprovar" $null $ana.token | Out-Null

Passo "5. Ana nega a do Hospital A; negar de novo deve dar 409"
Chamar Post "/api/paciente/solicitacoes-acesso/$($sA.id)/negar" $null $ana.token | Out-Host
Chamar Post "/api/paciente/solicitacoes-acesso/$($sA.id)/negar" $null $ana.token | Out-Null

Passo "6. Clinica B tenta pedir de novo com acesso ja aprovado (409)"
Chamar Post "/api/instituicoes/$cnpjB/solicitacoes-acesso" @{ cpfPaciente = $ana.cpf; finalidade = "Repetido" } $tRecB | Out-Null

Passo "7. Ana revoga o acesso da Clinica B; revogar uma NEGADA deve dar 409"
Chamar Post "/api/paciente/solicitacoes-acesso/$($sB.id)/revogar" $null $ana.token | Out-Host
Chamar Post "/api/paciente/solicitacoes-acesso/$($sA.id)/revogar" $null $ana.token | Out-Null

Passo "8. Depois de revogado, a Clinica B pode pedir de novo; Ana aprova; o atendimento da Clinica B fica ligado a essa solicitacao"
$sB2 = Chamar Post "/api/instituicoes/$cnpjB/solicitacoes-acesso" @{ cpfPaciente = $ana.cpf; finalidade = "Novo pedido apos revogacao" } $tRecB
Chamar Post "/api/paciente/solicitacoes-acesso/$($sB2.id)/aprovar" $null $ana.token | Out-Null
$at = Chamar Post "/api/instituicoes/$cnpjB/atendimentos" @{ cpfPaciente = $ana.cpf; motivo = "Consulta de retorno" } $tRecB
"idSolicitacao do atendimento: $($at.idSolicitacao) (esperado: $($sB2.id))"

Passo "9. Historico completo da Ana"
Resumo (Chamar Get "/api/paciente/solicitacoes-acesso" $null $ana.token)

Passo "10. Isolamento e permissoes: todos os erros abaixo sao esperados"
"Bia tentando revogar a solicitacao da Ana (404, sem revelar que existe):"
Chamar Post "/api/paciente/solicitacoes-acesso/$($sB2.id)/revogar" $null $bia.token | Out-Null
"Lista da Bia (deve vir vazia):"
(Chamar Get "/api/paciente/solicitacoes-acesso" $null $bia.token).Count
"recepcionista acessando a area do paciente (403):"
Chamar Get "/api/paciente/solicitacoes-acesso" $null $tRecB | Out-Null
"paciente acessando a area da recepcao (403):"
Chamar Get "/api/instituicoes/$cnpjB/solicitacoes-acesso" $null $ana.token | Out-Null
"solicitacao inexistente (404):"
Chamar Post "/api/paciente/solicitacoes-acesso/999999/aprovar" $null $ana.token | Out-Null

Write-Host ""; Write-Host "Fim. Linhas amarelas 'HTTP 4xx' sao os erros esperados dos passos 4, 5, 6, 7 e 10." -ForegroundColor Green

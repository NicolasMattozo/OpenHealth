# Teste de ponta a ponta da recepcao (casos de uso 2, 3, 4 e 18).
# Rode com a aplicacao ligada e depois de testar-admin.ps1:
#   powershell -ExecutionPolicy Bypass -File .\testar-recepcao.ps1
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

# Gera um CPF valido aleatorio
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

# Cria um usuario na instituicao, entra com a senha temporaria, troca a senha e devolve o token
function EntrarComo($cnpj, $nome, $email, $papeis, $tAdmin) {
    $criado = Chamar Post "/api/instituicoes/$cnpj/usuarios" @{ nome = $nome; email = $email; papeis = $papeis } $tAdmin
    if (-not $criado) { return $null }
    $login = Chamar Post "/api/auth/login" @{ identificador = $email; senha = $criado.senhaTemporaria }
    $troca = Chamar Post "/api/auth/trocar-senha" @{ senhaAtual = $criado.senhaTemporaria; novaSenha = "NovaSenha123" } $login.token
    return $troca.token
}

$cnpjA = "11222333000181"     # Hospital A
$cnpjB = "12ABC34501DE35"     # Clinica B (CNPJ alfanumerico)
$sufixo = Get-Date -Format "yyyyMMddHHmmss"
$cpf = NovoCpf

Passo "1. Preparacao: admin do sistema, instituicoes e duas recepcionistas (409 = instituicao ja existe, ok)"
$admin = Chamar Post "/api/auth/login" @{ identificador = "admin@prontuarios.local"; senha = "Admin@12345" }
if (-not $admin) { Write-Host "Login do admin falhou." -ForegroundColor Red; exit 1 }
$tAdmin = $admin.token
Chamar Post "/api/instituicoes" @{ nome = "Hospital A"; cnpj = $cnpjA; tipo = "Hospital" } $tAdmin | Out-Null
Chamar Post "/api/instituicoes" @{ nome = "Clinica B"; cnpj = $cnpjB; tipo = "Clinica" } $tAdmin | Out-Null
$tRecA = EntrarComo $cnpjA "Maria Recepcao A" "recepcaoa$sufixo@teste.com" @("RECEPCIONISTA") $tAdmin
$tRecB = EntrarComo $cnpjB "Joana Recepcao B" "recepcaob$sufixo@teste.com" @("RECEPCIONISTA") $tAdmin
if (-not $tRecA -or -not $tRecB) { Write-Host "Nao foi possivel preparar as recepcionistas." -ForegroundColor Red; exit 1 }
"recepcionistas prontas (Hospital A e Clinica B)"

Passo "2. Cadastrar paciente (CPF $cpf)"
$novo = Chamar Post "/api/instituicoes/$cnpjA/pacientes" @{
    nome = "Ana Souza Teste"; cpf = $cpf; email = "ana$sufixo@teste.com"
    dataNascimento = "1990-05-20"; telefone = "53999990000"; endereco = "Rua das Flores 10, Pelotas" } $tRecA
$novo | ConvertTo-Json -Depth 5 | Out-Host

Passo "3. Mesmo CPF de novo: 409 com os dados ja cadastrados (fluxo A1)"
Chamar Post "/api/instituicoes/$cnpjA/pacientes" @{
    nome = "Outra Pessoa"; cpf = $cpf; email = "outra$sufixo@teste.com"; dataNascimento = "1991-01-01" } $tRecA | Out-Null

Passo "4. CPF invalido (400), e-mail repetido (409) e data futura (400)"
Chamar Post "/api/instituicoes/$cnpjA/pacientes" @{ nome = "X"; cpf = "12345678900"; email = "x$sufixo@teste.com"; dataNascimento = "1990-01-01" } $tRecA | Out-Null
Chamar Post "/api/instituicoes/$cnpjA/pacientes" @{ nome = "Y"; cpf = (NovoCpf); email = "ana$sufixo@teste.com"; dataNascimento = "1990-01-01" } $tRecA | Out-Null
Chamar Post "/api/instituicoes/$cnpjA/pacientes" @{ nome = "Z"; cpf = (NovoCpf); email = "z$sufixo@teste.com"; dataNascimento = "2999-01-01" } $tRecA | Out-Null

Passo "5. Consultar: por CPF (com mascara), por parte do nome, e busca curta demais (400)"
$mascara = "{0}.{1}.{2}-{3}" -f $cpf.Substring(0,3), $cpf.Substring(3,3), $cpf.Substring(6,3), $cpf.Substring(9,2)
(Chamar Get "/api/instituicoes/$cnpjA/pacientes?busca=$mascara" $null $tRecA) | Format-Table cpf, nome, email -AutoSize | Out-Host
(Chamar Get "/api/instituicoes/$cnpjA/pacientes?busca=Souza" $null $tRecA) | Format-Table cpf, nome -AutoSize | Out-Host
Chamar Get "/api/instituicoes/$cnpjA/pacientes?busca=ab" $null $tRecA | Out-Null

Passo "6. Atualizar telefone e endereco"
Chamar Put "/api/instituicoes/$cnpjA/pacientes/$cpf" @{ telefone = "53988887777"; endereco = "Avenida Brasil 500, Pelotas" } $tRecA | Out-Host

Passo "7. Abrir atendimento no Hospital A; o segundo deve dar 409; CPF inexistente deve dar 404"
Chamar Post "/api/instituicoes/$cnpjA/atendimentos" @{ cpfPaciente = $cpf; motivo = "Dor de cabeca ha 3 dias" } $tRecA | Out-Host
Chamar Post "/api/instituicoes/$cnpjA/atendimentos" @{ cpfPaciente = $cpf; motivo = "Outro motivo" } $tRecA | Out-Null
Chamar Post "/api/instituicoes/$cnpjA/atendimentos" @{ cpfPaciente = (NovoCpf); motivo = "Teste" } $tRecA | Out-Null

Passo "8. Listar atendimentos do Hospital A (so os ABERTO)"
(Chamar Get "/api/instituicoes/$cnpjA/atendimentos?status=ABERTO" $null $tRecA) | Format-Table id, nomePaciente, motivo, status, nomeRecepcionista -AutoSize | Out-Host

Passo "9. Clinica B solicita acesso ao paciente; repetir deve dar 409"
Chamar Post "/api/instituicoes/$cnpjB/solicitacoes-acesso" @{ cpfPaciente = $cpf; finalidade = "Continuidade do tratamento" } $tRecB | Out-Host
Chamar Post "/api/instituicoes/$cnpjB/solicitacoes-acesso" @{ cpfPaciente = $cpf; finalidade = "De novo" } $tRecB | Out-Null

Passo "10. Listar solicitacoes da Clinica B"
(Chamar Get "/api/instituicoes/$cnpjB/solicitacoes-acesso" $null $tRecB) | Format-Table id, nomePaciente, status, finalidade -AutoSize | Out-Host

Passo "11. Permissoes: todos os 403 abaixo sao esperados"
"recepcionista do Hospital A listando pacientes da Clinica B:"
Chamar Get "/api/instituicoes/$cnpjB/pacientes?busca=Souza" $null $tRecA | Out-Null
"recepcionista da Clinica B abrindo atendimento no Hospital A:"
Chamar Post "/api/instituicoes/$cnpjA/atendimentos" @{ cpfPaciente = $cpf; motivo = "x" } $tRecB | Out-Null
"admin do sistema cadastrando paciente (nao e recepcionista):"
Chamar Post "/api/instituicoes/$cnpjA/pacientes" @{ nome = "W"; cpf = (NovoCpf); email = "w$sufixo@teste.com"; dataNascimento = "1990-01-01" } $tAdmin | Out-Null

Passo "12. Paciente entra pelo CPF com a senha temporaria, troca a senha e recebe o papel PACIENTE"
$pl = Chamar Post "/api/auth/login" @{ identificador = $cpf; senha = $novo.senhaTemporaria }
"deveTrocarSenha: $($pl.usuario.deveTrocarSenha) | roles: [$($pl.usuario.roles -join ', ')]"
$pt = Chamar Post "/api/auth/trocar-senha" @{ senhaAtual = $novo.senhaTemporaria; novaSenha = "NovaSenha123" } $pl.token
"deveTrocarSenha: $($pt.usuario.deveTrocarSenha) | roles: $($pt.usuario.roles -join ', ') | cpf: $($pt.usuario.cpf)"

Write-Host ""; Write-Host "Fim. Linhas amarelas 'HTTP 4xx' sao os erros esperados dos passos 3, 4, 5, 7, 9 e 11." -ForegroundColor Green

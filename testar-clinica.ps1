# Teste de ponta a ponta da parte clinica (casos de uso 5 a 14).
# Rode com a aplicacao ligada:
#   powershell -ExecutionPolicy Bypass -File .\testar-clinica.ps1
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

# Cria o usuario na instituicao, entra com a senha temporaria, troca a senha e devolve o token.
# $extra: dados do conselho (medico/enfermeiro).
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

function Linha($registros) {
    $registros | Format-Table id, tipo, autor, visivelAoPaciente, classificacaoRisco, tipoDado, descricao, medicamento, tipoExame, statusExame -AutoSize | Out-Host
}

$cnpjA = "11222333000181"     # Hospital A
$cnpjB = "12ABC34501DE35"     # Clinica B
$s = Get-Date -Format "yyyyMMddHHmmss"

Passo "1. Preparacao: usuarios do Hospital A (recepcao, medico, enfermeiro) e da Clinica B (recepcao, medico); paciente Ana e Bia"
$admin = Chamar Post "/api/auth/login" @{ identificador = "admin@prontuarios.local"; senha = "Admin@12345" }
if (-not $admin) { Write-Host "Login do admin falhou." -ForegroundColor Red; exit 1 }
$tAdmin = $admin.token
Chamar Post "/api/instituicoes" @{ nome = "Hospital A"; cnpj = $cnpjA; tipo = "Hospital" } $tAdmin | Out-Null
Chamar Post "/api/instituicoes" @{ nome = "Clinica B"; cnpj = $cnpjB; tipo = "Clinica" } $tAdmin | Out-Null
$tRecA = EntrarComo $cnpjA "Maria Recepcao" "recepcaoa$s@teste.com" @("RECEPCIONISTA") $tAdmin
$tMedA = EntrarComo $cnpjA "Dr Carlos" "medicoa$s@teste.com" @("MEDICO") $tAdmin @{ numeroConselho = "11111"; ufConselho = "RS"; especialidade = "Clinica Geral" }
$tEnfA = EntrarComo $cnpjA "Enf Paula" "enfa$s@teste.com" @("ENFERMEIRO") $tAdmin @{ numeroConselho = "22222"; ufConselho = "RS" }
$tRecB = EntrarComo $cnpjB "Joana Recepcao B" "recepcaob$s@teste.com" @("RECEPCIONISTA") $tAdmin
$tMedB = EntrarComo $cnpjB "Dra Lucia" "medicob$s@teste.com" @("MEDICO") $tAdmin @{ numeroConselho = "33333"; ufConselho = "SC"; especialidade = "Cardiologia" }
$ana = NovoPaciente $cnpjA "Ana Souza Teste" "ana$s@teste.com" $tRecA
$bia = NovoPaciente $cnpjA "Bia Lima Teste" "bia$s@teste.com" $tRecA
if (-not ($tRecA -and $tMedA -and $tEnfA -and $tRecB -and $tMedB -and $ana -and $bia)) { Write-Host "Preparacao falhou." -ForegroundColor Red; exit 1 }
"tudo pronto. Ana: CPF $($ana.cpf)"

Passo "2. Recepcao abre o atendimento da Ana no Hospital A"
$at = Chamar Post "/api/instituicoes/$cnpjA/atendimentos" @{ cpfPaciente = $ana.cpf; motivo = "Tosse e febre ha 2 dias" } $tRecA
$idAt = $at.id
"atendimento $idAt, status $($at.status)"

Passo "3. Enfermeira registra triagem e sinais vitais; classificacao invalida (400)"
Chamar Post "/api/instituicoes/$cnpjA/atendimentos/$idAt/triagem" @{ classificacaoRisco = "AMARELO"; sintomas = "Tosse seca, febre 38.5" } $tEnfA | Out-Null
Chamar Post "/api/instituicoes/$cnpjA/atendimentos/$idAt/triagem" @{ classificacaoRisco = "ROXO" } $tEnfA | Out-Null
Chamar Post "/api/instituicoes/$cnpjA/atendimentos/$idAt/dados-clinicos" @{ tipo = "Pressao arterial"; valor = "120/80 mmHg" } $tEnfA | Out-Null
Chamar Post "/api/instituicoes/$cnpjA/atendimentos/$idAt/dados-clinicos" @{ tipo = "Anotacao interna"; valor = "Paciente ansiosa"; visivelAoPaciente = $false } $tEnfA | Out-Null

Passo "4. Medico tenta registrar antes de iniciar (409); enfermeira tenta iniciar (403); medico da Clinica B tenta iniciar (403)"
Chamar Post "/api/instituicoes/$cnpjA/atendimentos/$idAt/diagnosticos" @{ descricao = "Teste"; cid = "J06.9" } $tMedA | Out-Null
Chamar Post "/api/instituicoes/$cnpjA/atendimentos/$idAt/iniciar" $null $tEnfA | Out-Null
Chamar Post "/api/instituicoes/$cnpjA/atendimentos/$idAt/iniciar" $null $tMedB | Out-Null

Passo "5. Dr Carlos inicia o atendimento (repetir e idempotente)"
$ini = Chamar Post "/api/instituicoes/$cnpjA/atendimentos/$idAt/iniciar" $null $tMedA
"status: $($ini.status) | medico: $($ini.nomeMedico)"
Chamar Post "/api/instituicoes/$cnpjA/atendimentos/$idAt/iniciar" $null $tMedA | Out-Null

Passo "6. Dr Carlos registra diagnostico, prescricao e requisicao de exame"
Chamar Post "/api/instituicoes/$cnpjA/atendimentos/$idAt/diagnosticos" @{ descricao = "Infeccao das vias aereas superiores"; cid = "j06.9" } $tMedA | Out-Null
Chamar Post "/api/instituicoes/$cnpjA/atendimentos/$idAt/prescricoes" @{ medicamento = "Dipirona"; dosagem = "500 mg"; via = "Oral"; frequencia = "A cada 6 horas" } $tMedA | Out-Null
$exame = Chamar Post "/api/instituicoes/$cnpjA/atendimentos/$idAt/requisicoes-exames" @{ tipoExame = "Hemograma completo" } $tMedA
"requisicao de exame $($exame.id), status $($exame.statusExame)"

Passo "7. Permissoes dos registros: enfermeira nao diagnostica (403); recepcao nao faz triagem (403)"
Chamar Post "/api/instituicoes/$cnpjA/atendimentos/$idAt/diagnosticos" @{ descricao = "x" } $tEnfA | Out-Null
Chamar Post "/api/instituicoes/$cnpjA/atendimentos/$idAt/triagem" @{ classificacaoRisco = "VERDE" } $tRecA | Out-Null

Passo "8. Enfermeira lanca o resultado do exame; lancar de novo cria a versao 2"
(Chamar Post "/api/instituicoes/$cnpjA/requisicoes-exames/$($exame.id)/resultados" @{ resultado = "Leucocitos 12.000" } $tEnfA) | Out-Host
(Chamar Post "/api/instituicoes/$cnpjA/requisicoes-exames/$($exame.id)/resultados" @{ resultado = "Leucocitos 11.800 (corrigido)" } $tEnfA) | Out-Host

Passo "9. Dr Carlos finaliza; depois disso nao ha mais triagem (409), mas o resultado de exame ainda pode ser lancado (versao 3)"
$fim = Chamar Post "/api/instituicoes/$cnpjA/atendimentos/$idAt/finalizar" $null $tMedA
"status: $($fim.status)"
Chamar Post "/api/instituicoes/$cnpjA/atendimentos/$idAt/triagem" @{ classificacaoRisco = "VERDE" } $tEnfA | Out-Null
(Chamar Post "/api/instituicoes/$cnpjA/requisicoes-exames/$($exame.id)/resultados" @{ resultado = "Leucocitos 11.800 (conferido)" } $tEnfA).versao

Passo "10. Prontuario detalhado visto pelo medico (todos os registros, na ordem)"
$detMed = Chamar Get "/api/instituicoes/$cnpjA/atendimentos/$idAt/prontuario" $null $tMedA
Linha $detMed.registros
"total para o medico: $($detMed.registros.Count)"
"resultados do exame (versoes): $(($detMed.registros | Where-Object { $_.tipo -eq 'REQUISICAO_EXAME' }).resultados.versao -join ', ')"

Passo "11. Historico da Ana visto pelo medico do Hospital A"
Chamar Get "/api/instituicoes/$cnpjA/pacientes/$($ana.cpf)/historico" $null $tMedA | Format-Table id, nomeInstituicao, motivo, status, nomeMedico -AutoSize | Out-Host

Passo "12. Consultas por tipo: medicamentos (so enfermeiro), requisicoes (medico/enfermeiro), exames (so medico)"
"medicamentos pela enfermeira:"
Chamar Get "/api/instituicoes/$cnpjA/pacientes/$($ana.cpf)/medicamentos" $null $tEnfA | Format-Table medicamento, dosagem, via, frequencia -AutoSize | Out-Host
"medicamentos pelo medico (403, o caso de uso 12 e da enfermagem):"
Chamar Get "/api/instituicoes/$cnpjA/pacientes/$($ana.cpf)/medicamentos" $null $tMedA | Out-Null
"requisicoes pela enfermeira:"
Chamar Get "/api/instituicoes/$cnpjA/pacientes/$($ana.cpf)/requisicoes-exames" $null $tEnfA | Format-Table tipoExame, statusExame -AutoSize | Out-Host
"exames pelo medico:"
Chamar Get "/api/instituicoes/$cnpjA/pacientes/$($ana.cpf)/exames" $null $tMedA | Format-Table tipoExame, statusExame -AutoSize | Out-Host

Passo "13. Outra instituicao: a Dra Lucia (Clinica B) NAO ve nada da Ana sem acesso aprovado"
"historico (deve vir vazio):"
(Chamar Get "/api/instituicoes/$cnpjB/pacientes/$($ana.cpf)/historico" $null $tMedB).Count
"prontuario do atendimento $idAt (403):"
Chamar Get "/api/instituicoes/$cnpjB/atendimentos/$idAt/prontuario" $null $tMedB | Out-Null

Passo "14. Clinica B solicita acesso, Ana aprova: agora a Dra Lucia ve o historico e o prontuario"
$sol = Chamar Post "/api/instituicoes/$cnpjB/solicitacoes-acesso" @{ cpfPaciente = $ana.cpf; finalidade = "Avaliacao cardiologica" } $tRecB
Chamar Post "/api/paciente/solicitacoes-acesso/$($sol.id)/aprovar" $null $ana.token | Out-Null
"historico (deve ter 1):"
(Chamar Get "/api/instituicoes/$cnpjB/pacientes/$($ana.cpf)/historico" $null $tMedB).Count
$detB = Chamar Get "/api/instituicoes/$cnpjB/atendimentos/$idAt/prontuario" $null $tMedB
"prontuario visto pela Clinica B: $($detB.registros.Count) registros"
"Dra Lucia tentando registrar no atendimento do Hospital A (404, atendimento e de outra instituicao):"
Chamar Post "/api/instituicoes/$cnpjB/atendimentos/$idAt/diagnosticos" @{ descricao = "x" } $tMedB | Out-Null

Passo "15. Ana revoga o acesso: a Clinica B perde a visao na hora"
Chamar Post "/api/paciente/solicitacoes-acesso/$($sol.id)/revogar" $null $ana.token | Out-Null
"historico (deve voltar a vazio):"
(Chamar Get "/api/instituicoes/$cnpjB/pacientes/$($ana.cpf)/historico" $null $tMedB).Count
Chamar Get "/api/instituicoes/$cnpjB/atendimentos/$idAt/prontuario" $null $tMedB | Out-Null

Passo "16. A propria Ana consulta o prontuario: so ve o que e visivel a ela"
Chamar Get "/api/paciente/historico" $null $ana.token | Format-Table id, nomeInstituicao, motivo, status, nomeMedico -AutoSize | Out-Host
$detAna = Chamar Get "/api/paciente/atendimentos/$idAt" $null $ana.token
"registros que a Ana ve: $($detAna.registros.Count) (o medico viu $($detMed.registros.Count); a diferenca e a anotacao interna)"
"medicamentos da Ana:"
Chamar Get "/api/paciente/medicamentos" $null $ana.token | Format-Table medicamento, dosagem, frequencia -AutoSize | Out-Host
"requisicoes de exame da Ana:"
Chamar Get "/api/paciente/requisicoes-exames" $null $ana.token | Format-Table tipoExame, statusExame -AutoSize | Out-Host
"exames da Ana (com resultado):"
$exAna = Chamar Get "/api/paciente/exames" $null $ana.token
$exAna | Format-Table tipoExame, statusExame -AutoSize | Out-Host
"versoes do resultado: $($exAna.resultados.versao -join ', ')"

Passo "17. Isolamento: Bia nao ve o atendimento da Ana (404) e a lista dela vem vazia; medico nao acessa a area do paciente (403)"
Chamar Get "/api/paciente/atendimentos/$idAt" $null $bia.token | Out-Null
(Chamar Get "/api/paciente/historico" $null $bia.token).Count
Chamar Get "/api/paciente/historico" $null $tMedA | Out-Null

Write-Host ""; Write-Host "Fim. Linhas amarelas 'HTTP 4xx' sao os erros esperados dos passos 3, 4, 5, 7, 9, 12, 13, 14, 15 e 17." -ForegroundColor Green

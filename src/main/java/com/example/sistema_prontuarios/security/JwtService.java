package com.example.sistema_prontuarios.security;

import com.example.sistema_prontuarios.web.dto.UsuarioLogadoDto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Gera os tokens JWT (assinados com HS256) entregues ao frontend no login. */
@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final long expiracaoSegundos;

    public JwtService(JwtEncoder encoder,
                      @Value("${app.jwt.expiracao-minutos:60}") long expiracaoMinutos) {
        this.encoder = encoder;
        this.expiracaoSegundos = expiracaoMinutos * 60;
    }

    public long getExpiracaoSegundos() {
        return expiracaoSegundos;
    }

    public String gerar(UsuarioLogadoDto usuario) {
        Instant agora = Instant.now();

        List<Map<String, Object>> instituicoes = usuario.instituicoes().stream()
                .map(v -> Map.<String, Object>of("cnpj", v.cnpj(), "papeis", v.papeis()))
                .toList();

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("sistema-prontuarios")
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(expiracaoSegundos))
                .subject(String.valueOf(usuario.id()))
                .claim("nome", usuario.nome())
                .claim("email", usuario.email())
                .claim("roles", usuario.roles())
                .claim("instituicoes", instituicoes);
        if (usuario.cpf() != null) {
            claims.claim("cpf", usuario.cpf());
        }

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }
}

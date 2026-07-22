package com.biblioteca.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;

// Es el servicio encriptador, recibe el token, confirma que fue firmado con la clave correcta y que no expiró. Retorna el contenido del token.

@Service
public class JwtService {

    private final SecretKey clave;

    public JwtService(@Value("${jwt.internal.secret}") String secretoBase64) {
        byte[] decodedKey = Base64.getDecoder().decode(secretoBase64);
        this.clave = Keys.hmacShaKeyFor(decodedKey);
    }

    // Valida el token y retorna sus claims si es válido. Lanza excepción si no lo es.
    public Claims validarToken(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Extrae el uid de Firebase (subject) desde un token ya validado.
    public String extraerUid(String token) {
        return validarToken(token).getSubject();
    }
}
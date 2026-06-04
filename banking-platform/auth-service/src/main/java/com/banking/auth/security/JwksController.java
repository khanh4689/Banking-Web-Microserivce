package com.banking.auth.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class JwksController {

    private final JwtUtil jwtUtil;

    public JwksController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> keys() {
        RSAPublicKey publicKey = jwtUtil.getPublicKey();
        
        // Manual Modulus and Exponent Encoding to avoid nimbus dependency
        byte[] nBytes = publicKey.getModulus().toByteArray();
        // Remove leading zero byte for correct JWK formatting
        if (nBytes[0] == 0) {
            byte[] tmp = new byte[nBytes.length - 1];
            System.arraycopy(nBytes, 1, tmp, 0, tmp.length);
            nBytes = tmp;
        }

        Map<String, Object> jwk = new HashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("kid", "auth-service-key-1");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("n", Base64.getUrlEncoder().withoutPadding().encodeToString(nBytes));
        jwk.put("e", Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.getPublicExponent().toByteArray()));

        return Map.of("keys", List.of(jwk));
    }
}

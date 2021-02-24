package com.microsoft.dagx.iam.oauth2.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import com.microsoft.dagx.iam.oauth2.spi.OAuth2Service;
import com.microsoft.dagx.iam.oauth2.spi.VerificationResult;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.function.Function;

public class OAuth2ServiceImpl implements OAuth2Service {
    private JWTVerifier verifier;

    public OAuth2ServiceImpl(Function<String, RSAPublicKey> publicProvider, Function<String, RSAPrivateKey> privateProvider) {
       
        verifier = JWT.require(Algorithm.RSA256(new RSAKeyProvider() {
            @Override
            public RSAPublicKey getPublicKeyById(String keyId) {
                return publicProvider.apply(keyId);
            }

            @Override
            public RSAPrivateKey getPrivateKey() {
                return privateProvider.apply(null);
            }

            @Override
            public String getPrivateKeyId() {
                return null;  // TODO
            }
        })).build();
    }

    @Override
    public String obtainClientCredentials(String scope) {
        return null;
    }

    @Override
    public VerificationResult verifyJwtToken(String token) {
        try {
            DecodedJWT jwt = verifier.verify(token);
            return VerificationResult.VALID_TOKEN;
        } catch (JWTVerificationException e) {
            return new VerificationResult(e.getMessage());
        }
    }
}

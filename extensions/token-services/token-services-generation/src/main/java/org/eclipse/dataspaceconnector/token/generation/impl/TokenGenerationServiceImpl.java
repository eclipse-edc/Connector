package org.eclipse.dataspaceconnector.token.generation.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.token.spi.TokenGenerationService;
import org.jetbrains.annotations.NotNull;

public class TokenGenerationServiceImpl implements TokenGenerationService {

    private final JWSSigner tokenSigner;
    private final JWSAlgorithm jwsAlgorithm;

    public TokenGenerationServiceImpl(JWSSigner signer) {
        this.tokenSigner = signer;
        if (tokenSigner instanceof ECDSASigner) {
            jwsAlgorithm = JWSAlgorithm.ES256;
        } else {
            jwsAlgorithm = JWSAlgorithm.RS256;
        }
    }

    @Override
    public Result<String> generate(@NotNull JWTClaimsSet claims) {
        var header = new JWSHeader(jwsAlgorithm);
        var token = new SignedJWT(header, claims);
        try {
            token.sign(tokenSigner);
        } catch (JOSEException e) {
            return Result.failure("Failed to sign token");
        }
        return Result.success(token.serialize());
    }
}

package org.eclipse.dataspaceconnector.token.validation.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.token.spi.TokenValidationService;
import org.eclipse.dataspaceconnector.token.spi.ValidationRule;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class TokenValidationServiceImpl implements TokenValidationService {

    private final PublicKey publicKey;
    private final List<ValidationRule> validationRules;

    public TokenValidationServiceImpl(PublicKey publicKey, List<ValidationRule> validationRules) {
        this.publicKey = publicKey;
        this.validationRules = Collections.unmodifiableList(validationRules);
    }

    @Override
    public Result<ClaimToken> validate(@NotNull String token) {
        JWTClaimsSet claimsSet;
        try {
            var jwt = SignedJWT.parse(token);
            var verifier = new DefaultJWSVerifierFactory().createJWSVerifier(jwt.getHeader(), publicKey);
            if (!jwt.verify(verifier)) {
                return Result.failure("Token verification failed");
            }

            claimsSet = jwt.getJWTClaimsSet();
        } catch (JOSEException e) {
            return Result.failure(e.getMessage());
        } catch (ParseException e) {
            return Result.failure("Failed to decode token");
        }

        var errors = validationRules.stream()
                .map(r -> r.checkRule(claimsSet))
                .filter(Result::failed)
                .map(Result::getFailureMessages)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        if (!errors.isEmpty()) {
            return Result.failure(errors);
        }

        var tokenBuilder = ClaimToken.Builder.newInstance();
        claimsSet.getClaims().entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), Objects.toString(entry.getValue())))
                .filter(entry -> entry.getValue() != null)
                .forEach(entry -> tokenBuilder.claim(entry.getKey(), entry.getValue()));

        return Result.success(tokenBuilder.build());
    }
}

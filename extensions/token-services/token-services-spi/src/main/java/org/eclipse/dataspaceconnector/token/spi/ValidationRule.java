package org.eclipse.dataspaceconnector.token.spi;

import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ValidationRule {

    Result<JWTClaimsSet> checkRule(@NotNull JWTClaimsSet toVerify);
}

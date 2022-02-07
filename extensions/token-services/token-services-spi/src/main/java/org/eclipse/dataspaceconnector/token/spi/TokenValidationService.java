package org.eclipse.dataspaceconnector.token.spi;

import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;

public interface TokenValidationService {

    Result<ClaimToken> validate(@NotNull String token);
}

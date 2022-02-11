package org.eclipse.dataspaceconnector.token;

import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.dataspaceconnector.spi.iam.ValidationRule;

public interface JwtClaimValidationRule extends ValidationRule<JWTClaimsSet> {
}

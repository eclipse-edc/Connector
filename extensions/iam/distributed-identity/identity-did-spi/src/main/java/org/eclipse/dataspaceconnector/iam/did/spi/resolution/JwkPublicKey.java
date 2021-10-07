package org.eclipse.dataspaceconnector.iam.did.spi.resolution;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(as = EllipticCurvePublicKey.class)
public interface JwkPublicKey {
    String getKty();
}

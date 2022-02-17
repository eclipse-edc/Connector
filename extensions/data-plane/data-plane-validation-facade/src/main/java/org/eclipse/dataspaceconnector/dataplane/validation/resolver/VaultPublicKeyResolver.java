package org.eclipse.dataspaceconnector.dataplane.validation.resolver;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.PublicKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.PublicKey;

public class VaultPublicKeyResolver implements PublicKeyResolver {

    private final Vault vault;
    private final String publicKeyAlias;

    public VaultPublicKeyResolver(@NotNull Vault vault, @NotNull String publicKeyAlias) {
        this.vault = vault;
        this.publicKeyAlias = publicKeyAlias;
    }

    @Override
    public @Nullable PublicKey resolveKey(String id) {
        var secret = vault.resolveSecret(publicKeyAlias);
        if (secret == null) {
            throw new EdcException("Failed to retrieve secret with id: " + publicKeyAlias);
        }
        try {
            ECKey jwk = (ECKey) JWK.parseFromPEMEncodedObjects(secret);
            return jwk.toRSAKey().toPublicKey();
        } catch (JOSEException e) {
            throw new EdcException(e);
        }
    }
}

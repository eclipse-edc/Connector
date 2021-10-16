package org.eclipse.dataspaceconnector.iam.did.resolver;

import org.eclipse.dataspaceconnector.iam.did.crypto.key.KeyConverter;
import org.eclipse.dataspaceconnector.iam.did.spi.document.DidConstants;
import org.eclipse.dataspaceconnector.iam.did.spi.document.VerificationMethod;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolver;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class DefaultDidPublicKeyResolver implements DidPublicKeyResolver {
    private final DidResolver didResolver;
    // this is NOT a comprehensive list as specified in https://www.w3.org/TR/did-spec-registries/#verification-method-types

    public DefaultDidPublicKeyResolver(DidResolver didResolver) {
        this.didResolver = didResolver;
    }

    @Override
    public @Nullable PublicKeyWrapper resolvePublicKey(String didUrl) {
        var didDocument = didResolver.resolve(didUrl);
        if (didDocument == null) {
            return null;
        }
        if (didDocument.getVerificationMethod() == null || didDocument.getVerificationMethod().isEmpty()) {
            throw new PublicKeyResolutionException("DID does not contain a Public Key!");
        }

        List<VerificationMethod> verificationMethods = didDocument.getVerificationMethod().stream().filter(vm -> DidConstants.ALLOWED_VERIFICATION_TYPES.contains(vm.getType())).collect(Collectors.toList());
        if (verificationMethods.size() > 1) {
            throw new PublicKeyResolutionException("DID contains more than one \"Allowed Verification Type\"!");
        }

        VerificationMethod verificationMethod = didDocument.getVerificationMethod().get(0);
        var jwk = verificationMethod.getPublicKeyJwk();
        try {
            return KeyConverter.toPublicKeyWrapper(jwk, verificationMethod.getId());
        } catch (IllegalArgumentException e) {
            throw new PublicKeyResolutionException("Public Key was not a valid EC Key!  Details: " + e.getMessage());
        }
    }

}

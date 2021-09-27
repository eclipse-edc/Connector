package org.eclipse.dataspaceconnector.verifiablecredential;

import org.eclipse.dataspaceconnector.iam.did.spi.hub.keys.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidConstants;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.resolution.VerificationMethod;
import org.eclipse.dataspaceconnector.ion.spi.IonClient;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class IonDidPublicKeyResolver implements DidPublicKeyResolver {
    private final IonClient ionClient;
    // this is NOT a comprehensive list as specified in https://www.w3.org/TR/did-spec-registries/#verification-method-types

    public IonDidPublicKeyResolver(IonClient ionClient) {
        this.ionClient = ionClient;
    }

    @Override
    public @Nullable PublicKeyWrapper resolvePublicKey(String didUrl) {
        var didDocument = ionClient.resolve(didUrl);
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

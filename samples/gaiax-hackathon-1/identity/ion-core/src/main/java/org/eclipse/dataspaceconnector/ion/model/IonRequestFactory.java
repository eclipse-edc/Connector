package org.eclipse.dataspaceconnector.ion.model;

import com.nimbusds.jose.jwk.JWK;
import kotlin.NotImplementedError;
import org.eclipse.dataspaceconnector.ion.IonException;
import org.eclipse.dataspaceconnector.ion.util.JsonCanonicalizer;
import org.eclipse.dataspaceconnector.ion.util.MultihashHelper;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class IonRequestFactory {
    @NotNull
    public static IonRequest createCreateRequest(JWK recoveryKey, JWK updateKey, Map<String, Object> document) {

        List<PublicKeyDescriptor> didDocumentKeys = (List<PublicKeyDescriptor>) document.get("publicKeys");
        var services = document.get("services");

        InputValidator.validateEs256kOperationKey(recoveryKey, "public");
        InputValidator.validateEs256kOperationKey(updateKey, "public");

        validateDidDocumentKeys(didDocumentKeys);

        try {
            var patches = Collections.singletonList(Map.of(
                    "action", "replace",
                    "document", document));

            var buffer = JsonCanonicalizer.canonicalizeAsBytes(updateKey);

            var delta = new Delta(MultihashHelper.doubleHashAndEncode(buffer), patches);

            var deltaHash = MultihashHelper.hashAndEncode(JsonCanonicalizer.canonicalizeAsBytes(delta));

            var rcBuffer = JsonCanonicalizer.canonicalizeAsBytes(recoveryKey);
            var suffixData = new SuffixData(MultihashHelper.doubleHashAndEncode(rcBuffer), deltaHash);

            return new IonCreateRequest(suffixData, delta);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IonException(ex);
        }
    }


    private static void validateDidDocumentKeys(List<PublicKeyDescriptor> didDocumentKeys) {
        if (didDocumentKeys == null) {
            return;
        }

        //make sure IDs are unique
        if (!didDocumentKeys.stream().map(PublicKeyDescriptor::getId).allMatch(new HashSet<>()::add)) {
            throw new IonException("All public key IDs in the DID document must be unique!");
        }

        //make sure all public key purposes are unique
        var purposesValid = didDocumentKeys.stream().map(PublicKeyDescriptor::getPurposes)
                .allMatch(IonRequestFactory::validateKeyPurposes);

        if (!purposesValid) {
            throw new IonException("Public key purposes are not unique in on of the key descriptors!");
        }
    }

    private static boolean validateKeyPurposes(String[] purposes) {
        return Arrays.stream(purposes).allMatch(new HashSet<>()::add);
    }

    public static IonRequest createUpdateRequest() {
        throw new NotImplementedError();
    }

    public static IonRequest createRecoverRequest() {
        throw new NotImplementedError();
    }

    public static IonRequest createDeactivateRequest() {
        throw new NotImplementedError();
    }
}

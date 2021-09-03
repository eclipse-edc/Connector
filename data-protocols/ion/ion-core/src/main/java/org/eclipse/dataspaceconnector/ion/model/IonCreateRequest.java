package org.eclipse.dataspaceconnector.ion.model;

import org.eclipse.dataspaceconnector.ion.util.JsonCanonicalizer;
import org.eclipse.dataspaceconnector.ion.util.MultihashHelper;

import java.util.Base64;

/**
 * Contains the necessary data to issue a CREATE request to ION.
 *
 * @see IonRequestFactory
 * @see org.eclipse.dataspaceconnector.ion.spi.IonClient#submit(IonRequest)
 */
public class IonCreateRequest extends IonRequest {
    private final SuffixData suffixData;
    private final Delta delta;

    public IonCreateRequest(SuffixData suffixData, Delta delta) {
        super("create");
        this.suffixData = suffixData;
        this.delta = delta;
    }

    @Override
    public SuffixData getSuffixData() {
        return suffixData;
    }

    @Override
    public Delta getDelta() {
        return delta;
    }

    public String getDidUri() {
        var didUniqueSuffix = computeDidUniqueSuffix(getSuffixData());
        return "did:ion:" + didUniqueSuffix;
    }

    public String getSuffix() {
        String uriShort = getDidUri();
        var index = uriShort.lastIndexOf(":");
        return uriShort.substring(index);
    }

    private String computeDidUniqueSuffix(SuffixData suffixData) {

        var bytes = JsonCanonicalizer.canonicalizeAsBytes(suffixData);
        byte[] multihash = MultihashHelper.hash(bytes);

        return new String(Base64.getUrlEncoder().withoutPadding().encode(multihash));

    }
}

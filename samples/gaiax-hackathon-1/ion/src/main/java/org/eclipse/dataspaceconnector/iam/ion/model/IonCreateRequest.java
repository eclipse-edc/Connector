package org.eclipse.dataspaceconnector.iam.ion.model;

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
}

package org.eclipse.dataspaceconnector.iam.ion.dto.did;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DidDocumentMetadata {
    Method method;
    List<String> equivalentId;

    @JsonProperty("method")
    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    @JsonProperty("equivalentId")
    public List<String> getEquivalentId() {
        return equivalentId;
    }

    public void setEquivalentId(List<String> equivalentId) {
        this.equivalentId = equivalentId;
    }
}

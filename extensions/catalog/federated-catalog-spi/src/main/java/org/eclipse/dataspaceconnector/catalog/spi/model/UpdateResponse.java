package org.eclipse.dataspaceconnector.catalog.spi.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;

public class UpdateResponse {
    private String source;
    private Collection<String> assetNames;


    @JsonCreator
    public UpdateResponse(@JsonProperty("source") String source, @JsonProperty("assets") Collection<String> assetNames) {
        this.source = source;

        this.assetNames = assetNames;
    }

    public UpdateResponse() {

    }

    public Collection<String> getAssetNames() {
        return assetNames;
    }

    public String getSource() {
        return source;
    }
}

package org.eclipse.dataspaceconnector.iam.ion.dto.did;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 *
 */
public class ServiceEndpoint {
    private String context;
    private String type;
    private List<String> locations;

    @JsonProperty("@context")
    public String getContext() {
        return context;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("locations")
    public List<String> getLocations() {
        return locations;
    }

    public ServiceEndpoint(@JsonProperty("@context") String context, @JsonProperty("type") String type, @JsonProperty("locations") List<String> locations) {
        this.context = context;
        this.type = type;
        this.locations = locations;
    }
}

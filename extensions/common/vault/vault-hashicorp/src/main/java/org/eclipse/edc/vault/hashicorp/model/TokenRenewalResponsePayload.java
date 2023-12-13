package org.eclipse.edc.vault.hashicorp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenRenewalResponsePayload {
    @JsonProperty("warnings")
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<String> warnings;

    @JsonProperty("auth")
    private TokenRenewalResponsePayloadAuth auth;

    public List<String> getWarnings() {
        return warnings;
    }

    public TokenRenewalResponsePayloadAuth getAuth() {
        return auth;
    }
}


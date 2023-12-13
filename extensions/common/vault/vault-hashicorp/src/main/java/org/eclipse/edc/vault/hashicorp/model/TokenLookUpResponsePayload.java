package org.eclipse.edc.vault.hashicorp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenLookUpResponsePayload {
    private TokenLookUpResponsePayloadData data;

    public TokenLookUpResponsePayloadData getData() {
        return data;
    }
}


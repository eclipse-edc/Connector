package org.eclipse.dataspaceconnector.iam.ion.dto.did;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VerificationMethod {
    String id;
    String controller;
    String type;
    PublicKeyJwk publicKeyJwk;

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("controller")
    public String getController() {
        return controller;
    }

    public void setController(String controller) {
        this.controller = controller;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("publicKeyJwk")
    public PublicKeyJwk getPublicKeyJwk() {
        return publicKeyJwk;
    }

    public void setPublicKeyJwk(PublicKeyJwk publicKeyJwk) {
        this.publicKeyJwk = publicKeyJwk;
    }
}

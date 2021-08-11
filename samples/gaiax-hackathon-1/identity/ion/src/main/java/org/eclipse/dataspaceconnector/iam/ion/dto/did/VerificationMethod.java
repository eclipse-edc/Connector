package org.eclipse.dataspaceconnector.iam.ion.dto.did;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = VerificationMethod.Builder.class)
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


    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        String id;
        String controller;
        String type;
        PublicKeyJwk publicKeyJwk;

        private Builder() {
        }

        @JsonCreator
        public static Builder create() {
            return new Builder();
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder controller(String controller) {
            this.controller = controller;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder publicKeyJwk(PublicKeyJwk publicKeyJwk) {
            this.publicKeyJwk = publicKeyJwk;
            return this;
        }

        public VerificationMethod build() {
            VerificationMethod verificationMethod = new VerificationMethod();
            verificationMethod.setId(id);
            verificationMethod.setController(controller);
            verificationMethod.setType(type);
            verificationMethod.setPublicKeyJwk(publicKeyJwk);
            return verificationMethod;
        }
    }
}

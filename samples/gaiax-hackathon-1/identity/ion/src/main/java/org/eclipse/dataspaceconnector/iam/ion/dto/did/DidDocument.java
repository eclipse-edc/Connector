package org.eclipse.dataspaceconnector.iam.ion.dto.did;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.List;

@JsonDeserialize(builder = DidDocument.Builder.class)
public class DidDocument {
    String id;
    List<Object> context;
    List<Service> service;
    List<VerificationMethod> verificationMethod;
    List<String> authentication;

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("@context")
    public List<Object> getContext() {
        return context;
    }

    public void setContext(List<Object> context) {
        this.context = context;
    }

    @JsonProperty("service")
    public List<Service> getService() {
        return service;
    }

    public void setService(List<Service> service) {
        this.service = service;
    }

    @JsonProperty("verificationMethod")
    public List<VerificationMethod> getVerificationMethod() {
        return verificationMethod;
    }

    public void setVerificationMethod(List<VerificationMethod> verificationMethod) {
        this.verificationMethod = verificationMethod;
    }

    @JsonProperty("authentication")
    public List<String> getAuthentication() {
        return authentication;
    }

    public void setAuthentication(List<String> authentication) {
        this.authentication = authentication;
    }


    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        String id;
        List<Object> context;
        List<Service> service;
        List<VerificationMethod> verificationMethod;
        List<String> authentication;

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

        public Builder context(List<Object> context) {
            this.context = context;
            return this;
        }

        public Builder service(List<Service> service) {
            this.service = service;
            return this;
        }

        public Builder verificationMethod(List<VerificationMethod> verificationMethod) {
            this.verificationMethod = verificationMethod;
            return this;
        }

        public Builder authentication(List<String> authentication) {
            this.authentication = authentication;
            return this;
        }

        public DidDocument build() {
            DidDocument didDocument = new DidDocument();
            didDocument.setId(id);
            didDocument.setContext(context);
            didDocument.setService(service);
            didDocument.setVerificationMethod(verificationMethod);
            didDocument.setAuthentication(authentication);
            return didDocument;
        }
    }
}



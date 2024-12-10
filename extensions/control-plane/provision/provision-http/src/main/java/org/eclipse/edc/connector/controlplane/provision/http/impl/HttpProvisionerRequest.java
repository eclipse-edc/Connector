/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.provision.http.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.policy.model.Policy;

/**
 * A request to provision or deprovision asset content that can be sent to an out-of-process systen.
 */
@JsonTypeName("dataspaceconnector:httpprovisionerrequest")
@JsonDeserialize(builder = HttpProvisionerRequest.Builder.class)
public class HttpProvisionerRequest {

    private String assetId;
    private String transferProcessId;
    private Policy policy;
    private String callbackAddress;
    private Type type = Type.PROVISION;
    private String resourceDefinitionId;

    private HttpProvisionerRequest() {
    }

    public String getAssetId() {
        return assetId;
    }

    public String getTransferProcessId() {
        return transferProcessId;
    }

    public Policy getPolicy() {
        return policy;
    }

    public String getCallbackAddress() {
        return callbackAddress;
    }

    public String getResourceDefinitionId() {
        return resourceDefinitionId;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        @JsonProperty("provision")
        PROVISION,

        @JsonProperty("deprovision")
        DEPROVISION
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {

        private final HttpProvisionerRequest request;

        private Builder() {
            request = new HttpProvisionerRequest();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder assetId(String assetId) {
            request.assetId = assetId;
            return this;
        }

        public Builder transferProcessId(String transferProcessId) {
            request.transferProcessId = transferProcessId;
            return this;
        }

        public Builder policy(Policy policy) {
            request.policy = policy;
            return this;
        }

        public Builder callbackAddress(String callbackAddress) {
            request.callbackAddress = callbackAddress;
            return this;
        }

        public Builder type(Type type) {
            request.type = type;
            return this;
        }

        public Builder resourceDefinitionId(String resourceDefinitionId) {
            request.resourceDefinitionId = resourceDefinitionId;
            return this;
        }

        public HttpProvisionerRequest build() {
            return request;
        }
    }

}

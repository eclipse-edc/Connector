/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *       Microsoft Corporation
 *
 */

package org.eclipse.dataspaceconnector.spi.types.domain.transfer;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * An asynchronous response to a provision request.
 */
public class ProvisionResponse {
    private final ProvisionedResource resource;
    private final SecretToken secretToken;
    private final boolean inProcess;

    /**
     * Instantiates a response with a provisioned resource and optional secret token.
     *
     * @param resource the provisioned result
     * @param secretToken the optional secret
     */
    private ProvisionResponse(ProvisionedResource resource, @Nullable SecretToken secretToken) {
        this.resource = resource;
        this.secretToken = secretToken;
        this.inProcess = false;
    }

    /**
     * Instantiates a response indicating the provisioning operation is in-process and will be completed at a later point.
     */
    public ProvisionResponse() {
        this.resource = null;
        this.secretToken = null;
        this.inProcess = true;
    }

    /**
     * True if the provisioning operation is in-process and will be completed at a later point.
     */
    public boolean isInProcess() {
        return inProcess;
    }

    public ProvisionedResource getResource() {
        return resource;
    }

    @Nullable
    public SecretToken getSecretToken() {
        return secretToken;
    }

    public static class Builder {
        private ProvisionedResource resource;
        private SecretToken secretToken;

        private Builder() {

        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder resource(ProvisionedResource resource) {
            this.resource = resource;
            return this;
        }

        public Builder secretToken(SecretToken secretToken) {
            this.secretToken = secretToken;
            return this;
        }

        public ProvisionResponse build() {
            Objects.requireNonNull(resource, "resource");
            return new ProvisionResponse(resource, secretToken);
        }
    }
}

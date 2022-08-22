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

package org.eclipse.dataspaceconnector.transfer.provision.http.config;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.net.URL;

import static java.util.Objects.requireNonNull;

/**
 * Configuration to create a resource definition and provisioner pair.
 */
public class ProvisionerConfiguration {

    private String name;
    private ProvisionerType provisionerType = ProvisionerType.PROVIDER;
    private String dataAddressType;
    private String policyScope;
    private URL endpoint;

    private ProvisionerConfiguration() {
    }

    public String getName() {
        return name;
    }

    public ProvisionerType getProvisionerType() {
        return provisionerType;
    }

    public String getDataAddressType() {
        return dataAddressType;
    }

    public String getPolicyScope() {
        return policyScope;
    }

    public URL getEndpoint() {
        return endpoint;
    }

    public enum ProvisionerType {
        CLIENT,
        PROVIDER
    }

    public static class Builder {

        private final ProvisionerConfiguration configuration;

        private Builder() {
            configuration = new ProvisionerConfiguration();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder name(String name) {
            configuration.name = name;
            return this;
        }

        public Builder provisionerType(ProvisionerType type) {
            configuration.provisionerType = type;
            return this;
        }

        public Builder dataAddressType(String type) {
            configuration.dataAddressType = type;
            return this;
        }

        public Builder policyScope(String policyScope) {
            configuration.policyScope = policyScope;
            return this;
        }

        public Builder endpoint(URL endpoint) {
            configuration.endpoint = endpoint;
            return this;
        }

        public ProvisionerConfiguration build() {
            requireNonNull(configuration.name, "name");
            requireNonNull(configuration.provisionerType, "type");
            requireNonNull(configuration.policyScope, "policyScope");
            return configuration;
        }

    }
}

/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.spi.provision;

import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResourceStates.CREATED;
import static org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResourceStates.DEPROVISIONED;
import static org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResourceStates.PROVISIONED;

/**
 * Resource that needs to be provisioned to support a data flow.
 */
public class ProvisionResource extends StatefulEntity<ProvisionResource> {

    private String flowId;
    private String type;
    private DataAddress dataAddress;
    private final Map<String, Object> properties = new HashMap<>();
    private ProvisionedResource provisionedResource;
    private DeprovisionedResource deprovisionedResource;

    public DataAddress getDataAddress() {
        return dataAddress;
    }

    public String getType() {
        return type;
    }

    public String getFlowId() {
        return flowId;
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public ProvisionResource copy() {
        var builder = Builder.newInstance()
                .dataAddress(dataAddress)
                .type(type)
                .flowId(flowId)
                .properties(properties);

        return copy(builder);
    }

    @Override
    public String stateAsString() {
        return ProvisionResourceStates.from(state).name();
    }

    public void transitionProvisioned(ProvisionedResource provisionedResource) {
        this.provisionedResource = provisionedResource;
        transitionTo(PROVISIONED.code());
    }

    public ProvisionedResource getProvisionedResource() {
        return provisionedResource;
    }

    public void transitionDeprovisioned(DeprovisionedResource deprovisionedResource) {
        this.deprovisionedResource = deprovisionedResource;
        transitionTo(DEPROVISIONED.code());
    }

    public boolean hasToBeProvisioned() {
        return state == CREATED.code();
    }

    public boolean hasToBeDeprovisioned() {
        return state == PROVISIONED.code();
    }

    public static class Builder extends StatefulEntity.Builder<ProvisionResource, Builder> {

        public static Builder newInstance() {
            return new Builder(new ProvisionResource());
        }

        private Builder(ProvisionResource resource) {
            super(resource);
        }

        @Override
        public Builder self() {
            return this;
        }

        public ProvisionResource build() {
            super.build();

            if (entity.id == null) {
                entity.id = UUID.randomUUID().toString();
            }
            if (entity.state == 0) {
                entity.transitionTo(CREATED.code());
            }
            return entity;
        }

        public Builder dataAddress(DataAddress dataAddress) {
            entity.dataAddress = dataAddress;
            return this;
        }

        public Builder type(String type) {
            entity.type = type;
            return this;
        }

        public Builder flowId(String flowId) {
            entity.flowId = flowId;
            return this;
        }

        public Builder property(String key, Object value) {
            entity.properties.put(key, value);
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            entity.properties.putAll(properties);
            return this;
        }
    }
}

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

import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Provisioned resource
 */
public class ProvisionedResource {

    private String id;
    private String flowId;
    private DataAddress dataAddress;
    private boolean pending = false;
    private final Map<String, Object> properties = new HashMap<>();

    public DataAddress getDataAddress() {
        return dataAddress;
    }

    public String getId() {
        return id;
    }

    public String getFlowId() {
        return flowId;
    }

    public boolean isPending() {
        return pending;
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public static class Builder {

        private final ProvisionedResource resource;

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            resource = new ProvisionedResource();
        }

        public static Builder from(ProvisionResource definition) {
            var builder = new Builder();
            builder.resource.id = definition.getId();
            builder.resource.flowId = definition.getFlowId();
            return builder;
        }

        public ProvisionedResource build() {
            if (resource.id == null) {
                resource.id = UUID.randomUUID().toString();
            }
            return resource;
        }

        public Builder dataAddress(DataAddress dataAddress) {
            resource.dataAddress = dataAddress;
            return this;
        }

        public Builder flowId(String flowId) {
            resource.flowId = flowId;
            return this;
        }

        public Builder pending(boolean pending) {
            resource.pending = pending;
            return this;
        }

        public Builder id(String id) {
            resource.id = id;
            return this;
        }

        public Builder property(String key, Object value) {
            resource.properties.put(key, value);
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            resource.properties.putAll(properties);
            return this;
        }
    }
}

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
 * Definition of a resource that needs to be provisioned to support a data flow.
 */
public class ProvisionResourceDefinition {

    private String id;
    private String flowId;
    private String type;
    private DataAddress dataAddress;
    private Map<String, Object> properties = new HashMap<>();

    public DataAddress getDataAddress() {
        return dataAddress;
    }

    public String getId() {
        return id;
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

    public static class Builder {

        private ProvisionResourceDefinition definition;

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            definition = new ProvisionResourceDefinition();
        }

        public ProvisionResourceDefinition build() {
            if (definition.id == null) {
                definition.id = UUID.randomUUID().toString();
            }
            return definition;
        }

        public Builder dataAddress(DataAddress dataAddress) {
            definition.dataAddress = dataAddress;
            return this;
        }

        public Builder type(String type) {
            definition.type = type;
            return this;
        }

        public Builder flowId(String flowId) {
            definition.flowId = flowId;
            return this;
        }

        public Builder property(String key, Object value) {
            definition.properties.put(key, value);
            return this;
        }
    }
}

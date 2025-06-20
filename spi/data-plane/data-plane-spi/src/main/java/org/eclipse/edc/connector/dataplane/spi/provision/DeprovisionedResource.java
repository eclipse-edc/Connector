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

import java.util.UUID;

/**
 * Deprovisioned resource
 */
public class DeprovisionedResource {

    private String id;
    private String flowId;
    private boolean pending = false;

    public String getId() {
        return id;
    }

    public String getFlowId() {
        return flowId;
    }

    public boolean isPending() {
        return pending;
    }

    public static class Builder {

        private DeprovisionedResource resource;

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            resource = new DeprovisionedResource();
        }

        public static Builder from(ProvisionResource definition) {
            var builder = new Builder();
            builder.resource.id = definition.getId();
            builder.resource.flowId = definition.getFlowId();
            return builder;
        }

        public DeprovisionedResource build() {
            if (resource.id == null) {
                resource.id = UUID.randomUUID().toString();
            }
            return resource;
        }

        public Builder flowId(String flowId) {
            resource.flowId = flowId;
            return this;
        }

        public Builder pending(boolean pending) {
            resource.pending = pending;
            return this;
        }
    }
}

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

import java.util.UUID;

public class ProvisionResourceDefinition {

    private String id;
    private DataAddress dataAddress;

    public DataAddress getDataAddress() {
        return dataAddress;
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
    }
}

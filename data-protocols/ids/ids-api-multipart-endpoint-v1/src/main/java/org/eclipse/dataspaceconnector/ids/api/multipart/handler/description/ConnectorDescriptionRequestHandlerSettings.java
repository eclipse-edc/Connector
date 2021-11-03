/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.handler.description;

import org.jetbrains.annotations.Nullable;

public class ConnectorDescriptionRequestHandlerSettings {
    private final String id;

    private ConnectorDescriptionRequestHandlerSettings(@Nullable String id) {
        this.id = id;
    }

    @Nullable
    public String getId() {
        return id;
    }

    static class Builder {
        private String id;

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
        }

        public Builder id(@Nullable String id) {
            this.id = id;
            return this;
        }

        public ConnectorDescriptionRequestHandlerSettings build() {
            return new ConnectorDescriptionRequestHandlerSettings(id);
        }
    }
}

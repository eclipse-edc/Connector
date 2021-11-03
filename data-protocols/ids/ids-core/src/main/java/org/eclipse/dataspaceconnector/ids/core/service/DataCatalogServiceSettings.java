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

package org.eclipse.dataspaceconnector.ids.core.service;

import org.jetbrains.annotations.Nullable;

public class DataCatalogServiceSettings {
    private final String catalogId;

    private DataCatalogServiceSettings(@Nullable String catalogId) {
        this.catalogId = catalogId;
    }

    @Nullable
    public String getCatalogId() {
        return catalogId;
    }

    static class Builder {
        private String catalogId;

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
        }

        public Builder catalogId(@Nullable String catalogId) {
            this.catalogId = catalogId;
            return this;
        }

        public DataCatalogServiceSettings build() {
            return new DataCatalogServiceSettings(catalogId);
        }
    }
}

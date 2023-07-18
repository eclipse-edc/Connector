/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.selector.api;

import org.eclipse.edc.spi.types.domain.DataAddress;

import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

/**
 * Represents the request body that the {@link DataplaneSelectorApi#find(jakarta.json.JsonObject)} endpoint requires
 * Contains source and destination address and optionally the name of a selection strategy
 */
public class SelectionRequest {
    public static final String SELECTION_REQUEST_TYPE = EDC_NAMESPACE + "SelectionRequest";
    public static final String SOURCE_ADDRESS = EDC_NAMESPACE + "source";
    public static final String DEST_ADDRESS = EDC_NAMESPACE + "destination";
    public static final String STRATEGY = EDC_NAMESPACE + "strategy";
    private DataAddress source;
    private DataAddress destination;
    private String strategy;

    private SelectionRequest() {
    }

    public DataAddress getSource() {
        return source;
    }

    public DataAddress getDestination() {
        return destination;
    }

    public String getStrategy() {
        return strategy;
    }


    public static final class Builder {
        private final SelectionRequest instance;

        private Builder() {
            instance = new SelectionRequest();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder source(DataAddress source) {
            this.instance.source = source;
            return this;
        }

        public Builder destination(DataAddress destination) {
            this.instance.destination = destination;
            return this;
        }

        public Builder strategy(String strategy) {
            this.instance.strategy = strategy;
            return this;
        }

        public SelectionRequest build() {
            return instance;
        }
    }
}

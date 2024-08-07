/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.selector.control.api.model;

import org.eclipse.edc.spi.types.domain.DataAddress;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Represents the request body that the 'select' endpoint requires
 * Contains source and destination address and optionally the name of a selection strategy
 */
public class SelectionRequest {
    public static final String SELECTION_REQUEST_TYPE = EDC_NAMESPACE + "SelectionRequest";
    public static final String SOURCE_ADDRESS = EDC_NAMESPACE + "source";
    public static final String DEST_ADDRESS = EDC_NAMESPACE + "destination";
    public static final String TRANSFER_TYPE = EDC_NAMESPACE + "transferType";
    public static final String STRATEGY = EDC_NAMESPACE + "strategy";
    private DataAddress source;
    @Deprecated(since = "0.8.1")
    private DataAddress destination;
    private String strategy = "random";
    private String transferType;

    private SelectionRequest() {
    }

    public DataAddress getSource() {
        return source;
    }

    @Deprecated(since = "0.8.1")
    public DataAddress getDestination() {
        return destination;
    }

    public String getStrategy() {
        return strategy;
    }

    public String getTransferType() {
        return transferType;
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

        public Builder transferType(String transferType) {
            this.instance.transferType = transferType;
            return this;
        }

        public SelectionRequest build() {
            return instance;
        }
    }
}

/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.transfer;

import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;

/**
 *
 */
public class TransferInitiateResponse {
    private String id;
    private String error;
    private ResponseStatus status = ResponseStatus.OK;

    private TransferInitiateResponse() {
    }

    /**
     * The unique process id, which can be used for correlation.
     */
    public String getId() {
        return id;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public static class Builder {
        private final TransferInitiateResponse response;

        private Builder() {
            response = new TransferInitiateResponse();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            response.id = id;
            return this;
        }

        public Builder status(ResponseStatus status) {
            response.status = status;
            return this;
        }

        public Builder error(String error) {
            response.error = error;
            return this;
        }

        public TransferInitiateResponse build() {
            return response;
        }
    }


}

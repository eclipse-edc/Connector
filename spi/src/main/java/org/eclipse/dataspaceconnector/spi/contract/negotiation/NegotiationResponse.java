/*
 *  Copyright (c) 2021 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.spi.contract.negotiation;

import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;

/**
 * A response to a contract negotiation operation.
 */
public class NegotiationResponse {
    private final Status status;
    private ContractNegotiation contractNegotiation;

    public enum Status {
        /**
         * The operation completed successfully.
         */
        OK,

        /**
         * The operation errored and should be retried.
         */
        ERROR_RETRY,

        /**
         * The operation errored and should not be retried.
         */
        FATAL_ERROR,

        /**
         * The client that requested this operation submitted an invalid negotiation state check.
         */
        INVALID_STATE
    }

    public NegotiationResponse(Status status, ContractNegotiation contractNegotiation) {
        this.status = status;
        this.contractNegotiation = contractNegotiation;
    }

    public NegotiationResponse(Status status) {
        this.status = status;
    }

    /**
     * Returns the operation status.
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Returns the contract negotiation or null if the status is {@link Status#ERROR_RETRY}, {@link Status#FATAL_ERROR}, or {@link Status#INVALID_STATE}.
     */
    public ContractNegotiation getContractNegotiation() {
        return contractNegotiation;
    }
}

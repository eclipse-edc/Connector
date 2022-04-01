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

package org.eclipse.dataspaceconnector.spi.contract.negotiation.response;

import org.eclipse.dataspaceconnector.spi.result.AbstractResult;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;

/**
 * A response to a contract negotiation operation.
 */
public class NegotiationResult extends AbstractResult<ContractNegotiation, StatusFailure> {

    public static NegotiationResult success(ContractNegotiation negotiation) {
        return new NegotiationResult(negotiation, null);
    }

    public static NegotiationResult failure(Status status) {
        return new NegotiationResult(null, new StatusFailure(status));
    }

    public NegotiationResult(ContractNegotiation contractNegotiation, StatusFailure failure) {
        super(contractNegotiation, failure);
    }

    public Status getStatus() {
        return getFailure().getStatus();
    }

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
         * The consumer that requested this operation submitted an invalid negotiation state check.
         */
        INVALID_STATE
    }

}

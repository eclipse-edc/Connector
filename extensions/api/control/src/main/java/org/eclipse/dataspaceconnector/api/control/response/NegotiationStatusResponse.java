/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.api.control.response;

import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;

/**
 * Response for requesting the status of a {@link ContractNegotiation}.
 */
public class NegotiationStatusResponse {
    
    /**
     * Status of the {@link ContractNegotiation}.
     */
    private ContractNegotiationStates status;
    
    /**
     * Id of the ContractAgreement associated with the ContractNegotiation. Null, if the
     * negotiation has not yet been completed successfully.
     */
    private String contractAgreementId;
    
    public ContractNegotiationStates getStatus() {
        return status;
    }
    
    public String getContractAgreementId() {
        return contractAgreementId;
    }
    
    /**
     * Constructs a NegotiationStatusResponse for a given ContractNegotiation.
     *
     * @param negotiation the ContractNegotiation.
     */
    public NegotiationStatusResponse(ContractNegotiation negotiation) {
        this.status = ContractNegotiationStates.from(negotiation.getState());
        if (negotiation.getContractAgreement() != null) {
            this.contractAgreementId = negotiation.getContractAgreement().getId();
        }
    }
    
}

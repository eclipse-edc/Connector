/*
 * Copyright (c) 2022 cluetec GmbH
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *   cluetec GmbH - Initial API and Implementation
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation;

import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation.Type;

class ContractNegotiationDto {
    private String id;
    private String counterPartyAddress;
    private String protocol;
    private final Type type = Type.CONSUMER;
    private String state;
    private String errorDetail;
    private String contractAgreementId; // is null until state == CONFIRMED

    ContractNegotiationDto(ContractNegotiation contractNegotiation) {
        setId(contractNegotiation.getId());
        setCounterPartyAddress(contractNegotiation.getCounterPartyAddress());
        setProtocol(contractNegotiation.getProtocol());
        setState(String.valueOf(contractNegotiation.getState()));
        setErrorDetail(contractNegotiation.getErrorDetail());
        setContractAgreementId(contractNegotiation.getContractAgreement() != null ?
                contractNegotiation.getContractAgreement().getId() : null);
    }

    // TODO remove after controller logic was implemented
    ContractNegotiationDto() {
        setId("contract-negotiation-1");
        setCounterPartyAddress("counter-party-address");
        setProtocol("protocol");
        setState("1");
        setErrorDetail(null);
        setContractAgreementId(null);
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    public void setCounterPartyAddress(String counterPartyAddress) {
        this.counterPartyAddress = counterPartyAddress;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Type getType() {
        return type;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
    }

    public String getContractAgreementId() {
        return contractAgreementId;
    }

    public void setContractAgreementId(String contractAgreementId) {
        this.contractAgreementId = contractAgreementId;
    }
}

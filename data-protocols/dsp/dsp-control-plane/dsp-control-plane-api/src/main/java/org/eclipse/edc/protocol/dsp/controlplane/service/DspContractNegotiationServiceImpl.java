/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
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

package org.eclipse.edc.protocol.dsp.controlplane.service;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.negotiation.ProviderContractNegotiationManager;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.protocol.dsp.spi.controlplane.service.DspContractNegotiationService;

public class DspContractNegotiationServiceImpl implements DspContractNegotiationService {

    private final ConsumerContractNegotiationManager consumerManager;

    private final ProviderContractNegotiationManager providerManager;

    private final ContractNegotiationService contractNegotiationService;

    public DspContractNegotiationServiceImpl(ConsumerContractNegotiationManager consumerManager,
                                             ProviderContractNegotiationManager providerManager,
                                             ContractNegotiationService contractNegotiationService) {
        this.consumerManager = consumerManager;
        this.providerManager = providerManager;
        this.contractNegotiationService = contractNegotiationService;
    }

    @Override
    public JsonObject getNegotiationById(String id) {
        var negotiation = contractNegotiationService.findbyId(id);


        return null;
    }

    @Override
    public JsonObject createNegotiation(JsonObject negotiation) {
        return null;
    }

    @Override
    public JsonObject consumerOffer(String id, JsonObject offer) {
        return null;
    }

    @Override
    public JsonObject acceptCurrentOffer(String id) {
        return null;
    }

    @Override
    public JsonObject verifyAgreement(String id) {
        return null;
    }

    @Override
    public JsonObject terminateNegotiation(String id) {
        return null;
    }

    @Override
    public void providerOffer(String id, JsonObject body) {

    }

    @Override
    public void createAgreement(String id, JsonObject body) {

    }

    @Override
    public void finalizeAgreement(String id, JsonObject body) {

    }
}

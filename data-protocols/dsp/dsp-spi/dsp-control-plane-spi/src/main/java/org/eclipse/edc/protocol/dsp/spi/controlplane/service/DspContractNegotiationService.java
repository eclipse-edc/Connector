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

package org.eclipse.edc.protocol.dsp.spi.controlplane.service;

import jakarta.json.JsonObject;

/**
 * NOTE: Result for propagating errors to the controller.
 */
public interface DspContractNegotiationService {

    JsonObject getNegotiationById(String id);

    JsonObject createNegotiation(JsonObject message);

    void consumerOffer(String id, JsonObject message);

    void processEvent(String id, JsonObject message);

    void verifyAgreement(String id, JsonObject message);

    void terminateNegotiation(String id, JsonObject message);

    void providerOffer(String id, JsonObject message);

    void createAgreement(String id, JsonObject message);

    void acceptCurrentOffer(String id);

    void finalizeAgreement(String id);
}

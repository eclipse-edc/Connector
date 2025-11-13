/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.spi;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.spi.result.Result;

import java.util.List;

/**
 * Service responsible for requesting Verifiable Presentations according to the DCP presentation
 * flow.
 */
public interface PresentationRequestService {

    /**
     * Requests the Verifiable Presentation after receiving the SI token from the counter-party.
     * This includes creating the own SI token, resolving the credential service URL and making the
     * presentation request.
     *
     * @param participantContextId id of the participant context
     * @param ownDid the participant's DID
     * @param counterPartyDid the counter-party's DID
     * @param counterPartyToken the counter-party's SI token
     * @param scopes the scopes to request
     * @return list of Verifiable Presentations
     */
    Result<List<VerifiablePresentationContainer>> requestPresentation(String participantContextId, String ownDid,
                                                                      String counterPartyDid, String counterPartyToken,
                                                                      List<String> scopes);

}

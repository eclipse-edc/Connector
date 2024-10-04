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
 *       Fraunhofer Institute for Software and Systems Engineering - add policy scope
 *
 */

package org.eclipse.edc.connector.controlplane.contract.spi.validation;

import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

/**
 * Validates {@link ContractOffer}s and {@link ContractAgreement}s.
 */
@ExtensionPoint
public interface ContractValidationService {

    /**
     * Validates the contract offer for the consumer represented by the given claims.
     *
     * @param agent         The {@link ParticipantAgent} of the consumer
     * @param consumerOffer The initial {@link ValidatableConsumerOffer} id to validate
     * @return The referenced {@link ValidatedConsumerOffer}.
     */
    @NotNull
    Result<ValidatedConsumerOffer> validateInitialOffer(ParticipantAgent agent, ValidatableConsumerOffer consumerOffer);

    /**
     * Validates the contract agreement that the consumer referenced in its transfer request.
     * The {@code ParticipantAgent} must represent the counter-party that is referenced in the contract agreement.
     *
     * @param agent     The {@link ParticipantAgent} of the consumer
     * @param agreement The {@link ContractAgreement} between consumer and provider to validate
     * @return The result of the validation
     */
    @NotNull
    Result<ContractAgreement> validateAgreement(ParticipantAgent agent, ContractAgreement agreement);

    /**
     * Validates the request for a contract agreement. Verifies that the requesting party is involved
     * in the contract agreement, but does not perform policy evaluation.
     *
     * @param agent     The {@link ParticipantAgent} of the counter-party
     * @param agreement The agreement
     * @return The result of the validation
     */
    @NotNull
    Result<Void> validateRequest(ParticipantAgent agent, ContractAgreement agreement);

    /**
     * Validates the request for a contract negotiation.
     *
     * @param agent       The {@link ParticipantAgent} of the consumer
     * @param negotiation The negotiation
     * @return The result of the validation
     */
    @NotNull
    Result<Void> validateRequest(ParticipantAgent agent, ContractNegotiation negotiation);

    /**
     * When the negotiation has been confirmed by the provider, the consumer must validate it ensuring that it is the same that was sent in the last offer.
     *
     * @param agent       The {@link ParticipantAgent} the provider agent
     * @param agreement   The {@link ContractAgreement} between consumer and provider
     * @param latestOffer The last {@link ContractOffer}
     */
    @NotNull
    Result<Void> validateConfirmed(ParticipantAgent agent, ContractAgreement agreement, ContractOffer latestOffer);

}

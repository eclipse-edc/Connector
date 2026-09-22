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

package org.eclipse.edc.connector.controlplane.contract.spi.offer;

import org.eclipse.edc.connector.controlplane.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.controlplane.contract.spi.validation.ValidatableConsumerOffer;
import org.eclipse.edc.spi.result.ServiceResult;
import org.jetbrains.annotations.NotNull;

/**
 * Resolve the consumer offer into a {@link ValidatableConsumerOffer} which can be used
 * to validate incoming offer through {@link ContractValidationService#validateInitialOffer}
 */
public interface ConsumerOfferResolver {

    /**
     * Resolves the offer with the given id within the given participant context.
     *
     * @param participantContextId the id of the participant context (the provider) that owns the offered contract definition.
     * @param offerId              the offer id.
     * @return the resolved offer, a not-found failure if the contract definition or its policies do not exist in the participant context.
     */
    @NotNull
    ServiceResult<ValidatableConsumerOffer> resolveOffer(String participantContextId, String offerId);

}

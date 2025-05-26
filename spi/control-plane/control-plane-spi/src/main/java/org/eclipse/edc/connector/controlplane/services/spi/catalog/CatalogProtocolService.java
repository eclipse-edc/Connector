/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.services.spi.catalog;

import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.ServiceResult;
import org.jetbrains.annotations.NotNull;

/**
 * Mediates access to and modification of {@link Catalog}es on protocol messages reception.
 */
public interface CatalogProtocolService {

    /**
     * Returns a catalog given a {@link CatalogRequestMessage} and a {@link ClaimToken}
     *
     * @param message          the request message.
     * @param participantAgent the participant agent.
     * @param context          the context.
     * @return succeeded result with the {@link Catalog}, failed result otherwise.
     */
    @NotNull
    ServiceResult<Catalog> getCatalog(CatalogRequestMessage message, ParticipantAgent participantAgent, RequestCatalogContext context);

    /**
     * Returns a dataset given its id and a {@link ClaimToken}
     *
     * @param datasetId           the dataset id.
     * @param tokenRepresentation the claim token.
     * @return succeeded result with the {@link Dataset}, failed result otherwise.
     */
    @NotNull
    ServiceResult<Dataset> getDataset(String datasetId, TokenRepresentation tokenRepresentation, String protocol);

    // TODO: document
    ServiceResult<RequestCatalogContext> provideRequestContext(CatalogRequestMessage message);
}

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

package org.eclipse.edc.connector.controlplane.services.catalog;

import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataServiceRegistry;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.catalog.spi.DatasetResolver;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.RequestCatalogContext;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolTokenValidator;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.policy.context.request.spi.RequestCatalogPolicyContext;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;

import static java.lang.String.format;

public class CatalogProtocolServiceImpl implements CatalogProtocolService {

    private final DatasetResolver datasetResolver;
    private final DataServiceRegistry dataServiceRegistry;
    private final String participantId;
    private final TransactionContext transactionContext;

    private final ProtocolTokenValidator protocolTokenValidator;

    public CatalogProtocolServiceImpl(DatasetResolver datasetResolver,
                                      DataServiceRegistry dataServiceRegistry,
                                      ProtocolTokenValidator protocolTokenValidator,
                                      String participantId,
                                      TransactionContext transactionContext) {
        this.datasetResolver = datasetResolver;
        this.dataServiceRegistry = dataServiceRegistry;
        this.protocolTokenValidator = protocolTokenValidator;
        this.participantId = participantId;
        this.transactionContext = transactionContext;
    }

    @Override
    @NotNull
    public ServiceResult<Catalog> getCatalog(CatalogRequestMessage message, ParticipantAgent participantAgent, RequestCatalogContext context) {
        return transactionContext.execute(() -> {
            try (var datasets = datasetResolver.query(participantAgent, message.getQuerySpec(), message.getProtocol())) {
                var dataServices = dataServiceRegistry.getDataServices(message.getProtocol());

                var catalog = Catalog.Builder.newInstance()
                        .dataServices(dataServices)
                        .datasets(datasets.toList())
                        .participantId(participantId)
                        .build();
                return ServiceResult.success(catalog);
            }
        });
    }

    @Override
    public @NotNull ServiceResult<Dataset> getDataset(String datasetId, TokenRepresentation tokenRepresentation, String protocol) {
        return transactionContext.execute(() -> protocolTokenValidator.verify(tokenRepresentation, RequestCatalogPolicyContext::new)
                .map(agent -> datasetResolver.getById(agent, datasetId, protocol))
                .compose(dataset -> {
                    if (dataset == null) {
                        return ServiceResult.notFound(format("Dataset %s does not exist", datasetId));
                    }

                    return ServiceResult.success(dataset);
                }));
    }

    @Override
    @NotNull
    public ServiceResult<RequestCatalogContext> provideRequestContext(CatalogRequestMessage message) {
        return ServiceResult.success(new RequestCatalogContext());
    }

}


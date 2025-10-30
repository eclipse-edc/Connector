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
 *       Cofinity-X - add participantId to DataspaceProfileContext
 *       Cofinity-X - make participant id extraction dependent on dataspace profile context
 *
 */

package org.eclipse.edc.connector.controlplane.services.catalog;

import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataServiceRegistry;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.catalog.spi.DatasetRequestMessage;
import org.eclipse.edc.connector.controlplane.catalog.spi.DatasetResolver;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolTokenValidator;
import org.eclipse.edc.participantcontext.spi.identity.ParticipantIdentityResolver;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.context.request.spi.RequestCatalogPolicyContext;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;

import static java.lang.String.format;

public class CatalogProtocolServiceImpl implements CatalogProtocolService {

    private final DatasetResolver datasetResolver;
    private final DataServiceRegistry dataServiceRegistry;
    private final ParticipantIdentityResolver identityResolver;
    private final TransactionContext transactionContext;

    private final ProtocolTokenValidator protocolTokenValidator;

    public CatalogProtocolServiceImpl(DatasetResolver datasetResolver,
                                      DataServiceRegistry dataServiceRegistry,
                                      ProtocolTokenValidator protocolTokenValidator,
                                      ParticipantIdentityResolver identityResolver,
                                      TransactionContext transactionContext) {
        this.datasetResolver = datasetResolver;
        this.dataServiceRegistry = dataServiceRegistry;
        this.protocolTokenValidator = protocolTokenValidator;
        this.identityResolver = identityResolver;
        this.transactionContext = transactionContext;
    }

    @Override
    @NotNull
    public ServiceResult<Catalog> getCatalog(ParticipantContext participantContext, CatalogRequestMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> protocolTokenValidator.verify(participantContext, tokenRepresentation, RequestCatalogPolicyContext::new, message)
                .map(agent -> {
                    try (var datasets = datasetResolver.query(participantContext, agent, message.getQuerySpec(), message.getProtocol())) {
                        // TODO data services should be based on the participant context
                        var dataServices = dataServiceRegistry.getDataServices(message.getProtocol());

                        return Catalog.Builder.newInstance()
                                .dataServices(dataServices)
                                .datasets(datasets.toList())
                                .participantId(identityResolver.getParticipantId(participantContext.getParticipantContextId(), message.getProtocol()))
                                .build();
                    }
                })
        );
    }

    @Override
    public @NotNull ServiceResult<Dataset> getDataset(ParticipantContext participantContext, String datasetId, TokenRepresentation tokenRepresentation, String protocol) {
        var message = DatasetRequestMessage.Builder.newInstance()
                .protocol(protocol)
                .datasetId(datasetId)
                .build();
        return transactionContext.execute(() -> protocolTokenValidator.verify(participantContext, tokenRepresentation, RequestCatalogPolicyContext::new, message)
                .map(agent -> datasetResolver.getById(participantContext, agent, datasetId, protocol))
                .compose(dataset -> {
                    if (dataset == null) {
                        return ServiceResult.notFound(format("Dataset %s does not exist", datasetId));
                    }

                    return ServiceResult.success(dataset);
                }));
    }

}


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

package org.eclipse.edc.connector.service.catalog;

import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.catalog.spi.DataServiceRegistry;
import org.eclipse.edc.catalog.spi.Dataset;
import org.eclipse.edc.catalog.spi.DatasetResolver;
import org.eclipse.edc.connector.service.protocol.BaseProtocolService;
import org.eclipse.edc.connector.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.spi.agent.ParticipantAgentService;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;

import static java.lang.String.format;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

public class CatalogProtocolServiceImpl extends BaseProtocolService implements CatalogProtocolService {

    private static final String PARTICIPANT_ID_PROPERTY_KEY = "participantId";

    private final DatasetResolver datasetResolver;
    private final ParticipantAgentService participantAgentService;
    private final DataServiceRegistry dataServiceRegistry;
    private final String participantId;
    private final TransactionContext transactionContext;

    private PolicyEngine policyEngine;

    public CatalogProtocolServiceImpl(DatasetResolver datasetResolver,
                                      ParticipantAgentService participantAgentService,
                                      DataServiceRegistry dataServiceRegistry,
                                      IdentityService identityService,
                                      PolicyEngine policyEngine,
                                      Monitor monitor,
                                      String participantId,
                                      TransactionContext transactionContext) {
        super(identityService, policyEngine, monitor);
        this.datasetResolver = datasetResolver;
        this.participantAgentService = participantAgentService;
        this.dataServiceRegistry = dataServiceRegistry;
        this.participantId = participantId;
        this.transactionContext = transactionContext;
    }

    @Override
    @NotNull
    public ServiceResult<Catalog> getCatalog(CatalogRequestMessage message, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> verifyToken(tokenRepresentation)
                .map(participantAgentService::createFor)
                .map(agent -> {
                    try (var datasets = datasetResolver.query(agent, message.getQuerySpec())) {
                        var dataServices = dataServiceRegistry.getDataServices();

                        return Catalog.Builder.newInstance()
                                .dataServices(dataServices)
                                .datasets(datasets.toList())
                                .property(EDC_NAMESPACE + PARTICIPANT_ID_PROPERTY_KEY, participantId)
                                .build();
                    }
                })
        );
    }

    @Override
    public @NotNull ServiceResult<Dataset> getDataset(String datasetId, TokenRepresentation tokenRepresentation) {
        return transactionContext.execute(() -> verifyToken(tokenRepresentation)
                .map(participantAgentService::createFor)
                .map(agent -> datasetResolver.getById(agent, datasetId))
                .compose(dataset -> {
                    if (dataset == null) {
                        return ServiceResult.notFound(format("Dataset %s does not exist", datasetId));
                    }

                    return ServiceResult.success(dataset);
                }));
    }
}

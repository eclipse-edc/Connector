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
import org.eclipse.edc.connector.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.agent.ParticipantAgentService;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.jetbrains.annotations.NotNull;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

public class CatalogProtocolServiceImpl implements CatalogProtocolService {

    private static final String PARTICIPANT_ID_PROPERTY_KEY = "participantId";

    private final DatasetResolver datasetResolver;
    private final ParticipantAgentService participantAgentService;
    private final DataServiceRegistry dataServiceRegistry;
    private final IdentityService identityService;

    public CatalogProtocolServiceImpl(DatasetResolver datasetResolver,
                                      ParticipantAgentService participantAgentService,
                                      DataServiceRegistry dataServiceRegistry, IdentityService identityService) {
        this.datasetResolver = datasetResolver;
        this.participantAgentService = participantAgentService;
        this.dataServiceRegistry = dataServiceRegistry;
        this.identityService = identityService;
    }

    @Override
    @NotNull
    public ServiceResult<Catalog> getCatalog(CatalogRequestMessage message, ClaimToken claimToken) {
        var agent = participantAgentService.createFor(claimToken);

        try (var datasets = datasetResolver.query(agent, message.getQuerySpec())) {
            var dataServices = dataServiceRegistry.getDataServices();

            var catalog = Catalog.Builder.newInstance()
                    .dataServices(dataServices)
                    .datasets(datasets.collect(toList()))
                    .property(EDC_NAMESPACE + PARTICIPANT_ID_PROPERTY_KEY, message.getOwnPartyId()) // maybe use audience field of claimtoken instead
                    .build();

            return ServiceResult.success(catalog);
        }
    }

    @Override
    public @NotNull ServiceResult<Dataset> getDataset(String datasetId, ClaimToken claimToken) {
        var agent = participantAgentService.createFor(claimToken);

        var dataset = datasetResolver.getById(agent, datasetId);

        if (dataset == null) {
            return ServiceResult.notFound(format("Dataset %s does not exist", datasetId));
        }

        return ServiceResult.success(dataset);
    }
}

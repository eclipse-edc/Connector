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
import org.eclipse.edc.catalog.spi.DataService;
import org.eclipse.edc.catalog.spi.protocol.CatalogRequestMessage;
import org.eclipse.edc.connector.spi.catalog.CatalogProtocolService;
import org.eclipse.edc.connector.spi.catalog.DatasetResolver;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.agent.ParticipantAgentService;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;

public class CatalogProtocolServiceImpl implements CatalogProtocolService {

    private final DatasetResolver datasetResolver;
    private final ParticipantAgentService participantAgentService;

    public CatalogProtocolServiceImpl(DatasetResolver datasetResolver, ParticipantAgentService participantAgentService) {
        this.datasetResolver = datasetResolver;
        this.participantAgentService = participantAgentService;
    }

    @Override
    @NotNull
    public ServiceResult<Catalog> getCatalog(CatalogRequestMessage message, ClaimToken token) {
        var dataService = DataService.Builder.newInstance()
                .id(randomUUID().toString())
                .terms("dspace:connector")
                .endpointUrl("https://localhost:8282") // TODO get connector address. Should it be dswWebhookAddress?
                .build();

        var agent = participantAgentService.createFor(token);

        try (var datasets = datasetResolver.query(agent, message.getFilter(), dataService)) {
            var catalog = Catalog.Builder.newInstance()
                    .dataServices(List.of(dataService))
                    .datasets(datasets.collect(toList()))
                    .build();

            return ServiceResult.success(catalog);
        }
    }
}

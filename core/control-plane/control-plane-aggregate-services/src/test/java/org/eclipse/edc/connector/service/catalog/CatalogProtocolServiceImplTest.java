/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

import org.eclipse.edc.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.catalog.spi.DataService;
import org.eclipse.edc.catalog.spi.DataServiceRegistry;
import org.eclipse.edc.catalog.spi.Dataset;
import org.eclipse.edc.catalog.spi.DatasetResolver;
import org.eclipse.edc.catalog.spi.Distribution;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.agent.ParticipantAgentService;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.UNAUTHORIZED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogProtocolServiceImplTest {

    private final DatasetResolver datasetResolver = mock();
    private final ParticipantAgentService participantAgentService = mock();
    private final DataServiceRegistry dataServiceRegistry = mock();
    private final IdentityService identityService = mock();
    private final TransactionContext transactionContext = spy(new NoopTransactionContext());

    private final CatalogProtocolServiceImpl service = new CatalogProtocolServiceImpl(datasetResolver,
            participantAgentService, dataServiceRegistry, identityService, mock(), "participantId",
            transactionContext);

    @Test
    void getCatalog_shouldReturnCatalogWithConnectorDataServiceAndItsDataset() {
        var querySpec = QuerySpec.none();
        var message = CatalogRequestMessage.Builder.newInstance().protocol("protocol").querySpec(querySpec).build();
        var token = create();
        var tokenRepresentation = createTokenRepresentation();
        var participantAgent = createParticipantAgent();
        var dataService = DataService.Builder.newInstance().build();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), any())).thenReturn(Result.success(token));
        when(dataServiceRegistry.getDataServices()).thenReturn(List.of(dataService));
        when(datasetResolver.query(any(), any())).thenReturn(Stream.of(createDataset()));
        when(participantAgentService.createFor(any())).thenReturn(participantAgent);

        var result = service.getCatalog(message, tokenRepresentation);

        assertThat(result).isSucceeded().satisfies(catalog -> {
            assertThat(catalog.getDataServices()).hasSize(1).first().isSameAs(dataService);
            assertThat(catalog.getDatasets()).hasSize(1);
        });
        verify(datasetResolver).query(eq(participantAgent), eq(querySpec));
        verify(participantAgentService).createFor(token);
        verify(transactionContext).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void getCatalog_shouldFail_whenTokenValidationFails() {
        var querySpec = QuerySpec.none();
        var message = CatalogRequestMessage.Builder.newInstance().protocol("protocol").querySpec(querySpec).build();
        var tokenRepresentation = createTokenRepresentation();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), any())).thenReturn(Result.failure("unauthorized"));

        var result = service.getCatalog(message, tokenRepresentation);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(UNAUTHORIZED);
    }

    @Test
    void getDataset_shouldReturnDataset() {
        var claimToken = create();
        var tokenRepresentation = createTokenRepresentation();

        var participantAgent = createParticipantAgent();
        var dataset = createDataset();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), any())).thenReturn(Result.success(claimToken));
        when(participantAgentService.createFor(any())).thenReturn(participantAgent);
        when(datasetResolver.getById(any(), any())).thenReturn(dataset);

        var result = service.getDataset("datasetId", tokenRepresentation);

        assertThat(result).isSucceeded().isEqualTo(dataset);
        verify(participantAgentService).createFor(claimToken);
        verify(datasetResolver).getById(participantAgent, "datasetId");
        verify(transactionContext).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void getDataset_shouldFail_whenDatasetIsNull() {
        var claimToken = create();
        var tokenRepresentation = createTokenRepresentation();
        var participantAgent = createParticipantAgent();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), any())).thenReturn(Result.success(claimToken));
        when(participantAgentService.createFor(any())).thenReturn(participantAgent);
        when(datasetResolver.getById(any(), any())).thenReturn(null);

        var result = service.getDataset("datasetId", tokenRepresentation);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);
    }

    @Test
    void getDataset_shouldFail_whenTokenValidationFails() {
        var querySpec = QuerySpec.none();
        var tokenRepresentation = createTokenRepresentation();

        when(identityService.verifyJwtToken(eq(tokenRepresentation), any())).thenReturn(Result.failure("unauthorized"));

        var result = service.getDataset("datasetId", tokenRepresentation);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(UNAUTHORIZED);
    }

    private ParticipantAgent createParticipantAgent() {
        return new ParticipantAgent(emptyMap(), emptyMap());
    }

    private Dataset createDataset() {
        var dataService = DataService.Builder.newInstance().build();
        var distribution = Distribution.Builder.newInstance().dataService(dataService).format("any").build();
        return Dataset.Builder.newInstance()
                .offer(UUID.randomUUID().toString(), Policy.Builder.newInstance().build())
                .distribution(distribution)
                .build();
    }

    private ClaimToken create() {
        return ClaimToken.Builder.newInstance().build();
    }

    private TokenRepresentation createTokenRepresentation() {
        return TokenRepresentation.Builder.newInstance().build();
    }
}

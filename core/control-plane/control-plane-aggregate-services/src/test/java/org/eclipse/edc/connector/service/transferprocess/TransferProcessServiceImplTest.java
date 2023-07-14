/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - initiate provider process
 *
 */

package org.eclipse.edc.connector.service.transferprocess;

import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.transfer.spi.types.TransferRequest;
import org.eclipse.edc.connector.transfer.spi.types.command.DeprovisionRequest;
import org.eclipse.edc.connector.transfer.spi.types.command.SingleTransferProcessCommand;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.dataaddress.DataAddressValidator;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.service.spi.result.ServiceFailure.Reason.BAD_REQUEST;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransferProcessServiceImplTest {

    private final String id = UUID.randomUUID().toString();
    private final TransferProcess process1 = transferProcess();
    private final TransferProcess process2 = transferProcess();
    private final QuerySpec query = QuerySpec.Builder.newInstance().limit(5).offset(2).build();
    private final ArgumentCaptor<SingleTransferProcessCommand> commandCaptor = ArgumentCaptor.forClass(SingleTransferProcessCommand.class);

    private final TransferProcessStore store = mock(TransferProcessStore.class);
    private final TransferProcessManager manager = mock(TransferProcessManager.class);
    private final TransactionContext transactionContext = spy(new NoopTransactionContext());
    private final DataAddressValidator dataAddressValidator = mock(DataAddressValidator.class);

    private final TransferProcessService service = new TransferProcessServiceImpl(store, manager, transactionContext,
            dataAddressValidator);

    @Test
    void findById_whenFound() {
        when(store.findById(id)).thenReturn(process1);
        assertThat(service.findById(id)).isSameAs(process1);
        verify(transactionContext).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void findById_whenNotFound() {
        assertThat(service.findById(id)).isNull();
        verify(transactionContext).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void query() {
        when(store.findAll(query)).thenReturn(Stream.of(process1, process2));
        assertThat(service.query(query).getContent()).containsExactly(process1, process2);
        verify(transactionContext).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidFilters.class)
    void query_invalidFilter_raiseException(Criterion invalidFilter) {
        var spec = QuerySpec.Builder.newInstance().filter(invalidFilter).build();
        assertThat(service.query(spec).failed()).isTrue();
    }

    @ParameterizedTest
    @ArgumentsSource(ValidFilters.class)
    void query_validFilter(Criterion validFilter) {
        var spec = QuerySpec.Builder.newInstance().filter(validFilter).build();
        service.query(spec);
        verify(store).findAll(spec);
        verify(transactionContext).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void getState_whenFound() {
        when(store.findById(id)).thenReturn(process1);
        assertThat(service.getState(id)).isEqualTo(TransferProcessStates.from(process1.getState()).name());
        verify(transactionContext).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void getState_whenNotFound() {
        assertThat(service.getState(id)).isNull();
        verify(transactionContext).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void initiateTransfer() {
        var transferRequest = transferRequest();
        var transferProcess = transferProcess();
        when(dataAddressValidator.validate(any())).thenReturn(Result.success());
        when(manager.initiateConsumerRequest(transferRequest)).thenReturn(StatusResult.success(transferProcess));

        var result = service.initiateTransfer(transferRequest);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isEqualTo(transferProcess);
        verify(transactionContext).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void initiateTransfer_consumer_invalidDestination_shouldNotInitiateTransfer() {
        when(dataAddressValidator.validate(any())).thenReturn(Result.failure("invalid data address"));

        var result = service.initiateTransfer(transferRequest());

        assertThat(result).satisfies(ServiceResult::failed)
                .extracting(ServiceResult::reason)
                .isEqualTo(BAD_REQUEST);
        verifyNoInteractions(manager);
    }

    @ParameterizedTest
    @EnumSource(value = TransferProcessStates.class, mode = INCLUDE, names = { "COMPLETED", "DEPROVISIONING", "TERMINATED" })
    void deprovision(TransferProcessStates state) {
        var process = transferProcess(state, id);
        when(store.findById(id)).thenReturn(process);

        var result = service.deprovision(id);

        assertThat(result.succeeded()).isTrue();
        verify(manager).enqueueCommand(commandCaptor.capture());
        assertThat(commandCaptor.getValue()).isInstanceOf(DeprovisionRequest.class);
        assertThat(commandCaptor.getValue().getTransferProcessId())
                .isEqualTo(id);
        verify(transactionContext).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @ParameterizedTest
    @EnumSource(value = TransferProcessStates.class, mode = EXCLUDE, names = { "COMPLETED", "DEPROVISIONING", "DEPROVISIONED", "DEPROVISIONING_REQUESTED", "TERMINATED" })
    void deprovision_whenNonDeprovisionable(TransferProcessStates state) {
        var process = transferProcess(state, id);
        when(store.findById(id)).thenReturn(process);

        var result = service.deprovision(id);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly("Cannot DeprovisionRequest because TransferProcess " + process.getId() + " is in state " + state);
        verifyNoInteractions(manager);
        verify(transactionContext).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    @Test
    void deprovision_whenNotFound() {
        var result = service.deprovision(id);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).containsExactly("TransferProcess with id " + id + " not found");
        verify(transactionContext).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    private static class InvalidFilters implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    arguments(criterion("provisionedResourceSet.resources.hastoken", "=", "true")), // wrong case
                    arguments(criterion("resourceManifest.definitions.notexist", "=", "foobar")), // property not exist
                    arguments(criterion("contentDataAddress.properties.someKey", "=", "someval")) // map types not supported
            );
        }
    }

    private static class ValidFilters implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    arguments(criterion("deprovisionedResources.provisionedResourceId", "=", "someval")),
                    arguments(criterion("type", "=", "CONSUMER")),
                    arguments(criterion("provisionedResourceSet.resources.hasToken", "=", "true"))
            );
        }
    }

    private TransferProcess transferProcess() {
        var state = TransferProcessStates.values()[ThreadLocalRandom.current().nextInt(TransferProcessStates.values().length)];
        return transferProcess(state, UUID.randomUUID().toString());
    }

    private TransferProcess transferProcess(TransferProcessStates state, String id) {
        return TransferProcess.Builder.newInstance()
                .state(state.code())
                .id(id)
                .build();
    }

    private TransferRequest transferRequest() {
        return TransferRequest.Builder.newInstance()
                .dataDestination(DataAddress.Builder.newInstance().type("type").build())
                .build();
    }

}

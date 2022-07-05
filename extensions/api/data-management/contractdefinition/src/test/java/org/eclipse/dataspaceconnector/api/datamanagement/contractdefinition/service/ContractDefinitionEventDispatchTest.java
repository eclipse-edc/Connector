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

package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.service;

import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.event.EventRouter;
import org.eclipse.dataspaceconnector.spi.event.EventSubscriber;
import org.eclipse.dataspaceconnector.spi.event.contractdefinition.ContractDefinitionCreated;
import org.eclipse.dataspaceconnector.spi.event.contractdefinition.ContractDefinitionDeleted;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(EdcExtension.class)
public class ContractDefinitionEventDispatchTest {

    private final EventSubscriber eventSubscriber = mock(EventSubscriber.class);

    @Test
    void shouldDispatchEventOnContractDefinitionCreationAndDeletion(ContractDefinitionService service, EventRouter eventRouter) {
        eventRouter.register(eventSubscriber);
        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contractPolicyId(UUID.randomUUID().toString())
                .accessPolicyId(UUID.randomUUID().toString())
                .selectorExpression(AssetSelectorExpression.SELECT_ALL)
                .build();

        service.create(contractDefinition);

        await().untilAsserted(() -> verify(eventSubscriber).on(isA(ContractDefinitionCreated.class)));

        service.delete(contractDefinition.getId());

        await().untilAsserted(() -> verify(eventSubscriber).on(isA(ContractDefinitionDeleted.class)));
    }

}

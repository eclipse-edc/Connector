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

package org.eclipse.dataspaceconnector.api.datamanagement.policy.service;

import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.event.Event;
import org.eclipse.dataspaceconnector.spi.event.EventRouter;
import org.eclipse.dataspaceconnector.spi.event.EventSubscriber;
import org.eclipse.dataspaceconnector.spi.event.policydefinition.PolicyDefinitionCreated;
import org.eclipse.dataspaceconnector.spi.event.policydefinition.PolicyDefinitionDeleted;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(EdcExtension.class)
public class PolicyDefinitionEventDispatchTest {

    private final EventSubscriber eventSubscriber = mock(EventSubscriber.class);

    @Test
    void shouldDispatchEventOnPolicyDefinitionCreationAndDeletion(PolicyDefinitionService service, EventRouter eventRouter) throws InterruptedException {
        var createdLatch = onDispatchLatch(PolicyDefinitionCreated.class);
        var deletedLatch = onDispatchLatch(PolicyDefinitionDeleted.class);
        eventRouter.register(eventSubscriber);
        var policyDefinition = PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build();

        service.create(policyDefinition);

        assertThat(createdLatch.await(10, SECONDS)).isTrue();
        verify(eventSubscriber).on(isA(PolicyDefinitionCreated.class));

        service.deleteById(policyDefinition.getUid());

        assertThat(deletedLatch.await(10, SECONDS)).isTrue();
        verify(eventSubscriber).on(isA(PolicyDefinitionDeleted.class));
    }

    private CountDownLatch onDispatchLatch(Class<? extends Event> eventType) {
        var latch = new CountDownLatch(1);

        doAnswer(i -> {
            latch.countDown();
            return null;
        }).when(eventSubscriber).on(isA(eventType));

        return latch;
    }
}

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

package org.eclipse.edc.connector.service.policydefinition;

import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.event.policydefinition.PolicyDefinitionCreated;
import org.eclipse.edc.spi.event.policydefinition.PolicyDefinitionDeleted;
import org.eclipse.edc.spi.event.policydefinition.PolicyDefinitionEvent;
import org.eclipse.edc.spi.event.policydefinition.PolicyDefinitionUpdated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.junit.matchers.EventEnvelopeMatcher.isEnvelopeOf;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(EdcExtension.class)
public class PolicyDefinitionEventDispatchTest {

    private final EventSubscriber eventSubscriber = mock(EventSubscriber.class);

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.setConfiguration(Map.of(
                "web.http.port", String.valueOf(getFreePort()),
                "web.http.path", "/api")
        );
    }

    @Test
    void shouldDispatchEventOnPolicyDefinitionCreationAndDeletionAndUpdate(PolicyDefinitionService service, EventRouter eventRouter) throws InterruptedException {

        doAnswer(i -> null).when(eventSubscriber).on(argThat(isEnvelopeOf(PolicyDefinitionCreated.class)));

        doAnswer(i -> null).when(eventSubscriber).on(argThat(isEnvelopeOf(PolicyDefinitionDeleted.class)));

        doAnswer(i -> null).when(eventSubscriber).on(argThat(isEnvelopeOf(PolicyDefinitionUpdated.class)));

        eventRouter.register(PolicyDefinitionEvent.class, eventSubscriber);
        var policyDefinition = PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build();

        service.create(policyDefinition);
        await().untilAsserted(() -> {
            verify(eventSubscriber).on(argThat(isEnvelopeOf(PolicyDefinitionCreated.class)));
        });

        service.update(policyDefinition);
        await().untilAsserted(() -> {
            verify(eventSubscriber).on(argThat(isEnvelopeOf(PolicyDefinitionUpdated.class)));
        });

        service.deleteById(policyDefinition.getUid());
        await().untilAsserted(() -> {
            verify(eventSubscriber).on(argThat(isEnvelopeOf(PolicyDefinitionDeleted.class)));

        });
    }
}

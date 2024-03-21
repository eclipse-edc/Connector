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

import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.event.PolicyDefinitionBeforeCreate;
import org.eclipse.edc.connector.policy.spi.event.PolicyDefinitionBeforeUpdate;
import org.eclipse.edc.connector.policy.spi.event.PolicyDefinitionCreated;
import org.eclipse.edc.connector.policy.spi.event.PolicyDefinitionDeleted;
import org.eclipse.edc.connector.policy.spi.event.PolicyDefinitionEvent;
import org.eclipse.edc.connector.policy.spi.event.PolicyDefinitionUpdated;
import org.eclipse.edc.connector.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.protocol.ProtocolWebhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.junit.matchers.EventEnvelopeMatcher.isEnvelopeOf;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(EdcExtension.class)
public class PolicyDefinitionEventDispatchTest {

    private final EventSubscriber eventSubscriber = mock(EventSubscriber.class);

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.registerServiceMock(ProtocolWebhook.class, mock(ProtocolWebhook.class));
        extension.registerServiceMock(DataPlaneInstanceStore.class, mock(DataPlaneInstanceStore.class));
        extension.registerServiceMock(IdentityService.class, mock());
        extension.setConfiguration(Map.of(
                "web.http.port", String.valueOf(getFreePort()),
                "web.http.path", "/api")
        );
    }

    @Test
    void shouldDispatchEventOnPolicyDefinitionCreationAndDeletionAndUpdate(PolicyDefinitionService service, EventRouter eventRouter) {
        eventRouter.register(PolicyDefinitionEvent.class, eventSubscriber);
        var policyDefinition = PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build();

        ArgumentCaptor<EventEnvelope<PolicyDefinitionEvent>> argumentCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        service.create(policyDefinition);

        await().untilAsserted(() -> {
            verify(eventSubscriber, times(2)).on(argumentCaptor.capture());
            var events = argumentCaptor.getAllValues();
            events.forEach(event -> {
                verify(eventSubscriber).on(argThat(isEnvelopeOf(PolicyDefinitionBeforeCreate.class)));
                verify(eventSubscriber).on(argThat(isEnvelopeOf(PolicyDefinitionCreated.class)));

            });
        });

        service.update(policyDefinition);
        ArgumentCaptor<EventEnvelope<PolicyDefinitionEvent>> argumentCaptorForUpdate = ArgumentCaptor.forClass(EventEnvelope.class);
        await().untilAsserted(() -> {
            verify(eventSubscriber, times(4)).on(argumentCaptorForUpdate.capture());
            var events = argumentCaptorForUpdate.getAllValues();
            events.forEach(event -> {
                verify(eventSubscriber).on(argThat(isEnvelopeOf(PolicyDefinitionBeforeCreate.class)));
                verify(eventSubscriber).on(argThat(isEnvelopeOf(PolicyDefinitionCreated.class)));
                verify(eventSubscriber).on(argThat(isEnvelopeOf(PolicyDefinitionBeforeUpdate.class)));
                verify(eventSubscriber).on(argThat(isEnvelopeOf(PolicyDefinitionUpdated.class)));
            });
        });

        service.deleteById(policyDefinition.getUid());
        await().untilAsserted(() -> {
            verify(eventSubscriber).on(argThat(isEnvelopeOf(PolicyDefinitionDeleted.class)));

        });
    }

}

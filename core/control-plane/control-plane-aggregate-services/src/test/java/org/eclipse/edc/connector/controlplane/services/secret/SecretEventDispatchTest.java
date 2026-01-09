/*
 *  Copyright (c) 2024 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial API and Implementation
 *
 */

package org.eclipse.edc.connector.controlplane.services.secret;

import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.secret.spi.event.SecretCreated;
import org.eclipse.edc.connector.secret.spi.event.SecretDeleted;
import org.eclipse.edc.connector.secret.spi.event.SecretEvent;
import org.eclipse.edc.connector.spi.service.SecretService;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerMethodExtension;
import org.eclipse.edc.protocol.spi.ProtocolWebhook;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.types.domain.secret.Secret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.junit.matchers.EventEnvelopeMatcher.isEnvelopeOf;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ComponentTest
@ExtendWith(RuntimePerMethodExtension.class)
public class SecretEventDispatchTest {

    private final EventSubscriber eventSubscriber = mock();

    @BeforeEach
    void setUp(RuntimeExtension extension) {
        extension.registerServiceMock(ProtocolWebhook.class, mock());
        extension.registerServiceMock(DataPlaneInstanceStore.class, mock());
        extension.registerServiceMock(IdentityService.class, mock());
        extension.registerServiceMock(DataPlaneClientFactory.class, mock());
        extension.registerServiceMock(DataFlowController.class, mock());
        extension.setConfiguration(Map.of(
                "web.http.port", String.valueOf(getFreePort()),
                "web.http.path", "/api"
        ));
    }

    @Test
    void shouldDispatchEventsOnSecretCreationAndDeletion(SecretService service, EventRouter eventRouter) {
        eventRouter.register(SecretEvent.class, eventSubscriber);
        var secret = Secret.Builder.newInstance().id("secretId").value("secret-value").build();

        var result = service.create(secret);
        await().untilAsserted(() -> {
            verify(eventSubscriber).on(argThat(isEnvelopeOf(SecretCreated.class)));
        });


        service.delete(secret.getId());
        await().untilAsserted(() -> {
            verify(eventSubscriber).on(argThat(isEnvelopeOf(SecretDeleted.class)));
        });
    }
}

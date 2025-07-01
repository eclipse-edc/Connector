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

import org.eclipse.edc.connector.secret.spi.event.SecretCreated;
import org.eclipse.edc.connector.secret.spi.event.SecretDeleted;
import org.eclipse.edc.connector.secret.spi.event.SecretUpdated;
import org.eclipse.edc.connector.secret.spi.observe.SecretListener;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.types.domain.secret.Secret;

/**
 * Listener responsible for creating and publishing events regarding Secret state changes
 */
public class SecretEventListener implements SecretListener {
    private final EventRouter eventRouter;

    public SecretEventListener(EventRouter eventRouter) {
        this.eventRouter = eventRouter;
    }

    @Override
    public void created(Secret secret) {
        var event = SecretCreated.Builder.newInstance()
                .secretId(secret.getId())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void deleted(Secret secret) {
        var event = SecretDeleted.Builder.newInstance()
                .secretId(secret.getId())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void updated(Secret secret) {
        var event = SecretUpdated.Builder.newInstance()
                .secretId(secret.getId())
                .build();

        eventRouter.publish(event);
    }

}

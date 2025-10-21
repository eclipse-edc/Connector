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

package org.eclipse.edc.connector.controlplane.services.policydefinition;

import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.event.PolicyDefinitionCreated;
import org.eclipse.edc.connector.controlplane.policy.spi.event.PolicyDefinitionDeleted;
import org.eclipse.edc.connector.controlplane.policy.spi.event.PolicyDefinitionEvent;
import org.eclipse.edc.connector.controlplane.policy.spi.event.PolicyDefinitionUpdated;
import org.eclipse.edc.connector.controlplane.policy.spi.observe.PolicyDefinitionListener;
import org.eclipse.edc.spi.event.EventRouter;

/**
 * Listener responsible for creating and publishing events regarding PolicyDefinition state changes
 */
public class PolicyDefinitionEventListener implements PolicyDefinitionListener {
    private final EventRouter eventRouter;

    public PolicyDefinitionEventListener(EventRouter eventRouter) {
        this.eventRouter = eventRouter;
    }

    @Override
    public void created(PolicyDefinition policyDefinition) {
        var builder = PolicyDefinitionCreated.Builder.newInstance();

        eventRouter.publish(withBaseProperties(builder, policyDefinition));
    }

    @Override
    public void deleted(PolicyDefinition policyDefinition) {
        var builder = PolicyDefinitionDeleted.Builder.newInstance();

        eventRouter.publish(withBaseProperties(builder, policyDefinition));
    }

    @Override
    public void updated(PolicyDefinition policyDefinition) {
        var builder = PolicyDefinitionUpdated.Builder.newInstance();

        eventRouter.publish(withBaseProperties(builder, policyDefinition));
    }

    private <E extends PolicyDefinitionEvent> E withBaseProperties(PolicyDefinitionEvent.Builder<E, ?> builder, PolicyDefinition policyDefinition) {
        return builder
                .policyDefinitionId(policyDefinition.getId())
                .participantContextId(policyDefinition.getParticipantContextId())
                .build();
    }

}

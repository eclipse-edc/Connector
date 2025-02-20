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

package org.eclipse.edc.connector.policy.monitor.store.sql;

import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorEntry;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorEntryStates;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorStore;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.store.InMemoryStatefulEntityStore;

import java.time.Clock;
import java.util.UUID;

/**
 * In-memory implementation of the {@link PolicyMonitorStore}
 */
public class InMemoryPolicyMonitorStore extends InMemoryStatefulEntityStore<PolicyMonitorEntry> implements PolicyMonitorStore {

    public InMemoryPolicyMonitorStore(Clock clock, CriterionOperatorRegistry criterionOperatorRegistry) {
        this(UUID.randomUUID().toString(), clock, criterionOperatorRegistry);
    }

    public InMemoryPolicyMonitorStore(String owner, Clock clock, CriterionOperatorRegistry criterionOperatorRegistry) {
        super(PolicyMonitorEntry.class, owner, clock, criterionOperatorRegistry, state -> PolicyMonitorEntryStates.valueOf(state).code());
    }
}

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

package org.eclipse.edc.connector.policy.monitor;

import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorStore;
import org.eclipse.edc.connector.policy.monitor.store.sql.InMemoryPolicyMonitorStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;

import java.time.Clock;

import static org.eclipse.edc.connector.policy.monitor.PolicyMonitorDefaultServicesExtension.NAME;

@Extension(value = NAME)
public class PolicyMonitorDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "PolicyMonitor Default Services";

    @Override
    public String name() {
        return NAME;
    }

    @Inject
    private Clock clock;

    @Inject
    private CriterionOperatorRegistry criterionOperatorRegistry;

    @Provider(isDefault = true)
    public PolicyMonitorStore policyMonitorStore() {
        return new InMemoryPolicyMonitorStore(clock, criterionOperatorRegistry);
    }
}

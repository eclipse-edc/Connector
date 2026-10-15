/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.policy.monitor.store.sql;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorStore;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.extensions.TestExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DependencyInjectionExtension.class)
class SqlPolicyMonitorStoreExtensionTest {

    @BeforeEach
    void setUp(TestExtensionContext context) {
        context.registerService(TypeManager.class, new JacksonTypeManager());
    }

    @Test
    void shouldProvideSqlStore(TestExtensionContext context, ObjectFactory factory) {
        context.setConfig(ConfigFactory.fromMap(Map.of()));
        var extension = factory.constructInstance(SqlPolicyMonitorStoreExtension.class);

        var store = extension.policyMonitorStore(context);

        assertThat(store).isInstanceOf(SqlPolicyMonitorStore.class);
    }

    @Test
    void shouldUseConfiguredLeaseDuration(TestExtensionContext context, ObjectFactory factory) {
        context.setConfig(ConfigFactory.fromMap(Map.of(PolicyMonitorStore.LEASE_DURATION_SETTING, "PT5M")));
        var extension = factory.constructInstance(SqlPolicyMonitorStoreExtension.class);

        var store = extension.policyMonitorStore(context);

        assertThat(store).extracting("leaseDuration").isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void shouldUseDefaultLeaseDuration_whenNotConfigured(TestExtensionContext context, ObjectFactory factory) {
        context.setConfig(ConfigFactory.fromMap(Map.of()));
        var extension = factory.constructInstance(SqlPolicyMonitorStoreExtension.class);

        var store = extension.policyMonitorStore(context);

        assertThat(store).extracting("leaseDuration").isEqualTo(Duration.ofSeconds(60));
    }
}

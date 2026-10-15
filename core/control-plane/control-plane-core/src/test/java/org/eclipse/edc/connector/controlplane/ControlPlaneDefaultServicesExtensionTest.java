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

package org.eclipse.edc.connector.controlplane;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.extensions.TestExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DependencyInjectionExtension.class)
class ControlPlaneDefaultServicesExtensionTest {

    @Test
    void shouldUseConfiguredLeaseDurations(TestExtensionContext context, ObjectFactory factory) {
        context.setConfig(ConfigFactory.fromMap(Map.of(
                TransferProcessStore.LEASE_DURATION_SETTING, "PT5M",
                ContractNegotiationStore.LEASE_DURATION_SETTING, "PT10M")));
        var extension = factory.constructInstance(ControlPlaneDefaultServicesExtension.class);

        assertThat(extension.defaultTransferProcessStore()).extracting("leaseDuration").isEqualTo(Duration.ofMinutes(5));
        assertThat(extension.defaultContractNegotiationStore()).extracting("leaseDuration").isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void shouldUseDefaultLeaseDurations_whenNotConfigured(TestExtensionContext context, ObjectFactory factory) {
        context.setConfig(ConfigFactory.fromMap(Map.of()));
        var extension = factory.constructInstance(ControlPlaneDefaultServicesExtension.class);

        assertThat(extension.defaultTransferProcessStore()).extracting("leaseDuration").isEqualTo(Duration.ofSeconds(60));
        assertThat(extension.defaultContractNegotiationStore()).extracting("leaseDuration").isEqualTo(Duration.ofSeconds(60));
    }
}

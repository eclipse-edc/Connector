/*
 *  Copyright (c) 2023 Amazon Web Services
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amazon Web Services - Initial implementation
 *
 */

package org.eclipse.edc.vault.aws;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

class AwsSecretsManagerVaultExtensionTest {

    private final Monitor monitor = Mockito.mock(Monitor.class);
    private final AwsSecretsManagerVaultExtension extension = new AwsSecretsManagerVaultExtension();

    @Test
    void configOptionRegionNotProvided_shouldThrowException() {
        ServiceExtensionContext invalidContext = Mockito.mock(ServiceExtensionContext.class);
        Mockito.when(invalidContext.getMonitor()).thenReturn(monitor);
        Assertions.assertThrows(AwsSecretsManagerVaultException.class, () -> extension.initialize(invalidContext));
    }

    @Test
    void configOptionRegionProvided_shouldNotThrowException() {
        ServiceExtensionContext validContext = Mockito.mock(ServiceExtensionContext.class);
        when(validContext.getSetting("edc.vault.aws.region", null)).thenReturn("eu-west-1");
        Mockito.when(validContext.getMonitor()).thenReturn(monitor);
        extension.initialize(validContext);
    }

}

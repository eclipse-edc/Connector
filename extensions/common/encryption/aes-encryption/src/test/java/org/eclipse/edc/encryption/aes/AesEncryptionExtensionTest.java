/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.encryption.aes;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class AesEncryptionExtensionTest {


    @BeforeEach
    void setup(ServiceExtensionContext context) {
        var config = ConfigFactory.fromMap(Map.of(
                "edc.encryption.aes.key.alias", "test-alias"

        ));
        when(context.getConfig()).thenReturn(config);
    }

    @Test
    void verifyEncryptionService(AesEncryptionExtension extension) {
        var service = extension.encryptionService();
        assertThat(service).isInstanceOf(AesEncryptionService.class);
    }
}

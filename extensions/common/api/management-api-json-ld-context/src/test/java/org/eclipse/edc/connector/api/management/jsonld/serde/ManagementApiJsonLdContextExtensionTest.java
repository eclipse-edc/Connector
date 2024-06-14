/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.api.management.jsonld.serde;

import org.eclipse.edc.connector.api.management.jsonld.ManagementApiJsonLdContextExtension;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.edc.connector.api.management.jsonld.ManagementApiJsonLdContextExtension.EDC_CONNECTOR_MANAGEMENT_CONTEXT;
import static org.eclipse.edc.connector.api.management.jsonld.ManagementApiJsonLdContextExtension.EDC_CONNECTOR_MANAGEMENT_SCOPE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class ManagementApiJsonLdContextExtensionTest {

    private final JsonLd jsonLd = mock();
    private ManagementApiJsonLdContextExtension extension;

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(JsonLd.class, jsonLd);
    }

    @Test
    void initialize(ManagementApiJsonLdContextExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);
        verify(jsonLd).registerContext(EDC_CONNECTOR_MANAGEMENT_CONTEXT, EDC_CONNECTOR_MANAGEMENT_SCOPE);
    }
}

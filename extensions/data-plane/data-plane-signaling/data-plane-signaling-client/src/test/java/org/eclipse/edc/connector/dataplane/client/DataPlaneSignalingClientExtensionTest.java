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

package org.eclipse.edc.connector.dataplane.client;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.client.DataPlaneSignalingClientExtension.CONTROL_CLIENT_SCOPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_PREFIX;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneSignalingClientExtensionTest {

    @Test
    void verifyDataPlaneClientFactory(ServiceExtensionContext context, ObjectFactory factory) {
        var jsonLd = mock(JsonLd.class);
        context.registerService(DataPlaneManager.class, null);
        context.registerService(JsonLd.class, jsonLd);
        var extension = factory.constructInstance(DataPlaneSignalingClientExtension.class);

        var client = extension.dataPlaneClientFactory(context).createClient(createDataPlaneInstance());

        assertThat(client).isInstanceOf(DataPlaneSignalingClient.class);
        verify(jsonLd).registerNamespace(ODRL_PREFIX, ODRL_SCHEMA, CONTROL_CLIENT_SCOPE);
        verify(jsonLd).registerNamespace(DSPACE_PREFIX, DSPACE_SCHEMA, CONTROL_CLIENT_SCOPE);
        verify(jsonLd).registerNamespace(VOCAB, EDC_NAMESPACE, CONTROL_CLIENT_SCOPE);
    }

    @Test
    void verifyDataPlaneClientFactory_withEmbedded(ServiceExtensionContext context, DataPlaneSignalingClientExtension extension) {
        var client = extension.dataPlaneClientFactory(context).createClient(createDataPlaneInstance());

        assertThat(client).isInstanceOf(EmbeddedDataPlaneClient.class);
    }


    private DataPlaneInstance createDataPlaneInstance() {
        return DataPlaneInstance.Builder.newInstance().url("http://any").build();
    }
}

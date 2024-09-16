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

package org.eclipse.edc.iam.identitytrust.sts.client.configuration;

import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsClient;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsClientStore;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.sts.client.configuration.StsClientConfigurationExtension.CLIENT_DID;
import static org.eclipse.edc.iam.identitytrust.sts.client.configuration.StsClientConfigurationExtension.CLIENT_ID;
import static org.eclipse.edc.iam.identitytrust.sts.client.configuration.StsClientConfigurationExtension.CLIENT_NAME;
import static org.eclipse.edc.iam.identitytrust.sts.client.configuration.StsClientConfigurationExtension.CLIENT_PRIVATE_KEY_ALIAS;
import static org.eclipse.edc.iam.identitytrust.sts.client.configuration.StsClientConfigurationExtension.CLIENT_PUBLIC_KEY_REFERENCE;
import static org.eclipse.edc.iam.identitytrust.sts.client.configuration.StsClientConfigurationExtension.CLIENT_SECRET_ALIAS;
import static org.eclipse.edc.iam.identitytrust.sts.client.configuration.StsClientConfigurationExtension.CONFIG_PREFIX;
import static org.eclipse.edc.iam.identitytrust.sts.client.configuration.StsClientConfigurationExtension.ID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class StsClientConfigurationExtensionTest implements ServiceExtension {

    private final StsClientStore clientStore = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(StsClientStore.class, clientStore);
    }

    @Test
    void initialize_noClients(ServiceExtensionContext context, StsClientConfigurationExtension extension) {
        extension.initialize(context);
        verifyNoInteractions(clientStore);
    }

    @Test
    void initialize_withClient(ServiceExtensionContext context, StsClientConfigurationExtension extension) {
        var client = StsClient.Builder.newInstance()
                .id("id")
                .name("name")
                .clientId("client_id")
                .privateKeyAlias("pAlias")
                .secretAlias("sAlias")
                .did("did:example:subject")
                .publicKeyReference("publicReference")
                .build();
        var clientAlias = "client";
        var config = ConfigFactory.fromMap(clientConfig(client, clientAlias));

        when(context.getConfig(CONFIG_PREFIX)).thenReturn(config);
        extension.initialize(context);
        var capture = ArgumentCaptor.forClass(StsClient.class);
        verify(clientStore).create(capture.capture());

        assertThat(capture.getValue()).usingRecursiveComparison()
                .ignoringFields("createdAt")
                .isEqualTo(client);
    }

    private Map<String, String> clientConfig(StsClient client, String clientAlias) {
        return Map.of(
                clientAlias + "." + ID, client.getId(),
                clientAlias + "." + CLIENT_NAME, client.getName(),
                clientAlias + "." + CLIENT_ID, client.getClientId(),
                clientAlias + "." + CLIENT_SECRET_ALIAS, client.getSecretAlias(),
                clientAlias + "." + CLIENT_DID, client.getDid(),
                clientAlias + "." + CLIENT_PRIVATE_KEY_ALIAS, client.getPrivateKeyAlias(),
                clientAlias + "." + CLIENT_PUBLIC_KEY_REFERENCE, client.getPublicKeyReference()
        );
    }

}
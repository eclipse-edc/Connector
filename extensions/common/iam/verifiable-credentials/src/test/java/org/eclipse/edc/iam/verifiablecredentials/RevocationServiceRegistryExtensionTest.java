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

package org.eclipse.edc.iam.verifiablecredentials;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.verifiablecredentials.spi.TestFunctions;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.extensions.TestExtensionContext;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class RevocationServiceRegistryExtensionTest {

    private static final String STATUS_LIST_URL = "https://issuer.example/status/1";
    private final EdcHttpClient httpClient = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) throws IOException {
        context.registerService(EdcHttpClient.class, httpClient);
        context.registerService(TypeManager.class, new JacksonTypeManager());
        // the status list can't be downloaded: a registered service reports that, instead of the "unknown type" failure
        when(httpClient.execute(any(), anyList())).thenThrow(new IOException("connection refused"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "StatusList2021Entry",
            "https://w3id.org/vc/status-list#StatusList2021Entry",
            "BitstringStatusListEntry",
            "https://www.w3.org/ns/credentials/status#BitstringStatusListEntry"
    })
    void createRevocationListService_shouldRegisterCompactAndExpandedTypes(String statusType, ServiceExtensionContext context,
                                                                           RevocationServiceRegistryExtension extension) {
        var registry = extension.createRevocationListService(context);

        assertThat(registry.checkValidity(credentialWithStatus(statusType))).isFailed()
                .detail().contains("Failed to download status list credential");
    }

    @Test
    void createRevocationListService_unknownType_shouldFail(ServiceExtensionContext context, RevocationServiceRegistryExtension extension) {
        var registry = extension.createRevocationListService(context);

        assertThat(registry.checkValidity(credentialWithStatus("UnknownStatusEntry"))).isFailed()
                .detail().contains("No revocation service registered");
    }

    @Test
    void createRevocationListService_unknownType_whenIgnored_shouldSucceed(TestExtensionContext context, ObjectFactory objectFactory) {
        context.setConfig(ConfigFactory.fromMap(Map.of("edc.iam.credential.revocation.unknowntypes.ignore", "true")));
        var extension = objectFactory.constructInstance(RevocationServiceRegistryExtension.class);
        var registry = extension.createRevocationListService(context);

        assertThat(registry.checkValidity(credentialWithStatus("UnknownStatusEntry"))).isSucceeded();
    }

    private VerifiableCredential credentialWithStatus(String statusType) {
        var status = new CredentialStatus("https://issuer.example/status/1#0", statusType, Map.of(
                "statusPurpose", "revocation",
                "statusListIndex", "0",
                "statusListCredential", STATUS_LIST_URL));
        return TestFunctions.createCredentialBuilder().credentialStatus(status).build();
    }
}

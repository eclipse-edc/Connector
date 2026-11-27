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

package org.eclipse.edc.protocol.dsp.transform;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.test.TestJsonLd;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;

class DspMessageIdentifierTest {

    private static final JsonLdNamespace DSP_NAMESPACE = new JsonLdNamespace("https://w3id.org/dspace/2025/1/");
    private static final String MESSAGE_ID = "urn:uuid:3f0a6a31-5c3b-4f52-9a2e-2b1f0c7d9e11";

    @Test
    void shouldReturnId_whenUrnUuid() {
        var message = message(MESSAGE_ID, "consumerPid", "providerPid");

        assertThat(DspMessageIdentifier.of(message, DSP_NAMESPACE::toIri)).isEqualTo(MESSAGE_ID);
    }

    @Test
    void shouldReturnId_whenUpperCaseUuid() {
        var id = "urn:uuid:3F0A6A31-5C3B-4F52-9A2E-2B1F0C7D9E11";

        assertThat(DspMessageIdentifier.of(message(id, "consumerPid", "providerPid"), DSP_NAMESPACE::toIri)).isEqualTo(id);
    }

    @Test
    void shouldReturnNull_whenNoId() {
        var message = TestJsonLd.expand(Json.createObjectBuilder()
                .add(TYPE, DSP_NAMESPACE.toIri("TransferCompletionMessage"))
                .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_CONSUMER_PID_TERM), "consumerPid")
                .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_PROVIDER_PID_TERM), "providerPid")
                .build());

        assertThat(DspMessageIdentifier.of(message, DSP_NAMESPACE::toIri)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "3f0a6a31-5c3b-4f52-9a2e-2b1f0c7d9e11",
            "urn:uuid:not-a-uuid",
            "urn:uuid:3f0a6a31-5c3b-4f52-9a2e-2b1f0c7d9e11-extra",
            "urn:other:3f0a6a31-5c3b-4f52-9a2e-2b1f0c7d9e11",
            "https://example.com/messages/1"
    })
    void shouldReturnNull_whenIdIsNotUrnUuid(String id) {
        assertThat(DspMessageIdentifier.of(message(id, "consumerPid", "providerPid"), DSP_NAMESPACE::toIri)).isNull();
    }

    @Test
    void shouldReturnNull_whenIdIsTheConsumerPid() {
        assertThat(DspMessageIdentifier.of(message(MESSAGE_ID, MESSAGE_ID, "providerPid"), DSP_NAMESPACE::toIri)).isNull();
    }

    @Test
    void shouldReturnNull_whenIdIsTheProviderPid() {
        assertThat(DspMessageIdentifier.of(message(MESSAGE_ID, "consumerPid", MESSAGE_ID), DSP_NAMESPACE::toIri)).isNull();
    }

    @Test
    void shouldReturnNull_whenIdIsBuiltFromPid() {
        var pid = MESSAGE_ID.substring(DspMessageIdentifier.URN_UUID_PREFIX.length());

        assertThat(DspMessageIdentifier.of(message(MESSAGE_ID, "consumerPid", pid), DSP_NAMESPACE::toIri)).isNull();
    }

    private JsonObject message(String id, String consumerPid, String providerPid) {
        return TestJsonLd.expand(Json.createObjectBuilder()
                .add(ID, id)
                .add(TYPE, DSP_NAMESPACE.toIri("TransferCompletionMessage"))
                .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_CONSUMER_PID_TERM), consumerPid)
                .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_PROVIDER_PID_TERM), providerPid)
                .build());
    }
}

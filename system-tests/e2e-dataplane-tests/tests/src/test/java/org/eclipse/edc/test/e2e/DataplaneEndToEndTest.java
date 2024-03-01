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

package org.eclipse.edc.test.e2e;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.api.signaling.transform.SignalingApiTransformerRegistry;
import org.eclipse.edc.connector.api.signaling.transform.SignalingApiTransformerRegistryImpl;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataAddressTransformer;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowStartMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.to.JsonObjectToDataAddressTransformer;
import org.eclipse.edc.connector.dataplane.spi.Endpoint;
import org.eclipse.edc.connector.dataplane.spi.iam.PublicEndpointGeneratorService;
import org.eclipse.edc.core.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.spi.result.Failure;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.test.e2e.participant.DataPlaneParticipant;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

@EndToEndTest
public class DataplaneEndToEndTest {

    public static final String DATAPLANE_PUBLIC_ENDPOINT_URL = "http://fizz.buzz/bar";
    protected static final DataPlaneParticipant DATAPLANE = DataPlaneParticipant.Builder.newInstance()
            .name("provider")
            .id("urn:connector:provider")
            .build();
    @RegisterExtension
    static EdcRuntimeExtension runtime =
            new EdcRuntimeExtension(
                    ":system-tests:e2e-dataplane-tests:runtimes:data-plane",
                    "data-plane",
                    DATAPLANE.dataPlaneConfiguration()
            );

    protected final Duration timeout = Duration.ofSeconds(60);
    private ObjectMapper mapper;
    private SignalingApiTransformerRegistry registry;

    @BeforeEach
    void setup() {
        // this registry is entirely separate from the one that is included in the runtime
        registry = new SignalingApiTransformerRegistryImpl(new TypeTransformerRegistryImpl());
        var builderFactory = Json.createBuilderFactory(Map.of());
        mapper = JacksonJsonLd.createObjectMapper();
        registry.register(new JsonObjectFromDataFlowStartMessageTransformer(builderFactory, mapper));
        registry.register(new JsonObjectFromDataAddressTransformer(builderFactory, mapper));
        registry.register(new JsonObjectToDataAddressTransformer());
    }

    @Test
    void startTransfer_httpPull() throws JsonProcessingException {
        var generator = runtime.getContext().getService(PublicEndpointGeneratorService.class);
        generator.addGeneratorFunction("HttpData", dataAddress -> Endpoint.url(DATAPLANE_PUBLIC_ENDPOINT_URL));

        var flowMessage = DataFlowStartMessage.Builder.newInstance()
                .processId("test-processId")
                .sourceDataAddress(DataAddress.Builder.newInstance().type("HttpData").property(EDC_NAMESPACE + "baseUrl", "http://foo.bar/").build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type("HttpData").property(EDC_NAMESPACE + "baseUrl", "http://fizz.buzz").build())
                .flowType(FlowType.PULL)
                .participantId("some-participantId")
                .assetId("test-asset")
                .callbackAddress(URI.create("https://foo.bar/callback"))
                .agreementId("test-agreement")
                .build();
        var jo = registry.transform(flowMessage, JsonObject.class).orElseThrow(failTest());

        var resultJson = DATAPLANE.initiateTransfer(jo);
        var dataAddress = registry.transform(mapper.readValue(resultJson, JsonObject.class), DataAddress.class)
                .orElseThrow(failTest());

        // verify basic shape of the DSPACE data address
        assertThat(dataAddress).isNotNull();
        assertThat(dataAddress.getType()).isEqualTo("https://w3id.org/idsa/v4.1/HTTP");
        assertThat(dataAddress.getProperties())
                .containsKey("authorization")
                .containsEntry("endpoint", DATAPLANE_PUBLIC_ENDPOINT_URL)
                .containsEntry("authType", "bearer");
    }

    @NotNull
    private Function<Failure, AssertionError> failTest() {
        return f -> new AssertionError(f.getFailureDetail());
    }
}

/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.provision.http.port;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.connector.dataplane.provision.http.logic.ProvisionHttp;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.DataFlowStates;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.dataaddress.httpdata.spi.HttpDataAddressSchema;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimePerMethodExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.http.RequestMethod.GET;
import static com.github.tomakehurst.wiremock.http.RequestMethod.POST;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static java.util.Collections.emptyMap;
import static java.util.Map.entry;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.DEPROVISIONED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.DEPROVISION_REQUESTED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.PROVISION_REQUESTED;
import static org.eclipse.edc.http.client.testfixtures.HttpTestUtils.testHttpClient;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.types.domain.transfer.FlowType.PUSH;
import static org.eclipse.edc.util.io.Ports.getFreePort;

@ComponentTest
public class ProvisionHttpIntegrationTest {

    private final ObjectMapper objectMapper = new JacksonTypeManager().getMapper();
    private final WireMockServer controlPlane = new WireMockServer(getFreePort());
    private final WireMockServer provisionService = new WireMockServer(getFreePort());
    private final WireMockServer sourceService = new WireMockServer(getFreePort());
    private final WireMockServer destinationService = new WireMockServer(getFreePort());

    @RegisterExtension
    private final RuntimePerMethodExtension dataplane = new RuntimePerMethodExtension(new EmbeddedRuntime("dataplane", ":dist:bom:dataplane-base-bom")
            .configurationProvider(() -> ConfigFactory.fromMap(Map.ofEntries(
                    entry("edc.transfer.proxy.token.signer.privatekey.alias", "any"),
                    entry("edc.transfer.proxy.token.verifier.publickey.alias", "any"),
                    entry("edc.dpf.selector.url", "http://localhost:" + controlPlane.port())
            ))));

    @BeforeEach
    void setUp() {
        controlPlane.stubFor(any(anyUrl()).willReturn(ok()));
        controlPlane.start();
        provisionService.stubFor(any(anyUrl()).willReturn(ok()));
        provisionService.start();
        sourceService.stubFor(any(anyUrl()).willReturn(ok("data-to-transfer")));
        sourceService.start();
        destinationService.stubFor(any(anyUrl()).willReturn(ok()));
        destinationService.start();
    }

    @AfterEach
    void tearDown() {
        controlPlane.stop();
        provisionService.stop();
        sourceService.stop();
        destinationService.stop();
    }

    @Test
    void shouldProvision_andDeprovision(DataPlaneManager dataPlaneManager) {
        var flowId = UUID.randomUUID().toString();
        var startMessage = DataFlowStartMessage.Builder.newInstance()
                .processId(flowId)
                .sourceDataAddress(DataAddress.Builder.newInstance()
                        .type(ProvisionHttp.PROVISION_HTTP_TYPE)
                        .property(EDC_NAMESPACE + "baseUrl", "http://localhost:" + provisionService.port() + "/provision")
                        .build())
                .destinationDataAddress(DataAddress.Builder.newInstance()
                        .type("HttpData")
                        .property(EDC_NAMESPACE + "baseUrl", destinationService.baseUrl())
                        .property(EDC_NAMESPACE + "method", "POST")
                        .build()
                )
                .transferType(new TransferType("HttpData", PUSH))
                .callbackAddress(URI.create(controlPlane.baseUrl()))
                .build();

        assertThat(dataPlaneManager.validate(startMessage)).isSucceeded();
        var provisioningResult = dataPlaneManager.start(startMessage);

        assertThat(provisioningResult).isSucceeded().satisfies(it -> {
            assertThat(it.isProvisioning()).isTrue();
        });

        awaitDataFlowToBeInStatus(flowId, PROVISION_REQUESTED);
        provisionService.verify(newRequestPattern(POST, urlPathEqualTo("/provision"))
                .withRequestBody(containing("\"type\":\"provision\"")));

        provisionResponse(provisionService);

        await().untilAsserted(() -> {
            controlPlane.verify(newRequestPattern(POST, urlPathEqualTo("/transferprocess/" + flowId + "/provisioned")));
        });

        // flow automatically gets STARTED
        await().untilAsserted(() -> {
            sourceService.verify(newRequestPattern(GET, urlPathEqualTo("/")));
            destinationService.verify(newRequestPattern(POST, urlPathEqualTo("/")).withRequestBody(equalTo("data-to-transfer")));
        });
        // flow automatically gets COMPLETED

        awaitDataFlowToBeInStatus(flowId, DEPROVISION_REQUESTED);
        provisionService.verify(newRequestPattern(POST, urlPathEqualTo("/provision"))
                .withRequestBody(containing("\"type\":\"deprovision\"")));

        deprovisionResponse(provisionService);

        awaitDataFlowToBeInStatus(flowId, DEPROVISIONED);
    }

    private void awaitDataFlowToBeInStatus(String flowId, DataFlowStates status) {
        var store = dataplane.getService(DataPlaneStore.class);
        await().untilAsserted(() -> {
            assertThat(store.findById(flowId)).isNotNull().extracting(DataFlow::stateAsString).isEqualTo(status.name());
        });
    }

    private void provisionResponse(WireMockServer provisionServer) {
        var events = provisionServer.getAllServeEvents();
        assertThat(events).hasSize(1);
        var request = events.get(0).getRequest();
        try {
            var ingressRequestBody = objectMapper.readValue(request.getBody(), Map.class);

            if ("provision".equals(ingressRequestBody.get("type"))) {
                var callbackRequestBody = Map.of(
                        "dataAddress", DataAddress.Builder.newInstance()
                                .type("HttpData")
                                .property(HttpDataAddressSchema.BASE_URL, sourceService.baseUrl())
                                .property(EDC_NAMESPACE + "method", "GET")
                                .build()
                );

                var egressRequest = new Request.Builder()
                        .url("%s/%s/%s/provision".formatted(ingressRequestBody.get("callbackAddress"), ingressRequestBody.get("flowId"), ingressRequestBody.get("provisionResourceId")))
                        .post(RequestBody.create(objectMapper.writeValueAsString(callbackRequestBody), MediaType.get("application/json")))
                        .build();

                executeDelayed(egressRequest);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void deprovisionResponse(WireMockServer dummyProvisionService) {
        var events = dummyProvisionService.getAllServeEvents();
        assertThat(events).hasSize(2);
        var request = events.get(0).getRequest();
        try {
            var ingressRequestBody = objectMapper.readValue(request.getBody(), Map.class);

            if ("deprovision".equals(ingressRequestBody.get("type"))) {
                var egressRequest = new Request.Builder()
                        .url("%s/%s/%s/deprovision".formatted(ingressRequestBody.get("callbackAddress"), ingressRequestBody.get("flowId"), ingressRequestBody.get("provisionResourceId")))
                        .post(RequestBody.create(objectMapper.writeValueAsString(emptyMap()), MediaType.get("application/json")))
                        .build();

                executeDelayed(egressRequest);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void executeDelayed(Request request) {
        Executors.newScheduledThreadPool(1).schedule(() -> {
            try {
                testHttpClient().execute(request).close();
            } catch (Exception e) {
                throw new EdcException(e);
            }
        }, 1, SECONDS);
    }

}

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

import io.restassured.common.mapper.TypeRef;
import org.assertj.core.api.ThrowingConsumer;
import org.eclipse.edc.connector.controlplane.test.system.utils.Participant;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.util.io.Ports;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.io.File.separator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.util.io.Ports.getFreePort;

public class TransferEndToEndParticipant extends Participant {

    private final LazySupplier<URI> controlPlaneControl = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/control"));
    private final LazySupplier<URI> dataPlaneControl = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/control"));
    private final LazySupplier<Integer> httpProvisionerPort = new LazySupplier<>(Ports::getFreePort);

    protected TransferEndToEndParticipant() {
        super();
    }

    public Config controlPlaneConfig() {
        var settings = new HashMap<String, String>() {
            {
                put("edc.participant.id", id);
                put("web.http.port", String.valueOf(getFreePort()));
                put("web.http.path", "/api");
                put("web.http.protocol.port", String.valueOf(controlPlaneProtocol.get().getPort()));
                put("web.http.protocol.path", controlPlaneProtocol.get().getPath());
                put("web.http.management.port", String.valueOf(controlPlaneManagement.get().getPort()));
                put("web.http.management.path", controlPlaneManagement.get().getPath());
                put("web.http.control.port", String.valueOf(controlPlaneControl.get().getPort()));
                put("web.http.control.path", controlPlaneControl.get().getPath());
                put("edc.dsp.callback.address", controlPlaneProtocol.get().toString());
                put("edc.transfer.send.retry.limit", "1");
                put("edc.transfer.send.retry.base-delay.ms", "100");
                put("edc.negotiation.consumer.send.retry.limit", "1");
                put("edc.negotiation.provider.send.retry.limit", "1");
                put("edc.negotiation.consumer.send.retry.base-delay.ms", "100");
                put("edc.negotiation.provider.send.retry.base-delay.ms", "100");
                put("edc.negotiation.consumer.state-machine.iteration-wait-millis", "50");
                put("edc.negotiation.provider.state-machine.iteration-wait-millis", "50");
                put("edc.transfer.state-machine.iteration-wait-millis", "50");
                put("edc.data.plane.selector.state-machine.iteration-wait-millis", "100");
            }
        };

        return ConfigFactory.fromMap(settings);
    }

    public Config dataPlaneConfig() {
        var settings = new HashMap<String, String>() {
            {
                put("web.http.port", String.valueOf(getFreePort()));
                put("web.http.path", "/api");
                put("web.http.control.port", String.valueOf(dataPlaneControl.get().getPort()));
                put("web.http.control.path", dataPlaneControl.get().getPath());
                put("web.http.provision.port", String.valueOf(getFreePort()));
                put("web.http.provision.path", "/provision-callback");
                put("edc.transfer.proxy.token.signer.privatekey.alias", "private-key");
                put("edc.transfer.proxy.token.verifier.publickey.alias", "public-key");
                put("edc.dataplane.http.sink.partition.size", "1");
                put("edc.dataplane.send.retry.limit", "1");
                put("edc.dataplane.state-machine.iteration-wait-millis", "50");
                put("edc.dpf.selector.url", controlPlaneControl.get() + "/v1/dataplanes");
            }
        };
        return ConfigFactory.fromMap(settings);
    }

    public Config controlPlaneEmbeddedDataPlaneConfig() {
        return controlPlaneConfig().merge(dataPlaneConfig());
    }

    public int getHttpProvisionerPort() {
        return httpProvisionerPort.get();
    }

    /**
     * Get the EDR from the EDR cache by transfer process id.
     *
     * @param transferProcessId The transfer process id
     * @return The cached {@link DataAddress}
     */
    public DataAddress getEdr(String transferProcessId) {
        var dataAddressRaw = baseManagementRequest()
                .contentType(JSON)
                .when()
                .get("/v3/edrs/{id}/dataaddress", transferProcessId)
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(JSON)
                .extract().body().as(new TypeRef<Map<String, Object>>() {
                });


        var builder = DataAddress.Builder.newInstance();
        dataAddressRaw.forEach(builder::property);
        return builder.build();
    }

    /**
     * Pull data from provider using EDR.
     *
     * @param edr           endpoint data reference
     * @param queryParams   query parameters
     * @param bodyAssertion assertion to be verified on the body
     */
    public void pullData(DataAddress edr, Map<String, String> queryParams, ThrowingConsumer<String> bodyAssertion) {
        var data = given()
                .baseUri(edr.getStringProperty("endpoint"))
                .header("Authorization", edr.getStringProperty("authorization"))
                .queryParams(queryParams)
                .when()
                .get()
                .then()
                .log().ifError()
                .statusCode(200)
                .extract().body().asString();

        assertThat(data).satisfies(bodyAssertion);
    }

    public void postResponse(DataAddress edr, ThrowingConsumer<String> bodyAssertion) {
        var data = given()
                .baseUri(edr.getStringProperty("responseChannel-endpoint"))
                .header("Authorization", edr.getStringProperty("responseChannel-authorization"))
                .when()
                .post()
                .then()
                .log().ifError()
                .statusCode(200)
                .extract().body().asString();

        assertThat(data).satisfies(bodyAssertion);
    }

    @NotNull
    private String resourceAbsolutePath(String filename) {
        return System.getProperty("user.dir") + separator + "build" + separator + "resources" + separator + "test" + separator + filename;
    }

    public static class Builder extends Participant.Builder<TransferEndToEndParticipant, Builder> {

        protected Builder() {
            super(new TransferEndToEndParticipant());
        }

        public static Builder newInstance() {
            return new Builder();
        }

    }

}

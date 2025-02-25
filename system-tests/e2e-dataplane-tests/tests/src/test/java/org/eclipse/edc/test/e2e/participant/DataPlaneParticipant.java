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

package org.eclipse.edc.test.e2e.participant;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.connector.controlplane.test.system.utils.LazySupplier;
import org.eclipse.edc.connector.controlplane.test.system.utils.Participant;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static java.io.File.separator;
import static org.eclipse.edc.util.io.Ports.getFreePort;

public class DataPlaneParticipant extends Participant {

    private final LazySupplier<URI> dataPlaneControl = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/control"));
    private final LazySupplier<URI> dataPlanePublic = new LazySupplier<>(() -> URI.create("http://localhost:" + getFreePort() + "/public"));

    private DataPlaneParticipant() {
        super();
    }

    public RequestSpecification baseControlRequest() {
        return given().baseUri(dataPlaneControl.get().toString());
    }

    public Config dataPlaneConfig() {
        return ConfigFactory.fromMap(dataPlaneConfiguration());
    }

    public Map<String, String> dataPlaneConfiguration() {
        return new HashMap<>() {
            {
                put("edc.component.id", UUID.randomUUID().toString());
                put("web.http.port", String.valueOf(getFreePort()));
                put("web.http.path", "/api");
                put("web.http.control.port", String.valueOf(dataPlaneControl.get().getPort()));
                put("web.http.control.path", dataPlaneControl.get().getPath());
                put("edc.keystore", resourceAbsolutePath("certs/cert.pfx"));
                put("edc.keystore.password", "123456");
                put("edc.dataplane.http.sink.partition.size", "1");
                put("edc.transfer.proxy.token.signer.privatekey.alias", "1");
                put("edc.transfer.proxy.token.verifier.publickey.alias", "public-key");
                put("edc.dataplane.api.public.response.baseurl", dataPlanePublic.get().resolve("/public/responseChannel").toString());
                put("edc.core.retry.retries.max", "0");
            }
        };
    }

    @NotNull
    private String resourceAbsolutePath(String filename) {
        return System.getProperty("user.dir") + separator + "build" + separator + "resources" + separator + "test" + separator + filename;
    }

    public static final class Builder extends Participant.Builder<DataPlaneParticipant, Builder> {

        private Builder() {
            super(new DataPlaneParticipant());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

    }

}

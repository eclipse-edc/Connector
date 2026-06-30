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

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.test.system.utils.Participant;
import org.eclipse.edc.junit.extensions.ComponentRuntimeContext;
import org.eclipse.edc.junit.utils.LazySupplier;

import java.net.URI;

import static io.restassured.http.ContentType.JSON;
import static org.eclipse.edc.web.spi.configuration.ApiContext.MANAGEMENT;
import static org.eclipse.edc.web.spi.configuration.ApiContext.PROTOCOL;
import static org.eclipse.edc.web.spi.configuration.ApiContext.SIGNALING;

public class TransferEndToEndParticipant extends Participant {

    private LazySupplier<URI> signalingUrl;

    protected TransferEndToEndParticipant() {
        super();
    }

    public static TransferEndToEndParticipant forContext(ComponentRuntimeContext ctx) {
        return newInstance(ctx).build();
    }

    public static TransferEndToEndParticipant.Builder newInstance(ComponentRuntimeContext ctx) {
        var id = ctx.getConfig().getString("edc.participant.id");
        return TransferEndToEndParticipant.Builder.newInstance()
                .id(id)
                .name(ctx.getName())
                .managementUrl(ctx.getEndpoint(MANAGEMENT))
                .protocolUrl(ctx.getEndpoint(PROTOCOL))
                .signalingUrl(ctx.getEndpoint(SIGNALING));
    }

    public void registerDataPlane(JsonObject dataPlaneRegistrationMessage) {
        baseManagementRequest()
                .contentType(JSON)
                .body(dataPlaneRegistrationMessage)
                .put("/dataplanes")
                .then()
                .statusCode(200);
    }

    public URI getSignalingEndpointUrl() {
        return signalingUrl.get();
    }

    public static class Builder extends Participant.Builder<TransferEndToEndParticipant, Builder> {

        protected Builder() {
            super(new TransferEndToEndParticipant());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder managementUrl(LazySupplier<URI> managementUrl) {
            participant.controlPlaneManagement = managementUrl;
            return this;
        }

        public Builder protocolUrl(LazySupplier<URI> protocolUrl) {
            participant.controlPlaneProtocol = protocolUrl;
            return this;
        }

        public Builder signalingUrl(LazySupplier<URI> signalingUrl) {
            participant.signalingUrl = signalingUrl;
            return this;
        }
    }

}

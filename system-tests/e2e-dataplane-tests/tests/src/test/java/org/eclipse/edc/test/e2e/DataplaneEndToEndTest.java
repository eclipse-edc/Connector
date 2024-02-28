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

import org.eclipse.edc.connector.dataplane.spi.Endpoint;
import org.eclipse.edc.connector.dataplane.spi.iam.PublicEndpointGeneratorService;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.test.e2e.participant.DataPlaneParticipant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;

import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

@EndToEndTest
public class DataplaneEndToEndTest {

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

    @Test
    void placeHolder() {
        // no impl yet, we don't have a DataPlaneSignalingApiController yet
        var generator = runtime.getContext().getService(PublicEndpointGeneratorService.class);
        generator.addGeneratorFunction("HttpData", dataAddress -> Endpoint.url("http//fizz.buzz.com/bar"));

        var flowMessage = DataFlowStartMessage.Builder.newInstance()
                .processId("test-processId")
                .sourceDataAddress(DataAddress.Builder.newInstance().type("HttpData").property(EDC_NAMESPACE + "baseUrl", "http://foo.bar/").build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type("HttpData").property(EDC_NAMESPACE + "baseUrl", "http://fizz.buzz").build())
                .assetId("test-asset")
                .agreementId("test-agreement")
                .build();
        var result = DATAPLANE.initiateTransfer(flowMessage);
    }
}

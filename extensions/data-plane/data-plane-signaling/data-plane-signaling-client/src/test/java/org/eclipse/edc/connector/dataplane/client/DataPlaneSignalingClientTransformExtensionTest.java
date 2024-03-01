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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowStartMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowSuspendMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowTerminateMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.to.JsonObjectToDataFlowResponseMessageTransformer;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowSuspendMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowTerminateMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneSignalingClientTransformExtensionTest {

    @Test
    void verifyTransformerRegistry(DataPlaneSignalingClientTransformExtension extension) {

        var registry = extension.signalingApiTransformerRegistry();

        assertThat(registry.transformerFor(DataFlowSuspendMessage.Builder.newInstance().build(), JsonObject.class))
                .isInstanceOf(JsonObjectFromDataFlowSuspendMessageTransformer.class);

        assertThat(registry.transformerFor(DataFlowTerminateMessage.Builder.newInstance().build(), JsonObject.class))
                .isInstanceOf(JsonObjectFromDataFlowTerminateMessageTransformer.class);

        assertThat(registry.transformerFor(startMessage(), JsonObject.class))
                .isInstanceOf(JsonObjectFromDataFlowStartMessageTransformer.class);

        assertThat(registry.transformerFor(Json.createObjectBuilder().build(), DataFlowResponseMessage.class))
                .isInstanceOf(JsonObjectToDataFlowResponseMessageTransformer.class);
    }

    private DataFlowStartMessage startMessage() {
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();
        return DataFlowStartMessage.Builder.newInstance()
                .processId("processId")
                .sourceDataAddress(dataAddress)
                .destinationDataAddress(dataAddress)
                .build();
    }
}

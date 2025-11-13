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

package org.eclipse.edc.connector.controlplane.transform.edc.transferprocess.from;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferState;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferState.TRANSFER_STATE_STATE;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferState.TRANSFER_STATE_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.mockito.Mockito.mock;

class JsonObjectFromTransferStateTransformerTest {

    private final JsonObjectFromTransferStateTransformer transformer = new JsonObjectFromTransferStateTransformer(Json.createBuilderFactory(emptyMap()));
    private final TransformerContext context = mock(TransformerContext.class);

    @Test
    void types() {
        assertThat(transformer.getInputType()).isEqualTo(TransferState.class);
        assertThat(transformer.getOutputType()).isEqualTo(JsonObject.class);
    }

    @Test
    void transform() {
        var input = new TransferState("state");

        var result = transformer.transform(input, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(TYPE)).isEqualTo(TRANSFER_STATE_TYPE);
        assertThat(result.getString(TRANSFER_STATE_STATE)).isEqualTo("state");
    }

}

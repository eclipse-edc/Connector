/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.http.testfixtures;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Okio;
import org.eclipse.edc.connector.dataplane.spi.schema.DataFlowRequestSchema;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static okhttp3.Protocol.HTTP_2;

public class TestFunctions {


    private TestFunctions() {
    }

    public static DataFlowStartMessage.Builder createRequest(String type) {
        return createRequest(
                Map.of(DataFlowRequestSchema.METHOD, "GET"),
                DataAddress.Builder.newInstance().type(type).properties(emptyMap()).build(),
                DataAddress.Builder.newInstance().type(type).properties(emptyMap()).build()
        );
    }

    public static DataFlowStartMessage.Builder createRequest(Map<String, String> properties, DataAddress source, DataAddress destination) {
        return DataFlowStartMessage.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .processId(UUID.randomUUID().toString())
                .properties(properties)
                .sourceDataAddress(source)
                .flowType(FlowType.PUSH)
                .destinationDataAddress(destination);
    }

    public static Response.Builder createHttpResponse() {
        var body = ResponseBody.create("{}", MediaType.get("application/json"));
        var request = new Request.Builder().url("https://test.com").build();
        return new Response.Builder().code(200).body(body).request(request).protocol(HTTP_2).message("");
    }

    /**
     * Extract body from the {@link RequestBody} and format is a string.
     *
     * @param requestBody the request body
     * @return Body formatted as a string.
     * @throws IOException if body extraction fails.
     */
    public static String formatRequestBodyAsString(RequestBody requestBody) throws IOException {
        try (var os = new ByteArrayOutputStream()) {
            var sink = Okio.sink(os);
            var bufferedSink = Okio.buffer(sink);
            requestBody.writeTo(bufferedSink);
            return os.toString();
        }
    }
}

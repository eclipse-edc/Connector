/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.consumer.command.http;

import org.eclipse.dataspaceconnector.consumer.command.CommandResult;
import org.eclipse.dataspaceconnector.consumer.command.ExecutionContext;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.eclipse.dataspaceconnector.consumer.common.Output.error;


public class HttpOperations {

    private HttpOperations() {
    }

    public static CommandResult executePost(String path, Object payload, ExecutionContext context) {
        URI uri = buildURI(context.getEndpointUrl() + path);
        var request = HttpRequest.newBuilder(uri)
                .header("Content-Type", "application/json")
                .POST(context.write(payload))
                .build();

        return executeOperation(request, context);
    }

    public static CommandResult executeDelete(String path, ExecutionContext context) {
        URI uri = buildURI(context.getEndpointUrl() + path);
        var request = HttpRequest.newBuilder(uri)
                .header("Content-Type", "application/json")
                .DELETE()
                .build();

        return executeOperation(request, context);
    }

    public static CommandResult executeGet(String path, ExecutionContext context) {
        URI uri = buildURI(context.getEndpointUrl() + path);
        var request = HttpRequest.newBuilder(uri)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return executeOperation(request, context);
    }

    @NotNull
    private static CommandResult executeOperation(HttpRequest request, ExecutionContext context) {
        HttpClient client = context.getService(HttpClient.class);
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return handleResponse(response, context);
        } catch (IOException | InterruptedException e) {
            error(e, context.getTerminal());
            return new CommandResult(true, "");
        }
    }

    @NotNull
    private static CommandResult handleResponse(HttpResponse<String> response, ExecutionContext context) throws IOException {
        String message;
        if (response.body() == null) {
            message = response.statusCode() + "";
        } else {
            message = response.body();
            if (message.length() == 0) {
                message = response.statusCode() + "";
            }
        }

        int code = response.statusCode();
        if (code != 200) {
            return new CommandResult(code >= 300, code + ":" + message);
        }
        if (message.length() < 10000) {
            context.getTerminal().writer().println("Response: " + message);
        }
        return new CommandResult(message);
    }

    private static URI buildURI(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new EdcException("Cannot parse URI " + url, e);
        }
    }
}

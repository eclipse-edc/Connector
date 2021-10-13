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

package org.eclipse.dataspaceconnector.ids.core.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.QueryMessageBuilder;
import de.fraunhofer.iais.eis.TokenFormat;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.message.MessageContext;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.QueryRequest;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.eclipse.dataspaceconnector.common.types.Cast.cast;
import static org.eclipse.dataspaceconnector.ids.core.message.MessageFunctions.writeJsonPublisher;

public class QueryMessageSender implements IdsMessageSender<QueryRequest, List<String>> {
    private static final String JSON = "application/json";
    private static final String VERSION = "1.0";

    private final ObjectMapper mapper;
    private final Monitor monitor;
    private HttpClient httpClient;
    private final URI connectorId;

    private final IdentityService identityService;

    public QueryMessageSender(String connectorId,
                              IdentityService identityService,
                              ObjectMapper mapper,
                              Monitor monitor, HttpClient httpClient) {
        this.connectorId = URI.create(connectorId);
        this.identityService = identityService;
        this.mapper = mapper;
        this.monitor = monitor;
        this.httpClient = httpClient;
    }

    @Override
    public Class<QueryRequest> messageType() {
        return QueryRequest.class;
    }

    @Override
    public CompletableFuture<List<String>> send(QueryRequest queryRequest, MessageContext context) {
        var connectorId = queryRequest.getConnectorId();

        var tokenResult = identityService.obtainClientCredentials(connectorId);

        DynamicAttributeToken token = new DynamicAttributeTokenBuilder()._tokenFormat_(TokenFormat.JWT)._tokenValue_(tokenResult.getToken()).build();

        var queryMessage = new QueryMessageBuilder()
                // FIXME handle timezone issue ._issued_(gregorianNow())
                ._modelVersion_(VERSION)
                ._securityToken_(token)
                ._issuerConnector_(this.connectorId)
                //._queryLanguage_(queryRequest.getQueryLanguage())  // TODO report that this type should not be an Enum
                .build();
        queryMessage.setProperty("query", queryRequest.getQuery());
        queryMessage.setProperty("queryLanguage", queryRequest.getQueryLanguage());

        var connectorAddress = queryRequest.getConnectorAddress();
        if (connectorAddress == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Connector address not specified"));
        }

        URI uri = buildUri(connectorAddress + "/api/ids/query");
        HttpRequest.BodyPublisher bodyPublisher = writeJsonPublisher(queryMessage, mapper);

        HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                .header("Content-Type", JSON)
                .POST(bodyPublisher)
                .build();

        return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() <= 299) {
                        String body = response.body();
                        if (body == null) {
                            throw new EdcException("Received an empty body response from connector");
                        } else {
                            try {
                                return cast(mapper.readValue(body, List.class));
                            } catch (Exception e) {
                                throw new EdcException(e);
                            }
                        }
                    } else {
                        if (response.statusCode() == 403) {
                            throw new EdcException("Received not authorized from connector");
                        } else {
                            throw new EdcException("Received an error from connector: " + response.statusCode());
                        }
                    }
                });
    }

    @NotNull
    private URI buildUri(String str) {
        try {
            return new URI(str);
        } catch (URISyntaxException e) {
            throw new EdcException("URI " + str + " is not valid");
        }
    }
}

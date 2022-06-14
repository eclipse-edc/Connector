/*
 *  Copyright (c) 2021 - 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Fraunhofer Institute for Software and Systems Engineering - Improvements
 *       Microsoft Corporation - Use IDS Webhook address for JWT audience claim
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.controller;

import de.fraunhofer.iais.eis.Connector;
import de.fraunhofer.iais.eis.Message;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.Handler;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.serializer.jsonld.JsonldSerializer;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.malformedMessage;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.messageTypeNotSupported;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.notAuthenticated;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.notFound;

@Consumes({MediaType.MULTIPART_FORM_DATA})
@Produces({MediaType.MULTIPART_FORM_DATA})
@Path(MultipartController.PATH)
public class MultipartController {
    public static final String PATH = "/data";
    private static final String HEADER = "header";
    private static final String PAYLOAD = "payload";

    private final Monitor monitor;
    private final String connectorId;
    private final List<Handler> multipartHandlers;
    private final JsonldSerializer serializer;
    private final IdentityService identityService;
    private final String idsWebhookAddress;

    public MultipartController(@NotNull Monitor monitor,
                               @NotNull String connectorId,
                               @NotNull JsonldSerializer serializer,
                               @NotNull IdentityService identityService,
                               @NotNull List<Handler> multipartHandlers,
                               @NotNull String idsWebhookAddress) {
        this.monitor = Objects.requireNonNull(monitor);
        this.connectorId = Objects.requireNonNull(connectorId);
        this.serializer = Objects.requireNonNull(serializer);
        this.multipartHandlers = Objects.requireNonNull(multipartHandlers);
        this.identityService = Objects.requireNonNull(identityService);
        this.idsWebhookAddress = Objects.requireNonNull(idsWebhookAddress);
    }

    @POST
    public Response request(@FormDataParam(HEADER) InputStream headerInputStream, @FormDataParam(PAYLOAD) String payload) {
        if (headerInputStream == null) {
            return Response.ok(createFormDataMultiPart(malformedMessage(null, connectorId), null)).build();
        }

        Message header;
        try {
            header = (Message) serializer.deserialize(new String(headerInputStream.readAllBytes(), StandardCharsets.UTF_8), Message.class);
        } catch (IOException e) {
            return Response.ok(createFormDataMultiPart(malformedMessage(null, connectorId), null)).build();
        }

        if (header == null) {
            return Response.ok(createFormDataMultiPart(malformedMessage(null, connectorId), null)).build();
        }

        var dynamicAttributeToken = header.getSecurityToken();
        if (dynamicAttributeToken == null || dynamicAttributeToken.getTokenValue() == null) {
            monitor.warning("MultipartController: Token is missing in header");
            return Response.ok(createFormDataMultiPart(notAuthenticated(header, connectorId), null)).build();
        }

        var additional = new HashMap<String, Object>();
        // IDS token validation requires issuerConnector and securityProfile
        additional.put("issuerConnector", header.getIssuerConnector());
        try {
            additional.put("securityProfile", ((Connector) serializer.deserialize(payload, Connector.class)).getSecurityProfile());
        } catch (Exception e) {
            // payload no connector instance, nothing to do
        }

        var tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(dynamicAttributeToken.getTokenValue())
                .additional(additional)
                .build();

        Result<ClaimToken> verificationResult = identityService.verifyJwtToken(tokenRepresentation, idsWebhookAddress);
        if (verificationResult.failed()) {
            monitor.warning(format("MultipartController: Token validation failed %s", verificationResult.getFailure().getMessages()));
            return Response.ok(createFormDataMultiPart(notAuthenticated(header, connectorId), null)).build();
        }

        var claimToken = verificationResult.getContent();
        var multipartRequest = MultipartRequest.Builder.newInstance()
                .header(header)
                .payload(payload)
                .claimToken(claimToken)
                .build();

        var handler = multipartHandlers.stream()
                .filter(h -> h.canHandle(multipartRequest))
                .findFirst()
                .orElse(null);

        if (handler == null) {
            return Response.ok(createFormDataMultiPart(messageTypeNotSupported(header, connectorId), null)).build();
        }

        var multipartResponse = handler.handleRequest(multipartRequest, claimToken);
        if (multipartResponse != null) {
            return Response.ok(createFormDataMultiPart(multipartResponse.getHeader(), multipartResponse.getPayload())).build();
        }

        return Response.ok(createFormDataMultiPart(notFound(header, connectorId), null)).build();
    }

    private FormDataMultiPart createFormDataMultiPart(Object header, Object payload) {
        var multiPart = new FormDataMultiPart();
        if (header != null) {
            try {
                multiPart.bodyPart(new FormDataBodyPart(HEADER, serializer.serialize(header), MediaType.APPLICATION_JSON_TYPE));
            } catch (IOException e) {
                throw new EdcException(e);
            }
        }

        if (payload != null) {
            try {
                multiPart.bodyPart(new FormDataBodyPart(PAYLOAD, serializer.serialize(payload), MediaType.APPLICATION_JSON_TYPE));
            } catch (IOException e) {
                throw new EdcException(e);
            }
        }

        return multiPart;
    }
}

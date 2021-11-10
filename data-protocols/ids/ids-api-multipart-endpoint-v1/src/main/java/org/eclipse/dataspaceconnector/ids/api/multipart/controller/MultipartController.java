/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.controller;

import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.RequestMessage;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.Handler;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.malformedMessage;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.messageTypeNotSupported;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.notAuthenticated;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.notAuthorized;
import static org.eclipse.dataspaceconnector.ids.api.multipart.util.RejectionMessageUtil.notFound;

@Consumes({ MediaType.MULTIPART_FORM_DATA })
@Produces({ MediaType.MULTIPART_FORM_DATA })
@Path(MultipartController.PATH)
public class MultipartController {
    public static final String PATH = "/ids/multipart";
    private static final String HEADER = "header";
    private static final String PAYLOAD = "payload";

    private final IdentityService identityService;
    private final List<Handler> multipartHandlers;
    private final String connectorId;

    public MultipartController(
            @NotNull String connectorId,
            @NotNull IdentityService identityService,
            @NotNull List<Handler> multipartHandlers) {
        this.identityService = Objects.requireNonNull(identityService);
        this.multipartHandlers = Objects.requireNonNull(multipartHandlers);
        this.connectorId = Objects.requireNonNull(connectorId);
    }

    @POST
    public Response request(
            @FormDataParam("header") RequestMessage header,
            @FormDataParam("payload") String payload) {
        if (header == null) {
            return Response.ok(
                    createFormDataMultiPart(
                            malformedMessage(null, connectorId))).build();
        }

        DynamicAttributeToken dynamicAttributeToken = header.getSecurityToken();
        if (dynamicAttributeToken == null || dynamicAttributeToken.getTokenValue() == null) {
            return Response.ok(
                    createFormDataMultiPart(
                            notAuthenticated(header, connectorId))).build();
        }

        VerificationResult verificationResult = identityService.verifyJwtToken(
                dynamicAttributeToken.getTokenValue(), null);
        if (verificationResult == null) {
            return Response.ok(
                    createFormDataMultiPart(
                            notAuthenticated(header, connectorId))).build();
        }

        if (!verificationResult.valid()) {
            return Response.ok(
                    createFormDataMultiPart(
                            notAuthorized(header, connectorId))).build();
        }

        MultipartRequest multipartRequest = MultipartRequest.Builder.newInstance()
                .header(header)
                .payload(payload)
                .verificationResult(verificationResult)
                .build();

        Handler handler = getRequestHandler(multipartRequest);
        if (handler == null) {
            return Response.ok(
                    createFormDataMultiPart(
                            messageTypeNotSupported(header, connectorId))).build();
        }

        MultipartResponse multipartResponse = handler.handleRequest(multipartRequest, verificationResult);
        if (multipartResponse != null) {
            return Response.ok(
                    createFormDataMultiPart(multipartResponse)).build();
        }

        return Response.ok(
                createFormDataMultiPart(
                        notFound(header, connectorId))).build();
    }

    private FormDataMultiPart createFormDataMultiPart(MultipartResponse multipartResponse) {
        return createFormDataMultiPart(multipartResponse.getHeader(), multipartResponse.getPayload());
    }

    private FormDataMultiPart createFormDataMultiPart(Object header) {
        return createFormDataMultiPart(header, null);
    }

    private FormDataMultiPart createFormDataMultiPart(Object header, Object payload) {
        FormDataMultiPart multiPart = new FormDataMultiPart();
        if (header != null) {
            multiPart.bodyPart(new FormDataBodyPart(HEADER, header, MediaType.APPLICATION_JSON_TYPE));
        }

        if (payload != null) {
            multiPart.bodyPart(new FormDataBodyPart(PAYLOAD, payload, MediaType.APPLICATION_JSON_TYPE));
        }

        return multiPart;
    }

    private Handler getRequestHandler(MultipartRequest multipartRequest) {
        for (Handler multipartHandler : multipartHandlers) {
            if (multipartHandler.canHandle(multipartRequest)) {
                return multipartHandler;
            }
        }

        return null;
    }
}

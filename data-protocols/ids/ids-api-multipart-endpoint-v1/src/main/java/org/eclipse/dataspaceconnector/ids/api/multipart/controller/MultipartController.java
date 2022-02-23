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
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.Handler;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.api.multipart.util.MessageFactory;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Consumes({MediaType.MULTIPART_FORM_DATA})
@Produces({MediaType.MULTIPART_FORM_DATA})
@Path(MultipartController.PATH)
public class MultipartController {
    public static final String PATH = "/ids/multipart";
    private static final String HEADER = "header";
    private static final String PAYLOAD = "payload";

    private final MessageFactory messageFactory;
    private final List<Handler> multipartHandlers;
    private final Serializer serializer;
    private final IdentityService identityService;

    public MultipartController(@NotNull MessageFactory messageFactory,
                               @NotNull Serializer serializer,
                               @NotNull IdentityService identityService,
                               @NotNull List<Handler> multipartHandlers) {
        this.messageFactory = Objects.requireNonNull(messageFactory);
        this.serializer = Objects.requireNonNull(serializer);
        this.identityService = Objects.requireNonNull(identityService);
        this.multipartHandlers = Objects.requireNonNull(multipartHandlers);
    }

    @POST
    public Response request(@FormDataParam(HEADER) String multipartHeader,
                            @FormDataParam(PAYLOAD) String payload) {

        if (multipartHeader == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Message header;
        try {
            header = serializer.deserialize(multipartHeader, Message.class);
        } catch (IOException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (header == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        DynamicAttributeToken dynamicAttributeToken = header.getSecurityToken();
        if (dynamicAttributeToken == null || dynamicAttributeToken.getTokenValue() == null) {
            return Response.ok(
                    createFormDataMultiPart(messageFactory.rejectNotAuthenticated(header))).build();
        }

        var verificationResult = identityService.verifyJwtToken(dynamicAttributeToken.getTokenValue());
        if (verificationResult == null) {
            return Response.ok(createFormDataMultiPart(messageFactory.rejectNotAuthenticated(header))).build();
        }

        if (verificationResult.failed()) {
            return Response.ok(createFormDataMultiPart(messageFactory.notAuthorized(header))).build();
        }

        MultipartRequest multipartRequest = MultipartRequest.Builder.newInstance()
                .header(header)
                .payload(payload)
                .verificationResult(verificationResult)
                .build();

        Handler handler = multipartHandlers.stream()
                .filter(h -> h.canHandle(multipartRequest))
                .findFirst()
                .orElse(null);
        if (handler == null) {
            return Response.ok(createFormDataMultiPart(messageFactory.messageTypeNotSupported(header))).build();
        }

        MultipartResponse multipartResponse = handler.handleRequest(multipartRequest, verificationResult);
        if (multipartResponse != null) {
            return Response.ok(createFormDataMultiPart(multipartResponse)).build();
        }

        return Response.ok(createFormDataMultiPart(messageFactory.rejectNotFound(header))).build();
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
            multiPart.bodyPart(new FormDataBodyPart(HEADER, toJson(header), MediaType.APPLICATION_JSON_TYPE));
        }

        if (payload != null) {
            multiPart.bodyPart(new FormDataBodyPart(PAYLOAD, toJson(payload), MediaType.APPLICATION_JSON_TYPE));
        }

        return multiPart;
    }

    private String toJson(Object object) {
        try {
            return serializer.serialize(object);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }
}

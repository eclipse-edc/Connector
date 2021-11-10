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

import de.fraunhofer.iais.eis.DescriptionRequestMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.RejectionMessage;
import de.fraunhofer.iais.eis.RejectionReason;
import de.fraunhofer.iais.eis.RequestMessage;
import de.fraunhofer.iais.eis.ResponseMessageBuilder;
import jakarta.ws.rs.core.Response;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.api.multipart.handler.Handler;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MultipartControllerTest {

    private static final String CONNECTOR_ID = "urn:connector:edc";

    private List<Handler> multipartHandlers;

    // mocks
    private IdentityService identityService;

    @BeforeEach
    public void setup() {
        multipartHandlers = new ArrayList<>();

        identityService = EasyMock.createMock(IdentityService.class);
    }

    @Test
    public void testMalformedMessageOnBodyHeaderNull() {
        // prepare
        var controller = new MultipartController(CONNECTOR_ID, identityService, multipartHandlers);

        // invoke
        var response = controller.request(null, null);

        // validate
        var rejectionMessage = extractIdsMessage(response, RejectionMessage.class);
        Assertions.assertEquals(RejectionReason.MALFORMED_MESSAGE, rejectionMessage.getRejectionReason());
    }

    @Test
    public void testNotAuthenticatedOnDatNull() {
        // prepare
        var controller = new MultipartController(CONNECTOR_ID, identityService, multipartHandlers);
        var header = new DescriptionRequestMessageBuilder()
                ._securityToken_(null)
                .build();

        // invoke
        var response = controller.request(header, null);

        // validate
        var rejectionMessage = extractIdsMessage(response, RejectionMessage.class);
        Assertions.assertEquals(RejectionReason.NOT_AUTHENTICATED, rejectionMessage.getRejectionReason());
    }

    @Test
    public void testNotAuthenticatedOnDatValueNull() {
        // prepare
        var controller = new MultipartController(CONNECTOR_ID, identityService, multipartHandlers);
        var dynamicAttributeToken = new DynamicAttributeTokenBuilder()
                ._tokenValue_(null)
                .build();
        var header = new DescriptionRequestMessageBuilder()
                ._securityToken_(dynamicAttributeToken)
                .build();
        EasyMock.replay(identityService);

        // invoke
        var response = controller.request(header, null);

        // validate
        var rejectionMessage = extractIdsMessage(response, RejectionMessage.class);
        Assertions.assertEquals(RejectionReason.NOT_AUTHENTICATED, rejectionMessage.getRejectionReason());
    }

    @Test
    public void testNotAuthenticatedOnVerificationResultNull() {
        // prepare
        var controller = new MultipartController(CONNECTOR_ID, identityService, multipartHandlers);
        var header = createValidBodyHeader();

        EasyMock.expect(identityService.verifyJwtToken("not-null", null))
                .andReturn(null);
        EasyMock.replay(identityService);

        // invoke
        var response = controller.request(header, null);

        // validate
        var rejectionMessage = extractIdsMessage(response, RejectionMessage.class);
        Assertions.assertEquals(RejectionReason.NOT_AUTHENTICATED, rejectionMessage.getRejectionReason());
    }


    @Test
    public void testNotAuthorizedOnVerificationResultInvalid() {
        // prepare
        var controller = new MultipartController(CONNECTOR_ID, identityService, multipartHandlers);
        var header = createValidBodyHeader();

        EasyMock.expect(identityService.verifyJwtToken("not-null", null))
                .andReturn(new VerificationResult("error"));
        EasyMock.replay(identityService);

        // invoke
        var response = controller.request(header, null);

        // validate
        var rejectionMessage = extractIdsMessage(response, RejectionMessage.class);
        Assertions.assertEquals(RejectionReason.NOT_AUTHORIZED, rejectionMessage.getRejectionReason());
    }

    @Test
    public void testNotSupportedOnNoHandlerFound() {
        // prepare
        var controller = new MultipartController(CONNECTOR_ID, identityService, multipartHandlers);
        var header = createValidBodyHeader();

        EasyMock.expect(identityService.verifyJwtToken("not-null", null))
                .andReturn(new VerificationResult(ClaimToken.Builder.newInstance().build()));
        EasyMock.replay(identityService);

        // invoke
        var response = controller.request(header, null);

        // validate
        var rejectionMessage = extractIdsMessage(response, RejectionMessage.class);
        Assertions.assertEquals(RejectionReason.MESSAGE_TYPE_NOT_SUPPORTED, rejectionMessage.getRejectionReason());
    }

    @Test
    public void testReturnsHandlerResponse() {
        // prepare
        var controller = new MultipartController(CONNECTOR_ID, identityService, multipartHandlers);
        var header = createValidBodyHeader();
        var expectedPayload = "helloPayload";

        multipartHandlers.add(new MyHandler(expectedPayload));

        EasyMock.expect(identityService.verifyJwtToken("not-null", null))
                .andReturn(new VerificationResult(ClaimToken.Builder.newInstance().build()));
        EasyMock.replay(identityService);

        // invoke
        var response = controller.request(header, null);

        // validate
        var payload = extractPayload(response, String.class);
        Assertions.assertEquals(expectedPayload, payload);
    }


    @Test
    public void testReturnsNotFoundOnHandlerResponseEmpty() {
        // prepare
        var controller = new MultipartController(CONNECTOR_ID, identityService, multipartHandlers);
        var header = createValidBodyHeader();
        multipartHandlers.add(new NullHandler());

        EasyMock.expect(identityService.verifyJwtToken("not-null", null))
                .andReturn(new VerificationResult(ClaimToken.Builder.newInstance().build()));
        EasyMock.replay(identityService);

        // invoke
        var response = controller.request(header, null);

        // validate
        var rejectionMessage = extractIdsMessage(response, RejectionMessage.class);
        Assertions.assertEquals(RejectionReason.NOT_FOUND, rejectionMessage.getRejectionReason());
    }

    @SuppressWarnings("unused")
    private <T> T extractIdsMessage(Response response, Class<T> c) {
        var body = (FormDataMultiPart) Objects.requireNonNull(response.getEntity());
        var bodyHeader = body.getField("header");
        //noinspection unchecked
        return (T) bodyHeader.getEntity();
    }

    @SuppressWarnings("unused")
    private <T> T extractPayload(Response response, Class<T> c) {
        var body = (FormDataMultiPart) Objects.requireNonNull(response.getEntity());
        var bodyHeader = body.getField("payload");
        //noinspection unchecked
        return (T) bodyHeader.getEntity();
    }

    private RequestMessage createValidBodyHeader() {
        var dynamicAttributeToken = new DynamicAttributeTokenBuilder()
                ._tokenValue_("not-null")
                .build();
        return new DescriptionRequestMessageBuilder()
                ._securityToken_(dynamicAttributeToken)
                .build();
    }

    private static final class MyHandler implements Handler {

        private final Message message;
        private final Object payload;

        public MyHandler(Object payload) {
            this.message = new ResponseMessageBuilder().build();
            this.payload = payload;
        }

        @Override
        public boolean canHandle(@NotNull MultipartRequest multipartRequest) {
            return true;
        }

        @Override
        public @Nullable MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest, @NotNull VerificationResult verificationResult) {
            return MultipartResponse.Builder.newInstance().header(message).payload(payload).build();
        }
    }

    private static final class NullHandler implements Handler {

        @Override
        public boolean canHandle(@NotNull MultipartRequest multipartRequest) {
            return true;
        }

        @Override
        public @Nullable MultipartResponse handleRequest(@NotNull MultipartRequest multipartRequest, @NotNull VerificationResult verificationResult) {
            return null;
        }
    }

}

/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.protocol.ids.api.multipart.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.RejectionMessage;
import de.fraunhofer.iais.eis.RequestMessageBuilder;
import de.fraunhofer.iais.eis.ResponseMessageBuilder;
import de.fraunhofer.iais.eis.TokenFormat;
import org.eclipse.edc.protocol.ids.api.multipart.handler.Handler;
import org.eclipse.edc.protocol.ids.api.multipart.message.MultipartResponse;
import org.eclipse.edc.protocol.ids.spi.service.DynamicAttributeTokenService;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.ids.serialization.IdsTypeManagerUtil.customizeTypeManager;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultipartControllerTest {

    private final DynamicAttributeTokenService tokenService = mock(DynamicAttributeTokenService.class);
    private final Handler handler = mock(Handler.class);
    private ObjectMapper objectMapper;
    private MultipartController controller;

    @BeforeEach
    void setUp() {
        var typeManager = new TypeManager();
        customizeTypeManager(typeManager); // TODO: api could be improved?
        objectMapper = typeManager.getMapper("ids");

        controller = new MultipartController(mock(Monitor.class), mock(IdsId.class), objectMapper, tokenService, List.of(handler), "any");
    }

    @Test
    void shouldReturnSecurityToken_whenRequestIsAuthenticated() throws JsonProcessingException {
        when(tokenService.verifyDynamicAttributeToken(any(), any(), any()))
                .thenReturn(Result.success(ClaimToken.Builder.newInstance().build()));
        when(handler.canHandle(any())).thenReturn(true);
        when(handler.handleRequest(any())).thenReturn(MultipartResponse.Builder.newInstance()
                .header(new ResponseMessageBuilder().build())
                .build());
        var requestHeader = new RequestMessageBuilder(URI.create("id"))
                ._issuerConnector_(URI.create("issuer:connector"))
                ._senderAgent_(URI.create("sender:agent"))
                ._securityToken_(new DynamicAttributeTokenBuilder()._tokenValue_("token").build())
                ._recipientConnector_(URI.create("recipient:connector"))
                .build();
        byte[] bytes = objectMapper.writeValueAsBytes(requestHeader);

        var response = controller.request(new ByteArrayInputStream(bytes), "");

        var header = extractHeader(response);
        assertThat(header.getSecurityToken()).isNotNull();
    }

    @Test
    void shouldReturnSecurityToken_whenMessageTypeIsNotSupported() throws JsonProcessingException {
        when(tokenService.verifyDynamicAttributeToken(any(), any(), any()))
                .thenReturn(Result.success(ClaimToken.Builder.newInstance().build()));
        when(handler.canHandle(any())).thenReturn(false);
        when(tokenService.obtainDynamicAttributeToken(any())).thenReturn(Result.success(new DynamicAttributeTokenBuilder()
                ._tokenValue_("token")
                ._tokenFormat_(TokenFormat.JWT)
                .build()));
        var requestHeader = new RequestMessageBuilder(URI.create("id"))
                ._issuerConnector_(URI.create("issuer:connector"))
                ._senderAgent_(URI.create("sender:agent"))
                ._securityToken_(new DynamicAttributeTokenBuilder()._tokenValue_("token").build())
                ._recipientConnector_(URI.create("recipient:connector"))
                .build();
        byte[] bytes = objectMapper.writeValueAsBytes(requestHeader);

        var response = controller.request(new ByteArrayInputStream(bytes), "");

        var header = extractHeader(response);
        assertThat(header).isInstanceOf(RejectionMessage.class);
        assertThat(header.getSecurityToken()).isNotNull();
    }

    @Test
    void shouldNotReturnSecurityToken_whenRequestBodyIsMalformed() {
        var response = controller.request(new ByteArrayInputStream("any".getBytes()), "");

        var header = extractHeader(response);
        assertThat(header).isInstanceOf(RejectionMessage.class);
        assertThat(header.getSecurityToken()).isNull();
    }

    @Test
    void shouldNotReturnSecurityToken_whenRequestIsNotAuthenticated() throws JsonProcessingException {
        when(tokenService.verifyDynamicAttributeToken(any(), any(), any()))
                .thenReturn(Result.failure("request not authenticated"));
        var requestHeader = new RequestMessageBuilder(URI.create("id"))
                ._issuerConnector_(URI.create("issuer:connector"))
                ._senderAgent_(URI.create("sender:agent"))
                ._securityToken_(new DynamicAttributeTokenBuilder()._tokenValue_("token").build())
                .build();
        byte[] bytes = objectMapper.writeValueAsBytes(requestHeader);

        var response = controller.request(new ByteArrayInputStream(bytes), "");

        var header = extractHeader(response);
        assertThat(header).isInstanceOf(RejectionMessage.class);
        assertThat(header.getSecurityToken()).isNull();
    }

    private Message extractHeader(FormDataMultiPart multiPart) {
        var header = multiPart.getField("header");
        var entity = (byte[]) header.getEntity();
        try {
            return objectMapper.readValue(entity, Message.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

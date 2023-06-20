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

package org.eclipse.edc.protocol.dsp;

import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenDecorator;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DspHttpCoreExtensionTest {

    private DspHttpCoreExtension extension;

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(RemoteMessageDispatcherRegistry.class, mock(RemoteMessageDispatcherRegistry.class));

    }

    @Test
    @DisplayName("Assert usage of the default (noop) token decorator")
    void createDispatcher_noTokenDecorator_shouldUseNoop(ServiceExtensionContext context, ObjectFactory factory) {
        var isMock = mock(IdentityService.class);
        when(isMock.obtainClientCredentials(any())).thenReturn(Result.failure("not-important"));
        context.registerService(IdentityService.class, isMock);
        context.registerService(TokenDecorator.class, null);


        extension = factory.constructInstance(DspHttpCoreExtension.class);
        var dispatcher = extension.dspHttpRemoteMessageDispatcher(context);
        dispatcher.registerDelegate(new TestMessageDelegate());
        dispatcher.dispatch(String.class, new TestMessage());

        verify(isMock).obtainClientCredentials(argThat(tokenParams -> tokenParams.getScope() == null));
    }

    @Test
    @DisplayName("Assert usage of an injected TokenDecorator")
    void createDispatcher_withTokenDecorator_shouldUse(ServiceExtensionContext context, ObjectFactory factory) {
        var isMock = mock(IdentityService.class);
        when(isMock.obtainClientCredentials(any())).thenReturn(Result.failure("not-important"));
        context.registerService(IdentityService.class, isMock);
        context.registerService(TokenDecorator.class, (td) -> td.scope("test-scope"));


        extension = factory.constructInstance(DspHttpCoreExtension.class);
        var dispatcher = extension.dspHttpRemoteMessageDispatcher(context);
        dispatcher.registerDelegate(new TestMessageDelegate());
        dispatcher.dispatch(String.class, new TestMessage());

        verify(isMock).obtainClientCredentials(argThat(tokenParams -> tokenParams.getScope().equals("test-scope")));
    }

    private static class TestMessage implements RemoteMessage {
        @Override
        public String getProtocol() {
            return null;
        }

        @Override
        public String getCounterPartyAddress() {
            return "http://connector";
        }
    }

    private static class TestMessageDelegate extends DspHttpDispatcherDelegate<TestMessage, String> {
        protected TestMessageDelegate() {
            super(null);
        }

        @Override
        public Class<TestMessage> getMessageType() {
            return TestMessage.class;
        }

        @Override
        public Request buildRequest(TestMessage message) {
            return null;
        }

        @Override
        public Function<Response, String> parseResponse() {
            return null;
        }
    }
}

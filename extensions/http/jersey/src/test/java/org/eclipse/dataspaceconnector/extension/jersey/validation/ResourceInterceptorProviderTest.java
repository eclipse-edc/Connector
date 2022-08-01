/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.extension.jersey.validation;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.validation.InterceptorFunction;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.model.MethodHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceInterceptorProviderTest {

    private final ResourceInterceptorProvider provider = new ResourceInterceptorProvider();

    @BeforeEach
    void setUp() {
    }

    @Test
    void create_noFunctionsBound() {
        assertThat(provider.create(invocable())).isNull();
    }

    @Test
    void create_onlyGlobal() {
        InterceptorFunction globalFct = (obj) -> Result.success();
        provider.addFunction(globalFct);
        var handler = provider.create(invocable());

        assertThat(handler).isInstanceOf(ResourceInterceptor.class);
        assertThat(handler).hasFieldOrPropertyWithValue("interceptorFunctions", List.of(globalFct));
    }

    @Test
    void create_onlyMethodBound() {
        InterceptorFunction methodFct = (obj) -> Result.success();
        var invocable = invocable();
        provider.addFunction(invocable.getDefinitionMethod(), methodFct);
        var handler = provider.create(invocable);

        assertThat(handler).isInstanceOf(ResourceInterceptor.class);
        assertThat(handler).hasFieldOrPropertyWithValue("interceptorFunctions", List.of(methodFct));
    }

    @Test
    void create_onlyTypeBound() {
        InterceptorFunction typeFct = (obj) -> Result.success();
        provider.addFunction(Integer.class, typeFct);
        var handler = provider.create(invocable());

        assertThat(handler).isInstanceOf(ResourceInterceptor.class);
        assertThat(handler).hasFieldOrPropertyWithValue("interceptorFunctions", List.of(typeFct));
    }

    @Test
    void create_verifySequence() {
        InterceptorFunction methodFct = (obj) -> Result.success();
        InterceptorFunction typeFct = (obj) -> Result.success();
        InterceptorFunction globalFct = (obj) -> Result.success();

        var invocable = invocable();
        provider.addFunction(globalFct);
        provider.addFunction(Integer.class, typeFct);
        provider.addFunction(invocable.getDefinitionMethod(), methodFct);

        var handler = provider.create(invocable);

        assertThat(handler).isInstanceOf(ResourceInterceptor.class);
        // functions should be in correct sequence, even though they were registered differently
        assertThat(handler).hasFieldOrPropertyWithValue("interceptorFunctions", List.of(methodFct, typeFct, globalFct));
    }

    private Invocable invocable() {
        return Invocable.create(new DummyMethodHandler(), TestObject.getMethodWithArg());
    }

    private static class DummyMethodHandler extends MethodHandler {
        @Override
        public Class<?> getHandlerClass() {
            return getClass();
        }

        @Override
        public Object getInstance(InjectionManager injectionManager) {
            return null;
        }

        @Override
        public boolean isClassBased() {
            return true;
        }

        @Override
        protected Object getHandlerInstance() {
            return this;
        }
    }
}

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

package org.eclipse.dataspaceconnector.ids.core.transform;

import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies registry dispatching and problem reporting.
 */
class IdsTransformerRegistryImplTest {
    private IdsTransformerRegistryImpl registry;

    @Test
    void verifyDispatch() {
        var fooBarTransformer = createMock(Foo.class, Bar.class);
        when(fooBarTransformer.transform(isA(Foo.class), isA(TransformerContext.class))).thenReturn(new Bar());

        var fooBazTransformer = createMock(Foo.class, Baz.class);

        registry.register(fooBarTransformer);
        registry.register(fooBazTransformer);

        assertNotNull(registry.transform(new Foo(), Bar.class).getContent());
        verify(fooBarTransformer).transform(isA(Foo.class), isA(TransformerContext.class));
    }

    @Test
    void verifyProblems() {
        var fooBarTransformer = createMock(Foo.class, Bar.class);
        when(fooBarTransformer.transform(isA(Foo.class), isA(TransformerContext.class))).thenAnswer(invocation -> {
            TransformerContext context = invocation.getArgument(1);
            context.reportProblem("problem");
            return null;
        });

        registry.register(fooBarTransformer);

        var result = registry.transform(new Foo(), Bar.class);

        assertTrue(result.failed());
        verify(fooBarTransformer).transform(isA(Foo.class), isA(TransformerContext.class));
    }

    @BeforeEach
    void setUp() {
        registry = new IdsTransformerRegistryImpl();
    }

    private <INPUT, OUTPUT> IdsTypeTransformer<INPUT, OUTPUT> createMock(Class<INPUT> input, Class<OUTPUT> output) {
        IdsTypeTransformer<INPUT, OUTPUT> transformer = mock(IdsTypeTransformer.class);
        when(transformer.getInputType()).thenReturn(input);
        when(transformer.getOutputType()).thenReturn(output);
        return transformer;
    }

    private static class Foo {

    }

    private static class Bar {

    }

    private static class Baz {

    }

}

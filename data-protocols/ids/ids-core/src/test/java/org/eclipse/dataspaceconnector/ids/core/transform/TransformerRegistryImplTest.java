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

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
class TransformerRegistryImplTest {
    private TransformerRegistryImpl registry;

    @Test
    void verifyDispatch() {
        var fooBarTransformer = createMock(Foo.class, Bar.class);
        EasyMock.expect(fooBarTransformer.transform(EasyMock.isA(Foo.class), EasyMock.isA(TransformerContext.class))).andReturn(new Bar());

        var fooBazTransformer = createMock(Foo.class, Baz.class);

        EasyMock.replay(fooBarTransformer, fooBazTransformer);

        registry.register(fooBarTransformer);
        registry.register(fooBazTransformer);

        assertNotNull(registry.transform(new Foo(), Bar.class).getOutput());

        EasyMock.verify(fooBarTransformer, fooBazTransformer);
    }


    @Test
    void verifyProblems() {
        var fooBarTransformer = createMock(Foo.class, Bar.class);
        EasyMock.expect(fooBarTransformer.transform(EasyMock.isA(Foo.class), EasyMock.isA(TransformerContext.class))).andStubAnswer(()->{
          TransformerContext context = EasyMock.getCurrentArgument(1);
          context.reportProblem("problem");
          return null;
        });

        EasyMock.replay(fooBarTransformer);

        registry.register(fooBarTransformer);


        var result = registry.transform(new Foo(), Bar.class);
        assertNull(result.getOutput());
        assertTrue(result.hasProblems());

        EasyMock.verify(fooBarTransformer);
    }


    @BeforeEach
    void setUp() {
        registry = new TransformerRegistryImpl();
    }

    private <INPUT, OUTPUT> IdsTypeTransformer<INPUT, OUTPUT> createMock(Class<INPUT> input, Class<OUTPUT> output) {
        IdsTypeTransformer<INPUT, OUTPUT> transformer = EasyMock.createMock(IdsTypeTransformer.class);
        EasyMock.expect(transformer.getInputType()).andReturn(input);
        EasyMock.expect(transformer.getOutputType()).andReturn(output);
        return transformer;
    }

    private static class Foo {

    }

    private static class Bar {

    }

    private static class Baz {

    }

}

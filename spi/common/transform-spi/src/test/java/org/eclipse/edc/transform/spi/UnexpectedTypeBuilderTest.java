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

package org.eclipse.edc.transform.spi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UnexpectedTypeBuilderTest {
    private TransformerContext context;
    private UnexpectedTypeBuilder builder;

    @BeforeEach
    void setUp() {
        context = mock(TransformerContext.class);
        builder = new UnexpectedTypeBuilder(context);
    }

    @Test
    void verify_reportNoData() {
        builder.report();
        verify(context).reportProblem(eq("Value was not of the expected type but was: unknown"));
    }

    @Test
    void verify_report() {
        builder.type("test").property("property").expected(String.class).actual(Integer.class).report();
        verify(context).reportProblem(eq("test property 'property' must be java.lang.String but was: java.lang.Integer"));
    }

    @Test
    void verify_reportMultipleExpected() {
        builder.type("test").property("property").expected(String.class).expected(Long.class).actual(Integer.class).report();
        verify(context).reportProblem(eq("test property 'property' must be java.lang.String or java.lang.Long but was: java.lang.Integer"));
    }

    @Test
    void verify_reportNoType() {
        builder.type("test").type("test").expected(String.class).actual(Integer.class).report();
        verify(context).reportProblem(eq("test must be java.lang.String but was: java.lang.Integer"));
    }

    @Test
    void verify_reportNoProperty() {
        builder.type("test").expected(String.class).actual(Integer.class).report();
        verify(context).reportProblem(eq("test must be java.lang.String but was: java.lang.Integer"));
    }

    @Test
    void verify_reportNoTypeNoProperty() {
        builder.expected(String.class).actual(Integer.class).report();
        verify(context).reportProblem(eq("Value must be java.lang.String but was: java.lang.Integer"));
    }

}

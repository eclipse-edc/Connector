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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractProblemBuilderTestBuilder {
    private AbstractProblemBuilder<?> builder;

    @BeforeEach
    void setUp() {
        builder = new AbstractProblemBuilder<>() {
            @Override
            public void report() {

            }
        };
    }

    @Test
    void verifyConcatList() {
        assertThat(builder.concatList(List.of("one"))).isEqualTo("one");
        assertThat(builder.concatList(List.of("one", "two"))).isEqualTo("one or two");
        assertThat(builder.concatList(List.of("one", "two", "three"))).isEqualTo("one, two, or three");
    }

}

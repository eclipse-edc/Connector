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

package org.eclipse.dataspaceconnector.tooling.module.processor.compiler;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.lang.model.element.VariableElement;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ElementFunctionsTest {
    @Test
    void verify() {
        var element = Mockito.mock(VariableElement.class);

        when(element.asType()).thenReturn(new MockTypeMirror("foo.Bar"));
        when(element.accept(any(), any())).thenAnswer(invocation -> ((VariableTypeResolver) invocation.getArguments()[0]).visitVariable(element, null));
        assertThat(ElementFunctions.typeFor(element)).isEqualTo("foo.Bar");
    }

}

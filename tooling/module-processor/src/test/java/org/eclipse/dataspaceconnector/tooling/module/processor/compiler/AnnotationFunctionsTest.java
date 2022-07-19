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

import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnnotationFunctionsTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void validateTestMirrors() {
        var element = Mockito.mock(Element.class);
        var mirror = Mockito.mock(AnnotationMirror.class);
        var declaredType = Mockito.mock(DeclaredType.class);
        List mirrors = List.of(mirror);

        when(element.getAnnotationMirrors()).thenReturn(mirrors);
        when(mirror.getAnnotationType()).thenReturn(declaredType);
        when(declaredType.toString()).thenReturn(Provides.class.getName());

        assertThat(AnnotationFunctions.mirrorFor(Provides.class, element)).isNotNull();

        assertThat(AnnotationFunctions.mirrorFor(Inject.class, element)).isNull();

        verify(mirror, atLeastOnce()).getAnnotationType();
        verify(element, atLeastOnce()).getAnnotationMirrors();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void validateAttributeValues() {
        var elementUtils = Mockito.mock(Elements.class);
        var mirror = Mockito.mock(AnnotationMirror.class);
        var element = Mockito.mock(ExecutableElement.class);
        Map mirrors = Map.of(element, new MockBooleanValue(false));

        when(elementUtils.getElementValuesWithDefaults(mirror)).thenReturn(mirrors);
        when(element.toString()).thenReturn("required()");

        assertThat(AnnotationFunctions.attributeValue(Boolean.class, "required", mirror, elementUtils)).isEqualTo(false);


        verify(elementUtils, atLeastOnce()).getElementValuesWithDefaults(any());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void validateAttributeStringValues() {
        var elementUtils = Mockito.mock(Elements.class);
        var mirror = Mockito.mock(AnnotationMirror.class);
        var element = Mockito.mock(ExecutableElement.class);
        Map mirrors = Map.of(element, new MockCollectionValues(List.of(new MockStringValue("foo"))));

        when(elementUtils.getElementValuesWithDefaults(mirror)).thenReturn(mirrors);
        when(element.toString()).thenReturn("value()");

        assertThat(AnnotationFunctions.attributeStringValues("value", mirror, elementUtils)).isEqualTo(List.of("foo"));


        verify(elementUtils, atLeastOnce()).getElementValuesWithDefaults(any());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void validateAttributeTypeValues() {
        var elementUtils = Mockito.mock(Elements.class);
        var mirror = Mockito.mock(AnnotationMirror.class);
        var element = Mockito.mock(ExecutableElement.class);
        Map mirrors = Map.of(element, new MockCollectionValues(List.of(new MockTypeValue(new MockTypeMirror("foo.Bar")))));

        when(elementUtils.getElementValuesWithDefaults(mirror)).thenReturn(mirrors);
        when(element.toString()).thenReturn("value()");

        assertThat(AnnotationFunctions.attributeTypeValues("value", mirror, elementUtils)).isEqualTo(List.of("foo.Bar"));

        verify(elementUtils, atLeastOnce()).getElementValuesWithDefaults(any());
    }

}

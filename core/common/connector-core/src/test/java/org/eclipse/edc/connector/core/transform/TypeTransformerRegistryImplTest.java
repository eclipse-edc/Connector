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

package org.eclipse.edc.connector.core.transform;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class TypeTransformerRegistryImplTest {

    private final TypeTransformerRegistry registry = new TypeTransformerRegistryImpl();

    @BeforeEach
    void setUp() {
        registry.register(new StringIntegerTypeTransformer());
    }

    @Test
    void transformerFor_shouldReturnTheCorrectTransformer() {
        var transformer = registry.transformerFor("a string", Integer.class);

        assertThat(transformer).isInstanceOf(StringIntegerTypeTransformer.class);
    }

    @Test
    void transformerFor_shouldThrowExceptionWhenTransformerDoesNotExist() {
        var notString = 4L;
        assertThatThrownBy(() -> registry.transformerFor(notString, Integer.class)).isInstanceOf(EdcException.class);
        assertThatThrownBy(() -> registry.transformerFor(String.class, Long.class)).isInstanceOf(EdcException.class);
        assertThatThrownBy(() -> registry.transformerFor(notString, Integer.class)).isInstanceOf(EdcException.class);
        assertThatThrownBy(() -> registry.transformerFor(notString, Float.class)).isInstanceOf(EdcException.class);
    }

    @Test
    void transform_shouldSucceed_whenInputAndOutputTypesAreHandledByRegisteredTransformer() {
        var result = registry.transform("5", Integer.class);

        assertThat(result).matches(Result::succeeded).extracting(Result::getContent).isEqualTo(5);
    }

    @Test
    void transform_shouldFail_whenTransformerFails() {
        var result = registry.transform("not an integer", Integer.class);

        assertThat(result).matches(Result::failed).extracting(Result::getFailureMessages).asList().hasSize(1);
    }

    @Test
    void transform_shouldThrowException_whenTransformerIsNotFound() {
        assertThatThrownBy(() -> registry.transform(3, String.class)).isInstanceOf(EdcException.class);
    }

    @Test
    void transform_shouldThrowException_whenInputIsNull() {
        assertThatThrownBy(() -> registry.transform(null, Integer.class)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void typeAlias_whenNoneExists() {
        assertThat(registry.typeAlias("test-alias")).isNull();
        assertThat(registry.typeAlias("test-alias", String.class)).isEqualTo(String.class);
    }

    @Test
    void typeAlias_shouldThrowException_whenNull() {
        assertThat(registry.typeAlias(null)).isNull();
    }

    @Test
    void typeAlias_whenExists() {
        registry.registerTypeAlias("test-alias", String.class);
        assertThat(registry.typeAlias("test-alias")).isEqualTo(String.class);
        assertThat(registry.typeAlias("test-alias", Integer.class)).isEqualTo(String.class);
    }

}

/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.dataplane.http.spi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class HttpRequestParamsTest {

    @Test
    void verifyExceptionThrownIfBaseUrlMissing() {
        var builder = HttpRequestParams.Builder.newInstance().method("GET");

        assertThatNullPointerException().isThrownBy(builder::build);
    }

    @Test
    void verifyExceptionThrownIfMethodMissing() {
        var builder = HttpRequestParams.Builder.newInstance().baseUrl("http://any");

        assertThatNullPointerException().isThrownBy(builder::build);
    }

    @Test
    void verifyExceptionIsRaisedIfContentTypeIsNull() {
        var builder = HttpRequestParams.Builder.newInstance()
                .baseUrl("http://any")
                .method("POST")
                .contentType(null)
                .body("Test Body");

        assertThatNullPointerException().isThrownBy(builder::build);
    }


}

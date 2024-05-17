/*
 *  Copyright (c) 2024 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e.managementapi;

import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.eclipse.edc.test.e2e.managementapi.Runtimes.inMemoryRuntime;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Secret V1 endpoints end-to-end tests
 */
public class VersionApiEndToEndTest {

    abstract static class Tests extends ManagementApiEndToEndTestBase {

        Tests(EdcRuntimeExtension runtime) {
            super(runtime);
        }

        @Test
        void getVersion() {

            baseRequest()
                    .get("/version")
                    .then()
                    .statusCode(200)
                    .body(notNullValue())
                    .body(CONTEXT, hasEntry(EDC_PREFIX, EDC_NAMESPACE))
                    .body("version", equalTo("3.0.0"))
                    .body("date", notNullValue());
        }
    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        public static final EdcRuntimeExtension RUNTIME = inMemoryRuntime();

        InMemory() {
            super(RUNTIME);
        }

    }
}
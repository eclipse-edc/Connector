/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.schema;

import jakarta.json.Json;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ManagementApiSchemaValidatorProviderTest {

    private static final String SCHEMA_CONTENT = """
            {
              "$schema": "https://json-schema.org/draft/2019-09/schema",
              "type": "object",
              "required": ["name"],
              "properties": {
                "name": { "type": "string" }
              }
            }
            """;

    @Test
    void validatorFor_resolvesSchemaThroughFileUriPrefixMapping(@TempDir Path tempDir) throws Exception {
        var schemaFile = tempDir.resolve("test-schema.json");
        Files.writeString(schemaFile, SCHEMA_CONTENT);

        var provider = ManagementApiSchemaValidatorProvider.Builder.newInstance()
                .objectMapper(JacksonJsonLd::createObjectMapper)
                .prefixMapping("https://example.org/schema/", tempDir.toUri().toString())
                .build();

        var validator = provider.validatorFor("https://example.org/schema/test-schema.json");

        var valid = Json.createObjectBuilder().add("name", "asset-1").build();
        assertThat(validator.validate(valid).succeeded()).isTrue();

        var invalid = Json.createObjectBuilder().build();
        assertThat(validator.validate(invalid).succeeded()).isFalse();
    }

    @Test
    void validatorFor_resolvesSchemaDirectlyFromFileUri(@TempDir Path tempDir) throws Exception {
        var schemaFile = tempDir.resolve("direct-schema.json");
        Files.writeString(schemaFile, SCHEMA_CONTENT);

        var provider = ManagementApiSchemaValidatorProvider.Builder.newInstance()
                .objectMapper(JacksonJsonLd::createObjectMapper)
                .build();

        var validator = provider.validatorFor(schemaFile.toUri().toString());

        var valid = Json.createObjectBuilder().add("name", "asset-1").build();
        assertThat(validator.validate(valid).succeeded()).isTrue();
    }
}

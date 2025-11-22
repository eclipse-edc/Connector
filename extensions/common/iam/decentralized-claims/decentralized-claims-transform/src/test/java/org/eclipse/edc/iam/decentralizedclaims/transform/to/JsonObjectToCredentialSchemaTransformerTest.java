/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.transform.to;

import jakarta.json.Json;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSchema.CREDENTIAL_SCHEMA_ID_PROPERTY;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSchema.CREDENTIAL_SCHEMA_TYPE_PROPERTY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JsonObjectToCredentialSchemaTransformerTest {

    private final JsonObjectToCredentialSchemaTransformer transformer = new JsonObjectToCredentialSchemaTransformer();
    private final @NotNull TransformerContext context = mock();

    @Test
    void transform() {
        var jo = Json.createObjectBuilder()
                .add(CREDENTIAL_SCHEMA_ID_PROPERTY, "http://foo.bar/id")
                .add(CREDENTIAL_SCHEMA_TYPE_PROPERTY, "JsonSchemaValidator2018")
                .build();
        var result = transformer.transform(jo, context);
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo("http://foo.bar/id");
        assertThat(result.type()).isEqualTo("JsonSchemaValidator2018");
        verify(context, never()).reportProblem(any());
    }

    @Test
    void transform_typeMissing() {
        var jo = Json.createObjectBuilder()
                .add(CREDENTIAL_SCHEMA_ID_PROPERTY, "http://foo.bar/id")
                .build();
        var result = transformer.transform(jo, context);
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo("http://foo.bar/id");
        verify(context).reportProblem(anyString());
    }

    @Test
    void transform_idNotUri() {
        var jo = Json.createObjectBuilder()
                .add(CREDENTIAL_SCHEMA_ID_PROPERTY, "not a uri")
                .add(CREDENTIAL_SCHEMA_TYPE_PROPERTY, "JsonSchemaValidator2018")
                .build();
        var result = transformer.transform(jo, context);
        assertThat(result).isNotNull();
        assertThat(result.type()).isEqualTo("JsonSchemaValidator2018");
        verify(context).reportProblem(anyString());
    }

}
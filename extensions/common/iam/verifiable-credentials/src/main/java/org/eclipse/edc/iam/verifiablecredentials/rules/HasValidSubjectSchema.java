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

package org.eclipse.edc.iam.verifiablecredentials.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.CredentialValidationRule;
import org.eclipse.edc.spi.result.Result;

import java.net.URI;
import java.util.Objects;

/**
 * Performs JSON Schema Validation of the credential subjects. Every credential subject must be validated against all
 * credential schemas, and all validations must succeed in order for this rule to pass.
 */
public class HasValidSubjectSchema implements CredentialValidationRule {
    private final ObjectMapper jsonMapper;
    private final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder -> builder.enableSchemaCache(true));

    public HasValidSubjectSchema(ObjectMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }


    @Override
    public Result<Void> apply(VerifiableCredential verifiableCredential) {
        if (verifiableCredential.getCredentialSchema() == null || verifiableCredential.getCredentialSchema().isEmpty()) {
            return Result.success();
        }
        return verifiableCredential.getCredentialSchema().stream().filter(Objects::nonNull).map(schema -> {
            var schemaUrl = schema.id();
            // returns the schema using the JsonSchemaFactory. The factory does some caching internally, so there is no need to cache again
            var jsonSchema = factory.getSchema(URI.create(schemaUrl));

            // validate all subjects against the current schema
            var validationMessages = verifiableCredential.getCredentialSubject().stream()
                    .map(subject -> jsonMapper.convertValue(subject, JsonNode.class))
                    .flatMap(jsonNode -> jsonSchema.validate(jsonNode).stream())
                    .toList();
            return validationMessages.isEmpty()
                    ? Result.success()
                    : Result.failure("Error validating CredentialSubject against schema: " + validationMessages); //ValidationMessage overwrites toString()

        }).reduce(Result::merge).orElseGet(Result::success).map(i -> null);
    }

}

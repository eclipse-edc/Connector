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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - validate entire VC against JSON schema
 *
 */

package org.eclipse.edc.iam.verifiablecredentials.rules;

import com.networknt.schema.InputFormat;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialects;
import com.networknt.schema.resource.SchemaLoader;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.CredentialValidationRule;
import org.eclipse.edc.spi.result.Result;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;

/**
 * Performs JSON Schema Validation of the credential subjects. Every credential subject must be validated against all
 * credential schemas, and all validations must succeed in order for this rule to pass.
 */
public class HasValidSchema implements CredentialValidationRule {
    private final ObjectMapper jsonMapper;
    private final boolean validateOnlySubject;
    private final SchemaRegistry factory = SchemaRegistry.withDialects(List.of(Dialects.getDraft202012(), Dialects.getDraft4()),
            builder -> {
                builder.schemaLoader(SchemaLoader.Builder::fetchRemoteResources)
                        .schemaCacheEnabled(true);
            }
    );

    public HasValidSchema(ObjectMapper jsonMapper, boolean validateOnlySubject) {
        this.jsonMapper = jsonMapper;
        this.validateOnlySubject = validateOnlySubject;
    }

    @Override
    public Result<Void> apply(VerifiableCredential verifiableCredential) {
        if (verifiableCredential.getCredentialSchema() == null || verifiableCredential.getCredentialSchema().isEmpty()) {
            return Result.success();
        }

        return verifiableCredential.getCredentialSchema().stream()
                .filter(Objects::nonNull)
                .map(schema -> validate(verifiableCredential, schema.id()))
                .reduce(Result::merge)
                .orElseGet(Result::success);
    }

    private Result<Void> validate(VerifiableCredential verifiableCredential, String schemaUrl) {
        try {
            // the factory does some caching internally, so there is no need to cache again
            var jsonSchema = factory.getSchema(SchemaLocation.of(schemaUrl));

            // for backward compatibility, previously just the subjects were validated
            if (validateOnlySubject) {
                // validate all subjects against the current schema
                var validationMessages = verifiableCredential.getCredentialSubject().stream()
                        .map(jsonMapper::writeValueAsString)
                        .flatMap(json -> jsonSchema.validate(json, InputFormat.JSON).stream())
                        .toList();
                return validationMessages.isEmpty()
                        ? Result.success()
                        : Result.failure("Error validating CredentialSubject against schema: " + validationMessages);
            }

            // validate entire VC against the current schema
            var validationMessages = jsonSchema.validate(jsonMapper.writeValueAsString(verifiableCredential), InputFormat.JSON);
            return validationMessages.isEmpty()
                    ? Result.success()
                    : Result.failure("Error validating credential against schema: " + validationMessages); // ValidationMessage overwrites toString()
        } catch (RuntimeException e) {
            var message = "Cannot validate the credential against the schema '%s': %s".formatted(schemaUrl, e.getMessage());
            // the schema could not be fetched now, a later attempt may succeed
            return hasIoCause(e) ? Result.retryableFailure(message) : Result.failure(message);
        }
    }

    private boolean hasIoCause(Throwable throwable) {
        for (var cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof IOException || cause instanceof UncheckedIOException) {
                return true;
            }
        }
        return false;
    }
}

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

package org.eclipse.edc.validator.registration.spi;

import jakarta.json.JsonObject;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.validator.spi.Validator;

/**
 * Builds a {@link Validator} for a JSON object from a JSON schema identified by its {@code $id}. The schema is
 * resolved through the management API schema resolution machinery, which serves locally cached schemas before
 * falling back to the network.
 */
@ExtensionPoint
public interface SchemaValidatorFactory {

    /**
     * Returns a validator that validates a {@link JsonObject} against the schema with the given {@code $id}.
     *
     * @param schemaId the absolute schema {@code $id}/URL.
     * @return the validator.
     */
    Validator<JsonObject> validatorFor(String schemaId);

}

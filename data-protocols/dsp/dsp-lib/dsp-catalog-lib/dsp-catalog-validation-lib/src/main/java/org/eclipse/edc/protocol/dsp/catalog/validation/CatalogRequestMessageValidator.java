/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.protocol.dsp.catalog.validation;

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.TypeIs;
import org.eclipse.edc.validator.jsonobject.validators.model.QuerySpecValidator;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.protocol.dsp.spi.type.DspCatalogPropertyAndTypeNames.DSPACE_PROPERTY_FILTER_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspCatalogPropertyAndTypeNames.DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_TERM;

/**
 * Validator for {@link CatalogRequestMessageValidator} Json-LD representation
 */
public class CatalogRequestMessageValidator {

    public static Validator<JsonObject> instance(CriterionOperatorRegistry criterionOperatorRegistry, JsonLdNamespace namespace) {
        return JsonObjectValidator.newValidator()
                .verify(path -> new TypeIs(path, namespace.toIri(DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_TERM)))
                .verifyObject(namespace.toIri(DSPACE_PROPERTY_FILTER_TERM), path -> QuerySpecValidator.instance(path, criterionOperatorRegistry))
                .build();
    }
}

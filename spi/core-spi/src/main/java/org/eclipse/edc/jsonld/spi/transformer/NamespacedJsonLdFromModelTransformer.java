/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.jsonld.spi.transformer;

import org.eclipse.edc.jsonld.spi.JsonLdNamespace;

/**
 * Abstract base class for JSON-LD transformers that are aware of a specific namespace.
 * This class extends {@link JsonLdFromModelTransformer} and provides additional functionality
 * to handle namespace-specific transformations.
 *
 * @param <INPUT>  the type of the input object to be transformed
 * @param <OUTPUT> the type of the output object after transformation
 */
public abstract class NamespacedJsonLdFromModelTransformer<INPUT, OUTPUT> extends JsonLdFromModelTransformer<INPUT, OUTPUT> {

    private final JsonLdNamespace namespace;

    protected NamespacedJsonLdFromModelTransformer(Class<INPUT> input, Class<OUTPUT> output, JsonLdNamespace namespace) {
        super(input, output);
        this.namespace = namespace;
    }

    protected String forNamespace(String term) {
        return namespace.toIri(term);
    }
}

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

package org.eclipse.edc.jsonld.spi.transformer;

/**
 * Abstract base class for JSON-LD transformers that are aware of a specific namespace.
 * This class extends {@link AbstractJsonLdTransformer} and provides additional functionality
 * to handle namespace-specific transformations.
 *
 * @param <INPUT>  the type of the input object to be transformed
 * @param <OUTPUT> the type of the output object after transformation
 */
public abstract class AbstractNamespaceAwareJsonLdTransformer<INPUT, OUTPUT> extends AbstractJsonLdTransformer<INPUT, OUTPUT> {

    private final String namespace;

    protected AbstractNamespaceAwareJsonLdTransformer(Class<INPUT> input, Class<OUTPUT> output, String namespace) {
        super(input, output);
        this.namespace = namespace;
    }

    protected String forNamespace(String term) {
        return namespace + term;
    }

}

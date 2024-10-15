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
 * Base JSON-LD transformer implementation.
 */
public abstract class AbstractNamespaceAwareJsonLdTransformer<INPUT, OUTPUT> extends AbstractJsonLdTransformer<INPUT, OUTPUT> {

    private final String namespace;

    protected AbstractNamespaceAwareJsonLdTransformer(Class<INPUT> input, Class<OUTPUT> output, String namespace) {
        super(input, output);
        this.namespace = namespace;
    }

    public String forNamespace(String term) {
        return namespace + term;
    }

}

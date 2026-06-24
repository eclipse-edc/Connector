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

package org.eclipse.edc.jsonld.spi;

/**
 * Represents a namespace for terms.
 */
public record JsonLdNamespace(String namespace) {

    /**
     * Converts a given term to its IRI (Internationalized Resource Identifier) by appending it to the namespace.
     *
     * @param term the term to be converted to an IRI.
     * @return the IRI as a string.
     */
    public String toIri(String term) {
        return namespace + term;
    }
}

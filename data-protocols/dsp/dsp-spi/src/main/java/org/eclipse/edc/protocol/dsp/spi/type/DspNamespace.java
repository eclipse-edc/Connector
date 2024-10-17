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

package org.eclipse.edc.protocol.dsp.spi.type;

import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;

/**
 * Enum representing different versions of the DSP (Data Space Protocol) namespace.
 * Each version is associated with a specific namespace URI.
 */
public enum DspNamespace {
    V_08(DSPACE_SCHEMA),
    V_2024_1(DSPACE_SCHEMA);

    private final String namespace;

    DspNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Returns the namespace URI associated with the version.
     *
     * @return the namespace URI as a string.
     */
    public String namespace() {
        return namespace;
    }

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

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

package org.eclipse.edc.iam.decentralizedclaims.spi.verification;


import com.apicatalog.vc.suite.SignatureSuite;

import java.util.Collection;

public interface SignatureSuiteRegistry {
    /**
     * Registers a signature suite for its W3C identifier, as per the <a href="https://w3c-ccg.github.io/ld-cryptosuite-registry/">Linked Data Cryptographic Suite Registry</a>
     *
     * @param w3cIdentifier The W3C identifier, e.g.Ed25519Signature2018 or JsonWebSignature2020
     * @param suite         the implementation of {@link SignatureSuite}
     */
    void register(String w3cIdentifier, SignatureSuite suite);

    /**
     * Gets the implementation for a particular W3C suite identifier (<a href="https://w3c-ccg.github.io/ld-cryptosuite-registry/">Linked Data Cryptographic Suite Registry</a>)
     *
     * @param w3cIdentifier the identifier
     * @return the {@link SignatureSuite}, or {@code null} if none was registered for that ID
     */
    SignatureSuite getForId(String w3cIdentifier);


    /**
     * Retrieves a collection of all registered signature suites.
     *
     * @return a collection of {@link SignatureSuite} objects representing all registered suites
     */
    Collection<SignatureSuite> getAllSuites();
}

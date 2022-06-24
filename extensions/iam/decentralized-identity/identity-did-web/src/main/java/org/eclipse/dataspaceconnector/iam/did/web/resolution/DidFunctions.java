/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.did.web.resolution;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

/**
 * Provides methods for manipulating Web DIDs.
 */
@SuppressWarnings("HttpUrlsUsage")
class DidFunctions {
    private static final String DID_SCHEME = "did";
    private static final String DID_METHOD_PREFIX = "web:";
    private static final String HTTP_PREFIX = "http://";
    private static final String HTTPS_PREFIX = "https://";
    private static final String DID_DOCUMENT = "did.json";
    private static final String WELL_KNOWN = "/.well-known/";

    /**
     * Converts a Web DID to a URL according to the Web DID specification.
     * <p>
     * To follow the specification, the {@code useHttpsScheme} parameter must be set to {@code true}.
     * The {@code false} value can be used for integration testing.
     *
     * @param did            Decentralized identifier in {@code "did:web:..."} format
     * @param useHttpsScheme whether to create DID URLs with {@code https}, otherwise uses {@code https} scheme
     * @return DID Document URL corresponding to {@code did}
     * @throws IllegalArgumentException if {@code did} has invalid format
     * @throws NullPointerException     if {@code did} is {@code null}
     * @see <a href="https://w3c-ccg.github.io/did-method-web/#read-resolve"did:web Method Specification: Read (Resolve)</a>
     */
    static String resolveDidDocumentUrl(String did, boolean useHttpsScheme) throws IllegalArgumentException {
        Objects.requireNonNull(did, "did");
        try {
            var uri = new URI(did);
            if (!DID_SCHEME.equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException("Invalid DID scheme: " + uri.getScheme());
            }

            var part = uri.getSchemeSpecificPart();
            if (!part.startsWith(DID_METHOD_PREFIX)) {
                throw new IllegalArgumentException("Invalid DID format, the URN must specify the 'web' DID Method: " + did);
            } else if (part.endsWith(":")) {
                throw new IllegalArgumentException("Invalid DID format, the URN must not end with ':': " + did);
            }

            var prefix = useHttpsScheme ? HTTPS_PREFIX : HTTP_PREFIX;
            var identifier = new URL(prefix + part.substring(DID_METHOD_PREFIX.length()).replace(':', '/'));
            if (identifier.getPath().length() == 0) {
                return identifier + WELL_KNOWN + DID_DOCUMENT;
            } else {
                return identifier + "/" + DID_DOCUMENT;
            }
        } catch (MalformedURLException | URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }

    }
}

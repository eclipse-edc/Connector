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

/**
 * Provides methods for manipulating Web DIDs.
 */
public class DidFunctions {
    private static final String DID_SCHEME = "did";
    private static final String DID_METHOD_PREFIX = "web:";
    private static final String HTTPS_PREFIX = "https://";
    private static final String DID_DOCUMENT = "did.json";
    private static final String WELL_KNOWN = "/.well-known/";

    /**
     * Converts a DID URN to a URL according to the Web DID specification, .cf https://w3c-ccg.github.io/did-method-web/#read-resolve.
     */
    public static String keyToUrl(String didKey) throws IllegalArgumentException {
        try {
            var uri = new URI(didKey);
            if (!DID_SCHEME.equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException("Invalid DID scheme: " + uri.getScheme());
            }

            var part = uri.getSchemeSpecificPart();
            if (!part.startsWith(DID_METHOD_PREFIX)) {
                throw new IllegalArgumentException("Invalid DID format, the URN must specify the 'web' DID Method: " + didKey);
            } else if (part.endsWith(":")) {
                throw new IllegalArgumentException("Invalid DID format, the URN must not end with ':': " + didKey);
            }

            var identifier = new URL(HTTPS_PREFIX + part.substring(DID_METHOD_PREFIX.length()).replace(':', '/'));
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

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

package org.eclipse.edc.util.uri;

import java.net.URI;

public class UriUtils {
    /**
     * Compares two URIs to check if they are equal after ignoring the fragment part.
     *
     * @param u1 The first URI to compare.
     * @param u2 The second URI to compare.
     * @return {@code true} if the URIs are equal after ignoring the fragment part, {@code false} otherwise.
     */
    public static boolean equalsIgnoreFragment(URI u1, URI u2) {
        var str1 = stripFragment(u1.toString());
        var str2 = stripFragment(u2.toString());

        return str1.equals(str2);
    }

    /**
     * Removes the fragment part from a given string representation of a URI.
     *
     * @param uri The string representation of the URI.
     * @return The string with the fragment part removed, if it exists, otherwise the original string.
     */
    private static String stripFragment(String uri) {
        var ix = uri.indexOf("#");
        return ix >= 0 ? uri.substring(0, ix) : uri;
    }

}

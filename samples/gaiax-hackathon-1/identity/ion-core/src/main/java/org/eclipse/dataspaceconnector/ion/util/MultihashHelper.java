/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.ion.util;

import io.ipfs.multihash.Multihash;
import org.eclipse.dataspaceconnector.ion.IonException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Convenience class that generates Multihashes (https://github.com/multiformats/multihash) based on SHA256
 */
public class MultihashHelper {

    private static final int SHA256_HASH_CODE = 18;

    public static byte[] hash(byte[] content) {

        byte[] conventionalHash;
        try {
            conventionalHash = hashAsNonMultihash(content);
        } catch (NoSuchAlgorithmException e) {
            throw new IonException(e);
        }

        Multihash.Type sha256Type = Multihash.Type.lookup(SHA256_HASH_CODE);

        Multihash mh = new Multihash(sha256Type, conventionalHash);

        return mh.toBytes();
    }

    public static byte[] hashAsNonMultihash(byte[] content) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(content);
        return digest.digest();
    }


    public static String hashAndEncode(byte[] content) {
        var multihashBuffer = hash(content);
        var base64String = Base64.getUrlEncoder().withoutPadding().encode(multihashBuffer);
        return new String(base64String);
    }


    public static String doubleHashAndEncode(byte[] content) throws NoSuchAlgorithmException {

        var intermediateBuffer = hashAsNonMultihash(content);
        return hashAndEncode(intermediateBuffer);

    }
}

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

import java.math.BigInteger;

public class StringUtils {

    /**
     * converts a byte array to its hexadecimal representation
     */
    public static String bytesToHexString(byte[] bytes) {
        char[] HEX_ARRAY = "0123456789ABCDEF".toLowerCase().toCharArray();

        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * compares two hex strings according to their numerical rather than lexical values
     */
    public static int compareHex(String hex1, String hex2) {
        BigInteger hex1Number = new BigInteger(hex1, 16);
        BigInteger hex2Number = new BigInteger(hex2, 16);

        return hex1Number.compareTo(hex2Number);
    }

    /**
     * converts a hex string to its binary representation
     */
    public static byte[] hexStringToBytes(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    public static String encodeToHex(String input) {
        if (input.length() % 2 != 0) {
            throw new IllegalArgumentException("input must have a length % 2 == 0!");
        }
        StringBuilder bldr = new StringBuilder();
        for (int i = 0; i < input.length(); i += 2) {
            var hexNumber = input.substring(i, i + 2);
            bldr.append((char) hexTupleToAscii(hexNumber));
        }
        return bldr.toString();
    }

    public static int hexTupleToAscii(String hex) {
        if (hex.length() != 2) {
            throw new IllegalArgumentException("Hex tuples must have 2 characters, found " + hex.length());
        }

        return Integer.parseInt(hex, 16);
    }

    public static byte[] encodeToHexBytes(String input) {
        if (input.length() % 2 != 0) {
            throw new IllegalArgumentException("input must have a length % 2 == 0!");
        }

        byte[] result = new byte[input.length() / 2];
        int j = 0;
        for (int i = 0; i < input.length(); i += 2) {
            var hexStr = input.substring(i, i + 2);
            var ascii = hexTupleToAscii(hexStr);

            result[j++] = (byte) ascii;
        }

        return result;
    }
}

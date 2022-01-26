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

package org.eclipse.dataspaceconnector.common.string;

import java.util.List;
import java.util.Objects;

public class StringUtils {

    public static boolean equals(String str1, String str2) {
        return Objects.equals(str1, str2);
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean isNullOrBlank(String str) {
        return str == null || str.isBlank();
    }

    public static boolean equalsIgnoreCase(String str1, String str2) {
        return str1 == null ? str2 == null : str1.equalsIgnoreCase(str2);
    }

    public static String toString(Object nullable) {
        return nullable != null ? nullable.toString() : null;
    }

    //algorithm taken from https://www.geeksforgeeks.org/longest-common-prefix-using-sorting/
    public static String getCommonPrefix(List<String> strs) {
        int size = strs.size();

        /* if size is 0, return empty string */
        if (size == 0) {
            return "";
        }

        if (size == 1) {
            return strs.get(0);
        }

        /* sort the array of strings */
        strs.sort(String::compareTo);

        /* find the minimum length from first and last string */
        int end = Math.min(strs.get(0).length(), strs.get(size - 1).length());

        /* find the common prefix between the first and
           last string */
        int i = 0;
        while (i < end && strs.get(0).charAt(i) == strs.get(size - 1).charAt(i)) {
            i++;
        }

        return strs.get(0).substring(0, i);
    }
}

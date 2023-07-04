/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Make secret data & metadata paths configurable
 *
 */

package org.eclipse.edc.vault.hashicorp;

final class PathUtil {

    private PathUtil() {
    }

    static String trimLeadingOrEndingSlash(String path) {
        var fixedPath = path;

        if (fixedPath.startsWith("/")) fixedPath = fixedPath.substring(1);
        if (fixedPath.endsWith("/")) fixedPath = fixedPath.substring(0, fixedPath.length() - 1);

        return fixedPath;
    }
}

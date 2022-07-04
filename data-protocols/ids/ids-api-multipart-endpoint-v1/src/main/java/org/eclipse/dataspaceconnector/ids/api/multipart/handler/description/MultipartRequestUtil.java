/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.ids.api.multipart.handler.description;

import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import org.jetbrains.annotations.NotNull;

import static java.util.Optional.ofNullable;

public class MultipartRequestUtil {

    public static int getInt(@NotNull DescriptionRequestMessage descriptionRequestMessage, String propertyName, int defaultValue) {
        return ofNullable(descriptionRequestMessage.getProperties())
                .map(map -> map.get(propertyName))
                .map(v -> Integer.parseInt(v.toString()))
                .orElse(defaultValue);
    }
}

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

package org.eclipse.edc.jsonld.util;

import org.eclipse.edc.json.JsonMapperFactory;
import tools.jackson.databind.ObjectMapper;

public class JacksonJsonLd {
    private JacksonJsonLd() {
    }

    @Deprecated(since = "1.0.0")
    public static ObjectMapper createObjectMapper() {
        return JsonMapperFactory.defaultJsonMapper();
    }
}

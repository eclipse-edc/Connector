/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.api.configuration.serdes;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.ext.ContextResolver;

public class JsonLdObjectMapperProvider implements ContextResolver<ObjectMapper> {
    
    private ObjectMapper objectMapper;
    
    public JsonLdObjectMapperProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public ObjectMapper getContext(Class<?> type) {
        return objectMapper;
    }
}

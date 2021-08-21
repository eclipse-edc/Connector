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
package org.eclipse.dataspaceconnector.iam.did.hub.spi.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.eclipse.dataspaceconnector.iam.did.hub.spi.message.InterfaceType.Actions;

/**
 *
 */
class ObjectQueryTest {

    @Test
    void verifySerializeDeserialize() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        var serialized = mapper.writeValueAsString(ObjectQuery.Builder.newInstance().interfaze(Actions).type("foo").build());
        var deserialized = mapper.readValue(serialized, ObjectQuery.class);
        Assertions.assertNotNull(deserialized);
        Assertions.assertEquals(Actions, deserialized.getInterface());
    }
}

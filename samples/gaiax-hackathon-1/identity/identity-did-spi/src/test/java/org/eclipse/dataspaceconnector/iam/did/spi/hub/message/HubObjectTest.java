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
package org.eclipse.dataspaceconnector.iam.did.spi.hub.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.eclipse.dataspaceconnector.iam.did.spi.hub.message.InterfaceType.Actions;

/**
 *
 */
class HubObjectTest {

    @Test
    void verifySerializeDeserialize() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        var serialized = mapper.writeValueAsString(HubObject.Builder.newInstance().interfaze(Actions).type("foo").id("123").createdBy("ion:did:foo").sub("sub").build());
        var deserialized = mapper.readValue(serialized, HubObject.class);
        Assertions.assertNotNull(deserialized);
        Assertions.assertEquals(Actions, deserialized.getInterface());
    }

}

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

/**
 *
 */
class CommitTest {

    @Test
    void verifySerializeDeserialize() throws JsonProcessingException {
        var commit = Commit.Builder.newInstance().context("foo").type("foo").objectId("123").iss("baz").sub("quux").payload("payload").alg("RSA256").kid("kid").build();
        ObjectMapper mapper = new ObjectMapper();
        var serialized = mapper.writeValueAsString(commit);
        var deserialized =  mapper.readValue(serialized,Commit.class);
        Assertions.assertNotNull(deserialized);
        Assertions.assertEquals("123", deserialized.getObjectId());
    }
}

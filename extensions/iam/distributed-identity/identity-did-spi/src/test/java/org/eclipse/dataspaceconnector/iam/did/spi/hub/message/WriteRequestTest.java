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
class WriteRequestTest {

    @Test
    void verifySerializeDeserialize() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        var commitObject = new JsonCommitObject("1233.333.112",new CommitHeader("1"));
        var serialized = mapper.writeValueAsString(WriteRequest.Builder.newInstance().iss("iss").aud("aud").sub("sub").commit(commitObject).build());
        var deserialized =  mapper.readValue(serialized,WriteRequest.class);
        Assertions.assertNotNull(deserialized);
    }
}

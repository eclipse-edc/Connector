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
package org.eclipse.dataspaceconnector.ion.dto.did;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.ion.model.did.resolution.DidDocument;
import org.eclipse.dataspaceconnector.ion.model.did.resolution.Service;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 *
 */
class DidDocumentTest {
    private ObjectMapper objectMapper;

    @Test
    void verifySerializeDeserialize() throws JsonProcessingException {

        // Resolve ION/IdentityHub discrepancy
        var service = new Service("#domain-1", "LinkedDomains", "https://test.service.com");
        var document = DidDocument.Builder.newInstance().id("did:ion:123").service(List.of(service)).build();
        var serialized = objectMapper.writeValueAsString(document);
        var deserialized = objectMapper.readValue(serialized, DidDocument.class);
        Assertions.assertEquals("did:ion:123", deserialized.getId());
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }
}

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

package org.eclipse.dataspaceconnector.catalog.node.directory.azure.model;

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNode;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class FederatedCacheNodeDocumentSerializationTest {

    private TypeManager typeManager;

    @BeforeEach
    void setup() {
        typeManager = new TypeManager();
        typeManager.registerTypes(FederatedCacheNodeDocument.class, FederatedCacheNode.class);
    }

    @Test
    void testSerialization() {
        var node = createNode();

        var document = new FederatedCacheNodeDocument(node, "test-process");

        String s = typeManager.writeValueAsString(document);

        assertThat(s).isNotNull()
                .contains("\"name\":\"node-test\"")
                .contains("\"url\":\"http://test.com\"")
                .contains("\"partitionKey\":\"test-process\"")
                .contains("\"supportedProtocols\":[\"ids\",\"rest\"]");
    }

    @Test
    void testDeserialization() {
        var node = createNode();

        var document = new FederatedCacheNodeDocument(node, "test-process");
        String json = typeManager.writeValueAsString(document);

        var transferProcessDeserialized = typeManager.readValue(json, FederatedCacheNodeDocument.class);
        assertThat(transferProcessDeserialized).usingRecursiveComparison().isEqualTo(document);
    }

    private static FederatedCacheNode createNode() {
        return new FederatedCacheNode("node-test", "http://test.com", Arrays.asList("ids", "rest"));
    }
}

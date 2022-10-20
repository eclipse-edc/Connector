/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.azure.cosmos;

import java.util.List;

public class TestCollectionDocument extends CosmosDocument<TestCollectionDocument.TestWrappedTarget> {


    protected TestCollectionDocument(TestCollectionDocument.TestWrappedTarget wrappedInstance, String partitionKey) {
        super(wrappedInstance, partitionKey);
    }

    @Override
    public String getId() {
        return null;
    }

    public static class TestWrappedTarget {
        private TestEmbedded embedded;
    }

    static class TestEmbedded {
        private List<TestCollection> collections;
    }

    static class TestCollection {
        private String name;
        private String value;
    }
}

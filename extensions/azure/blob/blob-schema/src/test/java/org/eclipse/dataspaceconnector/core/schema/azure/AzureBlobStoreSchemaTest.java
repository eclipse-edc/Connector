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

package org.eclipse.dataspaceconnector.core.schema.azure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AzureBlobStoreSchemaTest {

    @Test
    void verifySchema() {
        var azSchema = new AzureBlobStoreSchema();

        assertThat(azSchema.getName()).isEqualTo("AzureStorage");
        assertThat(azSchema.getRequiredAttributes()).hasSize(4);
        assertThat(azSchema.getRequiredAttributes()).anyMatch(sa -> sa.getName().equals("account"));
        assertThat(azSchema.getRequiredAttributes()).anyMatch(sa -> sa.getName().equals("container"));
        assertThat(azSchema.getRequiredAttributes()).anyMatch(sa -> sa.getName().equals("type"));
        assertThat(azSchema.getRequiredAttributes()).anyMatch(sa -> sa.getName().equals("keyName"));

    }
}

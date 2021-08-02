/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.schema.azure;

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
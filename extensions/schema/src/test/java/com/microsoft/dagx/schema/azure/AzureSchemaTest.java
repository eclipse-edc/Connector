package com.microsoft.dagx.schema.azure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AzureSchemaTest {

    @Test
    void verifySchema(){
        var azSchema= new AzureSchema();

        assertThat(azSchema.getName()).isEqualTo("AzureStorage");
        assertThat(azSchema.getRequiredAttributes()).hasSize(5);
        assertThat(azSchema.getRequiredAttributes()).anyMatch(sa -> sa.getName().equals("account"));
        assertThat(azSchema.getRequiredAttributes()).anyMatch(sa -> sa.getName().equals("blobname"));
        assertThat(azSchema.getRequiredAttributes()).anyMatch(sa -> sa.getName().equals("container"));
        assertThat(azSchema.getRequiredAttributes()).anyMatch(sa -> sa.getName().equals("type"));
        assertThat(azSchema.getRequiredAttributes()).anyMatch(sa -> sa.getName().equals("keyName"));

        assertThat(azSchema.getAttributes()).isEqualTo(azSchema.getRequiredAttributes());

    }
}
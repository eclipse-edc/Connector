/*
 *  Copyright (c) 2021 Siemens AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Siemens AG - initial implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.types.domain.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenericDataCatalogEntryTest {

    @Test
    void verifyPolymorphicDeserialization() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(GenericDataCatalogEntry.class);

        final GenericDataCatalogEntry genericDataCatalogEntry = GenericDataCatalogEntry.Builder.newInstance()
                .property("type", "test-type")
                .property("keyName", "test-key")
                .property("property", "test-property")
                .build();

        DataEntry entry = DataEntry.Builder.newInstance().id("id").catalogEntry(genericDataCatalogEntry).build();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, entry);

        final DataEntry deserialized = mapper.readValue(writer.toString(), DataEntry.class);

        assertNotNull(deserialized);
        final DataCatalogEntry catalogEntry = deserialized.getCatalogEntry();

        assertTrue(catalogEntry instanceof GenericDataCatalogEntry);

        final DataAddress address = catalogEntry.getAddress();

        assertNotNull(address);
        assertEquals("test-key", address.getKeyName());
        assertEquals("test-type", address.getType());
        assertEquals("test-property", address.getProperty("property"));
    }
}
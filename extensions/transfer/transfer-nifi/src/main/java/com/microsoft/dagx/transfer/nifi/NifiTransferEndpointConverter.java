/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.nifi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.dagx.schema.Schema;
import com.microsoft.dagx.schema.SchemaRegistry;
import com.microsoft.dagx.schema.SchemaValidationException;
import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;

import java.util.Map;
import java.util.Objects;

public class NifiTransferEndpointConverter {
    private final SchemaRegistry schemaRegistry;
    private final Vault vault;

    public NifiTransferEndpointConverter(SchemaRegistry registry, Vault vault) {

        schemaRegistry = registry;
        this.vault = vault;
    }

    NifiTransferEndpoint convert(DataAddress dataAddress) {
        var type = dataAddress.getType();

        if (type == null) {
            throw new NifiTransferException("No type was specified!");
        }

        var schema = schemaRegistry.getSchema(type);
        if (schema == null) {
            throw new NifiTransferException("No schema is registered for type " + type);
        }

        validate(dataAddress, schema);


        var keyName = dataAddress.getProperties().remove("keyName");
        String key = vault.resolveSecret(keyName);

        dataAddress.getProperties().remove("type");


        Map<String, String> properties = dataAddress.getProperties();

        //different endpoints might have different credentials, such as SAS token, access key id + secret, etc.
        // this requireds that the credentials are stored as JSON-encoded Map
        if (key != null) {
            try {
                //noinspection unchecked
                Map<String, String> secret = new ObjectMapper().readValue(key, Map.class);
                properties.putAll(secret);
            } catch (JsonProcessingException e) {
                throw new DagxException(e);
            }
        }


        return NifiTransferEndpoint.NifiTransferEndpointBuilder.newInstance()
                .type(type)
                .properties(properties)
                .build();
    }

    private void validate(DataAddress dataAddress, Schema schema) {
        Objects.requireNonNull(dataAddress.getKeyName(), "DataAddress must have a keyName!");
        Objects.requireNonNull(dataAddress.getType(), "DataAddress must have a type!");

        //validate required attributes
        schema.getRequiredAttributes().forEach(requiredAttr -> {
            String name = requiredAttr.getName();

            if (dataAddress.getProperty(name) == null) {
                throw new SchemaValidationException("Required property is missing in DataAddress: " + name);
            }
        });

        //validate the types of all properties
        schema.getAttributes().forEach(attr -> {
            var type = attr.getType();
        });

    }
}

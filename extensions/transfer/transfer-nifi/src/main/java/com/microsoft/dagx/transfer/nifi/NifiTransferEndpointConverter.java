package com.microsoft.dagx.transfer.nifi;

import com.microsoft.dagx.schema.Schema;
import com.microsoft.dagx.schema.SchemaRegistry;
import com.microsoft.dagx.schema.SchemaValidationException;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;

import java.util.Objects;

public class NifiTransferEndpointConverter {
    private final SchemaRegistry schemaRegistry;
    private final Vault vault;

    public NifiTransferEndpointConverter(SchemaRegistry registry, Vault vault) {

        this.schemaRegistry = registry;
        this.vault = vault;
    }

    NifiTransferEndpoint convert(DataAddress dataAddress) {
        var type = dataAddress.getType();

        if(type == null)
            throw new NifiTransferException("No type was specified!");

        var schema = schemaRegistry.getSchema(type);
        if (schema == null)
            throw new NifiTransferException("No schema is registered for type " + type);

        validate(dataAddress, schema);

        var keyName= dataAddress.getProperties().remove("keyName");
        dataAddress.getProperties().remove("type");
        return NifiTransferEndpoint.NifiTransferEndpointBuilder.newInstance()
                .type(type)
                .key(vault.resolveSecret(keyName))
                .properties(dataAddress.getProperties())
                .build();
    }

    private void validate(DataAddress dataAddress, Schema schema) {
        Objects.requireNonNull(dataAddress.getKeyName(), "DataAddress must have a keyName!");
        Objects.requireNonNull(dataAddress.getType(), "DataAddress must have a type!");

        //validate required attributes
        schema.getRequiredAttributes().forEach(requiredAttr -> {
            String name = requiredAttr.getName();

            if(dataAddress.getProperty(name) == null)
                throw new SchemaValidationException("Required property is missing in DataAddress: " + name);
        });

        //validate the types of all properties
        schema.getAttributes().forEach(attr -> {
            var type= attr.getType();
        });

    }
}

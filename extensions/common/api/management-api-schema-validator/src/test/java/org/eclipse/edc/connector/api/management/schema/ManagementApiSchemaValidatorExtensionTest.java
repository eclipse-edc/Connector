/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.extensions.TestExtensionContext;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class ManagementApiSchemaValidatorExtensionTest {

    private final JsonObjectValidatorRegistry validatorRegistry = mock(JsonObjectValidatorRegistry.class);
    private final TypeManager typeManager = mock(TypeManager.class);

    private ManagementApiSchemaValidatorExtension extension;

    @BeforeEach
    void setUp(TestExtensionContext context, ObjectFactory factory) {
        when(typeManager.getMapper(any())).thenReturn(new ObjectMapper());
        context.registerService(JsonObjectValidatorRegistry.class, validatorRegistry);
        context.registerService(TypeManager.class, typeManager);
        extension = factory.constructInstance(ManagementApiSchemaValidatorExtension.class);
    }

    @Test
    void initialize_registersBuiltInV4Validators(ServiceExtensionContext context) {
        extension.initialize(context);

        verify(validatorRegistry, atLeastOnce()).register(eq("v4:Asset"), any());
        verify(validatorRegistry, atLeastOnce()).register(eq("v4:PolicyDefinition"), any());
        verify(validatorRegistry, atLeastOnce()).register(eq("v4:ContractDefinition"), any());
    }

    @Test
    void initialize_registersCustomValidators_composedWithBuiltIn(TestExtensionContext context) {
        context.setConfig(ConfigFactory.fromMap(Map.of(
                "edc.mgmt.api.schema.custom.version", "v4",
                "edc.mgmt.api.schema.custom.validator.policy.type", "PolicyDefinition",
                "edc.mgmt.api.schema.custom.validator.policy.schema", "https://w3id.org/edc/connector/management/schema/v4/policy-definition-schema.json"
        )));

        extension.initialize(context);

        verify(validatorRegistry, times(2)).register(eq("v4:PolicyDefinition"), any());
    }

    @Test
    void initialize_registersCustomValidators_withDistinctVersionPrefix(TestExtensionContext context) {
        context.setConfig(ConfigFactory.fromMap(Map.of(
                "edc.mgmt.api.schema.custom.version", "v6",
                "edc.mgmt.api.schema.custom.validator.asset.type", "Asset",
                "edc.mgmt.api.schema.custom.validator.asset.schema", "https://w3id.org/edc/connector/management/schema/v4/asset-schema.json"
        )));

        extension.initialize(context);

        verify(validatorRegistry).register(eq("v6:Asset"), any());
    }

    @Test
    void initialize_appliesCustomPrefixMapping(TestExtensionContext context) {
        context.setConfig(ConfigFactory.fromMap(Map.of(
                "edc.mgmt.api.schema.custom.version", "v4",
                "edc.mgmt.api.schema.custom.mapping.from", "https://example.org/schema/v4",
                "edc.mgmt.api.schema.custom.mapping.to", "classpath:schema/management/v4",
                "edc.mgmt.api.schema.custom.validator.asset.type", "Asset",
                "edc.mgmt.api.schema.custom.validator.asset.schema", "https://example.org/schema/v4/asset-schema.json"
        )));

        extension.initialize(context);

        var key = ArgumentCaptor.forClass(String.class);
        verify(validatorRegistry, atLeastOnce()).register(key.capture(), any());
        assertThat(key.getAllValues()).contains("v4:Asset");
    }

    @Test
    void initialize_throws_whenVersionMissing(TestExtensionContext context) {
        context.setConfig(ConfigFactory.fromMap(Map.of(
                "edc.mgmt.api.schema.custom.validator.policy.type", "PolicyDefinition",
                "edc.mgmt.api.schema.custom.validator.policy.schema", "https://example.org/policy.json"
        )));

        assertThatThrownBy(() -> extension.initialize(context)).isInstanceOf(EdcException.class);
    }

    @Test
    void initialize_throws_whenValidatorEntryIncomplete(TestExtensionContext context) {
        context.setConfig(ConfigFactory.fromMap(Map.of(
                "edc.mgmt.api.schema.custom.version", "v4",
                "edc.mgmt.api.schema.custom.validator.policy.type", "PolicyDefinition"
        )));

        assertThatThrownBy(() -> extension.initialize(context)).isInstanceOf(EdcException.class);
    }
}

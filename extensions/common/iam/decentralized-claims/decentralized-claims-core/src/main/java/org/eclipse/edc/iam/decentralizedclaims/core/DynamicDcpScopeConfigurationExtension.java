/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.iam.decentralizedclaims.core;

import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScopeRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;

import java.util.Map;

import static org.eclipse.edc.iam.decentralizedclaims.core.DynamicDcpScopeConfigurationExtension.NAME;


@Extension(NAME)
public class DynamicDcpScopeConfigurationExtension implements ServiceExtension {

    public static final String NAME = "DCP Dynamic Scope Configuration Extension";

    public static final String CONFIG_PREFIX = "edc.iam.dcp.scopes";

    @Configuration(context = CONFIG_PREFIX)
    private Map<String, DcpScopeConfig> scopeConfig;

    @Inject
    private DcpScopeRegistry scopeRegistry;

    @Inject
    private Monitor monitor;

    private void addScope(DcpScopeConfig config) {
        var prefixMapping = config.prefixMapping() != null ? config.prefixMapping() : config.prefixMappingLegacy();
        var scope = DcpScope.Builder.newInstance().id(config.id())
                .type(DcpScope.Type.valueOf(config.type().toUpperCase()))
                .value(config.value())
                .prefixMapping(prefixMapping)
                .profile(config.profile())
                .build();

        scopeRegistry.register(scope).orElseThrow(e -> new EdcException("Failed to register DCP scope with id " + config.id()));
    }

    @Override
    public void prepare() {
        scopeConfig.values().forEach(this::addScope);
    }

    @Settings
    private record DcpScopeConfig(
            @Setting(key = "id", description = "ID of the scope.")
            String id,
            @Setting(key = "type", description = "The type of the scope. Supported values are 'DEFAULT' and 'POLICY'.")
            String type,
            @Setting(key = "value", description = "The value of the scope.")
            String value,
            @Deprecated(since = "0.17.0")
            @Setting(key = "prefix-mapping", description = "The prefix mapping for the left operand for applying the scope. Required if type is 'POLICY (Legacy Config)", required = false)
            String prefixMappingLegacy,
            @Setting(key = "prefix.mapping", description = "The prefix mapping for the left operand for applying the scope. Required if type is 'POLICY", required = false)
            String prefixMapping,
            @Setting(key = "profile", description = "The profile this scope applies to. Use '*' to apply to all profiles.", defaultValue = "*")
            String profile
    ) {

    }

}

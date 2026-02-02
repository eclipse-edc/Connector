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
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;

import static org.eclipse.edc.iam.decentralizedclaims.core.DynamicDcpScopeConfigurationExtension.NAME;


@Extension(NAME)
public class DynamicDcpScopeConfigurationExtension implements ServiceExtension {

    public static final String NAME = "DCP Dynamic Scope Configuration Extension";

    public static final String CONFIG_PREFIX = "edc.iam.dcp.scopes";
    public static final String CONFIG_ALIAS = CONFIG_PREFIX + ".<scopeAlias>.";

    @Setting(context = CONFIG_ALIAS, description = "ID of the scope.")
    public static final String ID_SUFFIX = "id";
    @Setting(context = CONFIG_ALIAS, description = "Additional properties of the issuer.")
    public static final String TYPE_SUFFIX = "type";
    @Setting(context = CONFIG_ALIAS, description = "The value of the scope.")
    public static final String VALUE_SUFFIX = "value";

    @Setting(context = CONFIG_ALIAS, description = "Prefix mapping for the scope.")
    public static final String PREFIX_MAPPING_SUFFIX = "prefix-mapping";
    @Setting(context = CONFIG_ALIAS, description = "Profile the scope.", defaultValue = "*")
    public static final String PROFILE_SUFFIX = "profile";

    @Inject
    private DcpScopeRegistry scopeRegistry;

    @Inject
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var config = context.getConfig(CONFIG_PREFIX);
        var configs = config.partition().toList();
        configs.forEach(this::addScope);

    }

    private void addScope(Config config) {
        var id = config.getString(ID_SUFFIX);
        var type = config.getString(TYPE_SUFFIX);
        var value = config.getString(VALUE_SUFFIX);
        var prefixMapping = config.getString(PREFIX_MAPPING_SUFFIX, null);
        var profile = config.getString(PROFILE_SUFFIX, "*");

        var scope = DcpScope.Builder.newInstance().id(id)
                .type(DcpScope.Type.valueOf(type.toUpperCase()))
                .value(value)
                .prefixMapping(prefixMapping)
                .profile(profile)
                .build();

        scopeRegistry.register(scope).orElseThrow(e -> new EdcException("Failed to register DCP scope with id " + id));
    }

}

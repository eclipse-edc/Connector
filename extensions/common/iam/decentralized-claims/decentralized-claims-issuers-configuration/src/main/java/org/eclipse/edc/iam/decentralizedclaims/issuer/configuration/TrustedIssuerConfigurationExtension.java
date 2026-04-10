/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.issuer.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.SettingContext;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import java.util.List;
import java.util.Map;

import static org.eclipse.edc.iam.decentralizedclaims.issuer.configuration.TrustedIssuerConfigurationExtension.NAME;

/**
 * This DCP extension makes it possible to configure a list of trusted issuers, that will be matched against the Verifiable Credential issuers.
 */
@Extension(NAME)
public class TrustedIssuerConfigurationExtension implements ServiceExtension {

    public static final String DEPRECATED_CONFIG_PREFIX = "edc.iam.trusted-issuer";
    public static final String CONFIG_PREFIX = "edc.iam.trustedissuer";

    protected static final String NAME = "Trusted Issuers Configuration Extensions";

    @Deprecated(since = "0.17.0")
    @SettingContext(DEPRECATED_CONFIG_PREFIX)
    @Configuration
    private Map<String, TrustedIssuerConfiguration> deprecatedTrustedIssuers;
    @SettingContext(CONFIG_PREFIX)
    @Configuration
    private Map<String, TrustedIssuerConfiguration> trustedIssuers;

    @Inject
    private TrustedIssuerRegistry trustedIssuerRegistry;
    @Inject
    private TypeManager typeManager;
    @Inject
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        if (!deprecatedTrustedIssuers.isEmpty()) {
            monitor.warning("Using deprecated configuration prefix '%s'. Please switch to the new prefix '%s'".formatted(DEPRECATED_CONFIG_PREFIX, CONFIG_PREFIX));
        }

        if (trustedIssuers.isEmpty()) {
            monitor.warning("No configured trusted issuer under '%s' setting group.".formatted(CONFIG_PREFIX));
        }

        trustedIssuers.forEach(this::addIssuer);
        deprecatedTrustedIssuers.forEach(this::addIssuer);
    }

    private void addIssuer(String alias, TrustedIssuerConfiguration config) {
        var supportedTypesConfig = config.supportedTypes();
        var supportedTypes = typeManager.readValue(supportedTypesConfig, new TypeReference<List<String>>() {
        });
        var propertiesConfig = config.properties();
        var properties = typeManager.readValue(propertiesConfig, new TypeReference<Map<String, Object>>() {
        });

        supportedTypes.forEach(type -> trustedIssuerRegistry.register(new Issuer(config.id(), properties), type));
    }

    @Settings
    record TrustedIssuerConfiguration(

            @Setting(
                    key = "id",
                    description = "ID of the issuer."
            )
            String id,

            @Setting(
                    key = "supportedtypes",
                    description = "Supported credential types for this issuer, as a JSON serialized list.",
                    defaultValue = "[\"*\"]")
            String supportedTypes,

            @Setting(
                    key = "properties",
                    description = "Additional properties of the issuer, as a JSON serialized object.",
                    defaultValue = "{}")
            String properties
    ) {

    }
}

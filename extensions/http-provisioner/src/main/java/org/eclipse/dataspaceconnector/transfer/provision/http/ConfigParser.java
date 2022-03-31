/*
 *  Copyright (c) 2022 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.transfer.provision.http;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.configuration.Config;
import org.eclipse.dataspaceconnector.transfer.provision.http.ProvisionerConfiguration.ProvisionerType;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

/**
 * Parses provisioner configuration.
 *
 * Multiple named provisioners can be configured per runtime.
 */
class ConfigParser {
    private static final String DEFAULT_POLICY_SCOPE = "http.provisioner";

    private static final String CONFIG_PREFIX = "provisioner.http";

    private static final String HTTP_PROVISIONER_ENTRIES = CONFIG_PREFIX + ".entries";

    @EdcSetting(required = true)
    private static final String PROVISIONER_TYPE = "provisioner.type";

    @EdcSetting(required = true)
    private static final String DATA_ADDRESS_TYPE = "data.address.type";

    @EdcSetting(required = true)
    private static final String ENDPOINT_URL = "endpoint";

    @EdcSetting
    private static final String POLICY_SCOPE = "policy.scope";

    @EdcSetting
    private static final String CALLBACK_ADDRESS = "callback.address";

    // TODO replace
    private static final String DEFAULT_CALLBACK_ADDRESS = "http://localhost:8080";

    /**
     * Parses the runtime configuration source, returning a provisioner configuration.
     */
    public static List<ProvisionerConfiguration> parseConfigurations(Config root) {

        var callbackAddress = parseCallback(root);

        var configurations = root.getConfig(HTTP_PROVISIONER_ENTRIES);

        return configurations.partition()
                .map(config -> {
                    var provisionerName = config.currentNode();

                    var provisionerType = parseProvisionerType(config, provisionerName);

                    var endpoint = parseEndpoint(config, provisionerName);

                    var policyScope = config.getString(POLICY_SCOPE, DEFAULT_POLICY_SCOPE);

                    var dataAddressType = config.getString(DATA_ADDRESS_TYPE);

                    return ProvisionerConfiguration.Builder.newInstance()
                            .name(provisionerName)
                            .provisionerType(provisionerType)
                            .dataAddressType(dataAddressType)
                            .policyScope(policyScope)
                            .endpoint(endpoint)
                            .callbackAddress(callbackAddress)
                            .build();
                }).collect(toList());
    }

    private static URL parseCallback(Config root) {
        var callbackAddress = root.getConfig(CONFIG_PREFIX).getString(CALLBACK_ADDRESS, DEFAULT_CALLBACK_ADDRESS);
        try {
            return new URL(callbackAddress);
        } catch (MalformedURLException e) {
            throw new EdcException("Invalid callback address for HTTP provisioners", e);
        }
    }

    private static URL parseEndpoint(Config config, String provisionerName) {
        var endpoint = config.getString(ENDPOINT_URL);
        try {
            return new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new EdcException("Invalid endpoint URL for HTTP provisioner: " + provisionerName, e);
        }
    }

    private static ProvisionerType parseProvisionerType(Config config, String provisionerName) {
        var typeValue = config.getString(PROVISIONER_TYPE, ProvisionerType.PROVIDER.name());
        try {
            return ProvisionerType.valueOf(typeValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new EdcException(format("Invalid provisioner type specified for %s: %s", provisionerName, typeValue));
        }
    }

    private ConfigParser() {
    }
}

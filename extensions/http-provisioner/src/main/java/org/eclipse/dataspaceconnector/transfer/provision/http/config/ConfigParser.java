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

package org.eclipse.dataspaceconnector.transfer.provision.http.config;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.configuration.Config;
import org.eclipse.dataspaceconnector.transfer.provision.http.config.ProvisionerConfiguration.ProvisionerType;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

/**
 * Parses provisioner configuration.
 * <p>
 * Multiple named provisioners can be configured per runtime.
 */
public class ConfigParser {
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

    private ConfigParser() {
    }

    /**
     * Parses the runtime configuration source, returning a provisioner configuration.
     */
    public static List<ProvisionerConfiguration> parseConfigurations(Config root) {

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
                            .build();
                }).collect(toList());
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
}

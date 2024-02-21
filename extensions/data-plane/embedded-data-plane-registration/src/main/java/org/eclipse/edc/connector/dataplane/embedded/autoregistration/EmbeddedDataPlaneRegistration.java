/*
 *  Copyright (c) 2024 Zub4t  (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Zub4t  (BMW AG) - initial API and implementation
 *
 */
package org.eclipse.edc.connector.dataplane.embedded.autoregistration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

public class EmbeddedDataPlaneRegistration implements ServiceExtension {

    private static final String NAME = "Embedded DataPlane Auto Registration";
    private static final String COMMA = ",";
    private static final String LOG_MISSING_CONFIGURATION = NAME + ": Missing configuration for " + EmbeddedDataPlaneConfig.CONFIG_PREFIX + ".%s.%s";
    private static final String LOG_SKIP_BC_MISSING_CONFIGURATION = NAME + ": Configuration issues. Skip registering of Data Plane Instance '%s'";
    private static final String LOG_REGISTERED = NAME + ": Registered Data Plane Instance. (id=%s, url=%s, sourceTypes=%s, destinationTypes=%s, properties=<omitted>)";
    private Monitor monitor;
    @Inject
    private DataPlaneSelectorService selectorService;
    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        this.monitor = context.getMonitor();
        final Config config = context.getConfig(EmbeddedDataPlaneConfig.CONFIG_PREFIX);
        context.getService(DataPlaneManager.class);
        configureDataPlaneInstance(config);
    }

    public DataPlaneInstance createDataPlane(final Config config){

        DataPlaneInstance dataPlaneInstance = null;
        final String id = config.currentNode();
        final String url = config.getString(EmbeddedDataPlaneConfig.URL_SUFFIX, "");
        final List<String> sourceTypes =
                Arrays.stream(config.getString(EmbeddedDataPlaneConfig.SOURCE_TYPES_SUFFIX, "").split(COMMA))
                        .map(String::trim)
                        .filter(Predicate.not(String::isEmpty))
                        .distinct()
                        .collect(Collectors.toList());
        final List<String> destinationTypes =
                Arrays.stream(config.getString(EmbeddedDataPlaneConfig.DESTINATION_TYPES_SUFFIX, "").split(COMMA))
                        .map(String::trim)
                        .filter(Predicate.not(String::isEmpty))
                        .distinct()
                        .collect(Collectors.toList());
        final String propertiesJson = config.getString(EmbeddedDataPlaneConfig.PROPERTIES_SUFFIX, "{}");

        if (url.isEmpty()) {
            monitor.warning(String.format(LOG_MISSING_CONFIGURATION, id, EmbeddedDataPlaneConfig.URL_SUFFIX));
        }

        if (sourceTypes.isEmpty()) {
            monitor.warning(String.format(LOG_MISSING_CONFIGURATION, id, EmbeddedDataPlaneConfig.SOURCE_TYPES_SUFFIX));
        }

        if (destinationTypes.isEmpty()) {
            monitor.warning(String.format(LOG_MISSING_CONFIGURATION, id, EmbeddedDataPlaneConfig.DESTINATION_TYPES_SUFFIX));
        }

        final Map<String, String> properties;
        try {
            ObjectMapper mapper = new ObjectMapper();
            properties = mapper.readValue(propertiesJson, new TypeReference<Map<String, String>>() {
            });
        } catch (JsonProcessingException e) {
            throw new EdcException(e);
        }

        final boolean missingPublicApiProperty = !properties.containsKey(EmbeddedDataPlaneConfig.PUBLIC_API_URL_PROPERTY);
        if (missingPublicApiProperty) {
            monitor.warning(String.format(LOG_MISSING_CONFIGURATION, id, EmbeddedDataPlaneConfig.PROPERTIES_SUFFIX) + "." + EmbeddedDataPlaneConfig.PUBLIC_API_URL_PROPERTY);
        }

        final boolean invalidConfiguration =
                url.isEmpty() || sourceTypes.isEmpty() || destinationTypes.isEmpty();
        if (invalidConfiguration || missingPublicApiProperty) {
            monitor.warning(String.format(LOG_SKIP_BC_MISSING_CONFIGURATION, id));
            return null;
        }
        final DataPlaneInstance.Builder builder =
                DataPlaneInstance.Builder.newInstance().id(id).url(url);

        sourceTypes.forEach(builder::allowedSourceType);
        destinationTypes.forEach(builder::allowedDestType);
        properties.forEach(builder::property);

        dataPlaneInstance = builder.build();

        monitor.debug(
                String.format(
                        LOG_REGISTERED,
                        id,
                        url,
                        String.join(", ", sourceTypes),
                        String.join(", ", destinationTypes)));
        return dataPlaneInstance;
    }
    private void configureDataPlaneInstance(final Config config) {
        var dataPlaneInstance = createDataPlane(config);
        if(nonNull(dataPlaneInstance)){
            selectorService.addInstance(dataPlaneInstance);
        }


    }
}
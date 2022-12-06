/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.selector;

import org.eclipse.edc.connector.api.management.configuration.ManagementApiConfiguration;
import org.eclipse.edc.connector.dataplane.selector.api.DataplaneSelectorApiController;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstanceImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

import static java.lang.String.format;

@Extension(value = "DataPlane selector API")
public class DataPlaneSelectorApiExtension implements ServiceExtension {

    /**
     * This deprecation is used to permit a softer transition from the deprecated `web.http.dataplane` config group to
     * the current `web.http.management`
     *
     * @deprecated "web.http.management" config should be used instead of "web.http.dataplane"
     */
    @Deprecated(since = "milestone8")
    private static final String DEPRECATED_CONTEXT = "dataplane";
    private static final String DEPRECATED_SETTINGS_GROUP = "web.http." + DEPRECATED_CONTEXT;

    @Inject
    private WebService webservice;

    @Inject
    private DataPlaneSelectorService selectionService;

    @Inject
    private ManagementApiConfiguration managementApiConfiguration;

    @Override
    public void initialize(ServiceExtensionContext context) {
        //todo: add authentication
        //var filter = new AuthenticationRequestFilter();

        context.getTypeManager().registerTypes(DataPlaneInstanceImpl.class);

        var controller = new DataplaneSelectorApiController(selectionService);

        if (context.getConfig().hasPath(DEPRECATED_SETTINGS_GROUP)) {
            webservice.registerResource(DEPRECATED_CONTEXT, controller);
            context.getMonitor().warning(
                    format("Deprecated settings group %s. These API are now meant to be configured under the management context",
                            DEPRECATED_SETTINGS_GROUP));
        } else {
            webservice.registerResource(managementApiConfiguration.getContextAlias(), controller);
        }
    }
}

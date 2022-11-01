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

import org.eclipse.edc.connector.dataplane.selector.api.DataplaneSelectorApiController;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstanceImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

@Extension(value = "DataPlane selector API")
public class DataPlaneSelectorApiExtension implements ServiceExtension {

    public static final String DATAPLANE_SELECTOR_CONTEXTALIAS = "dataplane";
    @Inject
    private WebService webservice;
    @Inject
    private DataPlaneSelectorService selectionService;

    @Override
    public void initialize(ServiceExtensionContext context) {

        //todo: add authentication
        //var filter = new AuthenticationRequestFilter();

        context.getTypeManager().registerTypes(DataPlaneInstanceImpl.class);

        var controller = new DataplaneSelectorApiController(selectionService);
        webservice.registerResource(DATAPLANE_SELECTOR_CONTEXTALIAS, controller);
    }
}

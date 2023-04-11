/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.transferprocess.api;

import org.eclipse.edc.protocol.dsp.api.configuration.DspApiConfiguration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebService;

@Extension(value = DspTransferProcessApiExtension.NAME)
public class DspTransferProcessApiExtension implements ServiceExtension {

    @Inject
    private DspApiConfiguration config;

    @Inject
    private TypeManager typeManager;

    @Inject
    private Monitor monitor;

    @Inject
    private WebService webService;

    public static final String NAME = "Dataspace Protocol: TransferProcess API Extension";

    public void initialize(ServiceExtensionContext context) {
        var controller = new DspTransferProcessApiController(monitor, typeManager);

        webService.registerResource(config.getContextAlias(), controller);
    }
}
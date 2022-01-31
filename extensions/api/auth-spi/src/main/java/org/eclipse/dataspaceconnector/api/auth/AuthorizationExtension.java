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

package org.eclipse.dataspaceconnector.api.auth;

import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

/**
 * Extension that registers the AuthorizationRequestFilter with the webservice
 */
public class AuthorizationExtension implements ServiceExtension {
    @Inject(required = false)
    private AuthorizationService authorizationService;

    @Override
    public void initialize(ServiceExtensionContext context) {
        //        if (authorizationService == null) {
        //            context.getMonitor().warning("No AuthorizationService was registered. Will deny all requests!");
        //            authorizationService = (headers) -> false;
        //        }

        // todo: this extension will eventually go away - the request filter should be registered by every API separately
    }
}

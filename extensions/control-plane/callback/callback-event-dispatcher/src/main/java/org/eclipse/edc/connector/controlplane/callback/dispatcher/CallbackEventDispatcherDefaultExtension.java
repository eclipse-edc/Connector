/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.callback.dispatcher;

import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackClient;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.types.TypeManager;

@Extension(value = CallbackEventDispatcherDefaultExtension.NAME)
public class CallbackEventDispatcherDefaultExtension implements ServiceExtension {

    public static final String NAME = "Callback event dispatcher default services";

    @Inject
    EdcHttpClient edcHttpClient;
    @Inject
    TypeManager typeManager;
    @Inject
    Vault vault;

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public CallbackClient callbackClient() {
        return new CallbackHttpClient(edcHttpClient, typeManager.getMapper(), vault);
    }
}

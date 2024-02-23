/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.transfer.dataplane;

import org.eclipse.edc.connector.transfer.dataplane.security.NoopDataEncrypter;
import org.eclipse.edc.connector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.edc.connector.transfer.dataplane.spi.token.ConsumerPullTokenExpirationDateFunction;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.time.Clock;
import java.util.Date;

import static org.eclipse.edc.connector.dataplane.spi.TransferDataPlaneConfig.DEFAULT_TOKEN_VALIDITY_SECONDS;
import static org.eclipse.edc.connector.dataplane.spi.TransferDataPlaneConfig.TOKEN_VALIDITY_SECONDS;

/**
 * Provides default service implementations for fallback
 * Omitted {@link Extension since this module already contains {@link TransferDataPlaneCoreExtension }}
 */
public class TransferDataPlaneDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Transfer Data Plane Default Services";

    @Inject
    private Clock clock;

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public DataEncrypter getDataEncrypter(ServiceExtensionContext context) {
        context.getMonitor().warning("No DataEncrypter registered, a no-op implementation will be used, not suitable for production environments");
        return new NoopDataEncrypter();
    }

    @Provider(isDefault = true)
    public ConsumerPullTokenExpirationDateFunction tokenExpirationDateFunction(ServiceExtensionContext context) {
        var validity = context.getSetting(TOKEN_VALIDITY_SECONDS, DEFAULT_TOKEN_VALIDITY_SECONDS);
        return (contentAddress, contractId) -> Result.success(Date.from(clock.instant().plusSeconds(validity)));
    }
}

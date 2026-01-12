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

package org.eclipse.edc.connector.controlplane.transfer;

import org.eclipse.edc.connector.controlplane.transfer.flow.TransferTypeParserImpl;
import org.eclipse.edc.connector.controlplane.transfer.observe.TransferProcessObservableImpl;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessPendingGuard;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.TransferTypeParser;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;

@Extension(value = TransferProcessDefaultServicesExtension.NAME)
public class TransferProcessDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Transfer Process Default Services";

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public TransferProcessObservable transferProcessObservable() {
        return new TransferProcessObservableImpl();
    }

    @Provider(isDefault = true)
    public TransferProcessPendingGuard pendingGuard() {
        return it -> false;
    }

    @Provider
    public TransferTypeParser transferTypeParser() {
        return new TransferTypeParserImpl();
    }

}

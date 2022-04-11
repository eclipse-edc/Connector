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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.core.observe;

import org.eclipse.dataspaceconnector.spi.observe.ObservableImpl;
import org.eclipse.dataspaceconnector.spi.transfer.observe.TransferProcessListener;
import org.eclipse.dataspaceconnector.spi.transfer.observe.TransferProcessObservable;

/**
 * Observes transfer process livecycle events.
 */
public class TransferProcessObservableImpl extends ObservableImpl<TransferProcessListener> implements TransferProcessObservable {
}

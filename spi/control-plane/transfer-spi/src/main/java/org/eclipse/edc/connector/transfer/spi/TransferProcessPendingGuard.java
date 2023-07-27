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

package org.eclipse.edc.connector.transfer.spi;

import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.entity.PendingGuard;

/**
 * Marker interface to define a service that permits to choose whether a TransferProcess will be waiting for an external
 * interaction.
 */
@FunctionalInterface
@ExtensionPoint
public interface TransferProcessPendingGuard extends PendingGuard<TransferProcess> {
}

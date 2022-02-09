/*
 *  Copyright (c) 2021-2022 Fraunhofer Institute for Software and Systems Engineering
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
package org.eclipse.dataspaceconnector.spi.types.domain.transfer.command;

import org.eclipse.dataspaceconnector.spi.command.CommandQueue;
import org.eclipse.dataspaceconnector.spi.system.Feature;

/**
 * Sub-type of {@link CommandQueue} for enqueuing {@link TransferProcessCommand}s.
 */
@Feature("edc:core:transfer:commandqueue")
public interface TransferProcessCommandQueue extends CommandQueue<TransferProcessCommand> {
}

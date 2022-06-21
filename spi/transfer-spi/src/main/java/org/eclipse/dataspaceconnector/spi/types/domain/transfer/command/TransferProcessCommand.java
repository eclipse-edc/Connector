/*
 *  Copyright (c) 2021 - 2022 Fraunhofer Institute for Software and Systems Engineering
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

import org.eclipse.dataspaceconnector.spi.command.Command;

/**
 * Sub-type of {@link Command} for modifying TransferProcesses.
 */
public abstract class TransferProcessCommand extends Command {
    
    protected TransferProcessCommand() {
        super();
    }
    
}

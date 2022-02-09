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

import org.eclipse.dataspaceconnector.spi.command.CommandRunner;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

/**
 * Sub-type of {@link CommandRunner} that runs commands for modifying TransferProcesses.
 */
public class TransferProcessCommandRunner extends CommandRunner<TransferProcessCommand> {
    
    public TransferProcessCommandRunner(TransferProcessCommandHandlerRegistry registry,
                                        Monitor monitor) {
        super(registry, monitor);
    }
    
}

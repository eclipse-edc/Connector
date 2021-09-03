/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.consumer.command.ids;

import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import de.fraunhofer.iais.eis.DescriptionRequestMessageBuilder;
import org.eclipse.dataspaceconnector.consumer.command.CommandExecutor;
import org.eclipse.dataspaceconnector.consumer.command.CommandResult;
import org.eclipse.dataspaceconnector.consumer.command.ExecutionContext;

import static org.eclipse.dataspaceconnector.consumer.command.http.HttpOperations.executePost;

/**
 * Sends a {@link DescriptionRequestMessage} to an IDS Controller.
 */
public class DescriptionRequestExecutor implements CommandExecutor {

    @Override
    public CommandResult execute(ExecutionContext context) {
        // TODO allow file input
        DescriptionRequestMessage message = new DescriptionRequestMessageBuilder().build();
        return executePost("/api/ids/description", message, context);
    }
}

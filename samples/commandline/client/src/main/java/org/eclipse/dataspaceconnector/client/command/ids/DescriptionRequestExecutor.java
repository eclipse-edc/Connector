/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.client.command.ids;

import org.eclipse.dataspaceconnector.client.command.CommandExecutor;
import org.eclipse.dataspaceconnector.client.command.CommandResult;
import org.eclipse.dataspaceconnector.client.command.ExecutionContext;
import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import de.fraunhofer.iais.eis.DescriptionRequestMessageBuilder;

import static org.eclipse.dataspaceconnector.client.command.http.HttpOperations.executePost;

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

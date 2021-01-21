package com.microsoft.dagx.client.command.ids;

import com.microsoft.dagx.client.command.CommandExecutor;
import com.microsoft.dagx.client.command.CommandResult;
import com.microsoft.dagx.client.command.ExecutionContext;
import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import de.fraunhofer.iais.eis.DescriptionRequestMessageBuilder;

import static com.microsoft.dagx.client.command.http.HttpOperations.executePost;

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

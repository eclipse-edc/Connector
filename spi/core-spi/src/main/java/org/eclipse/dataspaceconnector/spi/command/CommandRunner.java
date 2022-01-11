package org.eclipse.dataspaceconnector.spi.command;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;

/**
 * Used to execute a single {@link Command}. The necessary steps are:
 * <ol>
 *     <li>Obtain a {@link CommandHandler} instance for a given command</li>
 *     <li>run the command</li>
 *     <li>convert exception into a {@link Result}, increase error count</li>
 * </ol>
 */
public class CommandRunner {
    private final CommandHandlerRegistry commandHandlerRegistry;
    private final Monitor monitor;

    public CommandRunner(CommandHandlerRegistry commandHandlerRegistry, Monitor monitor) {
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.monitor = monitor;
    }

    public <C extends Command> Result<Void> runCommand(C command) {

        Class<C> commandClass = (Class<C>) command.getClass();

        var handler = commandHandlerRegistry.get(commandClass);
        if (handler == null) {
            command.increaseErrorCount();
            return Result.failure("No command handler found for command type " + commandClass);
        }
        try {
            handler.handle(command);
            return Result.success();
        } catch (RuntimeException ex) {
            command.increaseErrorCount();
            monitor.severe("Error when processing a command", ex);
            return Result.failure(ex.getMessage());
        }
    }

}

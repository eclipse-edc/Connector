package org.eclipse.dataspaceconnector.spi.command;

/**
 * CommandHandlers receive a {@link Command} object and act on it. If possible, command handlers should
 * not perform lengthy operations as this could block the command queue.
 *
 * @param <T> The concrete type of {@link Command}
 */
public interface CommandHandler<T extends Command> {

    void handle(T command);

    Class<T> getType();
}

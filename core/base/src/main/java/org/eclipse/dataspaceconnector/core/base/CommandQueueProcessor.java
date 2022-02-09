package org.eclipse.dataspaceconnector.core.base;

import org.eclipse.dataspaceconnector.spi.command.Command;
import org.eclipse.dataspaceconnector.spi.command.CommandQueue;
import org.eclipse.dataspaceconnector.spi.command.CommandRunner;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

/**
 * Abstract class for processing commands from a {@link CommandQueue} using a {@link CommandRunner}.
 * The CommandQueue, CommandRunner and Monitor have to be supplied by implementing classes.
 *
 * @param <C> the type of command a sub-class can process.
 */
public abstract class CommandQueueProcessor<C extends Command> {
    
    protected abstract CommandQueue<C> getCommandQueue();
    
    protected abstract CommandRunner<C> getCommandRunner();
    
    protected abstract Monitor getMonitor();
    
    /**
     * Fetches a batch of commands from the {@link CommandQueue} and processes them using the
     * {@link CommandRunner}.
     *
     * @return the number of successfully processed commands.
     */
    protected int processCommandQueue() {
        var batchSize = 5;
        var commands = getCommandQueue().dequeue(batchSize);
        AtomicInteger successCount = new AtomicInteger(); //needs to be an atomic because lambda.
        
        commands.forEach(command -> {
            var commandResult = getCommandRunner().runCommand(command);
            if (commandResult.failed()) {
                //re-queue if possible
                if (command.canRetry()) {
                    getMonitor().warning(format("Could not process command [%s], will retry. error: %s", command.getClass(), commandResult.getFailureMessages()));
                    getCommandQueue().enqueue(command);
                } else {
                    getMonitor().severe(format("Command [%s] has exceeded its retry limit, will discard now", command.getClass()));
                }
            } else {
                getMonitor().debug(format("Successfully processed command [%s]", command.getClass()));
                successCount.getAndIncrement();
            }
        });
        return successCount.get();
    }
    
}

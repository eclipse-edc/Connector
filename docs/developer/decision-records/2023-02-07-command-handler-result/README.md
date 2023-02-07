# CommandHandler Result

## Decision

Return the `CommandHandler`s' thrown exceptions, that are caught by the `CommandRunner`, should be returned as a failed `Result` instead. 


## Rationale


At the moment, some `CommandHandler`s throw exceptions to notify validation errors, that are catched by the `CommandRunner`, but these errors are seen as unexpected.
In order to make these errors detection easier, the `CommandHandler.handle` method is modified to return a `Result` that is evaluated by the `CommandRunner` , then the `Result` would be returned to the `CommandProcessor` and logged accordingly.

## Approach


1. Make `CommandHandler.handle` return a `Result`:
```java
public interface CommandHandler<T extends Command> {

    Class<T> getType();
    
    Result<Void> handle(T command);
}
```
2. Make the `CommandRunner.runCommand` evaluate the `Result` that is returned by the `CommandHandler`:
```java


   public <T extends C> Result<Void> runCommand(T command) {

        @SuppressWarnings("unchecked") var commandClass = (Class<T>) command.getClass();

        var handler = commandHandlerRegistry.get(commandClass);
        if (handler == null) {
            command.increaseErrorCount();
            return Result.failure("No command handler found for command type " + commandClass);
        }
        try {
            var result = handler.handle(command);
            return result;
        }
        .....
   }

```

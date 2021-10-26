package org.eclipse.dataspaceconnector.ids.core.configuration;

/**
 * The {@link IllegalSettingException} indicates that a configuration parameter was set to an invalid value.
 * </p>
 * The {@link IllegalSettingException} does not extend the {@link org.eclipse.dataspaceconnector.spi.EdcException} interface,
 * because it's not a {@link RuntimeException} and should never be unchecked.
 */
public class IllegalSettingException extends Exception {
    public IllegalSettingException(String message) {
        super(message);
    }

    public IllegalSettingException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalSettingException(Throwable cause) {
        super(cause);
    }

    public IllegalSettingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

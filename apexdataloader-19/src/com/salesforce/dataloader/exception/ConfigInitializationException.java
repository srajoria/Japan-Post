/**
 *
 */
package com.salesforce.dataloader.exception;

/**
 * ConfigInitializationException
 *
 * @author awarshavsky
 * @since before
 */
@SuppressWarnings("serial")
public class ConfigInitializationException extends ProcessInitializationException {

    /**
     *
     */
    public ConfigInitializationException() {
        super();
    }

    /**
     * @param message
     */
    public ConfigInitializationException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public ConfigInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public ConfigInitializationException(Throwable cause) {
        super(cause);
    }
}

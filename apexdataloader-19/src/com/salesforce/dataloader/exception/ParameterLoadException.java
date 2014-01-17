/**
 *
 */
package com.salesforce.dataloader.exception;

/**
 * ParameterLoadException
 *
 * @author awarshavsky
 * @since before
 */
@SuppressWarnings("serial")
public class ParameterLoadException extends ConfigInitializationException {

    /**
     *
     */
    public ParameterLoadException() {
        super();
    }

    /**
     * @param message
     */
    public ParameterLoadException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public ParameterLoadException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public ParameterLoadException(Throwable cause) {
        super(cause);
    }

}

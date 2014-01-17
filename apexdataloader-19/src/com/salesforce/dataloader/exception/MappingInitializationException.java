/**
 *
 */
package com.salesforce.dataloader.exception;

/**
 * Describe your class here.
 *
 * @author awarshavsky
 * @since before
 */
@SuppressWarnings("serial")
public class MappingInitializationException extends ProcessInitializationException {

    /**
     *
     */
    public MappingInitializationException() {
        super();
    }

    /**
     * @param message
     */
    public MappingInitializationException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public MappingInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public MappingInitializationException(Throwable cause) {
        super(cause);
    }

}

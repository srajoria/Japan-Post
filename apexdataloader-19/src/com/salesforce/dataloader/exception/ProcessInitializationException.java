/**
 *
 */
package com.salesforce.dataloader.exception;

/**
 * ProcessInitializationException
 *
 * @author awarshavsky
 * @since before
 */
@SuppressWarnings("serial")
public class ProcessInitializationException extends Exception {

    /**
     *
     */
    public ProcessInitializationException() {
        super();
    }

    /**
     * @param message
     */
    public ProcessInitializationException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public ProcessInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public ProcessInitializationException(Throwable cause) {
        super(cause);
    }

}

/**
 *
 */
package com.salesforce.dataloader.exception;

/**
 * OperationInitializationException
 *
 * @author awarshavsky
 * @since before
 */
@SuppressWarnings("serial")
public class OperationInitializationException extends ProcessInitializationException {

    /**
     *
     */
    public OperationInitializationException() {
        super();
    }

    /**
     * @param message
     */
    public OperationInitializationException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public OperationInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public OperationInitializationException(Throwable cause) {
        super(cause);
    }

}

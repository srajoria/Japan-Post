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
public class OperationException extends Exception {

    /**
     *
     */
    public OperationException() {
        super();
    }

    /**
     * @param message
     */
    public OperationException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public OperationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public OperationException(Throwable cause) {
        super(cause);
    }

}

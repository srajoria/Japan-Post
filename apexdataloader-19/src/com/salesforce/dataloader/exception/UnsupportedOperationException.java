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
public class UnsupportedOperationException extends OperationInitializationException {

    /**
     *
     */
    public UnsupportedOperationException() {
        super();
    }

    /**
     * @param message
     */
    public UnsupportedOperationException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public UnsupportedOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public UnsupportedOperationException(Throwable cause) {
        super(cause);
    }

}

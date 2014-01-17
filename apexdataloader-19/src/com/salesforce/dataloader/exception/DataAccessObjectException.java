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
public class DataAccessObjectException extends Exception {

    /**
     *
     */
    public DataAccessObjectException() {
        super();
    }

    /**
     * @param message
     */
    public DataAccessObjectException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public DataAccessObjectException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public DataAccessObjectException(Throwable cause) {
        super(cause);
    }

}

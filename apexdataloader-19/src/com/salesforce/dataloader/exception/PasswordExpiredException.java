/**
 *
 */
package com.salesforce.dataloader.exception;

import com.sforce.ws.ConnectionException;

/**
 * Describe your class here.
 *
 * @author awarshavsky
 * @since before
 */
@SuppressWarnings("serial")
public class PasswordExpiredException extends ConnectionException {

    /**
     *
     */
    public PasswordExpiredException() {
        super();
    }

    /**
     * @param message
     */
    public PasswordExpiredException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public PasswordExpiredException(String message, Throwable cause) {
        super(message, cause);
    }

}

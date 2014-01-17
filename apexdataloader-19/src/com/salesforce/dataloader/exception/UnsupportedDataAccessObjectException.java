/**
 *
 */
package com.salesforce.dataloader.exception;

import com.salesforce.dataloader.exception.DataAccessObjectInitializationException;

/**
 * UnsupportedDataAccessObjectException
 *
 * @author awarshavsky
 * @since before
 */
@SuppressWarnings("serial")
public class UnsupportedDataAccessObjectException extends DataAccessObjectInitializationException {

    /**
    *
    */
   public UnsupportedDataAccessObjectException() {
       super();
   }

    public UnsupportedDataAccessObjectException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedDataAccessObjectException(String message) {
        super(message);
    }

    public UnsupportedDataAccessObjectException(Throwable cause) {
        super(cause);
    }

}

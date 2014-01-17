package com.salesforce.dataloader.exception;

@SuppressWarnings("serial")
public class DataAccessObjectInitializationException extends DataAccessObjectException {

    public DataAccessObjectInitializationException() {
        super();
    }

    public DataAccessObjectInitializationException(String message) {
        super(message);
    }

    public DataAccessObjectInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessObjectInitializationException(Throwable cause) {
        super(cause);
    }

}

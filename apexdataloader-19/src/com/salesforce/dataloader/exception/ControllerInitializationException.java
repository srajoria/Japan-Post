package com.salesforce.dataloader.exception;

@SuppressWarnings("serial")
public class ControllerInitializationException extends ProcessInitializationException {

    public ControllerInitializationException() {
        super();
    }

    public ControllerInitializationException(String message) {
        super(message);
    }

    public ControllerInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ControllerInitializationException(Throwable cause) {
        super(cause);
    }

}

/**
 *
 */
package com.salesforce.dataloader.exception;


/**
 * XmlBeanFactoryException
 *
 * @author awarshavsky
 * @since before
 */
@SuppressWarnings("serial")
public class XmlBeanFactoryException extends ControllerInitializationException {

    public XmlBeanFactoryException(String message, Throwable cause) {
        super(message, cause);
    }

    public XmlBeanFactoryException(String message) {
        super(message);
    }

    public XmlBeanFactoryException(Throwable cause) {
        super(cause);
    }


}

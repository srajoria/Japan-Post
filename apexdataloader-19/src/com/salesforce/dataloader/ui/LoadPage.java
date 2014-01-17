/**
 * 
 */
package com.salesforce.dataloader.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;

/**
 * This is the base class for the LoadWizard ui pages. Allows navigation to be done dynamically by forcing setupPage to
 * be implemented by each wizard page
 * 
 * @author awarshavsky
 * @since before
 */
public abstract class LoadPage extends WizardPage {

    /**
     * @param pageName 
     * @param title 
     * @param titleImage 
     * 
     */
    public LoadPage(String pageName, String title, ImageDescriptor titleImage) {
        super(pageName, title, titleImage);
    }
    
    abstract boolean setupPage();

    /* 
     * Common code for getting the next page
     */
    @Override
    public LoadPage getNextPage() {
        LoadPage nextPage = (LoadPage)super.getNextPage();
        if(nextPage != null && nextPage.setupPage()) {
            return nextPage;
        } else {
            return this;
        }
    }

    /**
     * Need to subclass this function to prevent the getNextPage() function being called before the button is clicked.
     */
    @Override
    public boolean canFlipToNextPage() {
        return isPageComplete();
    }
}

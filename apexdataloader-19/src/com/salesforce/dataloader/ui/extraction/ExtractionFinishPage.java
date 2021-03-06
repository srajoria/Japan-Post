/*
 * Copyright (c) 2006, salesforce.com, inc.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided 
 * that the following conditions are met:
 * 
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the 
 *    following disclaimer.
 *  
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and 
 *    the following disclaimer in the documentation and/or other materials provided with the distribution. 
 *    
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or 
 *    promote products derived from this software without specific prior written permission.
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED 
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR 
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED 
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.salesforce.dataloader.ui.extraction;

import java.io.File;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.salesforce.dataloader.config.Config;
import com.salesforce.dataloader.controller.Controller;
import com.salesforce.dataloader.exception.ProcessInitializationException;
import com.salesforce.dataloader.ui.Labels;
import com.salesforce.dataloader.ui.UIUtils;

/**
 * Last page of extraction wizard which lets the user select directory for status files (currently, only error status is
 * generated in case of extract)
 * 
 * @author awarshavsky
 * @since before
 */
public class ExtractionFinishPage extends ExtractionPage {

    private final Controller controller;
    private DirectoryFieldEditor dirFE;

    public ExtractionFinishPage(Controller controller) {
        super(Labels.getString("FinishPage.title"), Labels.getString("FinishPage.finishMsg"), UIUtils.getImageRegistry().getDescriptor("splashscreens")); //$NON-NLS-1$ //$NON-NLS-2$

        this.controller = controller;
        setPageComplete(false);

        // Set the description
        setDescription(Labels.getString("FinishPage.selectDir")); //$NON-NLS-1$

    }

    public void createControl(Composite parent) {
        Composite comp = new Composite(parent, SWT.NONE);
        //comp.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

        GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.horizontalSpacing = 10;
        gridLayout.marginHeight = 20;
        comp.setLayout(gridLayout);

        Label label = new Label(comp, SWT.CENTER);
        label.setText(Labels.getString("FinishPage.overwritten")); //$NON-NLS-1$

        Composite dirComp = new Composite(comp, SWT.NONE);
        GridData data = new GridData();
        data.widthHint = 400;
        dirComp.setLayoutData(data);

        dirFE = new DirectoryFieldEditor(
                Labels.getString("FinishPage.output"), Labels.getString("FinishPage.chooseDir"), dirComp); //$NON-NLS-1$ //$NON-NLS-2$
        dirFE.setStringValue(controller.getConfig().getString(Config.OUTPUT_STATUS_DIR));

        setControl(comp);
    }

    public String getOutputDir() {
        return dirFE.getStringValue();
    }

    @Override
    public boolean canFlipToNextPage() {
        // this is always the last page, disable next
        return false;
    }

    /*
     * (non-Javadoc)
     * @see com.salesforce.dataloader.ui.extraction.ExtractionPage#finishPage()
     */
    @Override
    public boolean finishPage() {
        // validate the status output
        String outputDirName = getOutputDir();
        File statusDir = new File(outputDirName);
        if (!statusDir.exists() || !statusDir.isDirectory()) {
            UIUtils.errorMessageBox(getShell(), Labels.getString("LoadWizard.errorValidDirectory")); //$NON-NLS-1$
            return false;
        }
        // set the files for status output
        try {
            controller.setStatusFiles(outputDirName, false, true);
            controller.saveConfig();
        } catch (ProcessInitializationException e) {
            UIUtils.errorMessageBox(getShell(), e);
            return false;
        }
        return true;
    }
}

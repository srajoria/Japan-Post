/*
 * Copyright (c) 2005, salesforce.com, inc.
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

package com.salesforce.dataloader.action.progress;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;

import com.salesforce.dataloader.action.OperationInfo;
import com.salesforce.dataloader.controller.Controller;
import com.salesforce.dataloader.ui.LoadFinishDialog;
import com.salesforce.dataloader.ui.LoaderWindow;
import com.salesforce.dataloader.ui.UIUtils;
import com.salesforce.dataloader.ui.extraction.ExtractionFinishDialog;

/**
 * @author Lexi
 */
public class SWTProgressAdapter {

    private IProgressMonitor monitor = null;
    private String dispMessage;
    private Controller controller;

    public SWTProgressAdapter(Controller controller) {
       // monitor = monitor_;
        this.controller = controller;
    }

    /*
     * @see com.sfdc.action.progress.ILoaderProgress#beginTask(java.lang.String, int)
     */
    public void beginTask(String name, int totalWork) {
       // monitor.beginTask(name, totalWork);
    }

    public void done() {
       // monitor.done();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sfdc.action.progress.ILoaderProgress#done()
     */
    public void doneSuccess(String message) {
      /*  monitor.done();

        dispMessage = message;
        Display.getDefault().syncExec(new Thread() {
            @Override
            public void run() {

                //if extraction pop open an extraction finished dialog
                if (controller.getConfig().getOperationInfo() == OperationInfo.extract) {
                    ExtractionFinishDialog dlg = new ExtractionFinishDialog(LoaderWindow.getApp().getShell(), controller);
                    dlg.setMessage(dispMessage);
                    dlg.open();
                } else {
                    LoadFinishDialog dlg = new LoadFinishDialog(LoaderWindow.getApp().getShell(), controller);
                    dlg.setMessage(dispMessage);
                    dlg.open();
                }
            }
        });
*/
    }

    public void doneSuccess(String message, final boolean didErrorOccur) {

    }

    public void doneError(String message) {
       /* monitor.done();
        dispMessage = message;
        Display.getDefault().syncExec(new Thread() {
            @Override
            public void run() {
                 UIUtils.errorMessageBox(LoaderWindow.getApp().getShell(), dispMessage);
            }
        });*/
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sfdc.action.progress.ILoaderProgress#worked(int)
     */
    public void worked(int worked) {
        monitor.worked(worked);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sfdc.action.progress.ILoaderProgress#setTaskName(java.lang.String)
     */
    public void setTaskName(String name) {
     //  monitor.setTaskName(name);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sfdc.action.progress.ILoaderProgress#setSubTask(java.lang.String)
     */
    public void setSubTask(String name) {
      //  monitor.subTask(name);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sfdc.action.progress.ILoaderProgress#isCanceled()
     */
    public boolean isCanceled() {
        //return monitor.isCanceled();
    	return false;
    }

}

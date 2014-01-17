package com.salesforce.dataloader.ui.uiActions;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;

import com.salesforce.dataloader.action.OperationInfo;
import com.salesforce.dataloader.config.Config;
import com.salesforce.dataloader.controller.Controller;
import com.salesforce.dataloader.ui.Labels;
import com.salesforce.dataloader.ui.LoaderWindow;
import com.salesforce.dataloader.ui.LoaderWizardDialog;
import com.salesforce.dataloader.ui.UIUtils;

public class OperationUIAction extends Action {
    private final Controller controller;
    private final OperationInfo opInfo;

    public OperationUIAction(Controller controller, OperationInfo info) {
        super(info.getMenuLabel(), info.getIconImageDescriptor());
        setToolTipText(info.getToolTipText());

        this.controller = controller;
        this.opInfo = info;

        setEnabled(info.isOperationAllowed(controller.getConfig()));
    }

    @Override
    public void run() {
    	
    	if(this.opInfo.name().equalsIgnoreCase("Upsert")){
    		if(UIUtils.warningConfMessageBox(this.controller.shl, Labels.getString("DataSelectionPage.confirmUpsert")) != SWT.YES){
    			return;
    		}
    	}
    	else if(this.opInfo.name().equalsIgnoreCase("Insert")){
    		if(UIUtils.warningConfMessageBox(this.controller.shl, Labels.getString("DataSelectionPage.confirmDeleteInsert")) != SWT.YES){
    			return;
    		}
    	}
    	
       // LoaderWizardDialog dlg = new LoaderWizardDialog(LoaderWindow.getApp().getShell(), this.opInfo
       //         .instantiateWizard(this.controller));
        //dlg.open();
    }
}

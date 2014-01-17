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

package com.salesforce.dataloader.ui.extraction;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

import com.salesforce.dataloader.config.Config;
import com.salesforce.dataloader.controller.Controller;
import com.salesforce.dataloader.dao.DataAccessObjectFactory;
import com.salesforce.dataloader.exception.DataAccessObjectInitializationException;
import com.salesforce.dataloader.exception.MappingInitializationException;
import com.salesforce.dataloader.ui.Labels;
import com.salesforce.dataloader.ui.UIUtils;
import com.salesforce.dataloader.ui.entitySelection.*;
import com.sforce.async.SObject;
import com.sforce.soap.partner.DescribeGlobalSObjectResult;
import com.sforce.soap.partner.QueryResult;
import com.sforce.ws.ConnectionException;

/**
 * Describe your class here.
 *
 * @author lviripaeff
 * @since before
 */
public class ExtractionDataSelectionPage extends WizardPage {

    private Controller controller;

    // These filter extensions are used to filter which files are displayed.
    private EntityFilter filter = new EntityFilter();
    private ListViewer lv;
    private Text fileText;

    public Composite comp;

    private boolean success;
    
    private Combo fieldCombo;
    
    public ExtractionDataSelectionPage(Controller controller) {
        super(
                Labels.getString("ExtractionDataSelectionPage.title"), Labels.getString("ExtractionDataSelectionPage.titleMsg"), UIUtils.getImageRegistry().getDescriptor("splashscreens")); //$NON-NLS-1$ //$NON-NLS-2$

        this.controller = controller;

        // Set the description
        setDescription(Labels.getString("ExtractionDataSelectionPage.description")); //$NON-NLS-1$

        setPageComplete(false);
    }

    public void createControl(Composite parent) {
        getShell().setImage(UIUtils.getImageRegistry().get("sfdc_icon")); //$NON-NLS-1$

        GridData data;

        comp = new Composite(parent, SWT.NONE);

        GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.horizontalSpacing = 10;
        gridLayout.marginHeight = 15;
        gridLayout.verticalSpacing = 5;
        comp.setLayout(gridLayout);
        
        
        Composite compChooser1 = new Composite(comp, SWT.NONE);
        data = new GridData(GridData.CENTER | GridData.VERTICAL_ALIGN_END);

        compChooser1.setLayoutData(data);

        GridLayout gLayout1 = new GridLayout(2, false);
        compChooser1.setLayout(gLayout1);
        
        Label label = new Label(compChooser1, SWT.RIGHT);
        label.setText(Labels.getString("ExtractionDataSelectionPage.selectSforce")); //$NON-NLS-1$
        data = new GridData();
        label.setLayoutData(data);

        // Add a checkbox to toggle filter
        //Button filterAll = new Button(comp, SWT.CHECK);
        //filterAll.setText(Labels.getString("ExtractionDataSelectionPage.showAll")); //$NON-NLS-1$
        //data = new GridData();
        //filterAll.setLayoutData(data);

        fieldCombo = new Combo(compChooser1, SWT.DROP_DOWN | SWT.READ_ONLY);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.widthHint = 200;
        fieldCombo.setLayoutData(data);
        fieldCombo.removeAll();

        fieldCombo.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent arg0) {
            	fieldCombo.getItem(fieldCombo.getSelectionIndex());
            	lv.getList().indexOf(fieldCombo.getItem(fieldCombo.getSelectionIndex()));
                lv.getList().select(lv.getList().indexOf(fieldCombo.getItem(fieldCombo.getSelectionIndex())));
                setPageComplete(true);
            }
            public void widgetDefaultSelected(SelectionEvent arg0) {
                
            }
        });
        
        lv = new ListViewer(comp, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        lv.setContentProvider(new EntityContentProvider());
        lv.setLabelProvider(new EntityLabelProvider());
        lv.setInput(null);
        data = new GridData(GridData.FILL_VERTICAL);
        data.heightHint = 140;
        data.widthHint = 140;
        lv.getControl().setLayoutData(data);
        //lv.addFilter(filter);
        lv.setSorter(new EntityViewerSorter());
        lv.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                checkPageComplete();
            }

        });
        lv.getControl().setVisible(false);
         //if we're logged in, set the input
        if (controller.isLoggedIn()) {
            setInput(controller.getEntityDescribes());
        }

        /*filterAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (((Button)event.widget).getSelection())
                    lv.removeFilter(filter);
                else
                    lv.addFilter(filter);
            }
        });*/

        Label clearLabel = new Label(comp, SWT.NONE);
        data = new GridData(GridData.VERTICAL_ALIGN_END);
        data.heightHint = 20;
        clearLabel.setLayoutData(data);

        //now select the file
        Composite compChooser = new Composite(comp, SWT.NONE);
        data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_END);

        compChooser.setLayoutData(data);

        GridLayout gLayout = new GridLayout(3, false);
        compChooser.setLayout(gLayout);

        //file Label
        Label fileLabel = new Label(compChooser, SWT.NONE);
        fileLabel.setText(Labels.getString("ExtractionDataSelectionPage.chooseTarget")); //$NON-NLS-1$

        //file text
        fileText = new Text(compChooser, SWT.BORDER);
        fileText.setText(Labels.getString("ExtractionDataSelectionPage.defaultFileName")); //$NON-NLS-1$
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.widthHint = 350;
        fileText.setLayoutData(data);

        fileText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                checkPageComplete();
            }
        });

        Button fileButton = new Button(compChooser, SWT.PUSH);
        fileButton.setText(Labels.getString("ExtractionDataSelectionPage.chooseFile")); //$NON-NLS-1$
        fileButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                FileDialog dlg = new FileDialog(getShell(), SWT.SAVE);
                String initialFile = fileText.getText();
                if(initialFile.length() == 0) {
                    initialFile = Labels.getString("ExtractionDataSelectionPage.defaultFileName"); //$NON-NLS-1$
                }
                dlg.setFileName(initialFile);
                String filename = dlg.open();
                if (filename != null && !"".equals(filename)) { //$NON-NLS-1$
                    //set the text, and see if the page is valid
                    fileText.setText(filename);
                    checkPageComplete();
                }
            }
        });

        setControl(comp);
    }

    /**
     * Function to dynamically set the entity list
     */
    public void setInput(Map<String, DescribeGlobalSObjectResult> entityDescribes) {
    	controller.mapUsrObject.clear();
    	for(com.sforce.soap.partner.sobject.SObject s : controller.arrSObj){
			if(controller.isHQ){
				if(s.getChild("HQRole__c") != null && (s.getChild("HQRole__c").getValue().toString().equalsIgnoreCase("R") || s.getChild("HQRole__c").getValue().toString().equalsIgnoreCase("U"))){
					controller.mapUsrObject.put((String) s.getChild("Object_API_Name__c").getValue(), s);
				}
			}
			else{
				if(s.getChild("Branch__c") != null && (s.getChild("Branch__c").getValue().toString().equalsIgnoreCase("R") || s.getChild("Branch__c").getValue().toString().equalsIgnoreCase("U"))){
					controller.mapUsrObject.put((String) s.getChild("Object_API_Name__c").getValue(), s);
				}
			}
		}
    	
		
        Map<String, DescribeGlobalSObjectResult> inputDescribes = new HashMap<String, DescribeGlobalSObjectResult>();

        if (entityDescribes != null) {
            // for each object, check whether the object is valid for the Extract operation
            for (Entry<String, DescribeGlobalSObjectResult> entry : entityDescribes.entrySet()) {
                if (entry.getValue().isQueryable() && controller.mapUsrObject.containsKey(entry.getKey())) {
                    inputDescribes.put(entry.getKey(), entry.getValue());
                }
            }
        }
        lv.setInput(inputDescribes);
        lv.refresh();
        lv.getControl().getParent().pack();
        //lv.getList().indexOf("Account");
        //lv.getList().select(arg0)
      // lv.getList().getClass().getTypeParameters()
      // setPageComplete(true);
        String[] arr = lv.getList().getItems();
        
        
        
        Arrays.sort(arr);
       // String[] fieldNames = new String[arr.length];
       /* for (int i = 0; i < arr.length; i++) {
        	if(arr[i] !=null && inputDescribes.containsKey(arr[i]) && inputDescribes.get(arr[i]).isQueryable()){
        		
        		 if (arr[i].equals("Account") || arr[i].equals("Case") || arr[i].equals("Contact")
        	                || arr[i].equals("Event") || arr[i].equals("Lead") || arr[i].equals("Opportunity")
        	                || arr[i].equals("Pricebook2") || arr[i].equals("Product2") || arr[i].equals("Task")
        	                || arr[i].equals("User")) {
        			 	//fieldNames[i] = arr[i];
        			 	 fieldCombo.add(arr[i]);
        	        } else if (arr[i].endsWith("__c")) {
        	        	//fieldNames[i] = arr[i];
        	        	 fieldCombo.add(arr[i]);
        	        }
        		//fieldNames[i] = arr[i];
        	}
        }*/
        
      //  Arrays.sort(fieldNames);
       fieldCombo.setItems(arr);
       // fieldCombo.getParent().pack();
        //comp.layout();

    }

    private boolean checkEntityStatus() {
    	
        IStructuredSelection selection = (IStructuredSelection)lv.getSelection();
        DescribeGlobalSObjectResult entity = (DescribeGlobalSObjectResult)selection.getFirstElement();
        if (entity != null) { return true; }
        return false;

    }

    private void checkPageComplete() {

        if (!(fileText.getText().equals("")) && checkEntityStatus()) { //$NON-NLS-1$
            setPageComplete(true);
        } else {
            setPageComplete(false);
        }

    }

    /**
     * Need to subclass this function to prevent the getNextPage() function being called before the button is clicked.
     */
    @Override
    public boolean canFlipToNextPage() {
        return isPageComplete();
    }

    /**
     * Returns the next page, describes SObject and performs the total size calculation
     *
     * @return IWizardPage
     */

    @Override
    public IWizardPage getNextPage() {

        // if output file already exists, confirm that the user wants to replace it
        if(new File(fileText.getText()).exists()) {
            int button = UIUtils.warningConfMessageBox(getShell(), Labels.getString("UI.fileAlreadyExists"));
            if(button == SWT.NO) {
                return this;
            }
        }

        Config config = controller.getConfig();
        //get entity
        IStructuredSelection selection = (IStructuredSelection)lv.getSelection();
        DescribeGlobalSObjectResult entity = (DescribeGlobalSObjectResult)selection.getFirstElement();
        config.setValue(Config.ENTITY, entity.getName());
        // set DAO - CSV file name
        config.setValue(Config.DAO_NAME, fileText.getText());
        // set DAO type to CSV
        config.setValue(Config.DAO_TYPE, DataAccessObjectFactory.CSV_WRITE_TYPE);
        controller.saveConfig();

        try {
            // create data access object for the extraction output
            controller.createDao();
            // reinitialize the data mapping (UI extraction currently uses only implicit mapping)
            config.setValue(Config.MAPPING_FILE, "");
            controller.createMappingManager();
        } catch (DataAccessObjectInitializationException e) {
            MessageBox msgBox = new MessageBox(getShell(), SWT.OK | SWT.ICON_ERROR);
            msgBox.setMessage(Labels.getString("ExtractionDataSelectionPage.extractOutputError")); //$NON-NLS-1$
            msgBox.open();
            return this;
        } catch (MappingInitializationException e) {
            UIUtils.errorMessageBox(getShell(), e);
            return this;
        }

        BusyIndicator.showWhile(Display.getDefault(), new Thread() {
            @Override
            public void run() {
                try {
                    controller.setFieldTypes();
                    controller.setReferenceDescribes();
                    success = true;
                } catch (ConnectionException e) {
                    success = false;
                }
            }
        });

        if (success) {
            //set the query
            ExtractionSOQLPage soql = (ExtractionSOQLPage)getWizard().getPage("SOQL"); //$NON-NLS-1$
            soql.initializeSOQLText();
            soql.setPageComplete(true);

            return super.getNextPage();
        } else {
            MessageBox msgBox = new MessageBox(getShell(), SWT.OK | SWT.ICON_ERROR);
            msgBox.setMessage(Labels.getString("ExtractionDataSelectionPage.initError")); //$NON-NLS-1$
            msgBox.open();
            return this;
        }
    }
}

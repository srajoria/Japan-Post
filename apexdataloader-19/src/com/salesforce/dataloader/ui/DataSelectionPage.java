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

package com.salesforce.dataloader.ui;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.salesforce.dataloader.action.OperationInfo;
import com.salesforce.dataloader.config.Config;
import com.salesforce.dataloader.controller.Controller;
import com.salesforce.dataloader.dao.DataAccessObjectFactory;
import com.salesforce.dataloader.exception.DataAccessObjectInitializationException;
import com.salesforce.dataloader.ui.entitySelection.EntityContentProvider;
import com.salesforce.dataloader.ui.entitySelection.EntityFilter;
import com.salesforce.dataloader.ui.entitySelection.EntityLabelProvider;
import com.salesforce.dataloader.ui.entitySelection.EntityViewerSorter;
import com.sforce.soap.partner.DescribeGlobalSObjectResult;

/**
 * Describe your class here.
 *
 * @author lviripaeff
 * @since before
 */
public class DataSelectionPage extends LoadPage {

    private final Logger logger = Logger.getLogger(DataSelectionPage.class);

    private final Controller controller;

    // These filter extensions are used to filter which files are displayed.
    private static final String[] FILTER_EXTS = { "*.csv" }; //$NON-NLS-1$
    private final EntityFilter filter = new EntityFilter();
    private ListViewer lv;

    private FileFieldEditor csvChooser;
    private Combo fieldCombo;
    
    public DataSelectionPage(Controller controller) {
        super(Labels.getString("DataSelectionPage.data"), Labels.getString("DataSelectionPage.dataMsg"), UIUtils.getImageRegistry().getDescriptor("splashscreens")); //$NON-NLS-1$ //$NON-NLS-2$

        this.controller = controller;

        // Set the description
        setDescription(Labels.getString("DataSelectionPage.message")); //$NON-NLS-1$

        setPageComplete(false);
    }

    public void createControl(Composite parent) {
        getShell().setImage(UIUtils.getImageRegistry().get("sfdc_icon")); //$NON-NLS-1$

        GridData data;

        Composite comp = new Composite(parent, SWT.NONE);

        GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.horizontalSpacing = 10;
        gridLayout.marginHeight = 20;
        gridLayout.verticalSpacing = 7;

        comp.setLayout(gridLayout);

        Composite compChooser1 = new Composite(comp, SWT.NONE);
        data = new GridData(GridData.CENTER | GridData.VERTICAL_ALIGN_END);

        compChooser1.setLayoutData(data);

        GridLayout gLayout1 = new GridLayout(2, false);
        compChooser1.setLayout(gLayout1);
        
        Label label = new Label(compChooser1, SWT.RIGHT);
        label.setText(Labels.getString("DataSelectionPage.selectObject")); //$NON-NLS-1$
        data = new GridData();
        label.setLayoutData(data);

        // Add a checkbox to toggle filter
       // Button filterAll = new Button(comp, SWT.CHECK);
       // filterAll.setText(Labels.getString("DataSelectionPage.showAll")); //$NON-NLS-1$
       // data = new GridData();
       // filterAll.setLayoutData(data);
        
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
        data.heightHint = 120;
        data.widthHint = 150;
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

       /* filterAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (((Button)event.widget).getSelection())
                    lv.removeFilter(filter);
                else
                    lv.addFilter(filter);
            }
        });*/

        new Label(comp, SWT.NONE);

        final String infoMessage = this.controller.getConfig().getOperationInfo().getInfoMessageForDataSelectionPage();
        if (infoMessage != null) {
            Label l = new Label(comp, SWT.RIGHT);
            GridData gd = new GridData();
            gd.horizontalAlignment = GridData.HORIZONTAL_ALIGN_END;
            l.setLayoutData(gd);
            l.setText(infoMessage);
            l.setForeground(new Color(getShell().getDisplay(), 0xff, 0, 0));
        }

        new Label(comp, SWT.NONE);

        //now select the csv

        Composite compChooser = new Composite(comp, SWT.NONE);
        data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_END);
        data.widthHint = 400;
        compChooser.setLayoutData(data);

        csvChooser = new FileFieldEditor(
                Labels.getString("DataSelectionPage.csv"), Labels.getString("DataSelectionPage.csvMessage"), compChooser); //$NON-NLS-1$ //$NON-NLS-2$
        csvChooser.setFileExtensions(FILTER_EXTS);
        csvChooser.setEmptyStringAllowed(false);
        csvChooser.setPropertyChangeListener(new IPropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                if ("field_editor_is_valid".equals(event.getProperty())) { //$NON-NLS-1$
                    try {

                        if (!((Boolean)event.getNewValue()).booleanValue()) {
                            setErrorMessage(Labels.getString("DataSelectionPage.selectValid")); //$NON-NLS-1$
                            checkPageComplete();

                        } else {
                            setErrorMessage(null);
                            checkPageComplete();
                        }
                    } catch (ClassCastException cle) {
                        logger.error(Labels.getString("DataSelectionPage.errorClassCast"), cle); //$NON-NLS-1$
                    }
                }
            }
        });
        
        Button preview = new Button(comp, SWT.PUSH);
        preview.setText("       "+Labels.getString("DataSelectionPage.preview")+"      ");
        preview.setLayoutData(new GridData( GridData.VERTICAL_ALIGN_END|GridData.HORIZONTAL_ALIGN_END));   
        preview.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
            	
            	if(!csvChooser.getStringValue().isEmpty())
            	{
            		  openViewer(csvChooser.getStringValue());
            		
            	}
            	else 
            	{
            		 UIUtils.errorMessageBox(getShell(), Labels.getString("DataSelectionPage.EmptyCSV"));
            	
            	}
            	
            }

            public void widgetDefaultSelected(SelectionEvent e) {}
        });
        
        final Button btnInsert = createOperationButton(comp, (OperationInfo)java.lang.reflect.Array.get(OperationInfo.ALL_OPERATIONS_IN_ORDER, 0));
        //final Button btnUpdate = createOperationButton(buttons, (OperationInfo)java.lang.reflect.Array.get(OperationInfo.ALL_OPERATIONS_IN_ORDER, 1));
        final Button btnUpsert = createOperationButton(comp, (OperationInfo)java.lang.reflect.Array.get(OperationInfo.ALL_OPERATIONS_IN_ORDER, 2));

        setControl(comp);
    }
    
    private Button createOperationButton(Composite parent, final OperationInfo info) {

    	final Button butt = new Button(parent, SWT.PUSH);
        
        butt.setText("   "+info.getLabel()+"   ");
        butt.setEnabled(info.isOperationAllowed(this.controller.getConfig()));

       // butt.setImage(info.getIconImage());
        butt.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                info.createUIAction(controller).run();
            }
        });
       // this.operationButtonsByIndex.put(info, butt);
        return butt;
    }
    
    private void openViewer(String filename) {
        
       	CSVViewerDialog dlg = new CSVViewerDialog(LoaderWindow.getApp().getShell(), controller);
            dlg.setNumberOfRows(200000);
           dlg.setFileName(filename);
           try {
               dlg.open();
            } catch (DataAccessObjectInitializationException e) {
                
            }
        }
    
    /**
     * Function to dynamically set the entity list
     */
    private void setInput(Map<String, DescribeGlobalSObjectResult> entityDescribes) {
    	controller.mapUsrObject.clear();
    	Config config = controller.getConfig();
    	String oprName=config.getOperationInfo().name();
        controller.saveConfig();
    	String UIDRights = "";
    	
    	if(oprName.equalsIgnoreCase("Upsert")){
    		UIDRights = "UPS";
    	}
    	else if(oprName.equalsIgnoreCase("Insert")){
    		UIDRights = "DI";
    	}
    	else {
    		UIDRights = "UPSDI";
    	}
    	for(com.sforce.soap.partner.sobject.SObject s : controller.arrSObj){
			if(controller.isHQ){
				if(s.getChild("HQRole__c") != null && (s.getChild("HQRole__c").getValue().toString().equalsIgnoreCase("U"))){
					controller.mapUsrObject.put((String) s.getChild("Object_API_Name__c").getValue(), s);
				}
			}
			else{
				if(s.getChild("Branch__c") != null && (s.getChild("Branch__c").getValue().toString().equalsIgnoreCase("U"))){
					controller.mapUsrObject.put((String) s.getChild("Object_API_Name__c").getValue(), s);
				}
			}
		}
    	
        OperationInfo operation = controller.getConfig().getOperationInfo();
        Map<String, DescribeGlobalSObjectResult> inputDescribes = new HashMap<String, DescribeGlobalSObjectResult>();

        // for each object, check whether the object is valid for the current
        // operation
        if (entityDescribes != null) {
            for (Entry<String, DescribeGlobalSObjectResult> entry : entityDescribes.entrySet()) {
                String objectName = entry.getKey();
                DescribeGlobalSObjectResult objectDesc = entry.getValue();
                if (operation.isDelete() && objectDesc.isDeletable() && controller.mapUsrObject.containsKey(entry.getKey())) {
                    inputDescribes.put(objectName, objectDesc);
                } else if (operation == OperationInfo.insert && objectDesc.isCreateable() && controller.mapUsrObject.containsKey(entry.getKey())) {
                    inputDescribes.put(objectName, objectDesc);
                } else if (operation == OperationInfo.update && objectDesc.isUpdateable() && controller.mapUsrObject.containsKey(entry.getKey())) {
                    inputDescribes.put(objectName, objectDesc);
                } else if (operation == OperationInfo.upsert && (objectDesc.isUpdateable() || objectDesc.isCreateable()) && controller.mapUsrObject.containsKey(entry.getKey())) {
                    inputDescribes.put(objectName, objectDesc);
                }
            }
        }
        lv.setInput(inputDescribes);
        lv.refresh();
        lv.getControl().getParent().pack();
        String[] arr = lv.getList().getItems();

        Arrays.sort(arr);
        fieldCombo.setItems(arr);

    }

    private boolean checkEntityStatus() {
        IStructuredSelection selection = (IStructuredSelection)lv.getSelection();
        DescribeGlobalSObjectResult entity = (DescribeGlobalSObjectResult)selection.getFirstElement();
        if (entity != null) {
            return true;
        }
        return false;

    }

    private void checkPageComplete() {

        if (csvChooser.isValid() && checkEntityStatus()) {
            setPageComplete(true);
        } else {
            setPageComplete(false);
        }

    }

    /**
     * Returns the next page, describes SObject and performs the total size calculation
     *
     * @return IWizardPage
     */

    @Override
    public LoadPage getNextPage() {
        //attempt to login
        //get entity
        IStructuredSelection selection = (IStructuredSelection)lv.getSelection();
        DescribeGlobalSObjectResult entity = (DescribeGlobalSObjectResult)selection.getFirstElement();

        String entityName= entity.getName();
        String entityExternalId = "";
        for(com.sforce.soap.partner.sobject.SObject s : controller.arrSObj){
        	if(s.getChild("Object_API_Name__c") != null && s.getChild("Object_API_Name__c").getValue().toString().equalsIgnoreCase(entityName)){
        		if(s.getChild("External_Id__c") != null){
        			entityExternalId = s.getChild("External_Id__c").getValue().toString();
        		}
        	}
		}
        
        if(entityExternalId.equalsIgnoreCase("")){
        	entityExternalId = "Id";
        }
        Config config = controller.getConfig();
        config.setValue(Config.ENTITY, entityName );
        // set DAO - CSV file name
        config.setValue(Config.DAO_NAME, csvChooser.getStringValue());
        // set DAO type to CSV
        config.setValue(Config.DAO_TYPE, DataAccessObjectFactory.CSV_READ_TYPE);
        config.setValue(Config.EXTERNAL_ID_FIELD, entityExternalId);
        controller.saveConfig();

        DataSelectionDialog dlg = new DataSelectionDialog(getShell(), controller);
        if (dlg.open()) {
            return super.getNextPage();
        } else {
            return this;
        }
    }

    /*
     * (non-Javadoc)
     * @see com.salesforce.dataloader.ui.LoadPage#setupPage()
     */
    @Override
    boolean setupPage() {
        Map<String, DescribeGlobalSObjectResult> describes = controller
				.getEntityDescribes();
        if(describes != null) {
            setInput(describes);
            return true;
        }
        return false;
    }
}

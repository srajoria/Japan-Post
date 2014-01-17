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

//import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.eclipse.swt.layout.GridLayout;
import org.apache.log4j.Logger;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;

import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.salesforce.dataloader.action.OperationInfo;
import com.salesforce.dataloader.config.Config;
import com.salesforce.dataloader.config.Config.ConfigListener;
import com.salesforce.dataloader.controller.Controller;
import com.salesforce.dataloader.ui.uiActions.AdvancedSettingsUIAction;
import com.salesforce.dataloader.ui.uiActions.ExitUIAction;
import com.salesforce.dataloader.ui.uiActions.HelpUIAction;
import com.salesforce.dataloader.ui.uiActions.LogoutUIAction;
import com.salesforce.dataloader.ui.uiActions.OperationUIAction;
import com.salesforce.dataloader.ui.uiActions.ViewCSVFileAction;
import com.salesforce.dataloader.util.ExceptionUtil;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.fault.ApiFault;
import com.sforce.soap.partner.fault.LoginFault;
import com.sforce.ws.ConnectionException;
import com.sun.xml.internal.bind.v2.model.core.ArrayInfo;
import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

/**
 * The main class for the Loader UI.
 *
 * @author lviripaeff
 * @since before
 */
public class LoaderWindow extends ApplicationWindow {

    //UI actions
    private final TreeMap<Integer, OperationUIAction> operationActionsByIndex;
    private final EnumMap<OperationInfo, Button> operationButtonsByIndex = new EnumMap<OperationInfo, Button>(
            OperationInfo.class);

    private TreeMap<Integer, OperationUIAction> createActionMap(Controller controller) {
        TreeMap<Integer, OperationUIAction> map = new TreeMap<Integer, OperationUIAction>();
        for (OperationInfo info : OperationInfo.values())
            map.put(info.getDialogIdx(), info.createUIAction(controller));

        return map;
    }

    private final ExitUIAction uiActionExit;
    private final ViewCSVFileAction uiActionViewSuccess;
    private final HelpUIAction uiActionHelp;
    private final AdvancedSettingsUIAction uiActionSettings;
    private final LogoutUIAction uiActionLogout;
    private final Controller controller;


    private static LoaderWindow app;

    public LoaderWindow(Controller controller) {
        super(null);

        //need to initialize the Display
        Display.getDefault();

        //Create UI actions
        this.operationActionsByIndex = createActionMap(controller);

        this.uiActionExit = new ExitUIAction();
        this.uiActionViewSuccess = new ViewCSVFileAction(controller);
        this.uiActionSettings = new AdvancedSettingsUIAction(controller);
        this.uiActionHelp = new HelpUIAction(controller);
        this.uiActionLogout = new LogoutUIAction(controller);

        app = this;
        
        addMenuBar();
        addStatusLine();

        // load values from last run
        controller.getConfig().initLastRunFile();
        this.controller = controller;

        final ConfigListener listener = new ConfigListener() {
            public void configValueChanged(String key, String oldValue, String newValue) {
                if (Config.USE_BULK_API.equals(key)) {
                    boolean boolVal = false;
                    if (newValue != null) boolVal = Boolean.valueOf(newValue);
                    LoaderWindow.this.operationButtonsByIndex.get(OperationInfo.hard_delete).setEnabled(boolVal);
                    LoaderWindow.this.operationActionsByIndex.get(OperationInfo.hard_delete.getDialogIdx()).setEnabled(
                            boolVal);
                    getShell().redraw();
                }
            }
        };

        this.controller.getConfig().addListener(listener);

    }

    public void dispose() {
        // make sure configuration gets written
        if(this.controller != null) {
            controller.saveConfig();
        }
    }

    public static LoaderWindow getApp() {
        return app;

    }

    public Controller getController() {
        return controller;
    }

    /**
     * This runs the Loader Window
     */
    public void run() {

        setBlockOnOpen(true);
        open();
        Display.getCurrent().dispose();
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        // Set the title bar text
        shell.setText(Labels.getString("SettingsPage.titleMsg")); 
        shell.setMinimumSize(430, 260);
        shell.setBounds(400, 0, 430, 600);
        //shell.setImage(UIUtils.getImageRegistry().get("sfdc_icon")); 
    }
    
    
    
    private OperationUIAction getOperationAction(int i) {
        return this.operationActionsByIndex.get(i);
    }

    private Text textPassword;
    private Text textUsername;
    private Button isSessionIdLogin;
    private Text textSessionId;
    private Text textEndpoint;
    private Label loginLabel;
    private final String nestedException = "nested exception is:";
    private static Logger logger = Logger.getLogger(SettingsPage.class);
    
    @Override
    protected Control createContents(Composite parent) {
        final Composite comp = createContainer(parent);
        createBeforeLoginControls(comp,parent);
        //createButtons(comp);

        //getStatusLineManager().setMessage(Labels.getString("LoaderWindow.chooseAction"));
        
        //comp.pack();
       // parent.pack();

        return parent;

    }
    
    public void createBeforeLoginControls(final Composite parent,final Composite mainparent) {
    	this.controller.shl = getShell();
        getShell().setImage(UIUtils.getImageRegistry().get("sfdc_icon")); //$NON-NLS-1$

        GridData data;

        Config config = controller.getConfig();

        Composite comp = new Composite(parent, SWT.NONE);
        
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 3;
        gridLayout.marginHeight = 30;
        comp.setLayout(gridLayout);
        Label labelUsername = new Label(comp, SWT.RIGHT);
        labelUsername.setText(Labels.getString("SettingsPage.username")); //$NON-NLS-1$

        textUsername = new Text(comp, SWT.BORDER);
        textUsername.setText(config.getString(Config.USERNAME));
        data = new GridData();
        data.widthHint = 300;
        textUsername.setLayoutData(data);

        Composite composite2 = new Composite(comp, SWT.NONE);
        data = new GridData();
        data.verticalSpan = 2;
        composite2.setLayoutData(data);

        Label labelPassword = new Label(comp, SWT.RIGHT);
        labelPassword.setText(Labels.getString("SettingsPage.password")); //$NON-NLS-1$

        textPassword = new Text(comp, SWT.BORDER | SWT.PASSWORD);
        // don't want to cache the password
        config.setValue(Config.PASSWORD, ""); //$NON-NLS-1$
        textPassword.setText(config.getString(Config.PASSWORD));

        data = new GridData();
        data.widthHint = 300;
        textPassword.setLayoutData(data);

        Composite composite3 = new Composite(comp, SWT.NONE);
        data = new GridData();
        data.verticalSpan = 2;
        composite3.setLayoutData(data);
        
        final Button checkBox1 = new Button(comp, SWT.CHECK);//Siddharth
        checkBox1.setText("Connect to the development environment");
        GridData rcheckBox1LData = new GridData();
        rcheckBox1LData.horizontalSpan = 3;
        rcheckBox1LData.verticalAlignment = GridData.BEGINNING;
        rcheckBox1LData.grabExcessHorizontalSpace = true;
        rcheckBox1LData.horizontalAlignment = GridData.CENTER;
        checkBox1.setLayoutData(rcheckBox1LData);
        checkBox1.addSelectionListener(new SelectionAdapter(){
            @Override public void widgetSelected(SelectionEvent e){
            	Config config = controller.getConfig();
             	if (checkBox1.getSelection()) {
             		config.setValue(Config.ENDPOINT, "https://test.salesforce.com");
             	} else {
             		config.setValue(Config.ENDPOINT, "https://login.salesforce.com");
             	}
             	controller.saveConfig();
         		controller.logout();
            }
          }
        );
        
        if(config.getBoolean(Config.SFDC_INTERNAL)) {
	        //spacer
	        Label spacer = new Label(comp, SWT.NONE);
	        data = new GridData();
	        data.horizontalSpan = 3;
	        data.widthHint = 15;
	        spacer.setLayoutData(data);
	        
	        //lIsSessionLogin checkbox        
	        Label labelIsSessionIdLogin = new Label(comp, SWT.RIGHT);
	        labelIsSessionIdLogin.setText(Labels.getString("SettingsPage.isSessionIdLogin")); //$NON-NLS-1$
	
	        isSessionIdLogin = new Button(comp, SWT.CHECK);
	        isSessionIdLogin.setSelection(config.getBoolean(Config.SFDC_INTERNAL_IS_SESSION_ID_LOGIN));
	        data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
	        data.horizontalSpan = 2;
	        isSessionIdLogin.setLayoutData(data);
	        isSessionIdLogin.addSelectionListener(new SelectionAdapter(){
	        	@Override
	        	public void widgetSelected(SelectionEvent event) {
	    			reconcileLoginCredentialFieldsEnablement();
	        	}
	        });
	
	        //sessionId
	        Label labelSessionId = new Label(comp, SWT.RIGHT);
	        labelSessionId.setText(Labels.getString("SettingsPage.sessionId")); //$NON-NLS-1$
	
	        textSessionId = new Text(comp, SWT.BORDER);
	        textSessionId.setText(config.getString(Config.SFDC_INTERNAL_SESSION_ID));
	        data = new GridData();
	        data.widthHint = 150;
	        data.horizontalSpan = 2;
	        textSessionId.setLayoutData(data);
	
	        //endpoint
	        Label labelEndpoint = new Label(comp, SWT.RIGHT);
	        labelEndpoint.setText(Labels.getString("SettingsPage.instServerUrl")); //$NON-NLS-1$
	
	        textEndpoint = new Text(comp, SWT.BORDER);
	        textEndpoint.setText(config.getString(Config.ENDPOINT));
	        data = new GridData();
	        data.widthHint = 150;
	        textEndpoint.setLayoutData(data);

	        reconcileLoginCredentialFieldsEnablement();
        }
        
        Label clearLabel = new Label(comp, SWT.NONE);
        data = new GridData();
        data.horizontalSpan = 3;
        data.widthHint = 15;
        clearLabel.setLayoutData(data);

        loginLabel = new Label(comp, SWT.NONE);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = 3;
        data.widthHint = 220;
        loginLabel.setLayoutData(data);

        Button loginButton = new Button(comp, SWT.PUSH);
        loginButton.setText(Labels.getString("SettingsPage.login")); //$NON-NLS-1$
        data = new GridData(GridData.HORIZONTAL_ALIGN_END);
        data.horizontalSpan = 2;
        data.widthHint = 75;
        loginButton.setLayoutData(data);
        loginButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                Config config = controller.getConfig();
                config.setValue(Config.USERNAME, textUsername.getText());
                config.setValue(Config.PASSWORD, textPassword.getText());
                
                if(config.getBoolean(Config.SFDC_INTERNAL)) {
                	config.setValue(Config.SFDC_INTERNAL_IS_SESSION_ID_LOGIN, isSessionIdLogin.getSelection());
                	config.setValue(Config.SFDC_INTERNAL_SESSION_ID, textSessionId.getText());
                	config.setValue(Config.ENDPOINT, textEndpoint.getText());
                }
                
                controller.saveConfig();

                loginLabel.setText(Labels.getString("SettingsPage.verifyingLogin")); //$NON-NLS-1$

                BusyIndicator.showWhile(Display.getDefault(), new Thread() {
                    @Override
                    public void run() {
                        try {
                            if (controller.login() && controller.setEntityDescribes()) {
                                loginLabel.setText(Labels.getString("SettingsPage.loginSuccessful")); //$NON-NLS-1$
                                controller.saveConfig();
                                
                                QueryResult qr;
                            	try {
                        			String query ="SELECT ID,UserRole__c from user where id =\'"+controller.getPartnerClient().getUserInfo().getUserId()+"\'";
                        			qr = controller.getPartnerClient().query(query);
                        			com.sforce.soap.partner.sobject.SObject[] userArr= qr.getRecords();
                        			if(userArr[0] != null && userArr[0].getChild("SyozokuKyokusyoCode__c") != null && userArr[0].getChild("SyozokuKyokusyoCode__c").getValue().toString().equalsIgnoreCase("019000")){
                        				controller.isHQ = true;
                        			}
                        			else{
                        				controller.isHQ = false;
                        			}
                        			
                        		} catch (ConnectionException e1) {
                        			// TODO Auto-generated catch block
                        			e1.printStackTrace();
                        		}
                            	
                            	try {
                        			qr = controller.getPartnerClient().query("Select Name, External_Id__c,Object_API_Name__c, JPN_Object_Name__c, Object_Name_Eng__c, HQRole__c, Branch__c,Upsert_DelInsertFLG__c from ObjectPermissions__c");
                        			controller.arrSObj= qr.getRecords();
                        			
                        			
                        		} catch (ConnectionException e) {
                        			// TODO Auto-generated catch block
                        			e.printStackTrace();
                        		}
                                
                                parent.setVisible(false);
                                mainparent.getShell().setText(Labels.getString("LoaderWindow.title"));
                                //mainparent.getShell().setSize(352, 262);
                                
                                mainparent.setSize(502, 262);
                                createAfterLoginControls(mainparent);
                               // mainparent.getShell().redraw();
                               // loadDataSelectionPage(controller);

                            } else {
                                loginLabel.setText(Labels.getString("SettingsPage.invalidLogin")); //$NON-NLS-1$
                                //setPageComplete(false);
                            }
                        } catch (LoginFault lf ) {
                            loginLabel.setText(Labels.getString("SettingsPage.invalidLogin"));
                            //setPageComplete(false);
                        } catch (ApiFault e) {
                            String msg = e.getExceptionMessage();
                            processException(msg);
                            logger.error(msg);
                        } catch (ConnectionException e) {
                            String msg = e.getMessage();
                            processException(msg);
                            logger.error(msg);
                        } catch (Throwable e) {
                            String msg = e.getMessage();
                            processException(msg);
                            logger.error(msg);
                            logger.error("\n" + ExceptionUtil.getStackTraceString(e));
                        }
                    }

                    /**
                     * @param msg
                     */
                    private void processException(String msg) {
                        if (msg == null || msg.length() < 1) {
                            loginLabel.setText(Labels.getString("SettingsPage.invalidLogin"));
                        } else {
                            int x = msg.indexOf(nestedException);
                            if (x >= 0) {
                                x += nestedException.length();
                                msg = msg.substring(x);
                            }
                            loginLabel.setText(msg.replace('\n', ' ').trim());
                        }
                      //  setPageComplete(false);
                    }

                });

            }

        });
        parent.getShell().setDefaultButton(loginButton);

        Composite composite5 = new Composite(comp, SWT.NONE);
        data = new GridData();
        data.horizontalSpan = 2;
        composite5.setLayoutData(data);

        //setControl(comp);
    }
    
    public void createAfterLoginControls(Composite parent){
    	final Composite comp = createContainer(parent);
        createButtons(comp);

        getStatusLineManager().setMessage(Labels.getString("LoaderWindow.chooseAction"));
        comp.setSize(482, 222);
        //comp.pack();
        //parent.pack();
    }

    private void reconcileLoginCredentialFieldsEnablement() {
		textUsername.setEnabled(!isSessionIdLogin.getSelection());
		textPassword.setEnabled(!isSessionIdLogin.getSelection());
		textSessionId.setEnabled(isSessionIdLogin.getSelection());
		textEndpoint.setEnabled(isSessionIdLogin.getSelection());
	}
    
    private Composite createContainer(Composite parent) {
        Composite comp = new Composite(parent, SWT.BORDER);
        setBackground(comp);
        comp.setLayout(new FillLayout(SWT.VERTICAL));
        Label titleImage = new Label(comp, SWT.CENTER);
        setBackground(titleImage);
        titleImage.setImage(UIUtils.getImageRegistry().get("logo"));
        //titleImage.setSize(100, 100);
        comp.pack();
        return comp;
    }

    private void setBackground(Control comp) {
        comp.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
    }

    private void createButtons(Composite parent) {
    	
    	Composite rbuttons = new Composite(parent, SWT.NONE);
    	RowLayout rowLayout1 = new RowLayout(SWT.HORIZONTAL);
    	rowLayout1.wrap = true;
    	rowLayout1.pack = true;
    	rowLayout1.justify = true;
    	rowLayout1.marginLeft = 60;
    	rowLayout1.marginRight = 10;
    	rowLayout1.marginTop = 2;
    	rowLayout1.marginBottom = 2;
    	rowLayout1.spacing = 5;
        rbuttons.setLayout(rowLayout1);
        Button upLoad = new Button(rbuttons, SWT.PUSH);
        upLoad.setText(Labels.getString("UI.upload"));
        upLoad.setSelection(true);
        
        final Button downLoad = new Button(rbuttons, SWT.PUSH);
        downLoad.setText(Labels.getString("UI.download"));

        /*Composite buttons = new Composite(parent, SWT.NONE);
        setBackground(buttons);
        RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
        rowLayout.wrap = true;
        rowLayout.pack = true;
        rowLayout.justify = true;
        rowLayout.marginLeft = 30;
        rowLayout.marginRight = 10;
        rowLayout.marginTop = 20;
        rowLayout.marginBottom = 10;
        rowLayout.spacing = 20;
        buttons.setLayout(rowLayout);*/
        
        // create all the buttons, in order
        //final Button btnInsert = createOperationButton(buttons, (OperationInfo)java.lang.reflect.Array.get(OperationInfo.ALL_OPERATIONS_IN_ORDER, 0));
        //final Button btnUpdate = createOperationButton(buttons, (OperationInfo)java.lang.reflect.Array.get(OperationInfo.ALL_OPERATIONS_IN_ORDER, 1));
      //  final Button btnUpsert = createOperationButton(buttons, (OperationInfo)java.lang.reflect.Array.get(OperationInfo.ALL_OPERATIONS_IN_ORDER, 2));
        //final Button btnDelete = createOperationButton(buttons, (OperationInfo)java.lang.reflect.Array.get(OperationInfo.ALL_OPERATIONS_IN_ORDER, 3));
       // Button btnHard_delete = createOperationButton(buttons, (OperationInfo)java.lang.reflect.Array.get(OperationInfo.ALL_OPERATIONS_IN_ORDER, 4));
       // final Button btnExtract = createOperationButton(buttons, (OperationInfo)java.lang.reflect.Array.get(OperationInfo.ALL_OPERATIONS_IN_ORDER, 5));
       // btnInsert.setEnabled(true);
    	//btnUpsert.setEnabled(true);
    	//btnExtract.setEnabled(false);
    	
        upLoad.addSelectionListener(new SelectionAdapter(){
            @Override public void widgetSelected(SelectionEvent e){
        //    	btnInsert.setEnabled(true);
            	//btnUpdate.setEnabled(true);
         //   	btnUpsert.setEnabled(true);
            	//btnDelete.setEnabled(true);
         //   	btnExtract.setEnabled(false);
            	
            	DataSelectionPageNew ds = new DataSelectionPageNew(controller);
            	ds.createControl();
            }
          }
        );
        downLoad.addSelectionListener(new SelectionAdapter(){
            @Override public void widgetSelected(SelectionEvent e){
          //  	btnInsert.setEnabled(false);
            	//btnUpdate.setEnabled(false);
          //  	btnUpsert.setEnabled(false);
            	//btnDelete.setEnabled(false);
         //   	btnExtract.setEnabled(true);
            }
          }
        );
        /*for (final OperationInfo info : OperationInfo.ALL_OPERATIONS_IN_ORDER) {
            createOperationButton(buttons, info);
        }*/
        // buttons.pack();
    }

    private Button createOperationButton(Composite parent, final OperationInfo info) {

    	final Button butt = new Button(parent, SWT.PUSH);
        
        butt.setText("   "+info.getLabel()+"   ");
        butt.setEnabled(info.isOperationAllowed(this.controller.getConfig()));

       // butt.setImage(info.getIconImage());
        butt.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                info.createUIAction(getController()).run();
            }
        });
        this.operationButtonsByIndex.put(info, butt);
        return butt;
    }
    
    
    
    /*private void createOperationButton(Composite parent, final OperationInfo info) {
        final Button butt = new Button(parent, SWT.PUSH);

        butt.setText(info.getLabel());
        butt.setEnabled(info.isOperationAllowed(this.controller.getConfig()));
       // butt.setImage(info.getIconImage());
        butt.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                info.createUIAction(getController()).run();
            }
        });
        this.operationButtonsByIndex.put(info, butt);
    }*/

    private void displayTitleDialog(final Display display, final TreeMap<Integer, OperationUIAction> map,
            final Config cfg) {

        display.asyncExec(new Thread() {
            @Override
            public void run() { 
              //  LoaderTitleDialog dlg = new LoaderTitleDialog(display.getActiveShell(), cfg);
              //  int result = dlg.open();

              //  for (Entry<Integer, OperationUIAction> ent : map.entrySet())
               //     if (result == ent.getKey()) ent.getValue().run();
            }
        });
    }

    /**
     * Creates the menu for the application
     * 
     * @return MenuManager
     */
    @Override
    protected MenuManager createMenuManager() {
        // Create the main menu
        MenuManager mm = new MenuManager();

        // Create the File menu
        /*MenuManager fileMenu = new MenuManager(Labels.getString("LoaderWindow.file")); //$NON-NLS-1$
        mm.add(fileMenu);

        // Add the actions to the File menu, in the correct order
        for (OperationInfo info : OperationInfo.ALL_OPERATIONS_IN_ORDER)
            fileMenu.add(getOperationAction(info.getDialogIdx()));

        fileMenu.add(uiActionLogout);
        fileMenu.add(new Separator());
        fileMenu.add(uiActionExit);*/


        //Create the settings menu
       // MenuManager settingsMenu = new MenuManager(Labels.getString("LoaderWindow.settings")); //$NON-NLS-1$
        //settingsMenu.add(uiActionSettings);
       // mm.add(settingsMenu);

        // Create the View menu
        /*MenuManager viewMenu = new MenuManager(Labels.getString("LoaderWindow.view")); //$NON-NLS-1$
        mm.add(viewMenu);

        // Add the actions to the View menu
        viewMenu.add(uiActionViewSuccess);

        // Create the Help menu
        MenuManager helpMenu = new MenuManager(Labels.getString("LoaderWindow.help")); //$NON-NLS-1$
        helpMenu.add(uiActionHelp);
        mm.add(helpMenu);*/

        // Add the actions to the Help menu

        return mm;
    }

}

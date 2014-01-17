package com.salesforce.dataloader.ui;

import java.util.EnumMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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
import com.salesforce.dataloader.ui.uiActions.OperationUIAction;
import com.salesforce.dataloader.util.ExceptionUtil;
import com.sforce.soap.partner.fault.ApiFault;
import com.sforce.soap.partner.fault.LoginFault;
import com.sforce.ws.ConnectionException;

public class LoginWindow extends ApplicationWindow{
	 private final Controller controller;
	    private Text textPassword;
	    private Text textUsername;
	    private Button isSessionIdLogin;
	    private Text textSessionId;
	    private Text textEndpoint;
	    private Label loginLabel;
	    private final String nestedException = "nested exception is:";
	    
	    private static LoginWindow app;
	 // logger
	    private static Logger logger = Logger.getLogger(SettingsPage.class);
	    
	    private final TreeMap<Integer, OperationUIAction> operationActionsByIndex;
	    private final EnumMap<OperationInfo, Button> operationButtonsByIndex = new EnumMap<OperationInfo, Button>(
	            OperationInfo.class);
	    
	    private TreeMap<Integer, OperationUIAction> createActionMap(Controller controller) {
	        TreeMap<Integer, OperationUIAction> map = new TreeMap<Integer, OperationUIAction>();
	        for (OperationInfo info : OperationInfo.values())
	            map.put(info.getDialogIdx(), info.createUIAction(controller));

	        return map;
	    }
	    public LoginWindow(Controller controller) {
	        //super(Labels.getString("SettingsPage.title"), Labels.getString("SettingsPage.titleMsg"), UIUtils.getImageRegistry().getDescriptor("splashscreens")); //$NON-NLS-1$ //$NON-NLS-2$
	    	super(null);
	    	Display.getDefault();
	    	 this.operationActionsByIndex = createActionMap(controller);
	        this.controller = controller;

	        app = this;
	        
	        addMenuBar();
	        addStatusLine();
	        controller.getConfig().initLastRunFile();
	        
	        final ConfigListener listener = new ConfigListener() {
	            public void configValueChanged(String key, String oldValue, String newValue) {
	                if (Config.USE_BULK_API.equals(key)) {
	                    boolean boolVal = false;
	                    if (newValue != null) boolVal = Boolean.valueOf(newValue);
	                    LoginWindow.this.operationButtonsByIndex.get(OperationInfo.hard_delete).setEnabled(boolVal);
	                    LoginWindow.this.operationActionsByIndex.get(OperationInfo.hard_delete.getDialogIdx()).setEnabled(
	                            boolVal);
	                    getShell().redraw();
	                }
	            }
	        };

	        this.controller.getConfig().addListener(listener);

	        // Set the description
	        //setDescription(Labels.getString("SettingsPage.enterUsernamePassword")); //$NON-NLS-1$


	    }
	    
	    public static LoginWindow getApp() {
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
	        shell.setMinimumSize(350, 260);
	 		
	        //shell.setImage(UIUtils.getImageRegistry().get("sfdc_icon")); 
	    }
	    
	    @Override
	    protected Control createContents(Composite parent) {
	        final Composite comp = createContainer(parent);
	        createBeforeLoginControls(comp,parent);
	        //createButtons(comp);

	        //getStatusLineManager().setMessage(Labels.getString("LoaderWindow.chooseAction"));
	        
	        comp.pack();
	        parent.pack();

	        return parent;

	    }
	    
	    public void createBeforeLoginControls(final Composite parent,final Composite mainparent) {
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
	                                parent.setVisible(false);
	                                mainparent.getShell().setText(Labels.getString("LoaderWindow.title"));
	                                mainparent.getShell().setSize(352, 262);
	                                mainparent.setSize(352, 262);
	                                createAfterLoginControls(mainparent);
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
	        comp.pack();
	        parent.pack();
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
	    
	    private OperationUIAction getOperationAction(int i) {
	        return this.operationActionsByIndex.get(i);
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
	        Button upLoad = new Button(rbuttons, SWT.RADIO);
	        upLoad.setText(Labels.getString("UI.upload"));
	        upLoad.setSelection(true);
	        
	        final Button downLoad = new Button(rbuttons, SWT.RADIO);
	        downLoad.setText(Labels.getString("UI.download"));

	        Composite buttons = new Composite(parent, SWT.NONE);
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
	        buttons.setLayout(rowLayout);
	        
	        // create all the buttons, in order
	        final Button btnInsert = createOperationButton(buttons, (OperationInfo)java.lang.reflect.Array.get(OperationInfo.ALL_OPERATIONS_IN_ORDER, 0));
	        //final Button btnUpdate = createOperationButton(buttons, (OperationInfo)java.lang.reflect.Array.get(OperationInfo.ALL_OPERATIONS_IN_ORDER, 1));
	        final Button btnUpsert = createOperationButton(buttons, (OperationInfo)java.lang.reflect.Array.get(OperationInfo.ALL_OPERATIONS_IN_ORDER, 2));
	        //final Button btnDelete = createOperationButton(buttons, (OperationInfo)java.lang.reflect.Array.get(OperationInfo.ALL_OPERATIONS_IN_ORDER, 3));
	       // Button btnHard_delete = createOperationButton(buttons, (OperationInfo)java.lang.reflect.Array.get(OperationInfo.ALL_OPERATIONS_IN_ORDER, 4));
	        final Button btnExtract = createOperationButton(buttons, (OperationInfo)java.lang.reflect.Array.get(OperationInfo.ALL_OPERATIONS_IN_ORDER, 5));
	        btnInsert.setEnabled(true);
        	btnUpsert.setEnabled(true);
        	btnExtract.setEnabled(false);
	        upLoad.addSelectionListener(new SelectionAdapter(){
	            @Override public void widgetSelected(SelectionEvent e){
	            	btnInsert.setEnabled(true);
	            	//btnUpdate.setEnabled(true);
	            	btnUpsert.setEnabled(true);
	            	//btnDelete.setEnabled(true);
	            	btnExtract.setEnabled(false);
	            }
	          }
	        );
	        downLoad.addSelectionListener(new SelectionAdapter(){
	            @Override public void widgetSelected(SelectionEvent e){
	            	btnInsert.setEnabled(false);
	            	//btnUpdate.setEnabled(false);
	            	btnUpsert.setEnabled(false);
	            	//btnDelete.setEnabled(false);
	            	btnExtract.setEnabled(true);
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
}

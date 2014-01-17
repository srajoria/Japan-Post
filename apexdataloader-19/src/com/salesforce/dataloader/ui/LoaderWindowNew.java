package com.salesforce.dataloader.ui;

import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Label;
import java.awt.RenderingHints;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.ColorModel;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;



import org.apache.log4j.Logger;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.window.ApplicationWindow;
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
public class LoaderWindowNew  {
private JFrame window;
    //UI actions
    private final TreeMap<Integer, OperationUIAction> operationActionsByIndex;
    private final EnumMap<OperationInfo, Button> operationButtonsByIndex = new EnumMap<OperationInfo, Button>(
            OperationInfo.class);

    private TreeMap<Integer, OperationUIAction> createActionMap(Controller controller) {
        return null;
    }

    private final ExitUIAction uiActionExit;
    private final ViewCSVFileAction uiActionViewSuccess;
    private final HelpUIAction uiActionHelp;
    private final AdvancedSettingsUIAction uiActionSettings;
    private final LogoutUIAction uiActionLogout;
    private final Controller controller;


    private static LoaderWindowNew app;
    private  Config config;
    public LoaderWindowNew(Controller controller) {
         window=new JFrame("salesforce login");
         comp = new JPanel();
        //Create UI actions
        this.operationActionsByIndex = createActionMap(controller);

        this.uiActionExit = new ExitUIAction();
        this.uiActionViewSuccess = new ViewCSVFileAction(controller);
        this.uiActionSettings = new AdvancedSettingsUIAction(controller);
        this.uiActionHelp = new HelpUIAction(controller);
        this.uiActionLogout = new LogoutUIAction(controller);

        app = this;
        // load values from last run
        controller.getConfig().initLastRunFile();
        this.controller = controller;

        final ConfigListener listener = new ConfigListener() {
            public void configValueChanged(String key, String oldValue, String newValue) {
                if (Config.USE_BULK_API.equals(key)) {
                    boolean boolVal = false;
                    if (newValue != null) boolVal = Boolean.valueOf(newValue);
                    LoaderWindowNew.this.operationButtonsByIndex.get(OperationInfo.hard_delete).setEnabled(boolVal);
                    LoaderWindowNew.this.operationActionsByIndex.get(OperationInfo.hard_delete.getDialogIdx()).setEnabled(
                            boolVal);
                 }
            }
        };

        this.controller.getConfig().addListener(listener);

    }

    public static LoaderWindowNew getApp() {
        return app;

    }

    public Controller getController() {
        return controller;
    }

    /**
     * This runs the Loader Window
     */
    public void run() {
    	createBeforeLoginControls();
    }
    
    private OperationUIAction getOperationAction(int i) {
        return this.operationActionsByIndex.get(i);
    }

    private JPanel comp;   
    private JPasswordField textPassword;
    private JTextField textUsername;
    private JButton isSessionIdLogin;
    private JTextField textSessionId;
    private JTextField textEndpoint;
    private JLabel loginLabel;
    private final String nestedException = "nested exception is:";
    private JCheckBox checkBox1;
    private static Logger logger = Logger.getLogger(SettingsPage.class);
    JPanel btnPanel;
    
    public void createBeforeLoginControls() {
        //getShell().setImage(UIUtils.getImageRegistry().get("sfdc_icon")); //$NON-NLS-1$
    	
         config = controller.getConfig();

		comp.setLayout(new BoxLayout(comp, BoxLayout.Y_AXIS));;
        JLabel lblLogoImage = new JLabel("Logo Image");
        btnPanel = new JPanel();
        
        comp.add(lblLogoImage);
        
        JPanel userDetails = new JPanel();
        userDetails.setLayout(new GridLayout(0, 2));
        JLabel labelUsername = new JLabel(Labels.getString("SettingsPage.username"));
        labelUsername.setText(Labels.getString("SettingsPage.username")); //$NON-NLS-1$
        userDetails.add(labelUsername);
        textUsername = new JTextField(config.getString(Config.USERNAME));
        userDetails.add(textUsername);
        JLabel labelPassword = new JLabel(Labels.getString("SettingsPage.password"));
        labelPassword.setText(Labels.getString("SettingsPage.password")); //$NON-NLS-1$

        textPassword = new JPasswordField(config.getString(Config.PASSWORD));
        // don't want to cache the password
        config.setValue(Config.PASSWORD, "Appirio#123lUEZIQm5DqRubtSuH9i1W6ZQ"); //$NON-NLS-1$
        textPassword.setText(config.getString(Config.PASSWORD));
        textPassword.setSize(50, 50);
        userDetails.add(labelPassword);
        userDetails.add(textPassword);
        
        comp.add(userDetails); 
        checkBox1 = new JCheckBox("Connect to the development environment");//Siddharth
        comp.add(checkBox1);
        config = controller.getConfig();
		System.out.print("check box value  "+checkBox1.isEnabled());
		 
		checkBox1Handler checkBox1Listener = new checkBox1Handler();
		
	    checkBox1.addItemListener(checkBox1Listener);
        controller.saveConfig();
 		controller.logout();
 	
       
        
        if(config.getBoolean(Config.SFDC_INTERNAL)) {
        //lIsSessionLogin checkbox        
	        Label labelIsSessionIdLogin = new Label(Labels.getString("SettingsPage.isSessionIdLogin"));
	        labelIsSessionIdLogin.setText(Labels.getString("SettingsPage.isSessionIdLogin")); //$NON-NLS-1$
	       
	        
	        final Checkbox isSessionIdLogin = new Checkbox("Config.SFDC_INTERNAL_IS_SESSION_ID_LOGIN)");
	        if(checkBox1.isEnabled())
	        {
	        	 config = controller.getConfig();
	         	if (checkBox1.isEnabled()) {
	         		reconcileLoginCredentialFieldsEnablement();
	         	} 
	         	controller.saveConfig();
	     		controller.logout();
	        }
	        //sessionId
	        Label labelSessionId = new Label(Labels.getString("SettingsPage.sessionId"));
	        labelSessionId.setText(Labels.getString("SettingsPage.sessionId")); //$NON-NLS-1$
	
	        textSessionId = new JTextField(config.getString(Config.SFDC_INTERNAL_SESSION_ID));
	        textSessionId.setText(config.getString(Config.SFDC_INTERNAL_SESSION_ID));
	        //endpoint
	        Label labelEndpoint = new Label(Labels.getString("SettingsPage.instServerUrl"));
	        labelEndpoint.setText(Labels.getString("SettingsPage.instServerUrl")); //$NON-NLS-1$
	
	        textEndpoint = new JTextField(config.getString(Config.ENDPOINT));
	        textEndpoint.setText(config.getString(Config.ENDPOINT));

	        reconcileLoginCredentialFieldsEnablement();
        }
        loginLabel = new JLabel();
        comp.add(loginLabel);
        JButton loginButton = new JButton(Labels.getString("SettingsPage.login"));
        comp.add(loginButton);
        
        loginButtonHandler loginButtonListener = new loginButtonHandler();
        loginButton.addActionListener(loginButtonListener);

      window.setContentPane(comp);
      window.setSize(200, 150);
      window.setVisible(true);
      window.pack();
    }
    
    

    private void reconcileLoginCredentialFieldsEnablement() {
    	//vv
    	textUsername.setEnabled(true);
		textPassword.setEnabled(true);
		textSessionId.setEnabled(true);
		textEndpoint.setEnabled(true);
	}
    
    private Container createContainer(Container parent) {
    	Container comp =  new Container();
       comp.setLayout(new FlowLayout());
       Label titleImage = new Label("For Image");
       comp.add(titleImage);
       return comp;
    }

    
    private Button createOperationButton(Container parent, final OperationInfo info) {
        return null;
    }

    private class checkBox1Handler implements ItemListener {
    	public void itemStateChanged(ItemEvent e) {
	    
    		if (e.getStateChange() == ItemEvent.SELECTED) {
        		 System.out.print("check box value  "+checkBox1.isEnabled());
        		config.setValue(Config.ENDPOINT, "https://test.salesforce.com");
        	} 
        	else {
        		 System.out.print("check box value  "+checkBox1.isEnabled());
        		config.setValue(Config.ENDPOINT, "https://login.salesforce.com");
        	}
    		
    	}
    	}

    private class loginButtonHandler implements ActionListener {

    	public void actionPerformed(ActionEvent e) {
    		Config config = controller.getConfig();
            config.setValue(Config.USERNAME, textUsername.getText());
            config.setValue(Config.PASSWORD, textPassword.getText());
           
            if(config.getBoolean(Config.SFDC_INTERNAL)) {
            	config.setValue(Config.SFDC_INTERNAL_IS_SESSION_ID_LOGIN, isSessionIdLogin.getText());
            	config.setValue(Config.SFDC_INTERNAL_SESSION_ID, textSessionId.getText());
            	config.setValue(Config.ENDPOINT, textEndpoint.getText());
            }
            
            controller.saveConfig();
            
            loginLabel.setText(Labels.getString("SettingsPage.verifyingLogin")); //$NON-NLS-1$
            System.out.println("in loginButtonHandler bef try");
           
                    try {
                        if (controller.login() && controller.setEntityDescribes()) {
                            loginLabel.setText(Labels.getString("SettingsPage.loginSuccessful")); //$NON-NLS-1$
                            controller.saveConfig();
                            System.out.println("in loginButtonHandler try");
                            QueryResult qr;
                        	try {
                    			String query ="SELECT ID,UserRole__c from user where id ='"+controller.getPartnerClient().getUserInfo().getUserId()+"\'";
                    			qr = controller.getPartnerClient().query(query);
                    			com.sforce.soap.partner.sobject.SObject[] userArr= qr.getRecords();
                    			if(userArr[0] != null && userArr[0].getChild("SyozokuKyokusyoCode__c") != null && 

                                   userArr[0].getChild("SyozokuKyokusyoCode__c").getValue().toString().equalsIgnoreCase("019000")){
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
                    			qr = controller.getPartnerClient().query("Select Name,Object_API_Name__c, JPN_Object_Name__c, Object_Name_Eng__c, HQRole__c, Branch__c,Upsert_DelInsertFLG__c from ObjectPermissions__c");
                    			controller.arrSObj= qr.getRecords();
                    			
                    			
                    		} catch (ConnectionException ex) {
                    			// TODO Auto-generated catch block
                    			ex.printStackTrace();
                    		}
                    		window.setSize(502, 262);
                    		comp.setVisible(false);
                    		createAfterLoginButtons();

                        } else {
                            loginLabel.setText(Labels.getString("SettingsPage.invalidLogin")); //$NON-NLS-1$
                        }
                    } catch (LoginFault lf ) {
                        loginLabel.setText(Labels.getString("SettingsPage.invalidLogin"));
                    } catch (ApiFault e4) {
                        String msg = e4.getExceptionMessage();
                        logger.error(msg);
                    } catch (ConnectionException e5) {
                        String msg = e5.getMessage();
                        logger.error(msg);
                    } catch (Throwable e6) {
                        String msg = e6.getMessage();
                        logger.error(msg);
                        logger.error("\n" + ExceptionUtil.getStackTraceString(e6));
                    }
        }

    }
    
    
 private void createAfterLoginButtons() {
	 	JPanel parent = new JPanel();
		 JLabel lblLogoImage = new JLabel("Logo Image");
	     btnPanel = new JPanel();
	     
	     parent.add(lblLogoImage);
    	JPanel rbuttons = new JPanel();
    	parent.add(rbuttons);
    	rbuttons.setLayout(new GridLayout(0,2));
        JButton upLoad = new JButton(Labels.getString("UI.upload"));
        //upLoad.setSelection(true);
        
        final JButton downLoad = new JButton(Labels.getString("UI.download"));
        rbuttons.add(upLoad);
        rbuttons.add(downLoad);
        window.setContentPane(parent);
        upLoad.addActionListener(new ActionListener(){
            @Override public void actionPerformed(ActionEvent e){
            	
            	DataSelectionPageNew ds = new DataSelectionPageNew(controller);
            	ds.createControl();
            }
          }
        );
        downLoad.addActionListener(new ActionListener(){
            @Override public void actionPerformed(ActionEvent e){
            	
            	//TODO
            }
          }
        );
       
    }
    
    private GridBagConstraints addComponent(int x, int y, int width, int height) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = x;
		c.gridy = y;
		c.gridwidth = width;
		c.gridheight = height;
		return c;
	}
}
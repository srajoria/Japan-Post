package com.salesforce.dataloader.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Vector;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import org.apache.log4j.Logger;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
/*import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;*/

//import com.appirio.yef.tools.common.AppConsoleLogger;
import com.salesforce.dataloader.action.OperationInfo;
import com.salesforce.dataloader.config.Config;
import com.salesforce.dataloader.controller.Controller;
import com.salesforce.dataloader.dao.DataAccessObjectFactory;
import com.salesforce.dataloader.dao.DataReader;
import com.salesforce.dataloader.exception.DataAccessObjectException;
import com.salesforce.dataloader.exception.DataAccessObjectInitializationException;
import com.salesforce.dataloader.exception.MappingInitializationException;
import com.salesforce.dataloader.ui.entitySelection.EntityContentProvider;
import com.salesforce.dataloader.ui.entitySelection.EntityFilter;
import com.salesforce.dataloader.ui.entitySelection.EntityLabelProvider;
import com.salesforce.dataloader.ui.entitySelection.EntityViewerSorter;
import com.salesforce.dataloader.util.DAORowUtil;
//import com.sforce.async.BulkConnection;
import com.sforce.soap.partner.DescribeGlobalSObjectResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;

/**
 * Describe your class here.
 *
 * @author lviripaeff
 * @since before
 */
public class DataSelectionPageNew {

    private final Logger logger = Logger.getLogger(DataSelectionPageNew.class);

    private final Controller controller;
    private JFrame window;
	private JTextField filename;
	private JTextArea defaultConsole;
	private JPanel content;
	private JPanel header;
	private JPanel afterLogin;

	private JButton chooseButton;
	private JButton previewButton;
	private JButton diButton;
	private JButton upsertButton;
	private JButton validateButton;
	private DefaultTableModel csvPreview;
	private JTable csvTable;
	private JScrollPane csvScrollPane;
	private JTabbedPane tabbedpane = new JTabbedPane();
	private File file;
	private Properties properties = new Properties();
	
	// These filter extensions are used to filter which files are displayed.
    private static final String[] FILTER_EXTS = { "*.csv" }; //$NON-NLS-1$
    private final EntityFilter filter = new EntityFilter();
    private ListViewer lv;

    private FileFieldEditor csvChooser;
    private JComboBox fieldCombo;
    private JFileChooser chooser;
	private JTextField textField;
	public final static Cursor busyCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
	private JPanel csvview;
	
    public DataSelectionPageNew(Controller controller) {
       // super(Labels.getString("DataSelectionPage.data"), Labels.getString("DataSelectionPage.dataMsg"), UIUtils.getImageRegistry().getDescriptor("splashscreens")); //$NON-NLS-1$ //$NON-NLS-2$

    	window = new JFrame("DataIntegrator");
		window.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        this.controller = controller;
        header = new JPanel();
        content = new JPanel();
        // Set the description
        //setDescription(Labels.getString("DataSelectionPage.message")); //$NON-NLS-1$
    }
    public void createControl() {
    	GridBagLayout gbl = new GridBagLayout();
		header.setLayout(gbl);
		fieldCombo = new JComboBox();
		csvview = new JPanel();
		
		csvPreview = new DefaultTableModel();
		csvTable = new JTable(csvPreview);
		csvTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		
		JLabel label = new JLabel(Labels.getString("DataSelectionPage.selectObject"));

		final String infoMessage = this.controller.getConfig().getOperationInfo().getInfoMessageForDataSelectionPage();
		final JLabel lbl  = new JLabel();
		if (infoMessage != null) {
        	lbl.setText(infoMessage);
        };
        final JFileChooser csvFile = new JFileChooser();
		csvFile.setMultiSelectionEnabled(false);
		FileNameExtensionFilter ffilter = new FileNameExtensionFilter("CSV Files", "csv");
		csvFile.setFileFilter(ffilter);
		filename = new JTextField(18);
		filename.setEnabled(false);
		chooseButton = new JButton("Select");//
		ChooseButtonHandler chooseListener = new ChooseButtonHandler(csvFile, filename, window);
		chooseButton.addActionListener(chooseListener);
		
		previewButton = new JButton("Preview");
		PreviewButtonHandler prieviewListener = new PreviewButtonHandler();
		previewButton.addActionListener(prieviewListener);

		
		diButton = new JButton("delete/insert");
		upsertButton = new JButton("upsert");
		
		diButton.setEnabled(false);
		upsertButton.setEnabled(false);
		
		diButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
            	if(filename.getText().equalsIgnoreCase("")){
            		JOptionPane.showMessageDialog(window,"Please select file first");
            		return;
            	}
            	else{
            		
                    Config config = controller.getConfig();
                    // set DAO - CSV file name
                    config.setValue(Config.DAO_NAME,csvFile.getSelectedFile().getAbsolutePath());
                    // set DAO type to CSV
                    config.setValue(Config.DAO_TYPE, DataAccessObjectFactory.CSV_READ_TYPE);
                    controller.saveConfig();
                    
                    window.setCursor(busyCursor);
                    
                    Boolean success = true;
                    

                    try {
                        controller.setFieldTypes();
                        controller.setReferenceDescribes();

                        String daoPath = controller.getConfig().getString(Config.DAO_NAME);
                        File file = new File(daoPath);

                        if (!file.exists() || !file.canRead()) {
                            success = false;
                            lbl.setText(Labels.getString("DataSelectionDialog.errorRead")); //$NON-NLS-1$
                            return;
                        }

                        try {
                            controller.createDao();
                        } catch (DataAccessObjectInitializationException e2) {
                            success = false;
                            lbl.setText(Labels.getString("DataSelectionDialog.errorRead")); //$NON-NLS-1$
                            return;
                        }
                        DataReader dataReader = (DataReader)controller.getDao();

                        List header = null;
                        int totalRows = 0;
                        try {
                            dataReader.checkConnection();
                            dataReader.open();

                            String warning = DAORowUtil.validateColumns(dataReader);
                            if(warning != null && warning.length() != 0) {
                            	
                            	
                                int response = JOptionPane.showConfirmDialog(window, warning + "\n" + Labels.getString("DataSelectionDialog.warningConf"));
                                // in case user doesn't want to continue, treat this as an error
                                if(response != JOptionPane.YES_OPTION) {
                                    success = false;
                                    lbl.setText(Labels.getString("DataSelectionDialog.errorCSVFormat")); //$NON-NLS-1$
                                    return;
                                }
                            }
                            
                            totalRows = dataReader.getTotalRows();

                            if ((header = dataReader.getColumnNames())== null || header.size() == 0) {
                                success = false;
                                lbl.setText(Labels.getString("DataSelectionDialog.errorCSVFormat")); //$NON-NLS-1$
                                return;
                            }

                        } catch (DataAccessObjectException e1) {
                            success = false;
                            lbl.setText(Labels.getString("DataSelectionDialog.errorCSVFormat") + "  " + e1.getMessage()); //$NON-NLS-1$
                            return;
                        } finally {
                            dataReader.close();
                        }
                        success = true;
                        lbl.setText(Labels.getFormattedString(
                                "DataSelectionDialog.initSuccess", String.valueOf(totalRows))); //$NON-NLS-1$ //$NON-NLS-2$

                    } catch (ConnectionException ex) {
                        success = false;
                        lbl.setText(Labels.getString("DataSelectionDialog.errorEntity")); //$NON-NLS-1$
                        return;
                    }
                    
                    if(success){
                    	  controller.getConfig().setValue(Config.MAPPING_FILE, "");
                          try {
							controller.createMappingManager();
						} catch (MappingInitializationException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
                    }
                    
                    MappingPageNew mp = new MappingPageNew(controller);
                	mp.createControl();
            	}
            }
        });  
		
		upsertButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
            	if(filename.getText().equalsIgnoreCase("")){
            		JOptionPane.showMessageDialog(window,"Please select file first");
            		return;
            	}
            	else{
            		DescribeGlobalSObjectResult entity = (DescribeGlobalSObjectResult)((ComboItem)fieldCombo.getSelectedItem()).getValue();

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
            	}
            }
        });  
		
		gbl.setConstraints(label, addComponent(0, 0, 3, 1));
		gbl.setConstraints(lbl, addComponent(0, 1, 3, 1));
		gbl.setConstraints(fieldCombo, addComponent(4,0, 2, 1));
		gbl.setConstraints(filename, addComponent(0, 3, 3, 1));
		gbl.setConstraints(chooseButton, addComponent(4, 3, 3, 1));
		gbl.setConstraints(previewButton, addComponent(0, 4, 1, 1));
		gbl.setConstraints(diButton, addComponent(1, 4, 1, 1));
		gbl.setConstraints(upsertButton, addComponent(2, 4, 1, 1));
		
		header.add(label);
		header.add(lbl);
		header.add(fieldCombo);
		header.add(filename);
		header.add(chooseButton);
		header.add(previewButton);
		header.add(diButton);
		header.add(upsertButton);
		
		content.add(header, BorderLayout.CENTER);
		
		csvScrollPane = new JScrollPane(csvTable);
		csvScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		csvview.add(csvScrollPane,BorderLayout.SOUTH);
		content.add(csvview);


		 if (controller.isLoggedIn()) {
	            setInput(controller.getEntityDescribes());
	        }
		 fieldCombo.addItemListener(new ItemChangeListener());
		
		window.setContentPane(content);
		window.setSize(1000, 700);
		window.setLocation(100, 100);
		window.setVisible(true);
		
    }
    
    private class PreviewButtonHandler implements ActionListener {
    	public void actionPerformed(ActionEvent e) {
	    	if (filename.getText().length() == 0) {
	    	// logger.info("?????????????");
	    	} else {
	    	try {
	    	System.out.println("file:::"+file);
	    	BufferedReader fin = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
	    	csvPreview = createTableModel(fin, null);
	    	csvTable.setModel(csvPreview);
	    	csvTable.repaint();
	    	} catch (FileNotFoundException ex) {
	    	// logger.warn("??????????????????????????????????");
	    	} catch (UnsupportedEncodingException e1) {
	    	// logger.error(e1);
	    	}
	    	}
    	}
    }


    public static DefaultTableModel createTableModel(Reader in, Vector<Object> headers) {
    	DefaultTableModel model = null;
    	Scanner s = null;

	    try {
	    	Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
	    	s = new Scanner(in);
	
	    	int previewLines = 21;
	    	int currentLine = 0;
	
	    	while (s.hasNextLine() && currentLine < previewLines) {
		    	rows.add(new Vector<Object>(Arrays.asList(s.nextLine().split("\\s*,\\s*", -1))));
		    	currentLine++;
	    	}
	
	    	if (headers == null) {
		    	headers = rows.remove(0);
		    	model = new DefaultTableModel(rows, headers);
	    	} else {
	    		model = new DefaultTableModel(rows, headers);
	    	}
	
	    	return model;
	    } finally {
	    	if (s != null) {
	    	s.close();
	    	}
    	}
    }

    
    class ItemChangeListener implements ItemListener{
        @Override
        public void itemStateChanged(ItemEvent event) {
           if (event.getStateChange() == ItemEvent.SELECTED) {
      		if(controller.mapUsrObject.containsKey(event.getItem().toString()) && controller.mapUsrObject.get(event.getItem().toString()).getChild("Upsert_DelInsertFLG__c").getValue() != null && controller.mapUsrObject.get(event.getItem().toString()).getChild("Upsert_DelInsertFLG__c").getValue().toString().equalsIgnoreCase("DI")){
      			 diButton.setEnabled(true);
      			upsertButton.setEnabled(false);
      		}
      		else if(controller.mapUsrObject.containsKey(event.getItem().toString()) && controller.mapUsrObject.get(event.getItem().toString()).getChild("Upsert_DelInsertFLG__c").getValue() != null && controller.mapUsrObject.get(event.getItem().toString()).getChild("Upsert_DelInsertFLG__c").getValue().toString().equalsIgnoreCase("UPS")){
      			 upsertButton.setEnabled(true);
      			diButton.setEnabled(false);
      		}
      		else if(controller.mapUsrObject.containsKey(event.getItem().toString()) && controller.mapUsrObject.get(event.getItem().toString()).getChild("Upsert_DelInsertFLG__c").getValue() != null && controller.mapUsrObject.get(event.getItem().toString()).getChild("Upsert_DelInsertFLG__c").getValue().toString().equalsIgnoreCase("UPSDI")){
      			 diButton.setEnabled(true);
      			 upsertButton.setEnabled(true);
      		}
            else{
            	diButton.setEnabled(false);
        		upsertButton.setEnabled(false);
            }
           }
        }       
    }
    
    
    private class ChooseButtonHandler implements ActionListener {
		private JFileChooser chooser;
		private JTextField textField;
		private JFrame component;

		public ChooseButtonHandler(JFileChooser ch, JTextField fld, JFrame comp) {
			chooser = ch;
			textField = fld;
			component = comp;
		}

		public void actionPerformed(ActionEvent e) {
			int answer = chooser.showOpenDialog(component);
			if (answer == JFileChooser.APPROVE_OPTION) {
				file = chooser.getSelectedFile();
				textField.setText(file.getName());
			}
		}
	}
    
    /**
     * Function to dynamically set the entity list
     */
    private void setInput(Map<String, DescribeGlobalSObjectResult> entityDescribes) {
    	controller.mapUsrObject.clear();
    	controller.getConfig();
        controller.saveConfig();
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
        fieldCombo.addItem(new ComboItem("Choose", null));
        
        if (entityDescribes != null) {
            for (Entry<String, DescribeGlobalSObjectResult> entry : entityDescribes.entrySet()) {
                String objectName = entry.getKey();
                DescribeGlobalSObjectResult objectDesc = entry.getValue();
                if (operation.isDelete() && objectDesc.isDeletable() && controller.mapUsrObject.containsKey(entry.getKey())) {
                    inputDescribes.put(objectName, objectDesc);
                    fieldCombo.addItem(new ComboItem(objectName, objectDesc));
                } else if (operation == OperationInfo.insert && objectDesc.isCreateable() && controller.mapUsrObject.containsKey(entry.getKey())) {
                    inputDescribes.put(objectName, objectDesc);
                    fieldCombo.addItem(new ComboItem(objectName, objectDesc));
                } else if (operation == OperationInfo.update && objectDesc.isUpdateable() && controller.mapUsrObject.containsKey(entry.getKey())) {
                    inputDescribes.put(objectName, objectDesc);
                    fieldCombo.addItem(new ComboItem(objectName, objectDesc));
                } else if (operation == OperationInfo.upsert && (objectDesc.isUpdateable() || objectDesc.isCreateable()) && controller.mapUsrObject.containsKey(entry.getKey())) {
                    inputDescribes.put(objectName, objectDesc);
                    fieldCombo.addItem(new ComboItem(objectName, objectDesc));
                }
            }
        }
    }

    private boolean checkEntityStatus() {
        IStructuredSelection selection = (IStructuredSelection)lv.getSelection();
        DescribeGlobalSObjectResult entity = (DescribeGlobalSObjectResult)selection.getFirstElement();
        if (entity != null) {
            return true;
        }
        return false;

    }

    class ComboItem
    {
        private String key;
        private DescribeGlobalSObjectResult value;

        public ComboItem(String key, DescribeGlobalSObjectResult value)
        {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString()
        {
            return key;
        }

        public String getKey()
        {
            return key;
        }

        public DescribeGlobalSObjectResult getValue()
        {
            return value;
        }
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

package com.salesforce.dataloader.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.TableViewer;

import com.salesforce.dataloader.action.OperationInfo;
import com.salesforce.dataloader.config.Config;
import com.salesforce.dataloader.controller.Controller;
import com.salesforce.dataloader.dao.DataAccessObjectFactory;
import com.salesforce.dataloader.dyna.ObjectField;
import com.salesforce.dataloader.exception.MappingInitializationException;
import com.salesforce.dataloader.exception.ProcessInitializationException;
import com.salesforce.dataloader.mapping.MappingElement;
import com.salesforce.dataloader.mapping.MappingManager;
import com.salesforce.dataloader.ui.DataSelectionPageNew.ComboItem;
import com.salesforce.dataloader.ui.mapping.MappingContentProvider;
import com.salesforce.dataloader.ui.mapping.MappingLabelProvider;
import com.sforce.soap.partner.DescribeGlobalSObjectResult;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.FieldType;

public class MappingPageNew {
	private final Controller controller;
    private TableViewer mappingTblViewer;
    private final Logger logger = Logger.getLogger(MappingPage.class);
    private Map<String, Field> relatedFields;
    
    private JFrame window;
	private JPanel content;
	private JPanel header;
	   
    public MappingPageNew(Controller controller) {
       // super(Labels.getString("MappingPage.title"), Labels.getString("MappingPage.titleMsg"), UIUtils.getImageRegistry().getDescriptor("splashscreens")); //$NON-NLS-1$ //$NON-NLS-2$

        // Set the description
      //  setDescription(Labels.getString("MappingPage.description")); //$NON-NLS-1$
    	window = new JFrame("DataIntegrator");
		window.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        this.controller = controller;
        header = new JPanel();
        content = new JPanel();

    }
    
    private JTable table;
    private JPanel csvview;
    private JScrollPane csvScrollPane;
    
    public void createControl() {
    	GridBagLayout gbl = new GridBagLayout();
		header.setLayout(gbl);
		csvview = new JPanel();
		JButton buttonExisting = new JButton(Labels.getString("MappingPageNew.insertAndExit"));
		buttonExisting.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
            	String outputDirName = System.getProperty("user.dir");
                File statusDir = new File(outputDirName);
                if (!statusDir.exists() || !statusDir.isDirectory()) {
                	JOptionPane.showMessageDialog(window, Labels.getString("LoadWizard.errorValidDirectory"));
                    return ;
                }
                // set the files for status output
                try {
                    controller.setStatusFiles(outputDirName, false, true);
                    controller.saveConfig();
                } catch (ProcessInitializationException e1) {
                    JOptionPane.showMessageDialog(window, e1.getMessage());
                    return ;
                }
                
                int val=JOptionPane.showConfirmDialog(window, getConfirmationText());

                if (val != JOptionPane.YES_OPTION) { return ; }

				new SWTLoadRunable(controller).run();
				
                return ;
            }
        }); 

		JButton buttonCreateNew = new JButton(Labels.getString("MappingPageNew.mapAuto"));

        final MappingPageNew thisPage = this;
        buttonCreateNew.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
            	setMapper(controller.getMappingManager());
            	sforceFieldInfo = getFieldTypes();
            	updateSforce();
            	autoMatchFields();
            	updateMapping();
            }
        }); 

        JLabel label = new JLabel(Labels.getString("MappingPage.currentBelow"));
        
        MyTableModel model = new MyTableModel();
		table = new JTable(model);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        updateMapping();
        
        gbl.setConstraints(buttonExisting, addComponent(0, 0, 3, 1));
        gbl.setConstraints(buttonCreateNew, addComponent(0, 1, 3, 1));
        gbl.setConstraints(label, addComponent(0, 2, 3, 1));
        
        header.add(buttonExisting);
		header.add(buttonCreateNew);
		header.add(label);
		
		content.add(header, BorderLayout.CENTER);
		
		csvScrollPane = new JScrollPane(table);
		csvScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		csvview.add(csvScrollPane,BorderLayout.SOUTH);
		content.add(csvview);
		
        window.setContentPane(content);
		window.setSize(1000, 700);
		window.setLocation(100, 100);
		window.setVisible(true);
    }
    
    private String getConfirmationText() {
        return getLabel("confFirstLine") + System.getProperty("line.separator") //$NON-NLS-1$ //$NON-NLS-2$
                + getLabel("confSecondLine"); //$NON-NLS-1$
    }
    
    protected String getLabel(String name) {
        return Labels.getString(getLabelSection() + "." + name);
    }

    protected String getLabelSection() {
        return getClass().getSimpleName();
    }
    
    private Field[] fields;

    //all the fields
    private Field[] allFields;
    private HashSet<String> mappedFields;

    private MappingManager mapper;
    private Properties restore;
    public void setMapper(MappingManager mapper) {
        this.mapper = mapper;
        this.restore = new Properties();
        this.mappedFields = new HashSet<String>();
        MappingElement[] elements = mapper.getElements();
        for (MappingElement elem : elements) {
            this.restore.put(elem.getSourceColumn(), elem.getDestColumn());
            if(elem.getDestColumn().length() != 0) {
                this.mappedFields.add(elem.getDestColumn());
            }
        }
    }
    private Field[] sforceFieldInfo;
    private void updateSforce() {

        ArrayList<Field> mappableFieldList = new ArrayList<Field>();
        ArrayList<Field> allFieldList = new ArrayList<Field>();
        Field field;
        OperationInfo operation = controller.getConfig().getOperationInfo();
        for (int i = 0; i < sforceFieldInfo.length; i++) {

            field = sforceFieldInfo[i];
            boolean isMappable = false;
            switch (operation) {
            case insert:
                if (field.isCreateable()) {
                    isMappable = true;
                }
                break;
            case delete:
            case hard_delete:
                if (field.getType().toString().toLowerCase().equals("id")) {
                    isMappable = true;
                }
                break;
            case upsert:
                if (field.isUpdateable() || field.isCreateable()
                        || field.getType().toString().toLowerCase().equals("id")) {
                    isMappable = true;
                }
                break;
            case update:
                if (field.isUpdateable() || field.getType().toString().toLowerCase().equals("id")) {
                    isMappable = true;
                }
                break;
            default:
                throw new UnsupportedOperationException();
            }
            // only add the field to mappings if it's not already used in mapping
            if(isMappable) {
                if(!mappedFields.contains(field.getName())) {
                    mappableFieldList.add(field);
                }
                // this list is for all fields in case map is reset
                allFieldList.add(field);
            }
        }

        fields = mappableFieldList.toArray(new Field[mappableFieldList.size()]);
        allFields = allFieldList.toArray(new Field[allFieldList.size()]);
    }
    
    private void autoMatchFields() {

        LinkedList<Field> fieldList = new LinkedList<Field>(Arrays.asList(fields));

        //first match on name, then label
        ListIterator iterator = fieldList.listIterator();
        Field field;
        while (iterator.hasNext()) {
            field = (Field)iterator.next();
            String fieldName = field.getName();
            String fieldLabel = field.getLabel();
            String mappingSource = null;

            // field is already mapped
            // TODO: check with lexi if this is intended use of automatch
            if(mappedFields.contains(fieldName)) {
                continue;
            }
            
            if (mapper.containsMappingFor(fieldName)) {
                mappingSource = fieldName;
            } else if (mapper.containsMappingFor(fieldLabel)) {
                mappingSource = fieldLabel;
            }

            if(mappingSource != null) {
                // don't overwrite the fields that already have been mapped
                String oldFieldName = mapper.getMappingFor(mappingSource);
                if(oldFieldName == null || oldFieldName.length() == 0) {
                    mapper.addMapping(mappingSource, fieldName);
                }
                iterator.remove();
            }
        }

        fields = fieldList.toArray(new Field[fieldList.size()]);

    }
    /**
     * Responsible for updating the mapping model
     */
    
    public void updateMapping() {
        // Set the table viewer's input
    	if(table != null){
    		MyTableModel md= (MyTableModel)table.getModel();
    		md.RemoveAllData();
    		for(MappingElement me : controller.getMappingManager().getElements()){
    			md.addRow(Arrays.asList(me.getSourceColumn(), me.getDestColumn()));
    		}
    	}
    }

    public Field[] getFieldTypes() {
        Field[] result;
        if (!controller.getConfig().getOperationInfo().isDelete()) {
            Field[] fields = controller.getFieldTypes().getFields();
            if(relatedFields != null) {
                result = addRelatedFields(fields);
            } else {
                result = fields; 
            }
        } else {
            Field[] idFields = new Field[1];
            Field idField = new Field();
            idField.setName("Id");
            idField.setLabel("Id");
            idField.setType(FieldType.id);
            idFields[0] = idField;
            result = idFields;
        }
        return result;
    }

    /**
     * Add fields for the related objects
     * @param fields
     */
    public void setRelatedFields(Map<String,Field> fields) {
        this.relatedFields = fields;
    }
    
    /**
     * @param fields
     */
    private Field[] addRelatedFields(Field[] fields) {
        List<Field> relatedFieldList = new LinkedList<Field>();
        for(Entry<String,Field> relatedFieldInfo : relatedFields.entrySet()) {
            String relationshipName = relatedFieldInfo.getKey();
            Field relatedField = relatedFieldInfo.getValue();
            String mapFieldName = ObjectField.formatAsString(relationshipName, relatedField.getName());
            Field mapField = new Field();
            mapField.setName(mapFieldName);
            mapField.setLabel(relationshipName + " " + relatedField.getLabel());
            mapField.setType(FieldType.reference);
            mapField.setCreateable(relatedField.isCreateable());
            mapField.setUpdateable(relatedField.isUpdateable());
            relatedFieldList.add(mapField);
        }
        relatedFieldList.addAll(Arrays.asList(fields));
        return relatedFieldList.toArray(fields);
    }

    private GridBagConstraints addComponent(int x, int y, int width, int height) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = x;
		c.gridy = y;
		c.gridwidth = width;
		c.gridheight = height;
		return c;
	}
    
    public static class MyTableModel extends AbstractTableModel
    {
        private List<String> columnNames = new ArrayList();
        private List<List> data = new ArrayList();

        {
            columnNames.add(Labels.getString("MappingPage.fileColumn"));
            columnNames.add(Labels.getString("MappingPage.name"));
        }

        public void addRow(List rowData)
        {
            data.add(rowData);
            fireTableRowsInserted(data.size() - 1, data.size() - 1);
        }

        public int getColumnCount()
        {
            return columnNames.size();
        }

        public int getRowCount()
        {
            return data.size();
        }

        public String getColumnName(int col)
        {
            try
            {
                return columnNames.get(col);
            }
            catch(Exception e)
            {
                return null;
            }
        }

        public Object getValueAt(int row, int col)
        {
            return data.get(row).get(col);
        }

        public boolean isCellEditable(int row, int col)
        {
            return false;
        }
        
        public void RemoveAllData()
        {
        	data = new ArrayList();
        }

        public Class getColumnClass(int c)
        {
            return getValueAt(0, c).getClass();
        }
    };
}

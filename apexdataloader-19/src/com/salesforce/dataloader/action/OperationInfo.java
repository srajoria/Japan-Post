package com.salesforce.dataloader.action;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Image;

import com.salesforce.dataloader.action.progress.ILoaderProgress;
import com.salesforce.dataloader.config.Config;
import com.salesforce.dataloader.config.Messages;
import com.salesforce.dataloader.controller.Controller;
import com.salesforce.dataloader.ui.Labels;
import com.salesforce.dataloader.ui.UIUtils;
import com.salesforce.dataloader.ui.LoadWizard.DeleteWizard;
import com.salesforce.dataloader.ui.LoadWizard.HardDeleteWizard;
import com.salesforce.dataloader.ui.LoadWizard.InsertWizard;
import com.salesforce.dataloader.ui.LoadWizard.UpdateWizard;
import com.salesforce.dataloader.ui.LoadWizard.UpsertWizard;
import com.salesforce.dataloader.ui.extraction.ExtractionWizard;
import com.salesforce.dataloader.ui.uiActions.OperationUIAction;
import com.sforce.async.OperationEnum;

/**
 * Enum containing data and utility methods for data loader operations.
 * 
 * @author cjarvis
 */
public enum OperationInfo {

    insert(InsertAction.class), update(UpdateAction.class), upsert(
            UpsertAction.class), delete(DeleteAction.class), hard_delete(null,
            BulkLoadAction.class), extract(ExtractAction.class, null);

    /** all operations, in order */
    public static final OperationInfo[] ALL_OPERATIONS_IN_ORDER = { insert, update, upsert, delete, hard_delete,
            extract };

    private static final Logger logger = Logger.getLogger(OperationInfo.class);

    private final Class<? extends IAction> partnerAPIActionClass;
    private final Class<? extends BulkLoadAction> bulkAPIActionClass;
 
    private OperationInfo(Class<? extends IAction> partnerAPIActionClass,
            Class<? extends BulkLoadAction> bulkAPIActionClass) {

        this.partnerAPIActionClass = partnerAPIActionClass;
        this.bulkAPIActionClass = bulkAPIActionClass;
    }

    private OperationInfo(Class<? extends IAction> partnerAPIActionClass) {
        this(partnerAPIActionClass, BulkLoadAction.class);
    }

    public boolean bulkAPIEnabled() {
        return this.bulkAPIActionClass != null;
    }

    public boolean partnerAPIEnabled() {
        return this.partnerAPIActionClass != null;
    }

    public IAction instantiateAction(Controller ctl, ILoaderProgress loaderProgress) {
        logger.info(Messages.getMessage(getClass(), "creatingUIAction", this));
        final Class<? extends IAction> cls = ctl.getConfig().isBulkAPIEnabled() && bulkAPIEnabled() ? this.bulkAPIActionClass
                : this.partnerAPIActionClass;
        try {
            return cls.getConstructor(Controller.class, ILoaderProgress.class).newInstance(ctl, loaderProgress);
        } catch (Exception e) {
            throw unsupportedInstantiation(e, cls);
        }
    }
    
    public IAction instantiateAction(Controller ctl) {
        logger.info(Messages.getMessage(getClass(), "creatingUIAction", this));
        final Class<? extends IAction> cls = ctl.getConfig().isBulkAPIEnabled() && bulkAPIEnabled() ? this.bulkAPIActionClass
                : this.partnerAPIActionClass;
        try {
            return cls.getConstructor(Controller.class).newInstance(ctl);
        } catch (Exception e) {
            throw unsupportedInstantiation(e, cls);
        }
    }

    private RuntimeException unsupportedInstantiation(Exception e, Class<?> cls) {
        final String message = Messages
                .getMessage(getClass(), "errorOperationInstantiation", this, String.valueOf(cls));
        logger.fatal(message);
        return new UnsupportedOperationException(message, e);
    }

    public String getIconName() {
        if (this == hard_delete) return delete.getIconName();
        if (this == upsert) return update.getIconName();
        return name() + "_icon";
    }

    public String getIconLocation() {
        if (this == hard_delete) return delete.getIconLocation();
        if (this == upsert) return update.getIconLocation();
        return "img/icons/icon_" + name() + ".gif";
    }

    public String getMenuLabel() {
        return Labels.getString(name() + ".UIAction.menuText");
    }

    public String getToolTipText() {
        return Labels.getString(name() + ".UIAction.tooltipText"); //$NON-NLS-1$
    }

    public String getLabel() {
        return Labels.getString("UI." + name());
    }

   /* public Wizard instantiateWizard(Controller ctl) {
        logger.info(Messages.getMessage(getClass(), "creatingWizard", this));
        try {
            return ((Class<? extends Wizard>)this.wizardClass).getConstructor(Controller.class).newInstance(ctl);
        } catch (Exception e) {
            throw unsupportedInstantiation(e, this.wizardClass);
        }
    }*/

    public OperationUIAction createUIAction(Controller ctl) {
        return new OperationUIAction(ctl, this);
    }

    public OperationEnum getOperationEnum() {
        return this != hard_delete ? OperationEnum.valueOf(name()) : OperationEnum.hardDelete;
    }

    public boolean isDelete() {
        return this == delete || this == hard_delete;
    }

    public int getDialogIdx() {
        return IDialogConstants.CLIENT_ID + ordinal() + 1;
    }

    public boolean isOperationAllowed(Config cfg) {
        // all operations are always allowed except hard delete, which requires bulk api
        return this != hard_delete || cfg.isBulkAPIEnabled();
    }

    public Image getIconImage() {
        Image result = UIUtils.getImageRegistry().get(getIconName());
        if (result == null) { throw new NullPointerException(name() + ": cannot find image: " + getIconName() + ", "
                + getIconLocation()); }
        return result;
    }

    public ImageDescriptor getIconImageDescriptor() {
        ImageDescriptor result = UIUtils.getImageRegistry().getDescriptor(getIconName());
        if (result == null) { throw new NullPointerException(name() + ": cannot find image descriptor: "
                + getIconName() + ", " + getIconLocation()); }
        return result;
    }

    public String getInfoMessageForDataSelectionPage() {
        if (this == hard_delete) return Labels.getString("DataSelectionPage." + name());
        return null;
    }

}
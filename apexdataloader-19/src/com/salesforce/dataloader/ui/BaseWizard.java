package com.salesforce.dataloader.ui;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.graphics.Image;

import com.salesforce.dataloader.action.OperationInfo;
import com.salesforce.dataloader.config.Config;
import com.salesforce.dataloader.controller.Controller;

public abstract class BaseWizard extends Wizard {

    private final Controller controller;
    private final WizardPage finishPage;

    protected BaseWizard(Controller ctl, OperationInfo info) {
        this.controller = ctl;

        getConfig().setValue(Config.OPERATION, info.name());
        this.finishPage = setupPages();
        // Set the dialog window title
        setWindowTitle(getLabel("title"));

    }

    protected abstract SettingsPage createSettingsPage();

    protected abstract WizardPage setPages();

    private WizardPage setupPages() {
        if (SettingsPage.isNeeded(getController())) addPage(createSettingsPage());

        return setPages();
    }

    protected Controller getController() {
        return this.controller;
    }

    protected Config getConfig() {
        return getController().getConfig();
    }

    protected String getLabel(String name) {
        return Labels.getString(getLabelSection() + "." + name);
    }

    protected String getLabelSection() {
        return getClass().getSimpleName();
    }

    protected WizardPage getFinishPage() {
        return finishPage;
    }

    @Override
    public Image getDefaultPageImage() {
        return UIUtils.getImageRegistry().get("sfdc_icon"); //$NON-NLS-1$
    }

}

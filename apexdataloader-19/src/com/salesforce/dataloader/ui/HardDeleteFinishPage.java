package com.salesforce.dataloader.ui;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.salesforce.dataloader.action.OperationInfo;
import com.salesforce.dataloader.controller.Controller;

public class HardDeleteFinishPage extends FinishPage {

    public HardDeleteFinishPage(Controller controller) {
        super(controller);
    }

    private final AtomicBoolean canFinish = new AtomicBoolean(false);

    private void setCanFinish(boolean selection) {
        this.canFinish.set(selection);
        this.getContainer().updateButtons();
    }

    @Override
    protected void hook_createControl(Composite comp) {
        super.hook_createControl(comp);
        if (getController().getConfig().getOperationInfo() == OperationInfo.hard_delete) {
            Composite terms = new Composite(comp, SWT.NONE);
            GridLayout layout = new GridLayout(1, false);
            terms.setLayout(layout);
            GridData gd = new GridData();
            gd.horizontalAlignment = GridData.END;
            gd.grabExcessVerticalSpace = true;
            gd.grabExcessHorizontalSpace = true;
            gd.verticalAlignment = GridData.END;
            terms.setLayoutData(gd);

            final BaseWizard wiz = (BaseWizard)getWizard();

            final Label label = new Label(terms, SWT.RIGHT);
            label.setForeground(new Color(label.getDisplay(), 0xff, 0, 0));
            label.setText(wiz.getLabel("finishMessage"));

            final Button b = new Button(terms, SWT.CHECK);
            b.setText(wiz.getLabel("finishMessageConfirm"));
            b.setSelection(this.canFinish.get());
            b.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent se) {
                    setCanFinish(((Button)se.widget).getSelection());
                }
            });
        }
    }

    @Override
    public boolean finishAllowed() {
        return super.finishAllowed() && this.canFinish.get();
    }
}

package org.sagebionetworks.web.client.widget.table.modal.wizard;

import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Heading;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalSize;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.client.ui.html.Text;
import org.sagebionetworks.web.client.widget.HelpWidget;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ModalWizardViewImpl implements ModalWizardView {

	public interface Binder extends UiBinder<Modal, ModalWizardViewImpl> {}
	
	@UiField
	Button primaryButton;
	@UiField
	Button defaultButton;
	@UiField
	Text instructions;
	@UiField
	SimplePanel bodyPanel;
	@UiField
	Alert alert;
	@UiField
	Heading modalTitle;
	@UiField
	Span helpContainer;
	Modal modal;
	
	@Inject
	public ModalWizardViewImpl(Binder binder){
		modal = binder.createAndBindUi(this);
	}

	@Override
	public void setPresenter(final Presenter presenter) {
		primaryButton.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				presenter.onPrimary();
			}
		});
		defaultButton.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				presenter.onCancel();
			}
		});
	}

	@Override
	public Widget asWidget() {
		return modal;
	}


	@Override
	public void showModal() {
		modal.show();
	}

	@Override
	public void setBody(IsWidget body) {
		bodyPanel.clear();
		bodyPanel.add(body);
	}

	@Override
	public void showAlert(boolean visible) {
		this.alert.setVisible(visible);
	}

	@Override
	public void showErrorMessage(String error) {
		this.alert.setText(error);
	}

	@Override
	public void setInstructionsMessage(String message) {
		this.instructions.setText(message);
	}

	@Override
	public void setLoading(boolean loading) {
		primaryButton.setEnabled(!loading);
	}

	@Override
	public void hideModal() {
		modal.hide();
	}

	@Override
	public void setPrimaryButtonText(String text) {
		this.primaryButton.setText(text);
	}

	@Override
	public void setTile(String title) {
		modalTitle.setText(title);
	}

	@Override
	public void setSize(ModalSize size) {
		modal.setSize(size);
	}
	
	@Override
	public void setHelp(String helpMarkdown, String helpUrl) {
		helpContainer.clear();
		HelpWidget help = new HelpWidget();
		help.setHref(helpUrl);
		help.setHelpMarkdown(helpMarkdown);
		help.setAddStyleNames("margin-left-5");
		helpContainer.add(help);
	}
}

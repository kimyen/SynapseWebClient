package org.sagebionetworks.web.client.widget.accessrequirements;

import org.gwtbootstrap3.client.ui.constants.ButtonType;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PopupUtilsView;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.utils.Callback;
import org.sagebionetworks.web.client.utils.CallbackP;
import org.sagebionetworks.web.client.widget.Button;
import org.sagebionetworks.web.client.widget.asynch.IsACTMemberAsyncHandler;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class DeleteAccessRequirementButton implements IsWidget {
	public static final String DELETED_ACCESS_REQUIREMENT_SUCCESS_MESSAGE = "Successfully deleted access requirement";
	public static final String DELETE_ACCESS_REQUIREMENT_MESSAGE = "Are you sure?";
	public static final String DELETE_ACCESS_REQUIREMENT_TITLE = "Delete Access Requirement";
	public static final String DELETE_ACCESS_REQUIREMENT_BUTTON_TEXT = DELETE_ACCESS_REQUIREMENT_TITLE;
	public Button button;
	public IsACTMemberAsyncHandler isACTMemberAsyncHandler;
	RestrictableObjectDescriptor subject;
	AccessRequirement ar;
	SynapseClientAsync synapseClient;
	PopupUtilsView popupUtils;
	Callback confirmedDeleteCallback;
	GlobalApplicationState globalAppState;
	
	@Inject
	public DeleteAccessRequirementButton(Button button, 
			IsACTMemberAsyncHandler isACTMemberAsyncHandler,
			GlobalApplicationState globalAppState,
			SynapseClientAsync synapseClient, 
			PopupUtilsView popupUtils) {
		this.button = button;
		this.isACTMemberAsyncHandler = isACTMemberAsyncHandler;
		this.synapseClient = synapseClient;
		this.popupUtils = popupUtils;
		this.globalAppState = globalAppState;
		button.setVisible(false);
		button.addStyleName("margin-left-10");
		button.setType(ButtonType.DANGER);
		button.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				deleteAccessRequirement();
			}
		});
		confirmedDeleteCallback = new Callback() {
			@Override
			public void invoke() {
				deleteAccessRequirementAfterConfirmation();
			}
		};
	}	
	
	public void configure(AccessRequirement ar) {
		button.setText(DELETE_ACCESS_REQUIREMENT_BUTTON_TEXT);
		this.subject = null;
		this.ar = ar;
		showIfACTMember();
	}
	
	public void deleteAccessRequirement() {
		popupUtils.showConfirmDialog(DELETE_ACCESS_REQUIREMENT_TITLE, DELETE_ACCESS_REQUIREMENT_MESSAGE, confirmedDeleteCallback);
	}
	
	public void deleteAccessRequirementAfterConfirmation() {
		synapseClient.deleteAccessRequirement(ar.getId(), new AsyncCallback<Void>() {
			@Override
			public void onSuccess(Void result) {
				popupUtils.showInfo(DELETED_ACCESS_REQUIREMENT_SUCCESS_MESSAGE, "");
				globalAppState.refreshPage();
			}
			
			@Override
			public void onFailure(Throwable caught) {
				popupUtils.showErrorMessage(caught.getMessage());
			}
		});
	}
	
	private void showIfACTMember() {
		isACTMemberAsyncHandler.isACTMember(new CallbackP<Boolean>() {
			@Override
			public void invoke(Boolean isACTMember) {
				button.setVisible(isACTMember);
			}
		});
	}
	
	public Widget asWidget() {
		return button.asWidget();
	}
	
}

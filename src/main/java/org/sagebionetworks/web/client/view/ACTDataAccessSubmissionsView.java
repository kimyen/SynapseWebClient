package org.sagebionetworks.web.client.view;

import java.util.Date;
import java.util.List;

import org.sagebionetworks.web.client.SynapseView;

import com.google.gwt.user.client.ui.IsWidget;

public interface ACTDataAccessSubmissionsView extends IsWidget, SynapseView {
	void setPresenter(Presenter presenter);
	void setLoadMoreContainer(IsWidget w);
	void setStates(List<String> states);
	void setSynAlert(IsWidget w);
	void setSelectedStateText(String state);
	void setSelectedMinDate(Date date);
	void setSelectedMaxDate(Date date);
	void setShowHideButton(IsWidget button);
	void setAccessRequirementWidget(IsWidget w);
	
	void setAreOtherAttachmentsRequired(boolean value);
	void setIsAnnualReviewRequired(boolean value);
	void setIsCertifiedUserRequired(boolean value);
	void setIsDUCRequired(boolean value);
	void setIsIDUPublic(boolean value);
	void setIsIRBApprovalRequired(boolean value);
	void setIsValidatedProfileRequired(boolean value);
	
	void setDucColumnVisible(boolean visible);
	void setIrbColumnVisible(boolean visible);
	void setOtherAttachmentsColumnVisible(boolean visible);
	void setAccessRequirementUIVisible(boolean visible);
	void setRenewalColumnsVisible(boolean visible);
	
	public interface Presenter {
		void onClearDateFilter();
		void onClearStateFilter();
		void onStateSelected(String state);
		void onMinDateSelected(Date date);
		void onMaxDateSelected(Date date);
		void onCreatedOnClick();
	}

	
}

package org.sagebionetworks.web.client.view;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;

public interface SignedTokenView extends IsWidget {
	void setSynapseAlert(Widget w);
	void showSuccess(String successMessage);
	void showConfirmUnsubscribe();
	void clear();
	void setUnsubscribingUserBadge(Widget w);
	void setLoadingVisible(boolean visible);
	
	/**
	 * Set this view's presenter
	 * @param presenter
	 */
	void setPresenter(Presenter presenter);	
	
	public interface Presenter {
		void okClicked();
		void unsubscribeConfirmed();
	}

}

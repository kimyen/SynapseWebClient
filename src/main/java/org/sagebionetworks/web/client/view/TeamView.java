package org.sagebionetworks.web.client.view;

import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.web.client.SynapseView;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;

public interface TeamView extends IsWidget, SynapseView {
	
	/**
	 * Set this view's presenter
	 * @param presenter
	 */
	void setPresenter(Presenter presenter);	
	public interface Presenter {
		void goTo(Place place);
		void refresh(String teamId);
		void showEditModal();
		void showDeleteModal();
		void showLeaveModal();
		void showInviteModal();
		void clear();
		void onShowMap();
	}

	void setSynAlertWidget(Widget asWidget);
	void setLeaveTeamWidget(Widget asWidget);
	void setDeleteTeamWidget(Widget asWidget);
	void setEditTeamWidget(Widget asWidget);
	void setInviteMemberWidget(Widget inviteWidget);
	void setJoinTeamWidget(Widget asWidget);
	void showMemberMenuItems();
	void showAdminMenuItems();
	void setOpenMembershipRequestWidget(Widget asWidget);
	void setOpenUserInvitationsWidget(Widget asWidget);
	void setMemberListWidget(Widget asWidget);
	void setPublicJoinVisible(Boolean canPublicJoin);
	void setTotalMemberCount(String string);
	void setMediaObjectPanel(Team team, String xsrfToken);
	void setTeamEmailAddress(String teamEmail);
	void setMap(Widget w);
	void showMapModal();
	void setShowMapVisible(boolean visible);
	int getClientHeight();
	
}

package org.sagebionetworks.web.client.presenter;

import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PortalGinInjector;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.place.PeopleSearch;
import org.sagebionetworks.web.client.utils.Callback;
import org.sagebionetworks.web.client.view.PeopleSearchView;
import org.sagebionetworks.web.client.widget.LoadMoreWidgetContainer;
import org.sagebionetworks.web.client.widget.entity.controller.SynapseAlert;
import org.sagebionetworks.web.client.widget.user.BadgeSize;
import org.sagebionetworks.web.client.widget.user.UserBadge;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class PeopleSearchPresenter extends AbstractActivity implements PeopleSearchView.Presenter, Presenter<PeopleSearch> {
	
	public static final int SEARCH_PEOPLE_LIMIT = 30;
	
	private PeopleSearch place;
	private PeopleSearchView view;
	private GlobalApplicationState globalApplicationState;
	private SynapseClientAsync synapseClient;
	private SynapseAlert synAlert;
	private LoadMoreWidgetContainer loadMoreWidgetContainer;
	PortalGinInjector ginInjector;
	private int offset;
	private String searchTerm;
	
	@Inject
	public PeopleSearchPresenter(PeopleSearchView view,
			SynapseClientAsync synapseClient,
			GlobalApplicationState globalApplicationState,
			SynapseAlert synAlert,
			LoadMoreWidgetContainer loadMoreWidgetContainer,
			PortalGinInjector ginInjector) {
		this.view = view;
		this.globalApplicationState = globalApplicationState;
		this.synapseClient = synapseClient;
		this.synAlert = synAlert;
		this.loadMoreWidgetContainer = loadMoreWidgetContainer;
		this.ginInjector = ginInjector;
		view.setPresenter(this);
		loadMoreWidgetContainer.configure(new Callback() {
			@Override
			public void invoke() {
				loadMore();
			}
		});
		view.setLoadMoreContainer(loadMoreWidgetContainer.asWidget());
	}
	
	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// Install the view
		panel.setWidget(view);
	}

	@Override
	public void setPlace(PeopleSearch place) {
		this.place = place;
		this.view.setPresenter(this);
		loadMoreWidgetContainer.clear();
		view.setSynAlertWidget(synAlert.asWidget());
		searchTerm = place.getSearchTerm();
		view.setSearchTerm(searchTerm);
		offset = 0;
		loadMore();
	}
	
	@Override
	public void goTo(Place place) {
		globalApplicationState.getPlaceChanger().goTo(place);
	}
	
	public void loadMore() {
		this.synAlert.clear();
		
		//execute search, and update view with the results
		AsyncCallback<UserGroupHeaderResponsePage> callback = 
				new AsyncCallback<UserGroupHeaderResponsePage>() {

			@Override
			public void onSuccess(UserGroupHeaderResponsePage result) {
				for (UserGroupHeader header : result.getChildren()) {
					if (header.getIsIndividual()) {
						UserBadge badge = ginInjector.getUserBadgeWidget();
						badge.configure(header.getOwnerId(), true);
						badge.setSize(BadgeSize.LARGE);
						badge.setStyleNames("displayInlineBlock userBadgeTable col-sm-12 col-md-6 margin-top-15");
						badge.setHeight("80px");
						loadMoreWidgetContainer.add(badge.asWidget());
					}
				}
				offset += SEARCH_PEOPLE_LIMIT;
				loadMoreWidgetContainer.setIsMore(!result.getChildren().isEmpty());
			}
			
			@Override
			public void onFailure(Throwable caught) {
				loadMoreWidgetContainer.setIsMore(false);
				view.setSynAlertWidgetVisible(true);
				synAlert.handleException(caught);
			}
			
		};
		synapseClient.getUserGroupHeadersByPrefix(searchTerm, (long) SEARCH_PEOPLE_LIMIT, (long) this.offset, callback);
	}
}

package org.sagebionetworks.web.client.presenter;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.place.ProjectsHome;
import org.sagebionetworks.web.client.place.Synapse;
import org.sagebionetworks.web.client.view.ProjectsHomeView;
import org.sagebionetworks.web.client.widget.entity.controller.SynapseAlert;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class ProjectsHomePresenter extends AbstractActivity implements ProjectsHomeView.Presenter, Presenter<ProjectsHome>{
		
	private ProjectsHome place;
	private ProjectsHomeView view;
	private GlobalApplicationState globalApplicationState;
	private SynapseClientAsync synapseClient;
	private SynapseAlert synAlert;
	
	@Inject
	public ProjectsHomePresenter(ProjectsHomeView view,
			GlobalApplicationState globalApplicationState,		
			SynapseClientAsync synapseClient, 
			SynapseAlert synAlert) {
		this.view = view;
		this.globalApplicationState = globalApplicationState;
		this.synapseClient = synapseClient;
		this.synAlert = synAlert;
		
		view.setPresenter(this);
		view.setSynAlertWidget(synAlert.asWidget());
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// Install the view
		panel.setWidget(view);
	}

	@Override
	public void setPlace(ProjectsHome place) {
		this.place = place;
		view.setPresenter(this);
		view.setSynAlertWidget(synAlert.asWidget());
	}

	@Override
    public String mayStop() {
        view.clear();
        synAlert.clear();
        return null;
    }

	@Override
	public void createProject(final String name) {
		synAlert.clear();
		CreateEntityUtil.createProject(name, synapseClient, new AsyncCallback<String>() {
			@Override
			public void onSuccess(String newProjectId) {
				view.showInfo(DisplayConstants.LABEL_PROJECT_CREATED, name);
				globalApplicationState.getPlaceChanger().goTo(new Synapse(newProjectId));						
			}
			
			@Override
			public void onFailure(Throwable caught) {
				synAlert.handleException(caught);		
			}
		});
	}
	
}

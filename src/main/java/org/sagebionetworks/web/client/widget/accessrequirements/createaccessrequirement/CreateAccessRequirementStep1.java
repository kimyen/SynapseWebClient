package org.sagebionetworks.web.client.widget.accessrequirements.createaccessrequirement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.widget.accessrequirements.SubjectsWidget;
import org.sagebionetworks.web.client.widget.table.modal.wizard.ModalPage;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

/**
 * First page of creating an access requirement  
 * @author Jay
 *
 */
public class CreateAccessRequirementStep1 implements ModalPage, CreateAccessRequirementStep1View.Presenter {
	CreateAccessRequirementStep1View view;
	List<RestrictableObjectDescriptor> subjects;
	ModalPresenter modalPresenter;
	CreateACTAccessRequirementStep2 actStep2;
	CreateTermsOfUseAccessRequirementStep2 touStep2;
	ACCESS_TYPE currentAccessType;
	AccessRequirement accessRequirement;
	SynapseClientAsync synapseClient;
	SubjectsWidget subjectsWidget;
	
	@Inject
	public CreateAccessRequirementStep1(
			CreateAccessRequirementStep1View view,
			CreateACTAccessRequirementStep2 actStep2,
			CreateTermsOfUseAccessRequirementStep2 touStep2,
			SynapseClientAsync synapseClient,
			SubjectsWidget subjectsWidget) {
		super();
		this.view = view;
		this.actStep2 = actStep2;
		this.touStep2 = touStep2;
		this.subjectsWidget = subjectsWidget;
		this.synapseClient = synapseClient;
		view.setSubjects(subjectsWidget);
		view.setPresenter(this);
	}
	
	@Override
	public void onSetEntities() {
		currentAccessType = ACCESS_TYPE.DOWNLOAD;
		String entityIds = view.getEntityIds();
		String[] entities = entityIds.split("[,\\s]\\s*");
		List<RestrictableObjectDescriptor> newSubjects = new ArrayList<RestrictableObjectDescriptor>();
		for (int i = 0; i < entities.length; i++) {
			RestrictableObjectDescriptor newSubject = new RestrictableObjectDescriptor();
			newSubject.setId(entities[i]);
			newSubject.setType(RestrictableObjectType.ENTITY);
			newSubjects.add(newSubject);
		}
		setSubjects(newSubjects);
	}
	
	@Override
	public void onSetTeams() {
		currentAccessType = ACCESS_TYPE.PARTICIPATE;
		String teamIds = view.getTeamIds();
		String[] teams = teamIds.split("[,\\s]\\s*");
		List<RestrictableObjectDescriptor> newSubjects = new ArrayList<RestrictableObjectDescriptor>();
		for (int i = 0; i < teams.length; i++) {
			RestrictableObjectDescriptor newSubject = new RestrictableObjectDescriptor();
			newSubject.setId(teams[i]);
			newSubject.setType(RestrictableObjectType.TEAM);
			newSubjects.add(newSubject);
		}
		setSubjects(newSubjects);
	}
	
	
	/**
	 * Configure this widget before use.
	 * 
	 */
	public void configure(RestrictableObjectDescriptor initialSubject) {
		accessRequirement = null;
		view.setAccessRequirementTypeSelectionVisible(true);
		List<RestrictableObjectDescriptor> initialSubjects = new ArrayList<RestrictableObjectDescriptor>();
		initialSubjects.add(initialSubject);
		setSubjects(initialSubjects);
	}
	
	public void configure(AccessRequirement ar) {
		accessRequirement = ar;
		view.setAccessRequirementTypeSelectionVisible(false);
		setSubjects(accessRequirement.getSubjectIds());
	}
	
	private void setSubjects(List<RestrictableObjectDescriptor> initialSubjects) {
		subjects = initialSubjects;
		subjectsWidget.configure(subjects, false);
		String subjectIds = getSubjectIds(subjects);
		if (subjects.size() > 0) {
			if (subjects.get(0).getType().equals(RestrictableObjectType.ENTITY)) {
				currentAccessType = ACCESS_TYPE.DOWNLOAD;
				view.setEntityIdsString(subjectIds);
			} else {
				currentAccessType = ACCESS_TYPE.PARTICIPATE;
				view.setTeamIdsString(subjectIds);
			}
		}
	}
	public String getSubjectIds(List<RestrictableObjectDescriptor> subjects) {
		StringBuilder sb = new StringBuilder();
		for (Iterator iterator = subjects.iterator(); iterator.hasNext();) {
			RestrictableObjectDescriptor subject = (RestrictableObjectDescriptor) iterator.next();
			sb.append(subject.getId());
			if (iterator.hasNext()) {
				sb.append(", ");
			}
		}
		return sb.toString();
	}
	
	@Override
	public void onPrimary() {
		if (accessRequirement == null) {
			if (view.isACTAccessRequirementType()) {
				accessRequirement = new ACTAccessRequirement();
			} else {
				accessRequirement = new TermsOfUseAccessRequirement();
			}
		}
		accessRequirement.setAccessType(currentAccessType);
		accessRequirement.setSubjectIds(subjects);
		
		modalPresenter.setLoading(true);
		synapseClient.createOrUpdateAccessRequirement(accessRequirement, new AsyncCallback<AccessRequirement>() {
			@Override
			public void onFailure(Throwable caught) {
				modalPresenter.setLoading(false);
				modalPresenter.setErrorMessage(caught.getMessage());
			}
			@Override
			public void onSuccess(AccessRequirement accessRequirement) {
				modalPresenter.setLoading(false);
				if (accessRequirement instanceof ACTAccessRequirement) {
					actStep2.configure((ACTAccessRequirement)accessRequirement);
					modalPresenter.setNextActivePage(actStep2);
				} else {
					touStep2.configure((TermsOfUseAccessRequirement)accessRequirement);
					modalPresenter.setNextActivePage(touStep2);
				}		
			}
		});
		
	}

	@Override
	public Widget asWidget() {
		return view.asWidget();
	}

	@Override
	public void setModalPresenter(ModalPresenter modalPresenter) {
		this.modalPresenter = modalPresenter;
		modalPresenter.setPrimaryButtonText(DisplayConstants.NEXT);
	}


}

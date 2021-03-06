package org.sagebionetworks.web.client.widget.docker;

import static org.sagebionetworks.repo.model.EntityBundle.ACCESS_REQUIREMENTS;
import static org.sagebionetworks.repo.model.EntityBundle.ANNOTATIONS;
import static org.sagebionetworks.repo.model.EntityBundle.DOI;
import static org.sagebionetworks.repo.model.EntityBundle.ENTITY;
import static org.sagebionetworks.repo.model.EntityBundle.ENTITY_PATH;
import static org.sagebionetworks.repo.model.EntityBundle.PERMISSIONS;
import static org.sagebionetworks.repo.model.EntityBundle.UNMET_ACCESS_REQUIREMENTS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityChildrenResponse;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.entity.Direction;
import org.sagebionetworks.repo.model.entity.SortBy;
import org.sagebionetworks.repo.model.entity.query.Condition;
import org.sagebionetworks.repo.model.entity.query.EntityFieldName;
import org.sagebionetworks.repo.model.entity.query.EntityQuery;
import org.sagebionetworks.repo.model.entity.query.EntityQueryResult;
import org.sagebionetworks.repo.model.entity.query.EntityQueryResults;
import org.sagebionetworks.repo.model.entity.query.EntityQueryUtils;
import org.sagebionetworks.repo.model.entity.query.Operator;
import org.sagebionetworks.repo.model.entity.query.Sort;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.utils.Callback;
import org.sagebionetworks.web.client.utils.CallbackP;
import org.sagebionetworks.web.client.widget.LoadMoreWidgetContainer;
import org.sagebionetworks.web.client.widget.docker.modal.AddExternalRepoModal;
import org.sagebionetworks.web.client.widget.entity.controller.PreflightController;
import org.sagebionetworks.web.client.widget.entity.controller.SynapseAlert;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class DockerRepoListWidget implements DockerRepoListWidgetView.Presenter {

	private DockerRepoListWidgetView view;
	private SynapseClientAsync synapseClient;
	private AddExternalRepoModal addExternalRepoModal;
	private PreflightController preflightController;
	private SynapseAlert synAlert;
	private EntityBundle projectBundle;
	private EntityChildrenRequest query;
	private CallbackP<EntityBundle> onRepoClickCallback;
	private LoadMoreWidgetContainer membersContainer;


	@Inject
	public DockerRepoListWidget(
			DockerRepoListWidgetView view,
			SynapseClientAsync synapseClient,
			AddExternalRepoModal addExternalRepoModal,
			PreflightController preflightController,
			LoadMoreWidgetContainer membersContainer,
			SynapseAlert synAlert
			) {
		this.view = view;
		this.synapseClient = synapseClient;
		this.addExternalRepoModal = addExternalRepoModal;
		this.preflightController = preflightController;
		this.synAlert = synAlert;
		this.membersContainer = membersContainer;
		view.setPresenter(this);
		view.addExternalRepoModal(addExternalRepoModal.asWidget());
		view.setSynAlert(synAlert.asWidget());
		view.setMembersContainer(membersContainer);
		membersContainer.configure(new Callback() {
			@Override
			public void invoke() {
				loadMore();
			}
		});
	}

	public Widget asWidget() {
		return view.asWidget();
	}

	@Override
	public void onClickAddExternalRepo() {
		preflightController.checkCreateEntity(projectBundle, DockerRepository.class.getName(), new Callback() {
			@Override
			public void invoke() {
				addExternalRepoModal.show();
			}
		});
	}

	public void setRepoClickedCallback(CallbackP<EntityBundle> onRepoClickCallback) {
		this.onRepoClickCallback = onRepoClickCallback;
	}

	/**
	 * Configure this widget before use.
	 * @param projectBundle
	 */
	public void configure(EntityBundle projectBundle) {
		this.projectBundle = projectBundle;
		String projectId = projectBundle.getEntity().getId();
		this.query = createDockerRepoEntityQuery(projectId);
		view.setAddExternalRepoButtonVisible(projectBundle.getPermissions().getCanAddChild());
		addExternalRepoModal.configuration(projectId, new Callback(){		
			
			@Override		
			public void invoke() {		
				performInitialLoad();
			}		
		});
		performInitialLoad();
	}
	
	private void performInitialLoad() {
		view.clear();
		query.setNextPageToken(null);
		loadMore();
	}

	public void loadMore() {
		synAlert.clear();
		synapseClient.getEntityChildren(query, new AsyncCallback<EntityChildrenResponse>() {
			public void onSuccess(EntityChildrenResponse results) {
				query.setNextPageToken(results.getNextPageToken());
				membersContainer.setIsMore(results.getNextPageToken() != null);
				setResults(results.getPage());
			};
			@Override
			public void onFailure(Throwable caught) {
				synAlert.handleException(caught);
			}
			
		});
	}
	
	private void setResults(List<EntityHeader> results) {
		synAlert.clear();
		int mask = ENTITY | ANNOTATIONS | PERMISSIONS | ENTITY_PATH | ACCESS_REQUIREMENTS | UNMET_ACCESS_REQUIREMENTS | DOI;
		for (EntityHeader header: results) {
			synapseClient.getEntityBundle(header.getId(), mask, new AsyncCallback<EntityBundle>(){
				@Override
				public void onSuccess(EntityBundle bundle) {
					view.addRepo(bundle);
				}
				@Override
				public void onFailure(Throwable error) {
					synAlert.handleException(error);
				}
			});
		}
	}

	/**
	 * Create a new query.
	 * @param parentId
	 * @return
	 */
	public EntityChildrenRequest createDockerRepoEntityQuery(String projectId) {
		EntityChildrenRequest newQuery = new EntityChildrenRequest();
		newQuery.setParentId(projectId);
		newQuery.setSortBy(SortBy.CREATED_ON);
		newQuery.setSortDirection(Direction.DESC);
		List<EntityType> types = new ArrayList<EntityType>();
		types.add(EntityType.dockerrepo);
		newQuery.setIncludeTypes(types);
		return newQuery;
	}

	@Override
	public void onRepoClicked(EntityBundle bundle) {
		if (onRepoClickCallback != null) {
			onRepoClickCallback.invoke(bundle);
		}
	}
}

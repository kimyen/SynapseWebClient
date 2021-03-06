package org.sagebionetworks.web.unitclient.widget.docker;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.sagebionetworks.web.client.widget.docker.DockerRepoListWidget.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityChildrenResponse;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
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
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.utils.Callback;
import org.sagebionetworks.web.client.widget.LoadMoreWidgetContainer;
import org.sagebionetworks.web.client.widget.docker.DockerRepoListWidget;
import org.sagebionetworks.web.client.widget.docker.DockerRepoListWidgetView;
import org.sagebionetworks.web.client.widget.docker.modal.AddExternalRepoModal;
import org.sagebionetworks.web.client.widget.entity.controller.PreflightController;
import org.sagebionetworks.web.client.widget.entity.controller.SynapseAlert;
import org.sagebionetworks.web.client.widget.pagination.PaginationWidget;
import org.sagebionetworks.web.test.helper.AsyncMockStubber;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;

public class DockerRepoListWidgetTest {
	@Mock
	private PreflightController mockPreflightController;
	@Mock
	private DockerRepoListWidgetView mockView;
	@Mock
	private PaginationWidget mockPaginationWidget;
	@Mock
	private AddExternalRepoModal mockAddExternalRepoModal;
	@Mock
	private SynapseClientAsync mockSynapseClient;
	@Mock
	private EntityBundle mockProjectBundle;
	@Mock
	private Project mockProject;
	@Mock
	private SynapseAlert mockSynAlert;
	@Mock
	private UserEntityPermissions mockUserEntityPermissions;
	@Mock
	private LoadMoreWidgetContainer mockMembersContainer;
	@Mock
	EntityChildrenResponse mockResults;
	List<EntityHeader> searchResults;
	
	DockerRepoListWidget dockerRepoListWidget;
	String projectId;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		dockerRepoListWidget = new DockerRepoListWidget(mockView, mockSynapseClient,
				mockAddExternalRepoModal, mockPreflightController, mockMembersContainer,
				mockSynAlert);
		projectId = "syn123";
		when(mockProjectBundle.getEntity()).thenReturn(mockProject);
		when(mockProject.getId()).thenReturn(projectId);
		when(mockProjectBundle.getPermissions()).thenReturn(mockUserEntityPermissions);
		when(mockUserEntityPermissions.getCanAddChild()).thenReturn(true);
		AsyncMockStubber.callSuccessWith(mockResults).when(mockSynapseClient)
			.getEntityChildren(any(EntityChildrenRequest.class), any(AsyncCallback.class));
		searchResults = new ArrayList<EntityHeader>();
		when(mockResults.getPage()).thenReturn(searchResults);
	}

	@Test
	public void testConstruction() {
		verify(mockView).setPresenter(dockerRepoListWidget);
		verify(mockView).addExternalRepoModal(any(Widget.class));
		verify(mockView).setMembersContainer(mockMembersContainer);
		verify(mockView).setSynAlert(any(Widget.class));
	}

	@Test
	public void testAsWidget() {
		dockerRepoListWidget.asWidget();
		verify(mockView).asWidget();
	}

	@Test
	public void testOnClickAddExternalRepo() {
		dockerRepoListWidget.configure(mockProjectBundle);
		dockerRepoListWidget.onClickAddExternalRepo();
		verify(mockPreflightController).checkCreateEntity(eq(mockProjectBundle), eq(DockerRepository.class.getName()), any(Callback.class));
	}

	@Test
	public void testOnClickAddExternalRepoPreflightFailed(){
		AsyncMockStubber.callNoInvovke().when(mockPreflightController).checkCreateEntity(eq(mockProjectBundle), eq(DockerRepository.class.getName()), any(Callback.class));
		dockerRepoListWidget.configure(mockProjectBundle);
		dockerRepoListWidget.onClickAddExternalRepo();
		verify(mockAddExternalRepoModal, never()).show();
	}

	@Test
	public void testOnClickAddExternalRepoPreflightPassed(){
		AsyncMockStubber.callWithInvoke().when(mockPreflightController).checkCreateEntity(eq(mockProjectBundle), eq(DockerRepository.class.getName()), any(Callback.class));
		dockerRepoListWidget.configure(mockProjectBundle);
		dockerRepoListWidget.onClickAddExternalRepo();
		verify(mockAddExternalRepoModal).show();
	}

	@Test
	public void testCreateDockerRepoEntityQuery() {
		EntityChildrenRequest query = dockerRepoListWidget.createDockerRepoEntityQuery(projectId);
		assertEquals(projectId, query.getParentId());
		assertEquals(Collections.singletonList(EntityType.dockerrepo), query.getIncludeTypes());
		assertEquals(SortBy.CREATED_ON, query.getSortBy());
		assertEquals(Direction.DESC, query.getSortDirection());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testConfigurationSuccess() {
		String id1 = "syn1", id2 = "syn2";
		EntityHeader header1 = new EntityHeader();
		header1.setId(id1);
		EntityHeader header2 = new EntityHeader();
		header2.setId(id2);
		searchResults.add(header1);
		searchResults.add(header2);
		EntityBundle bundle1 = new EntityBundle();
		EntityBundle bundle2 = new EntityBundle();
		AsyncMockStubber.callSuccessWith(bundle1, bundle2)
			.when(mockSynapseClient).getEntityBundle(anyString(), anyInt(), any(AsyncCallback.class));
		dockerRepoListWidget.configure(mockProjectBundle);
		verify(mockAddExternalRepoModal).configuration(eq(projectId), any(Callback.class));
		verify(mockSynapseClient).getEntityChildren(any(EntityChildrenRequest.class), any(AsyncCallback.class));
		verify(mockView).clear();
		verify(mockView, atLeastOnce()).addRepo(bundle1);
		verify(mockView, atLeastOnce()).addRepo(bundle2);
		verify(mockSynapseClient).getEntityBundle(eq(id1), anyInt(), any(AsyncCallback.class));
		verify(mockSynapseClient).getEntityBundle(eq(id2), anyInt(), any(AsyncCallback.class));
		verify(mockView).setAddExternalRepoButtonVisible(true);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testConfigurationSuccessOverOnePage() {
		dockerRepoListWidget.configure(mockProjectBundle);
		verify(mockAddExternalRepoModal).configuration(eq(projectId), any(Callback.class));
		verify(mockSynapseClient).getEntityChildren(any(EntityChildrenRequest.class), any(AsyncCallback.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testConfigurationQueryFailure() {
		Throwable error = new Throwable();
		AsyncMockStubber.callFailureWith(error)
			.when(mockSynapseClient).getEntityChildren(any(EntityChildrenRequest.class), any(AsyncCallback.class));
		dockerRepoListWidget.configure(mockProjectBundle);
		verify(mockAddExternalRepoModal).configuration(eq(projectId), any(Callback.class));
		verify(mockSynapseClient).getEntityChildren(any(EntityChildrenRequest.class), any(AsyncCallback.class));
		verify(mockView).clear();
		verify(mockView, never()).addRepo(any(EntityBundle.class));
		verify(mockSynAlert).handleException(error);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testConfigurationFailToGetSecondBundle() {
		String id1 = "syn1", id2 = "syn2";
		EntityHeader header1 = new EntityHeader();
		header1.setId(id1);
		EntityHeader header2 = new EntityHeader();
		header2.setId(id2);
		searchResults.add(header1);
		searchResults.add(header2);
		EntityBundle bundle = new EntityBundle();
		AsyncMockStubber.callSuccessWith(bundle)
			.when(mockSynapseClient).getEntityBundle(eq(id1), anyInt(), any(AsyncCallback.class));
		Throwable error = new Throwable();
		AsyncMockStubber.callFailureWith(error)
			.when(mockSynapseClient).getEntityBundle(eq(id2), anyInt(), any(AsyncCallback.class));
		dockerRepoListWidget.configure(mockProjectBundle);
		verify(mockAddExternalRepoModal).configuration(eq(projectId), any(Callback.class));
		verify(mockSynapseClient).getEntityChildren(any(EntityChildrenRequest.class), any(AsyncCallback.class));
		verify(mockView).clear();
		verify(mockView).addRepo(bundle);
		verify(mockSynapseClient).getEntityBundle(eq(id1), anyInt(), any(AsyncCallback.class));
		verify(mockSynapseClient).getEntityBundle(eq(id2), anyInt(), any(AsyncCallback.class));
		verify(mockSynAlert).handleException(error);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testConfigurationFailToGetFirstBundle() {
		String id1 = "syn1", id2 = "syn2";
		EntityHeader header1 = new EntityHeader();
		header1.setId(id1);
		EntityHeader header2 = new EntityHeader();
		header2.setId(id2);
		searchResults.add(header1);
		searchResults.add(header2);
		EntityBundle bundle = new EntityBundle();
		AsyncMockStubber.callSuccessWith(bundle)
			.when(mockSynapseClient).getEntityBundle(eq(id2), anyInt(), any(AsyncCallback.class));
		Throwable error = new Throwable();
		AsyncMockStubber.callFailureWith(error)
			.when(mockSynapseClient).getEntityBundle(eq(id1), anyInt(), any(AsyncCallback.class));
		dockerRepoListWidget.configure(mockProjectBundle);
		verify(mockAddExternalRepoModal).configuration(eq(projectId), any(Callback.class));
		verify(mockSynapseClient).getEntityChildren(any(EntityChildrenRequest.class), any(AsyncCallback.class));
		verify(mockView).addRepo(bundle);
		verify(mockSynapseClient).getEntityBundle(eq(id1), anyInt(), any(AsyncCallback.class));
		verify(mockSynapseClient).getEntityBundle(eq(id2), anyInt(), any(AsyncCallback.class));
		verify(mockSynAlert).handleException(error);
	}

	@Test
	public void testConfigurationWithoutUploadPermission() {
		when(mockUserEntityPermissions.getCanAddChild()).thenReturn(false);
		dockerRepoListWidget.configure(mockProjectBundle);
		verify(mockView).setAddExternalRepoButtonVisible(false);
	}
	
	@Test
	public void testLoadMore() {
		when(mockResults.getNextPageToken()).thenReturn("not null");
		dockerRepoListWidget.configure(mockProjectBundle);
		verify(mockAddExternalRepoModal).configuration(eq(projectId), any(Callback.class));
		verify(mockSynapseClient).getEntityChildren(any(EntityChildrenRequest.class), any(AsyncCallback.class));
		verify(mockMembersContainer).setIsMore(true);
		when(mockResults.getNextPageToken()).thenReturn(null);
		dockerRepoListWidget.loadMore();
		verify(mockSynapseClient, times(2)).getEntityChildren(any(EntityChildrenRequest.class), any(AsyncCallback.class));
		verify(mockMembersContainer).setIsMore(false);
	}
}

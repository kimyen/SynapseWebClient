package org.sagebionetworks.web.unitclient.widget.entity;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.cache.ClientCache;
import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.client.events.EntityUpdatedEvent;
import org.sagebionetworks.web.client.events.EntityUpdatedHandler;
import org.sagebionetworks.web.client.place.Synapse;
import org.sagebionetworks.web.client.place.Synapse.EntityArea;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.utils.CallbackP;
import org.sagebionetworks.web.client.widget.display.ProjectDisplayDialog;
import org.sagebionetworks.web.client.widget.entity.EntityMetadata;
import org.sagebionetworks.web.client.widget.entity.EntityPageTop;
import org.sagebionetworks.web.client.widget.entity.EntityPageTopView;
import org.sagebionetworks.web.client.widget.entity.WikiPageWidget;
import org.sagebionetworks.web.client.widget.entity.controller.EntityActionController;
import org.sagebionetworks.web.client.widget.entity.menu.v2.Action;
import org.sagebionetworks.web.client.widget.entity.menu.v2.ActionMenuWidget;
import org.sagebionetworks.web.client.widget.entity.menu.v2.ActionMenuWidget.ActionListener;
import org.sagebionetworks.web.client.widget.entity.tabs.ChallengeTab;
import org.sagebionetworks.web.client.widget.entity.tabs.DiscussionTab;
import org.sagebionetworks.web.client.widget.entity.tabs.DockerTab;
import org.sagebionetworks.web.client.widget.entity.tabs.FilesTab;
import org.sagebionetworks.web.client.widget.entity.tabs.Tab;
import org.sagebionetworks.web.client.widget.entity.tabs.TablesTab;
import org.sagebionetworks.web.client.widget.entity.tabs.Tabs;
import org.sagebionetworks.web.client.widget.entity.tabs.WikiTab;
import org.sagebionetworks.web.test.helper.AsyncMockStubber;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;

public class EntityPageTopTest {

	@Mock
	EntityPageTopView mockView;
	@Mock
	EntityBundle mockProjectBundle;
	@Mock
	EntityHeader mockProjectHeader;
	@Mock
	Project mockProjectEntity;
	@Mock
	FileEntity mockFileEntity;
	@Mock
	TableEntity mockTableEntity;
	@Mock
	DockerRepository mockDockerEntity;
	@Mock
	SynapseClientAsync mockSynapseClientAsync;
	@Mock
	AuthenticationController mockAuthController;
	@Mock
	Tabs mockTabs;
	@Mock
	EntityMetadata mockEntityMetadata;
	@Mock
	WikiTab mockWikiTab;
	@Mock
	Tab mockWikiInnerTab;
	@Mock
	FilesTab mockFilesTab;
	@Mock
	Tab mockFilesInnerTab;
	@Mock
	TablesTab mockTablesTab;
	@Mock
	Tab mockTablesInnerTab;
	@Mock
	ChallengeTab mockChallengeTab;
	@Mock
	Tab mockChallengeInnerTab;
	@Mock
	DiscussionTab mockDiscussionTab;
	@Mock
	DockerTab mockDockerTab;
	@Mock
	Tab mockDiscussionInnerTab;
	@Mock
	Tab mockDockerInnerTab;
	@Mock
	UserEntityPermissions mockPermissions;
	@Mock
	EntityActionController mockEntityActionController;
	@Mock
	ActionMenuWidget mockActionMenuWidget;
	@Mock
	EntityUpdatedHandler mockEntityUpdatedHandler;
	@Mock
	AccessControlList mockACL;
	@Mock
	CookieProvider mockCookies;
	@Mock
	ClientCache mockStorage;
	@Captor
	ArgumentCaptor<WikiPageWidget.Callback> wikiCallbackCaptor; 
	
	EntityPageTop pageTop;
	String projectEntityId = "syn123";
	String projectName = "fooooo";
	String projectWikiId = "31415926666";
	String userId = "1234567";
	boolean canEdit = true;
	boolean canModerate = false;
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		when(mockFilesTab.asTab()).thenReturn(mockFilesInnerTab);
		when(mockWikiTab.asTab()).thenReturn(mockWikiInnerTab);
		when(mockTablesTab.asTab()).thenReturn(mockTablesInnerTab);
		when(mockChallengeTab.asTab()).thenReturn(mockChallengeInnerTab);
		when(mockDiscussionTab.asTab()).thenReturn(mockDiscussionInnerTab);
		when(mockDockerTab.asTab()).thenReturn(mockDockerInnerTab);
		pageTop = new EntityPageTop(mockView, mockSynapseClientAsync, mockAuthController, mockTabs, mockEntityMetadata,
				mockWikiTab, mockFilesTab, mockTablesTab, mockChallengeTab, mockDiscussionTab, mockDockerTab,
				mockEntityActionController, mockActionMenuWidget, mockCookies, mockStorage);
		pageTop.setEntityUpdatedHandler(mockEntityUpdatedHandler);
		AsyncMockStubber.callSuccessWith(mockProjectBundle).when(mockSynapseClientAsync).getEntityBundle(anyString(), anyInt(), any(AsyncCallback.class));
		when(mockProjectBundle.getEntity()).thenReturn(mockProjectEntity);
		when(mockProjectEntity.getId()).thenReturn(projectEntityId);
		when(mockProjectBundle.getRootWikiId()).thenReturn(projectWikiId);
		when(mockProjectHeader.getId()).thenReturn(projectEntityId);
		when(mockProjectHeader.getName()).thenReturn(projectName);
		when(mockProjectBundle.getPermissions()).thenReturn(mockPermissions);
		when(mockPermissions.getCanCertifiedUserEdit()).thenReturn(canEdit);
		when(mockPermissions.getCanModerate()).thenReturn(canModerate);
		when(mockProjectBundle.getAccessControlList()).thenReturn(mockACL);
		when(mockCookies.getCookie(DisplayUtils.SYNAPSE_TEST_WEBSITE_COOKIE_KEY)).thenReturn("fake cookie");
		
		AsyncMockStubber.callSuccessWith(true).when(mockSynapseClientAsync).isWiki(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(true).when(mockSynapseClientAsync).isFileOrFolder(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(true).when(mockSynapseClientAsync).isTable(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(true).when(mockSynapseClientAsync).isChallenge(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(true).when(mockSynapseClientAsync).isDocker(anyString(), any(AsyncCallback.class));
		
		when(mockWikiInnerTab.isTabListItemVisible()).thenReturn(true);
		when(mockFilesInnerTab.isTabListItemVisible()).thenReturn(true);
		when(mockTablesInnerTab.isTabListItemVisible()).thenReturn(true);
		when(mockChallengeInnerTab.isTabListItemVisible()).thenReturn(true);
		when(mockDiscussionInnerTab.isTabListItemVisible()).thenReturn(true);
		when(mockDockerInnerTab.isTabListItemVisible()).thenReturn(true);
		
		when(mockAuthController.getCurrentUserPrincipalId()).thenReturn(userId);
		
		when(mockWikiInnerTab.isContentStale()).thenReturn(true);
		when(mockFilesInnerTab.isContentStale()).thenReturn(true);
		when(mockTablesInnerTab.isContentStale()).thenReturn(true);
		when(mockDiscussionInnerTab.isContentStale()).thenReturn(true);
		when(mockDockerInnerTab.isContentStale()).thenReturn(true);
		when(mockChallengeInnerTab.isContentStale()).thenReturn(true);
		when(mockTabs.getTabCount()).thenReturn(6);
	}
	
	@Test
	public void testConstruction(){
		verify(mockView).setTabs(any(Widget.class));
		verify(mockView).setProjectMetadata(any(Widget.class));
		verify(mockView).setPresenter(pageTop);
		verify(mockActionMenuWidget).addControllerWidget(any(Widget.class));
		verify(mockView).setActionMenu(any(Widget.class));
		verify(mockTabs).addTab(mockFilesInnerTab);
		verify(mockTabs).addTab(mockWikiInnerTab);
		verify(mockTabs).addTab(mockTablesInnerTab);
		verify(mockTabs).addTab(mockChallengeInnerTab);
		verify(mockTabs).addTab(mockDiscussionInnerTab);
		verify(mockTabs).addTab(mockDockerInnerTab);
		
		verify(mockActionMenuWidget).addActionListener(eq(Action.TOGGLE_ANNOTATIONS), any(ActionListener.class));
		ArgumentCaptor<CallbackP> captor = ArgumentCaptor.forClass(CallbackP.class);
		verify(mockFilesTab).setShowProjectInfoCallback(captor.capture());
		
		//when this is invoked, the message is sent to the view
		CallbackP showHideProjectInfoCallback = captor.getValue();
		reset(mockView);
		showHideProjectInfoCallback.invoke(true);
		verify(mockView).setProjectInformationVisible(true);
		reset(mockView);
		showHideProjectInfoCallback.invoke(false);
		verify(mockView).setProjectInformationVisible(false);
		reset(mockView);
		verify(mockTablesTab).setShowProjectInfoCallback(any(CallbackP.class));
		
		pageTop.configure(mockFileEntity, null, mockProjectHeader, null, null);
		//when wiki tab is clicked, then wiki is configured and project info is shown (when a project bundle is configured)
		ArgumentCaptor<CallbackP> tabCaptor = ArgumentCaptor.forClass(CallbackP.class);
		verify(mockWikiTab).setTabClickedCallback(tabCaptor.capture());
		CallbackP showProjectInfoCallback = tabCaptor.getValue();
		showProjectInfoCallback.invoke(null);
		
		verify(mockView).setProjectInformationVisible(false);
		verify(mockEntityMetadata).setEntityUpdatedHandler(mockEntityUpdatedHandler);
		
		reset(mockView);
		pageTop.configure(mockProjectEntity, null, mockProjectHeader, null, null);
		showProjectInfoCallback.invoke(null);
		verify(mockView, atLeastOnce()).setProjectInformationVisible(true);
	}
	
	@Test
	public void testConfigureWithProject(){
		Synapse.EntityArea area = null;
		String areaToken = null;
		Long versionNumber = null;
		pageTop.configure(mockProjectEntity, versionNumber, mockProjectHeader, area, areaToken);
		//Area was not defined for this project, should try to go to wiki tab by default.
		
		//Once to show the active tab, and once after configuration so that the place is pushed into the history.
		verify(mockTabs, times(2)).showTab(mockWikiInnerTab, EntityPageTop.PUSH_TAB_URL_TO_BROWSER_HISTORY);
		verify(mockEntityMetadata).setEntityBundle(mockProjectBundle, null);
		verify(mockWikiInnerTab).setContentStale(true);
		verify(mockWikiInnerTab).setContentStale(false);
		
		verify(mockWikiTab).configure(eq(projectEntityId), eq(projectName), eq(projectWikiId), eq(canEdit), any(WikiPageWidget.Callback.class));
		verify(mockEntityActionController).configure(mockActionMenuWidget, mockProjectBundle, true, projectWikiId, mockEntityUpdatedHandler);
		
		verify(mockFilesTab, never()).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockFilesTab, never()).configure(mockProjectEntity, mockEntityUpdatedHandler, versionNumber);
		verify(mockTablesTab, never()).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockTablesTab, never()).configure(mockProjectEntity, mockEntityUpdatedHandler, areaToken);
		verify(mockChallengeTab, never()).configure(projectEntityId, projectName);
		verify(mockDiscussionTab, never()).configure(projectEntityId, projectName, areaToken, canModerate);
		verify(mockDockerTab, never()).configure(mockProjectEntity, mockEntityUpdatedHandler, areaToken);
		
		clickAllTabs();
		
		verify(mockFilesTab).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockFilesTab).configure(mockProjectEntity, mockEntityUpdatedHandler, versionNumber);
		verify(mockTablesTab).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockTablesTab).configure(mockProjectEntity, mockEntityUpdatedHandler, areaToken);
		verify(mockChallengeTab).configure(projectEntityId, projectName);
		verify(mockDiscussionTab).configure(projectEntityId, projectName, areaToken, canModerate);
		verify(mockDockerTab).configure(mockProjectEntity, mockEntityUpdatedHandler, areaToken);
	}
	
	private void clickAllTabs() {
		//now go through and click on the tabs
		ArgumentCaptor<CallbackP> tabCaptor = ArgumentCaptor.forClass(CallbackP.class);

		//click on the wiki tab
		verify(mockWikiTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);
		//click on the files tab
		verify(mockFilesTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);
		//click on the tables tab
		verify(mockTablesTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);
		//click on the challenge tab
		verify(mockChallengeTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);
		//click on the discussion tab
		verify(mockDiscussionTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);
		//click on the docker tab
		verify(mockDockerTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);
	}
	
	@Test
	public void testConfigureWithProjectWikiToken(){
		Synapse.EntityArea area = EntityArea.WIKI;
		//verify this wiki id area token is passed to the wiki tab configuration and the entity action controller configuration
		String areaToken = "1234";
		Long versionNumber = null;
		pageTop.configure(mockProjectEntity, versionNumber, mockProjectHeader, area, areaToken);
		verify(mockEntityActionController).configure(eq(mockActionMenuWidget), eq(mockProjectBundle), eq(true), eq(areaToken), any(EntityUpdatedHandler.class));
	}
	
	@Test
	public void testClear(){
		pageTop.clearState();
		verify(mockView).clear();
		verify(mockWikiTab).clear();
	}
	
	
	@Test
	public void testConfigureWithFile(){
		Synapse.EntityArea area = null;
		String areaToken = null;
		Long versionNumber = 5L;
		pageTop.configure(mockFileEntity, versionNumber, mockProjectHeader, area, areaToken);
		verify(mockTabs).showTab(mockFilesInnerTab, EntityPageTop.PUSH_TAB_URL_TO_BROWSER_HISTORY);
		verify(mockEntityMetadata).setEntityBundle(mockProjectBundle, null);
		
		verify(mockFilesTab).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockFilesTab).configure(mockFileEntity, mockEntityUpdatedHandler, versionNumber);
		
		verify(mockWikiTab, never()).configure(eq(projectEntityId), eq(projectName), eq(projectWikiId), eq(canEdit), any(WikiPageWidget.Callback.class));
		verify(mockTablesTab, never()).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockTablesTab, never()).configure(mockFileEntity, mockEntityUpdatedHandler, areaToken);
		verify(mockChallengeTab, never()).configure(projectEntityId, projectName);
		verify(mockDiscussionTab, never()).configure(projectEntityId, projectName, null, canModerate);
		verify(mockDockerTab, never()).configure(mockFileEntity, mockEntityUpdatedHandler, null);
	}
	
	@Test
	public void testConfigureWithFileAndFailureToLoadProject(){
		Exception projectLoadError = new Exception("failed to load project");
		AsyncMockStubber.callFailureWith(projectLoadError).when(mockSynapseClientAsync).getEntityBundle(anyString(), anyInt(), any(AsyncCallback.class));
		Synapse.EntityArea area = null;
		String areaToken = null;
		Long versionNumber = 5L;
		pageTop.configure(mockFileEntity, versionNumber, mockProjectHeader, area, areaToken);
		verify(mockTabs).showTab(mockFilesInnerTab, EntityPageTop.PUSH_TAB_URL_TO_BROWSER_HISTORY);
		
		verify(mockEntityMetadata, Mockito.never()).setEntityBundle(mockProjectBundle, null);
		EntityBundle expectedProjectEntityBundle = null;
		verify(mockFilesTab).setProject(projectEntityId, expectedProjectEntityBundle, projectLoadError);
		verify(mockFilesTab).configure(mockFileEntity, mockEntityUpdatedHandler, versionNumber);
		
		clickAllTabs();
		verify(mockWikiTab).configure(eq(projectEntityId), eq(projectName), eq((String)null), eq(false), any(WikiPageWidget.Callback.class));
		verify(mockTablesTab).setProject(projectEntityId, expectedProjectEntityBundle, projectLoadError);
		verify(mockTablesTab).configure(mockFileEntity, mockEntityUpdatedHandler, areaToken);
		verify(mockChallengeTab).configure(projectEntityId, projectName);
		verify(mockDiscussionTab).configure(projectEntityId, projectName, null, false);
		verify(mockDockerTab).configure(mockFileEntity, mockEntityUpdatedHandler, null);
	}
	
	@Test
	public void testConfigureWithTable(){
		Synapse.EntityArea area = null;
		String areaToken = "a table query area token";
		Long versionNumber = null;
		pageTop.configure(mockTableEntity, versionNumber, mockProjectHeader, area, areaToken);
		verify(mockTabs).showTab(mockTablesInnerTab, EntityPageTop.PUSH_TAB_URL_TO_BROWSER_HISTORY);
		
		verify(mockEntityMetadata).setEntityBundle(mockProjectBundle, null);
		
		verify(mockTablesTab).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockTablesTab).configure(mockTableEntity, mockEntityUpdatedHandler, areaToken);
		
		clickAllTabs();
		verify(mockWikiTab).configure(eq(projectEntityId), eq(projectName), eq(projectWikiId), eq(canEdit), any(WikiPageWidget.Callback.class));
		verify(mockFilesTab).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockFilesTab).configure(mockTableEntity, mockEntityUpdatedHandler, versionNumber);
		verify(mockChallengeTab).configure(projectEntityId, projectName);
		verify(mockDiscussionTab).configure(projectEntityId, projectName, null, canModerate);
		verify(mockDockerTab).configure(mockTableEntity, mockEntityUpdatedHandler, null);
	}

	@Test
	public void testConfigureWithDocker(){
		Synapse.EntityArea area = null;
		String areaToken = "docker area token";
		Long versionNumber = null;
		pageTop.configure(mockDockerEntity, versionNumber, mockProjectHeader, area, areaToken);
		verify(mockTabs).showTab(mockDockerInnerTab, EntityPageTop.PUSH_TAB_URL_TO_BROWSER_HISTORY);
		
		verify(mockEntityMetadata).setEntityBundle(mockProjectBundle, null);
		
		verify(mockDockerTab).configure(mockDockerEntity, mockEntityUpdatedHandler, areaToken);
		
		clickAllTabs();
		verify(mockWikiTab).configure(eq(projectEntityId), eq(projectName), eq(projectWikiId), eq(canEdit), any(WikiPageWidget.Callback.class));
		verify(mockFilesTab).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockFilesTab).configure(mockDockerEntity, mockEntityUpdatedHandler, versionNumber);
		verify(mockTablesTab).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockTablesTab).configure(mockDockerEntity, mockEntityUpdatedHandler, null);
		verify(mockChallengeTab).configure(projectEntityId, projectName);
		verify(mockDiscussionTab).configure(projectEntityId, projectName, null, canModerate);
	}

	@Test
	public void testConfigureWithFileGoToChallengeAdminTab(){
		Synapse.EntityArea area = Synapse.EntityArea.ADMIN;
		String areaToken = null;
		Long versionNumber = null;
		pageTop.configure(mockFileEntity, versionNumber, mockProjectHeader, area, areaToken);
		verify(mockTabs).showTab(mockChallengeInnerTab, EntityPageTop.PUSH_TAB_URL_TO_BROWSER_HISTORY);
		
		verify(mockEntityMetadata).setEntityBundle(mockProjectBundle, null);
		
		verify(mockChallengeTab).configure(projectEntityId, projectName);
		clickAllTabs();
		verify(mockWikiTab).configure(eq(projectEntityId), eq(projectName), eq(projectWikiId), eq(canEdit), any(WikiPageWidget.Callback.class));
		verify(mockFilesTab).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockFilesTab).configure(mockFileEntity, mockEntityUpdatedHandler, versionNumber);
		verify(mockTablesTab).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockTablesTab).configure(mockFileEntity, mockEntityUpdatedHandler, areaToken);
		verify(mockDiscussionTab).configure(projectEntityId, projectName, null, canModerate);
		verify(mockDockerTab).configure(mockFileEntity, mockEntityUpdatedHandler, null);
	}
	
	@Test
	public void testFireEntityUpdatedEvent() {
		pageTop.fireEntityUpdatedEvent();
		verify(mockEntityUpdatedHandler).onPersistSuccess(any(EntityUpdatedEvent.class));
	}
	
	@Test
	public void testGetWikiPageId() {
		String areaToken = "123";
		String rootWikiId = "456";
		//should use wiki area token wiki id if available
		assertEquals(areaToken, pageTop.getWikiPageId(areaToken, rootWikiId));
		//and the root wiki id if area token is not defined
		assertEquals(rootWikiId, pageTop.getWikiPageId("", rootWikiId));
		assertEquals(rootWikiId, pageTop.getWikiPageId(null, rootWikiId));
	}
	
	@Test
	public void testConfigureProjectInvalidWikiId() {
		Synapse.EntityArea area = null;
		String invalidWikiId = "1234";
		Long versionNumber = null;
		pageTop.configure(mockProjectEntity, versionNumber, mockProjectHeader, area, invalidWikiId);
		
		verify(mockWikiTab).configure(eq(projectEntityId), eq(projectName), eq(invalidWikiId), eq(canEdit), wikiCallbackCaptor.capture());
		
		when(mockWikiInnerTab.isContentStale()).thenReturn(true);
		//simulate not found
		wikiCallbackCaptor.getValue().noWikiFound();
		//since the project has a root wiki id, it should try to load that instead.
		verify(mockView).showInfo(anyString(), anyString());
		verify(mockWikiTab).configure(eq(projectEntityId), eq(projectName), eq(projectWikiId), eq(canEdit), any(WikiPageWidget.Callback.class));
	}
	
	@Test
	public void testConfigureProjectNoWiki() {
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseClientAsync).isWiki(anyString(), any(AsyncCallback.class));
		when(mockWikiInnerTab.isTabListItemVisible()).thenReturn(false);
		// we are asking for an invalid wiki id for a project that contains no wiki.
		Synapse.EntityArea area = null;
		String invalidWikiId = "1234";
		Long versionNumber = null;
		when(mockProjectBundle.getRootWikiId()).thenReturn(null);
		pageTop.configure(mockProjectEntity, versionNumber, mockProjectHeader, area, invalidWikiId);
		verify(mockWikiTab, never()).configure(anyString(), anyString(), anyString(), anyBoolean(), any(WikiPageWidget.Callback.class));
		
		when(mockWikiInnerTab.isContentStale()).thenReturn(true);
		//since the project does not have a root wiki id, it should go to the files tab
		verify(mockFilesTab).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockFilesTab).configure(mockProjectEntity, mockEntityUpdatedHandler, versionNumber);
	}
	
	@Test
	public void testContentMultipleTabsShown() {
		Synapse.EntityArea area = null;
		String areaToken = null;
		Long versionNumber = null;
		pageTop.configure(mockProjectEntity, versionNumber, mockProjectHeader, area, areaToken);
		verify(mockWikiInnerTab, atLeastOnce()).setTabListItemVisible(true);
		verify(mockFilesInnerTab, atLeastOnce()).setTabListItemVisible(true);
		verify(mockTablesInnerTab, atLeastOnce()).setTabListItemVisible(true);
		verify(mockChallengeInnerTab, atLeastOnce()).setTabListItemVisible(true);
		verify(mockDiscussionInnerTab, atLeastOnce()).setTabListItemVisible(true);
		verify(mockDockerInnerTab, atLeastOnce()).setTabListItemVisible(true);
	}
	
	@Test
	public void testContentMixtureOfTabsShownCanEdit() {
		when(mockPermissions.getCanEdit()).thenReturn(true);
		Synapse.EntityArea area = null;
		String areaToken = null;
		Long versionNumber = null;
		
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseClientAsync).isTable(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseClientAsync).isChallenge(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseClientAsync).isDocker(anyString(), any(AsyncCallback.class));
		
		pageTop.configure(mockProjectEntity, versionNumber, mockProjectHeader, area, areaToken);
		
		verify(mockSynapseClientAsync, never()).isWiki(anyString(), any(AsyncCallback.class));
		verify(mockSynapseClientAsync, never()).isFileOrFolder(anyString(), any(AsyncCallback.class));
		verify(mockSynapseClientAsync, never()).isTable(anyString(), any(AsyncCallback.class));
		verify(mockSynapseClientAsync).isChallenge(anyString(), any(AsyncCallback.class));
		verify(mockSynapseClientAsync, never()).isDocker(anyString(), any(AsyncCallback.class));
		verify(mockSynapseClientAsync, never()).isForum(anyString(), any(AsyncCallback.class));
		
		verify(mockWikiInnerTab).setTabListItemVisible(true);
		verify(mockFilesInnerTab).setTabListItemVisible(true);
		verify(mockTablesInnerTab).setTabListItemVisible(true);
		verify(mockChallengeInnerTab, atLeastOnce()).setTabListItemVisible(false);
		verify(mockChallengeInnerTab, never()).setTabListItemVisible(true);
		verify(mockDiscussionInnerTab).setTabListItemVisible(true);
		verify(mockDockerInnerTab).setTabListItemVisible(true);
	}
	
	@Test
	public void testContentMixtureOfTabsShownCannotEdit() {
		when(mockPermissions.getCanEdit()).thenReturn(false);
		Synapse.EntityArea area = null;
		String areaToken = null;
		Long versionNumber = null;
		
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseClientAsync).isTable(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseClientAsync).isChallenge(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseClientAsync).isDocker(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseClientAsync).isForum(anyString(), any(AsyncCallback.class));
		
		pageTop.configure(mockProjectEntity, versionNumber, mockProjectHeader, area, areaToken);
		
		verify(mockSynapseClientAsync).isWiki(anyString(), any(AsyncCallback.class));
		verify(mockSynapseClientAsync).isFileOrFolder(anyString(), any(AsyncCallback.class));
		verify(mockSynapseClientAsync).isTable(anyString(), any(AsyncCallback.class));
		verify(mockSynapseClientAsync).isChallenge(anyString(), any(AsyncCallback.class));
		verify(mockSynapseClientAsync).isDocker(anyString(), any(AsyncCallback.class));
		verify(mockSynapseClientAsync, never()).isForum(anyString(), any(AsyncCallback.class));
		
		InOrder order = Mockito.inOrder(mockWikiInnerTab);
		order.verify(mockWikiInnerTab).setTabListItemVisible(false);
		order.verify(mockWikiInnerTab).setTabListItemVisible(true);
		
		order = Mockito.inOrder(mockFilesInnerTab);
		order.verify(mockFilesInnerTab).setTabListItemVisible(false);
		order.verify(mockFilesInnerTab).setTabListItemVisible(true);
		
		order = Mockito.inOrder(mockTablesInnerTab);
		order.verify(mockTablesInnerTab, atLeastOnce()).setTabListItemVisible(false);
		order.verify(mockTablesInnerTab, never()).setTabListItemVisible(true);
		
		order = Mockito.inOrder(mockChallengeInnerTab);
		order.verify(mockChallengeInnerTab, atLeastOnce()).setTabListItemVisible(false);
		order.verify(mockChallengeInnerTab, never()).setTabListItemVisible(true);
		
		order = Mockito.inOrder(mockDiscussionInnerTab);
		order.verify(mockDiscussionInnerTab, atLeastOnce()).setTabListItemVisible(false);
		order.verify(mockDiscussionInnerTab).setTabListItemVisible(true);
		
		order = Mockito.inOrder(mockDockerInnerTab);
		order.verify(mockDockerInnerTab, atLeastOnce()).setTabListItemVisible(false);
		order.verify(mockDockerInnerTab, never()).setTabListItemVisible(true);
	}
	
	@Test
	public void testContentOneTabShown() {
		Synapse.EntityArea area = null;
		String areaToken = null;
		Long versionNumber = null;
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseClientAsync).isFileOrFolder(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseClientAsync).isTable(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseClientAsync).isChallenge(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseClientAsync).isForum(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseClientAsync).isDocker(anyString(), any(AsyncCallback.class));
		
		pageTop.configure(mockProjectEntity, versionNumber, mockProjectHeader, area, areaToken);
		//should hide all tabs when only one will be shown
		verify(mockWikiInnerTab, atLeastOnce()).setTabListItemVisible(false);
		verify(mockFilesInnerTab, atLeastOnce()).setTabListItemVisible(false);
		verify(mockTablesInnerTab, atLeastOnce()).setTabListItemVisible(false);
		verify(mockChallengeInnerTab, atLeastOnce()).setTabListItemVisible(false);
		verify(mockDiscussionInnerTab, atLeastOnce()).setTabListItemVisible(false);
		verify(mockDockerInnerTab, atLeastOnce()).setTabListItemVisible(false);
	}

	@Test
	public void testCachedTabShown() {
		Synapse.EntityArea area = null;
		String areaToken = null;
		Long versionNumber = null;
		String storageKey = userId + "_" + projectEntityId + "_" + ProjectDisplayDialog.FILES;
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseClientAsync).isFileOrFolder(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseClientAsync).isTable(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseClientAsync).isChallenge(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseClientAsync).isForum(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseClientAsync).isDocker(anyString(), any(AsyncCallback.class));

		when(mockStorage.get(storageKey)).thenReturn(Boolean.TRUE.toString());
		
		pageTop.configure(mockProjectEntity, versionNumber, mockProjectHeader, area, areaToken);
		verify(mockWikiInnerTab, atLeastOnce()).setTabListItemVisible(true);
		verify(mockFilesInnerTab, atLeastOnce()).setTabListItemVisible(true);
		verify(mockTablesInnerTab, atLeastOnce()).setTabListItemVisible(false);
		verify(mockChallengeInnerTab, atLeastOnce()).setTabListItemVisible(false);
		verify(mockDiscussionInnerTab, atLeastOnce()).setTabListItemVisible(false);
		verify(mockDockerInnerTab, atLeastOnce()).setTabListItemVisible(false);
	}
	
}

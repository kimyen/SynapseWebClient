package org.sagebionetworks.web.unitserver;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.sagebionetworks.repo.model.EntityBundle.ACCESS_REQUIREMENTS;
import static org.sagebionetworks.repo.model.EntityBundle.ANNOTATIONS;
import static org.sagebionetworks.repo.model.EntityBundle.BENEFACTOR_ACL;
import static org.sagebionetworks.repo.model.EntityBundle.ENTITY;
import static org.sagebionetworks.repo.model.EntityBundle.ENTITY_PATH;
import static org.sagebionetworks.repo.model.EntityBundle.FILE_HANDLES;
import static org.sagebionetworks.repo.model.EntityBundle.HAS_CHILDREN;
import static org.sagebionetworks.repo.model.EntityBundle.PERMISSIONS;
import static org.sagebionetworks.repo.model.EntityBundle.ROOT_WIKI_ID;
import static org.sagebionetworks.repo.model.EntityBundle.UNMET_ACCESS_REQUIREMENTS;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.UserEvaluationPermissions;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.ExampleEntity;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.JoinTeamSignedToken;
import org.sagebionetworks.repo.model.LogEntry;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.MembershipRqstSubmission;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ProjectHeader;
import org.sagebionetworks.repo.model.ProjectListSortColumn;
import org.sagebionetworks.repo.model.ProjectListType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.SignedTokenInterface;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.TeamMembershipStatus;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserSessionData;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.ThreadCount;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.sagebionetworks.repo.model.entity.query.EntityQuery;
import org.sagebionetworks.repo.model.entity.query.EntityQueryResult;
import org.sagebionetworks.repo.model.entity.query.EntityQueryResults;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.file.BatchFileHandleCopyRequest;
import org.sagebionetworks.repo.model.file.BatchFileHandleCopyResult;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandleCopyRequest;
import org.sagebionetworks.repo.model.file.FileHandleCopyResult;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.FileResultFailureCode;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.State;
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;
import org.sagebionetworks.repo.model.file.UploadDestination;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.NotificationSettingsSignedToken;
import org.sagebionetworks.repo.model.message.Settings;
import org.sagebionetworks.repo.model.principal.AddEmailInfo;
import org.sagebionetworks.repo.model.principal.PrincipalAliasRequest;
import org.sagebionetworks.repo.model.principal.PrincipalAliasResponse;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.quiz.Quiz;
import org.sagebionetworks.repo.model.quiz.QuizResponse;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.TableSchemaChangeRequest;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.ViewType;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiOrderHint;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.schema.adapter.AdapterFactory;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.AdapterFactoryImpl;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.SerializationUtils;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.view.TeamRequestBundle;
import org.sagebionetworks.web.server.servlet.MarkdownCacheRequest;
import org.sagebionetworks.web.server.servlet.NotificationTokenType;
import org.sagebionetworks.web.server.servlet.ServiceUrlProvider;
import org.sagebionetworks.web.server.servlet.SynapseClientImpl;
import org.sagebionetworks.web.server.servlet.SynapseProvider;
import org.sagebionetworks.web.server.servlet.TokenProvider;
import org.sagebionetworks.web.shared.AccessRequirementUtils;
import org.sagebionetworks.web.shared.EntityBundlePlus;
import org.sagebionetworks.web.shared.OpenTeamInvitationBundle;
import org.sagebionetworks.web.shared.ProjectPagedResults;
import org.sagebionetworks.web.shared.TeamBundle;
import org.sagebionetworks.web.shared.TeamMemberBundle;
import org.sagebionetworks.web.shared.TeamMemberPagedResults;
import org.sagebionetworks.web.shared.WikiPageKey;
import org.sagebionetworks.web.shared.exceptions.BadRequestException;
import org.sagebionetworks.web.shared.exceptions.ConflictException;
import org.sagebionetworks.web.shared.exceptions.NotFoundException;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;
import org.sagebionetworks.web.shared.exceptions.UnauthorizedException;
import org.sagebionetworks.web.shared.exceptions.UnknownErrorException;
import org.sagebionetworks.web.shared.users.AclUtils;
import org.sagebionetworks.web.shared.users.PermissionLevel;

import com.google.appengine.repackaged.com.google.common.base.Objects;
import com.google.common.cache.Cache;

/**
 * Test for the SynapseClientImpl
 * 
 * @author John
 * 
 */
public class SynapseClientImplTest {
	private static final String BANNER_2 = "Another Banner";
	private static final String BANNER_1 = "Banner 1";
	public static final String TEST_HOME_PAGE_BASE = "http://mysynapse.org/";
	public static final String MY_USER_PROFILE_OWNER_ID = "MyOwnerID";
	
	SynapseProvider mockSynapseProvider;
	TokenProvider mockTokenProvider;
	ServiceUrlProvider mockUrlProvider;
	SynapseClient mockSynapse;
	SynapseClientImpl synapseClient;
	String entityId = "123";
	String inviteeUserId = "900";
	UserProfile inviteeUserProfile;
	ExampleEntity entity;
	Annotations annos;
	UserEntityPermissions eup;
	UserEvaluationPermissions userEvaluationPermissions;
	List<EntityHeader> batchHeaderResults;

	String testFileName = "testFileEntity.R";
	EntityPath path;
	org.sagebionetworks.reflection.model.PaginatedResults<UserGroup> pgugs;
	org.sagebionetworks.reflection.model.PaginatedResults<UserProfile> pgups;
	org.sagebionetworks.reflection.model.PaginatedResults<Team> pguts;
	Team teamA, teamZ;
	AccessControlList acl;
	WikiPage page;
	V2WikiPage v2Page;
	S3FileHandle handle;
	Evaluation mockEvaluation;
	UserSessionData mockUserSessionData;
	UserProfile mockUserProfile;
	MembershipInvtnSubmission testInvitation;
	PaginatedResults mockPaginatedMembershipRequest;
	Activity mockActivity;

	MessageToUser sentMessage;
	Long storageLocationId = 9090L;
	UserProfile testUserProfile;
	Long version = 1L;
	
	//Token testing
	NotificationSettingsSignedToken notificationSettingsToken;
	JoinTeamSignedToken joinTeamToken;
	String encodedJoinTeamToken, encodedNotificationSettingsToken;
	
	@Mock
	UserGroupHeaderResponsePage mockUserGroupHeaderResponsePage;
	@Mock
	UserGroupHeader mockUserGroupHeader;
	@Mock
	PrincipalAliasResponse mockPrincipalAliasResponse;
	@Mock
	ColumnModel mockOldColumnModel;
	@Mock
	ColumnModel mockNewColumnModel;
	@Mock
	ColumnModel mockNewColumnModelAfterCreate;
	
	@Mock
	BatchFileHandleCopyResult mockBatchCopyResults;
	@Mock
	FileHandleCopyResult mockFileHandleCopyResult;
	@Mock
	FileEntity mockFileEntity;
	@Mock
	FileHandleCopyRequest mockFileHandleCopyRequest;
	@Mock
	MessageToUser mockMessageToUser;
	@Mock
	JSONObjectAdapter mockJSONObjAd;
	@Mock
	ExternalFileHandle mockExternalFileHandle;
	List<FileHandleCopyResult> batchCopyResultsList;
	
	@Mock
	EntityQueryResults mockEntityQueryResults;
	List<EntityQueryResult> entityQueryResultsList;
	@Mock
	EntityQueryResult mockEntityQueryResult; 
	
	public static final String OLD_COLUMN_MODEL_ID = "4444";
	public static final String NEW_COLUMN_MODEL_ID = "837837";
	private static final String testUserId = "myUserId";

	private static final String EVAL_ID_1 = "eval ID 1";
	private static AdapterFactory adapterFactory = new AdapterFactoryImpl();
	private TeamMembershipStatus membershipStatus;
	
	@Mock
	ThreadLocal<HttpServletRequest> mockThreadLocal;
	
	@Mock 
	HttpServletRequest mockRequest;
	
	String userIp = "127.0.0.1";

	@Before
	public void before() throws SynapseException, JSONObjectAdapterException {
		MockitoAnnotations.initMocks(this);
		mockSynapse = Mockito.mock(SynapseClient.class);
		mockSynapseProvider = Mockito.mock(SynapseProvider.class);
		mockUrlProvider = Mockito.mock(ServiceUrlProvider.class);
		when(mockSynapseProvider.createNewClient()).thenReturn(mockSynapse);
		mockTokenProvider = Mockito.mock(TokenProvider.class);
		mockPaginatedMembershipRequest = Mockito.mock(PaginatedResults.class);
		mockActivity = Mockito.mock(Activity.class);
		when(mockPaginatedMembershipRequest.getTotalNumberOfResults()).thenReturn(3L);
		synapseClient = new SynapseClientImpl();
		synapseClient.setSynapseProvider(mockSynapseProvider);
		synapseClient.setTokenProvider(mockTokenProvider);
		synapseClient.setServiceUrlProvider(mockUrlProvider);

		// Setup the the entity
		entity = new ExampleEntity();
		entity.setId(entityId);
		entity.setEntityType(ExampleEntity.class.getName());
		entity.setModifiedBy(testUserId);
		// the mock synapse should return this object
		when(mockSynapse.getEntityById(entityId)).thenReturn(entity);
		// Setup the annotations
		annos = new Annotations();
		annos.setId(entityId);
		annos.addAnnotation("string", "a string value");
		// the mock synapse should return this object
		when(mockSynapse.getAnnotations(entityId)).thenReturn(annos);
		// Setup the Permissions
		eup = new UserEntityPermissions();
		eup.setCanDelete(true);
		eup.setCanView(false);
		eup.setOwnerPrincipalId(999L);
		// the mock synapse should return this object
		when(mockSynapse.getUsersEntityPermissions(entityId)).thenReturn(eup);

		// user can change permissions on eval 2, but not on 1
		userEvaluationPermissions = new UserEvaluationPermissions();
		userEvaluationPermissions.setCanChangePermissions(false);
		when(mockSynapse.getUserEvaluationPermissions(EVAL_ID_1)).thenReturn(
				userEvaluationPermissions);

		when(mockOldColumnModel.getId()).thenReturn(OLD_COLUMN_MODEL_ID);
		when(mockNewColumnModelAfterCreate.getId()).thenReturn(NEW_COLUMN_MODEL_ID);
		
		// Setup the path
		path = new EntityPath();
		path.setPath(new ArrayList<EntityHeader>());
		EntityHeader header = new EntityHeader();
		header.setId(entityId);
		header.setName("RomperRuuuu");
		path.getPath().add(header);
		// the mock synapse should return this object
		when(mockSynapse.getEntityPath(entityId)).thenReturn(path);

		pgugs = new org.sagebionetworks.reflection.model.PaginatedResults<UserGroup>();
		List<UserGroup> ugs = new ArrayList<UserGroup>();
		ugs.add(new UserGroup());
		pgugs.setResults(ugs);
		when(mockSynapse.getGroups(anyInt(), anyInt())).thenReturn(pgugs);

		pgups = new org.sagebionetworks.reflection.model.PaginatedResults<UserProfile>();
		List<UserProfile> ups = new ArrayList<UserProfile>();
		ups.add(new UserProfile());
		pgups.setResults(ups);
		when(mockSynapse.getUsers(anyInt(), anyInt())).thenReturn(pgups);

		pguts = new org.sagebionetworks.reflection.model.PaginatedResults<Team>();
		List<Team> uts = new ArrayList<Team>();
		teamZ = new Team();
		teamZ.setId("1");
		teamZ.setName("zygote");
		uts.add(teamZ);
		teamA = new Team();
		teamA.setId("2");
		teamA.setName("Amplitude");
		uts.add(teamA);
		pguts.setResults(uts);
		when(mockSynapse.getTeamsForUser(anyString(), anyInt(), anyInt()))
				.thenReturn(pguts);

		acl = new AccessControlList();
		acl.setId("sys999");
		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(101L);
		ra.setAccessType(AclUtils
				.getACCESS_TYPEs(PermissionLevel.CAN_ADMINISTER));
		acl.setResourceAccess(ras);
		when(mockSynapse.getACL(anyString())).thenReturn(acl);
		when(mockSynapse.createACL((AccessControlList) any())).thenReturn(acl);
		when(mockSynapse.updateACL((AccessControlList) any())).thenReturn(acl);
		when(mockSynapse.updateACL((AccessControlList) any(), eq(true)))
				.thenReturn(acl);
		when(mockSynapse.updateACL((AccessControlList) any(), eq(false)))
				.thenReturn(acl);
		when(mockSynapse.updateTeamACL(any(AccessControlList.class))).thenReturn(acl);
		when(mockSynapse.getTeamACL(anyString())).thenReturn(acl);

		EntityHeader bene = new EntityHeader();
		bene.setId("syn999");
		when(mockSynapse.getEntityBenefactor(anyString())).thenReturn(bene);

		org.sagebionetworks.reflection.model.PaginatedResults<EntityHeader> batchHeaders = new org.sagebionetworks.reflection.model.PaginatedResults<EntityHeader>();
		batchHeaderResults = new ArrayList<EntityHeader>();
		for (int i = 0; i < 10; i++) {
			EntityHeader h = new EntityHeader();
			h.setId("syn" + i);
			batchHeaderResults.add(h);
		}
		batchHeaders.setResults(batchHeaderResults);
		when(mockSynapse.getEntityHeaderBatch(anyList())).thenReturn(
				batchHeaders);

		List<AccessRequirement> accessRequirements = new ArrayList<AccessRequirement>();
		accessRequirements.add(createAccessRequirement(ACCESS_TYPE.DOWNLOAD));

		int mask = ENTITY | ANNOTATIONS | PERMISSIONS | ENTITY_PATH
				| HAS_CHILDREN | ACCESS_REQUIREMENTS
				| UNMET_ACCESS_REQUIREMENTS;
		int emptyMask = 0;
		EntityBundle bundle = new EntityBundle();
		bundle.setEntity(entity);
		bundle.setAnnotations(annos);
		bundle.setPermissions(eup);
		bundle.setPath(path);
		bundle.setHasChildren(false);
		bundle.setAccessRequirements(accessRequirements);
		bundle.setUnmetAccessRequirements(accessRequirements);
		bundle.setBenefactorAcl(acl);
		when(mockSynapse.getEntityBundle(anyString(), Matchers.eq(mask)))
				.thenReturn(bundle);
		when(mockSynapse.getEntityBundle(anyString(), Matchers.eq(ENTITY | ANNOTATIONS | ROOT_WIKI_ID | FILE_HANDLES | PERMISSIONS | BENEFACTOR_ACL)))
				.thenReturn(bundle);

		EntityBundle emptyBundle = new EntityBundle();
		when(mockSynapse.getEntityBundle(anyString(), Matchers.eq(emptyMask)))
				.thenReturn(emptyBundle);

		when(mockSynapse.canAccess("syn101", ACCESS_TYPE.READ))
				.thenReturn(true);

		page = new WikiPage();
		page.setId("testId");
		page.setMarkdown("my markdown");
		page.setParentWikiId(null);
		page.setTitle("A Title");
		v2Page = new V2WikiPage();
		v2Page.setId("v2TestId");
		v2Page.setEtag("122333");
		handle = new S3FileHandle();
		handle.setId("4422");
		handle.setBucketName("bucket");
		handle.setFileName(testFileName);
		handle.setKey("key");
		when(mockSynapse.getRawFileHandle(anyString())).thenReturn(handle);
		org.sagebionetworks.reflection.model.PaginatedResults<AccessRequirement> ars = new org.sagebionetworks.reflection.model.PaginatedResults<AccessRequirement>();
		ars.setTotalNumberOfResults(0);
		ars.setResults(new ArrayList<AccessRequirement>());
		when(
				mockSynapse
						.getAccessRequirements(any(RestrictableObjectDescriptor.class), anyLong(), anyLong()))
				.thenReturn(ars);
		when(
				mockSynapse.getUnmetAccessRequirements(
						any(RestrictableObjectDescriptor.class),
						any(ACCESS_TYPE.class), anyLong(), anyLong())).thenReturn(ars);

		mockEvaluation = Mockito.mock(Evaluation.class);
		when(mockEvaluation.getStatus()).thenReturn(EvaluationStatus.OPEN);
		when(mockSynapse.getEvaluation(anyString())).thenReturn(mockEvaluation);
		mockUserSessionData = Mockito.mock(UserSessionData.class);
		mockUserProfile = Mockito.mock(UserProfile.class);
		when(mockSynapse.getUserSessionData()).thenReturn(mockUserSessionData);
		when(mockUserSessionData.getProfile()).thenReturn(mockUserProfile);
		when(mockUserProfile.getOwnerId()).thenReturn(MY_USER_PROFILE_OWNER_ID);
		when(mockSynapse.getMyProfile()).thenReturn(mockUserProfile);
		UploadDaemonStatus status = new UploadDaemonStatus();
		String fileHandleId = "myFileHandleId";
		status.setFileHandleId(fileHandleId);
		status.setState(State.COMPLETED);

		status = new UploadDaemonStatus();
		status.setState(State.PROCESSING);
		status.setPercentComplete(.05d);

		PaginatedResults<MembershipInvitation> openInvites = new PaginatedResults<MembershipInvitation>();
		openInvites.setTotalNumberOfResults(0);
		when(
				mockSynapse.getOpenMembershipInvitations(anyString(),
						anyString(), anyLong(), anyLong())).thenReturn(
				openInvites);

		PaginatedResults<MembershipRequest> openRequests = new PaginatedResults<MembershipRequest>();
		openRequests.setTotalNumberOfResults(0);
		when(
				mockSynapse.getOpenMembershipRequests(anyString(), anyString(),
						anyLong(), anyLong())).thenReturn(openRequests);
		membershipStatus = new TeamMembershipStatus();
		membershipStatus.setCanJoin(false);
		membershipStatus.setHasOpenInvitation(false);
		membershipStatus.setHasOpenRequest(false);
		membershipStatus.setHasUnmetAccessRequirement(false);
		membershipStatus.setIsMember(false);
		membershipStatus.setMembershipApprovalRequired(false);
		when(mockSynapse.getTeamMembershipStatus(anyString(), anyString()))
				.thenReturn(membershipStatus);

		sentMessage = new MessageToUser();
		sentMessage.setId("987");
		when(mockSynapse.sendMessage(any(MessageToUser.class))).thenReturn(sentMessage);
		when(mockSynapse.sendMessage(any(MessageToUser.class), anyString())).thenReturn(sentMessage);

		// getMyProjects getUserProjects
		PaginatedResults headers = new PaginatedResults<ProjectHeader>();
		headers.setTotalNumberOfResults(1100);
		List<ProjectHeader> projectHeaders = new ArrayList();
		List<UserProfile> userProfile = new ArrayList();
		projectHeaders.add(new ProjectHeader());
		headers.setResults(projectHeaders);
		when(
				mockSynapse.getMyProjects(any(ProjectListType.class),
						any(ProjectListSortColumn.class),
						any(SortDirection.class), anyInt(), anyInt()))
				.thenReturn(headers);
		when(
				mockSynapse.getProjectsFromUser(anyLong(),
						any(ProjectListSortColumn.class),
						any(SortDirection.class), anyInt(), anyInt()))
				.thenReturn(headers);
		when(
				mockSynapse.getProjectsForTeam(anyLong(),
						any(ProjectListSortColumn.class),
						any(SortDirection.class), anyInt(), anyInt()))
				.thenReturn(headers);
		
		testUserProfile = new UserProfile();
		testUserProfile.setUserName("Test User");
		when(mockSynapse.getUserProfile(eq(testUserId))).thenReturn(
				testUserProfile);
		
		joinTeamToken = new JoinTeamSignedToken();
		joinTeamToken.setHmac("98765");
		joinTeamToken.setMemberId("1");
		joinTeamToken.setTeamId("2");
		joinTeamToken.setUserId("3");
		encodedJoinTeamToken = SerializationUtils.serializeAndHexEncode(joinTeamToken);
		
		notificationSettingsToken = new NotificationSettingsSignedToken();
		notificationSettingsToken.setHmac("987654");
		notificationSettingsToken.setSettings(new Settings());
		notificationSettingsToken.setUserId("4");
		encodedNotificationSettingsToken = SerializationUtils.serializeAndHexEncode(notificationSettingsToken);
		
		when(mockSynapse.copyFileHandles(any(BatchFileHandleCopyRequest.class))).thenReturn(mockBatchCopyResults);
		batchCopyResultsList = new ArrayList<FileHandleCopyResult>();
		when(mockBatchCopyResults.getCopyResults()).thenReturn(batchCopyResultsList);
		
		Whitebox.setInternalState(synapseClient, "perThreadRequest", mockThreadLocal);
		userIp = "127.0.0.1";
		when(mockThreadLocal.get()).thenReturn(mockRequest);
		when(mockRequest.getRemoteAddr()).thenReturn(userIp);
		entityQueryResultsList = new ArrayList<EntityQueryResult>();
		when(mockEntityQueryResults.getEntities()).thenReturn(entityQueryResultsList);
		when(mockSynapse.entityQuery(any(EntityQuery.class))).thenReturn(mockEntityQueryResults);
	}

	private AccessRequirement createAccessRequirement(ACCESS_TYPE type) {
		TermsOfUseAccessRequirement accessRequirement = new TermsOfUseAccessRequirement();
		accessRequirement.setConcreteType(TermsOfUseAccessRequirement.class
				.getName());
		RestrictableObjectDescriptor descriptor = new RestrictableObjectDescriptor();
		descriptor.setId("101");
		descriptor.setType(RestrictableObjectType.ENTITY);
		accessRequirement.setSubjectIds(Arrays
				.asList(new RestrictableObjectDescriptor[] { descriptor }));
		accessRequirement.setAccessType(type);
		return accessRequirement;
	}

	private void setupTeamInvitations() throws SynapseException {
		ArrayList<MembershipInvtnSubmission> testInvitations = new ArrayList<MembershipInvtnSubmission>();
		testInvitation = new MembershipInvtnSubmission();
		testInvitation.setId("628319");
		testInvitation.setInviteeId(inviteeUserId);
		testInvitations.add(testInvitation);
		PaginatedResults<MembershipInvtnSubmission> paginatedInvitations = new PaginatedResults<MembershipInvtnSubmission>();
		paginatedInvitations.setResults(testInvitations);
		when(
				mockSynapse.getOpenMembershipInvitationSubmissions(anyString(),
						anyString(), anyLong(), anyLong())).thenReturn(
				paginatedInvitations);

		inviteeUserProfile = new UserProfile();
		inviteeUserProfile.setUserName("Invitee User");
		inviteeUserProfile.setOwnerId(inviteeUserId);
		when(mockSynapse.getUserProfile(eq(inviteeUserId))).thenReturn(
				inviteeUserProfile);

	}

	@Test
	public void testGetEntityBundleAll() throws RestServiceException {
		// Make sure we can get all parts of the bundel
		int mask = ENTITY | ANNOTATIONS | PERMISSIONS | ENTITY_PATH
				| HAS_CHILDREN | ACCESS_REQUIREMENTS
				| UNMET_ACCESS_REQUIREMENTS;
		EntityBundle bundle = synapseClient.getEntityBundle(entityId, mask);
		assertNotNull(bundle);
		// We should have all of the strings
		assertNotNull(bundle.getEntity());
		assertNotNull(bundle.getAnnotations());
		assertNotNull(bundle.getPath());
		assertNotNull(bundle.getPermissions());
		assertNotNull(bundle.getHasChildren());
		assertNotNull(bundle.getAccessRequirements());
		assertNotNull(bundle.getUnmetAccessRequirements());
	}

	@Test
	public void testGetEntityBundleNone() throws RestServiceException {
		// Make sure all are null
		int mask = 0x0;
		EntityBundle bundle = synapseClient.getEntityBundle(entityId, mask);
		assertNotNull(bundle);
		// We should have all of the strings
		assertNull(bundle.getEntity());
		assertNull(bundle.getAnnotations());
		assertNull(bundle.getPath());
		assertNull(bundle.getPermissions());
		assertNull(bundle.getHasChildren());
		assertNull(bundle.getAccessRequirements());
		assertNull(bundle.getUnmetAccessRequirements());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParseEntityFromJsonNoType()
			throws JSONObjectAdapterException {
		ExampleEntity example = new ExampleEntity();
		example.setName("some name");
		example.setDescription("some description");
		// do not set the type
		String json = EntityFactory.createJSONStringForEntity(example);
		// This will fail as the type is required
		synapseClient.parseEntityFromJson(json);
	}

	@Test
	public void testParseEntityFromJson() throws JSONObjectAdapterException {
		ExampleEntity example = new ExampleEntity();
		example.setName("some name");
		example.setDescription("some description");
		example.setEntityType(ExampleEntity.class.getName());
		String json = EntityFactory.createJSONStringForEntity(example);
		// System.out.println(json);
		// Now make sure this can be read back
		ExampleEntity clone = (ExampleEntity) synapseClient
				.parseEntityFromJson(json);
		assertEquals(example, clone);
	}

	@Test
	public void testCreateOrUpdateEntityFalse()
			throws JSONObjectAdapterException, RestServiceException,
			SynapseException {
		ExampleEntity in = new ExampleEntity();
		in.setName("some name");
		in.setDescription("some description");
		in.setEntityType(ExampleEntity.class.getName());

		ExampleEntity out = new ExampleEntity();
		out.setName("some name");
		out.setDescription("some description");
		out.setEntityType(ExampleEntity.class.getName());
		out.setId("syn123");
		out.setEtag("45");

		// when in comes in then return out.
		when(mockSynapse.putEntity(in)).thenReturn(out);
		String result = synapseClient.createOrUpdateEntity(in, null, false);
		assertEquals(out.getId(), result);
		verify(mockSynapse).putEntity(in);
	}

	@Test
	public void testCreateOrUpdateEntityTrue()
			throws JSONObjectAdapterException, RestServiceException,
			SynapseException {
		ExampleEntity in = new ExampleEntity();
		in.setName("some name");
		in.setDescription("some description");
		in.setEntityType(ExampleEntity.class.getName());

		ExampleEntity out = new ExampleEntity();
		out.setName("some name");
		out.setDescription("some description");
		out.setEntityType(ExampleEntity.class.getName());
		out.setId("syn123");
		out.setEtag("45");

		// when in comes in then return out.
		when(mockSynapse.createEntity(in)).thenReturn(out);
		String result = synapseClient.createOrUpdateEntity(in, null, true);
		assertEquals(out.getId(), result);
		verify(mockSynapse).createEntity(in);
	}

	@Test
	public void testCreateOrUpdateEntityTrueWithAnnos()
			throws JSONObjectAdapterException, RestServiceException,
			SynapseException {
		ExampleEntity in = new ExampleEntity();
		in.setName("some name");
		in.setDescription("some description");
		in.setEntityType(ExampleEntity.class.getName());

		Annotations annos = new Annotations();
		annos.addAnnotation("someString", "one");

		ExampleEntity out = new ExampleEntity();
		out.setName("some name");
		out.setDescription("some description");
		out.setEntityType(ExampleEntity.class.getName());
		out.setId("syn123");
		out.setEtag("45");

		// when in comes in then return out.
		when(mockSynapse.createEntity(in)).thenReturn(out);
		String result = synapseClient.createOrUpdateEntity(in, annos, true);
		assertEquals(out.getId(), result);
		verify(mockSynapse).createEntity(in);
		annos.setEtag(out.getEtag());
		annos.setId(out.getId());
		verify(mockSynapse).updateAnnotations(out.getId(), annos);
	}

	

	@Test
	public void testMoveEntity()
			throws JSONObjectAdapterException, RestServiceException,
			SynapseException {
		String entityId = "syn123";
		String oldParentId = "syn1", newParentId = "syn2";
		ExampleEntity in = new ExampleEntity();
		in.setName("some name");
		in.setParentId(oldParentId);
		in.setId(entityId);
		in.setEntityType(ExampleEntity.class.getName());

		ExampleEntity out = new ExampleEntity();
		out.setName("some name");
		out.setEntityType(ExampleEntity.class.getName());
		out.setId(entityId);
		out.setParentId(newParentId);
		out.setEtag("45");

		// when in comes in then return out.
		when(mockSynapse.putEntity(in)).thenReturn(out);
		when(mockSynapse.getEntityById(entityId)).thenReturn(in);
		Entity result = synapseClient.moveEntity(entityId, newParentId);
		assertEquals(newParentId, result.getParentId());
		verify(mockSynapse).getEntityById(entityId);
		verify(mockSynapse).putEntity(any(Entity.class));
	}
	@Test
	public void testGetEntityBenefactorAcl() throws Exception {
		EntityBundle bundle = new EntityBundle();
		bundle.setBenefactorAcl(acl);
		when(mockSynapse.getEntityBundle("syn101", EntityBundle.BENEFACTOR_ACL))
				.thenReturn(bundle);
		AccessControlList clone = synapseClient
				.getEntityBenefactorAcl("syn101");
		assertEquals(acl, clone);
	}

	@Test
	public void testCreateAcl() throws Exception {
		AccessControlList clone = synapseClient.createAcl(acl);
		assertEquals(acl, clone);
	}

	@Test
	public void testUpdateAcl() throws Exception {
		AccessControlList clone = synapseClient.updateAcl(acl);
		assertEquals(acl, clone);
	}

	@Test
	public void testUpdateAclRecursive() throws Exception {
		AccessControlList clone = synapseClient.updateAcl(acl, true);
		assertEquals(acl, clone);
		verify(mockSynapse).updateACL(any(AccessControlList.class), eq(true));
	}

	@Test
	public void testDeleteAcl() throws Exception {
		EntityBundle bundle = new EntityBundle();
		bundle.setBenefactorAcl(acl);
		when(mockSynapse.getEntityBundle("syn101", EntityBundle.BENEFACTOR_ACL))
				.thenReturn(bundle);
		AccessControlList clone = synapseClient.deleteAcl("syn101");
		assertEquals(acl, clone);
	}

	@Test
	public void testHasAccess() throws Exception {
		assertTrue(synapseClient.hasAccess("syn101", "READ"));
	}


	@Test
	public void testGetUserProfile() throws Exception {
		// verify call is directly calling the synapse client provider
		String testRepoUrl = "http://mytestrepourl";
		when(mockUrlProvider.getRepositoryServiceUrl()).thenReturn(testRepoUrl);
		UserProfile userProfile = synapseClient.getUserProfile(testUserId);
		assertEquals(userProfile, testUserProfile);
	}

	@Test
	public void testGetProjectById() throws Exception {
		String projectId = "syn1029";
		Project project = new Project();
		project.setId(projectId);
		when(mockSynapse.getEntityById(projectId)).thenReturn(project);

		Project actualProject = synapseClient.getProject(projectId);
		assertEquals(project, actualProject);
	}

	@Test
	public void testGetJSONEntity() throws Exception {

		JSONObject json = EntityFactory.createJSONObjectForEntity(entity);
		Mockito.when(mockSynapse.getEntity(anyString())).thenReturn(json);

		String testRepoUri = "/testservice";

		synapseClient.getJSONEntity(testRepoUri);
		// verify that this call uses Synapse.getEntity(testRepoUri)
		verify(mockSynapse).getEntity(testRepoUri);
	}

	@Test
	public void testGetWikiAttachmentHandles() throws Exception {
		FileHandleResults testResults = new FileHandleResults();
		Mockito.when(
				mockSynapse
						.getWikiAttachmenthHandles(any(org.sagebionetworks.repo.model.dao.WikiPageKey.class)))
				.thenReturn(testResults);
		synapseClient.getWikiAttachmentHandles(new WikiPageKey("syn123",
				ObjectType.ENTITY.toString(), "20"));
		verify(mockSynapse).getWikiAttachmenthHandles(
				any(org.sagebionetworks.repo.model.dao.WikiPageKey.class));
	}

	@Test
	public void testDeleteV2WikiPage() throws Exception {
		synapseClient.deleteV2WikiPage(new WikiPageKey("syn123",
				ObjectType.ENTITY.toString(), "20"));
		verify(mockSynapse).deleteV2WikiPage(
				any(org.sagebionetworks.repo.model.dao.WikiPageKey.class));
	}

	@Test
	public void testGetV2WikiPage() throws Exception {
		Mockito.when(
				mockSynapse
						.getV2WikiPage(any(org.sagebionetworks.repo.model.dao.WikiPageKey.class)))
				.thenReturn(v2Page);
		synapseClient.getV2WikiPage(new WikiPageKey("syn123", ObjectType.ENTITY
				.toString(), "20"));
		verify(mockSynapse).getV2WikiPage(
				any(org.sagebionetworks.repo.model.dao.WikiPageKey.class));

		Mockito.when(
				mockSynapse
						.getVersionOfV2WikiPage(
								any(org.sagebionetworks.repo.model.dao.WikiPageKey.class),
								any(Long.class))).thenReturn(v2Page);
		synapseClient.getVersionOfV2WikiPage(new WikiPageKey("syn123",
				ObjectType.ENTITY.toString(), "20"), new Long(0));
		verify(mockSynapse).getVersionOfV2WikiPage(
				any(org.sagebionetworks.repo.model.dao.WikiPageKey.class),
				any(Long.class));
	}

	@Test
	public void testUpdateV2WikiPage() throws Exception {
		Mockito.when(
				mockSynapse.updateV2WikiPage(anyString(),
						any(ObjectType.class), any(V2WikiPage.class)))
				.thenReturn(v2Page);
		synapseClient.updateV2WikiPage("testId", ObjectType.ENTITY.toString(),
				v2Page);
		verify(mockSynapse).updateV2WikiPage(anyString(),
				any(ObjectType.class), any(V2WikiPage.class));
	}

	@Test
	public void testRestoreV2WikiPage() throws Exception {
		String wikiId = "syn123";
		Mockito.when(
				mockSynapse.restoreV2WikiPage(anyString(),
						any(ObjectType.class), any(String.class), anyLong()))
				.thenReturn(v2Page);
		synapseClient.restoreV2WikiPage("ownerId",
				ObjectType.ENTITY.toString(), wikiId, new Long(2));
		verify(mockSynapse).restoreV2WikiPage(anyString(),
				any(ObjectType.class), any(String.class), anyLong());
	}
	@Test
	public void testGetV2WikiHeaderTree() throws Exception {
		PaginatedResults<V2WikiHeader> headerTreeResults = new PaginatedResults<V2WikiHeader>();
		headerTreeResults.setResults(new ArrayList<V2WikiHeader>());
		when(
				mockSynapse.getV2WikiHeaderTree(anyString(),
						any(ObjectType.class),
						anyLong(), anyLong())).thenReturn(headerTreeResults);
		synapseClient.getV2WikiHeaderTree("testId",
				ObjectType.ENTITY.toString());
		verify(mockSynapse).getV2WikiHeaderTree(anyString(),
				any(ObjectType.class),anyLong(), anyLong());
	}

	@Test
	public void testGetV2WikiHeaderTreeTwoPage() throws Exception {
		PaginatedResults<V2WikiHeader> headerTreePage1 = Mockito.mock(PaginatedResults.class);
		PaginatedResults<V2WikiHeader> headerTreePage2 = Mockito.mock(PaginatedResults.class);
		when(mockSynapse.getV2WikiHeaderTree(anyString(), any(ObjectType.class), anyLong(), anyLong()))
				.thenReturn(headerTreePage1, headerTreePage2);
		List<V2WikiHeader> page1Results = new ArrayList<V2WikiHeader>();
		for (int i = 0; i < SynapseClientImpl.LIMIT_50; i++) {
			page1Results.add(Mockito.mock(V2WikiHeader.class));
		}
		when(headerTreePage1.getResults()).thenReturn(page1Results);
		//second page has a single page
		V2WikiHeader singleHeader = Mockito.mock(V2WikiHeader.class);
		when(headerTreePage2.getResults()).thenReturn(Collections.singletonList(singleHeader));
		List<V2WikiHeader> results = synapseClient.getV2WikiHeaderTree("testId", ObjectType.ENTITY.toString());
		//1 full page of results, and 1 result on second page
		assertEquals(SynapseClientImpl.LIMIT_50 + 1, results.size());
		verify(mockSynapse).getV2WikiHeaderTree(anyString(), any(ObjectType.class), eq(SynapseClientImpl.LIMIT_50), eq(SynapseClientImpl.ZERO_OFFSET.longValue()));
		verify(mockSynapse).getV2WikiHeaderTree(anyString(), any(ObjectType.class), eq(SynapseClientImpl.LIMIT_50), eq(SynapseClientImpl.LIMIT_50));
	}
	@Test
	public void testGetV2WikiOrderHint() throws Exception {
		V2WikiOrderHint orderHint = new V2WikiOrderHint();
		when(
				mockSynapse
						.getV2OrderHint(any(org.sagebionetworks.repo.model.dao.WikiPageKey.class)))
				.thenReturn(orderHint);
		synapseClient.getV2WikiOrderHint(new WikiPageKey("syn123",
				ObjectType.ENTITY.toString(), "20"));
		verify(mockSynapse).getV2OrderHint(
				any(org.sagebionetworks.repo.model.dao.WikiPageKey.class));
	}

	@Test
	public void testUpdateV2WikiOrderHint() throws Exception {
		V2WikiOrderHint orderHint = new V2WikiOrderHint();
		when(mockSynapse.updateV2WikiOrderHint(any(V2WikiOrderHint.class)))
				.thenReturn(orderHint);
		synapseClient.updateV2WikiOrderHint(orderHint);
		verify(mockSynapse).updateV2WikiOrderHint(any(V2WikiOrderHint.class));
	}

	@Test
	public void testGetV2WikiHistory() throws Exception {
		PaginatedResults<V2WikiHistorySnapshot> historyResults = new PaginatedResults<V2WikiHistorySnapshot>();
		when(
				mockSynapse
						.getV2WikiHistory(
								any(org.sagebionetworks.repo.model.dao.WikiPageKey.class),
								any(Long.class), any(Long.class))).thenReturn(
				historyResults);
		synapseClient.getV2WikiHistory(new WikiPageKey("syn123",
				ObjectType.ENTITY.toString(), "20"), new Long(10), new Long(0));
		verify(mockSynapse).getV2WikiHistory(
				any(org.sagebionetworks.repo.model.dao.WikiPageKey.class),
				any(Long.class), any(Long.class));
	}

	@Test
	public void testGetV2WikiAttachmentHandles() throws Exception {
		FileHandleResults testResults = new FileHandleResults();
		Mockito.when(
				mockSynapse
						.getV2WikiAttachmentHandles(any(org.sagebionetworks.repo.model.dao.WikiPageKey.class)))
				.thenReturn(testResults);
		synapseClient.getV2WikiAttachmentHandles(new WikiPageKey("syn123",
				ObjectType.ENTITY.toString(), "20"));
		verify(mockSynapse).getV2WikiAttachmentHandles(
				any(org.sagebionetworks.repo.model.dao.WikiPageKey.class));

		Mockito.when(
				mockSynapse
						.getVersionOfV2WikiAttachmentHandles(
								any(org.sagebionetworks.repo.model.dao.WikiPageKey.class),
								any(Long.class))).thenReturn(testResults);
		synapseClient.getVersionOfV2WikiAttachmentHandles(new WikiPageKey(
				"syn123", ObjectType.ENTITY.toString(), "20"), new Long(0));
		verify(mockSynapse).getVersionOfV2WikiAttachmentHandles(
				any(org.sagebionetworks.repo.model.dao.WikiPageKey.class),
				any(Long.class));
	}

	@Test
	public void testGetMarkdown() throws IOException, RestServiceException,
			SynapseException {
		String someMarkDown = "someMarkDown";
		Mockito.when(
				mockSynapse
						.downloadV2WikiMarkdown(any(org.sagebionetworks.repo.model.dao.WikiPageKey.class)))
				.thenReturn(someMarkDown);
		synapseClient.getMarkdown(new WikiPageKey("syn123", ObjectType.ENTITY
				.toString(), "20"));
		verify(mockSynapse).downloadV2WikiMarkdown(
				any(org.sagebionetworks.repo.model.dao.WikiPageKey.class));

		Mockito.when(
				mockSynapse
						.downloadVersionOfV2WikiMarkdown(
								any(org.sagebionetworks.repo.model.dao.WikiPageKey.class),
								any(Long.class))).thenReturn(someMarkDown);
		synapseClient.getVersionOfMarkdown(new WikiPageKey("syn123",
				ObjectType.ENTITY.toString(), "20"), new Long(0));
		verify(mockSynapse).downloadVersionOfV2WikiMarkdown(
				any(org.sagebionetworks.repo.model.dao.WikiPageKey.class),
				any(Long.class));
	}

	@Test
	public void testCreateV2WikiPageWithV1() throws Exception {
		Mockito.when(
				mockSynapse.createWikiPage(anyString(), any(ObjectType.class),
						any(WikiPage.class))).thenReturn(page);
		synapseClient.createV2WikiPageWithV1("testId",
				ObjectType.ENTITY.toString(), page);
		verify(mockSynapse).createWikiPage(anyString(), any(ObjectType.class),
				any(WikiPage.class));
	}

	@Test
	public void testUpdateV2WikiPageWithV1() throws Exception {
		Mockito.when(
				mockSynapse.updateWikiPage(anyString(), any(ObjectType.class),
						any(WikiPage.class))).thenReturn(page);
		synapseClient.updateV2WikiPageWithV1("testId",
				ObjectType.ENTITY.toString(), page);
		verify(mockSynapse).updateWikiPage(anyString(), any(ObjectType.class),
				any(WikiPage.class));
	}

	@Test
	public void getV2WikiPageAsV1() throws Exception {
		Mockito.when(
				mockSynapse
						.getWikiPage(any(org.sagebionetworks.repo.model.dao.WikiPageKey.class)))
				.thenReturn(page);
		Mockito.when(
				mockSynapse
						.getV2WikiPage(any(org.sagebionetworks.repo.model.dao.WikiPageKey.class)))
				.thenReturn(v2Page);
		synapseClient.getV2WikiPageAsV1(new WikiPageKey("syn123",
				ObjectType.ENTITY.toString(), "20"));
		verify(mockSynapse).getWikiPage(
				any(org.sagebionetworks.repo.model.dao.WikiPageKey.class));
		// asking for the same page twice should result in a cache hit, and it
		// should not ask for it from the synapse client
		synapseClient.getV2WikiPageAsV1(new WikiPageKey("syn123",
				ObjectType.ENTITY.toString(), "20"));
		verify(mockSynapse, Mockito.times(1)).getWikiPage(
				any(org.sagebionetworks.repo.model.dao.WikiPageKey.class));

		Mockito.when(
				mockSynapse
						.getWikiPageForVersion(
								any(org.sagebionetworks.repo.model.dao.WikiPageKey.class),
								any(Long.class))).thenReturn(page);
		Mockito.when(
				mockSynapse
						.getVersionOfV2WikiPage(
								any(org.sagebionetworks.repo.model.dao.WikiPageKey.class),
								anyLong())).thenReturn(v2Page);
		synapseClient.getVersionOfV2WikiPageAsV1(new WikiPageKey("syn123",
				ObjectType.ENTITY.toString(), "20"), new Long(0));
		verify(mockSynapse).getWikiPageForVersion(
				any(org.sagebionetworks.repo.model.dao.WikiPageKey.class),
				any(Long.class));
		// asking for the same page twice should result in a cache hit, and it
		// should not ask for it from the synapse client
		synapseClient.getVersionOfV2WikiPageAsV1(new WikiPageKey("syn123",
				ObjectType.ENTITY.toString(), "20"), new Long(0));
		verify(mockSynapse, Mockito.times(1)).getWikiPageForVersion(
				any(org.sagebionetworks.repo.model.dao.WikiPageKey.class),
				any(Long.class));
	}
	
	
	@Test
	public void testHtmlTeamMembersCache() throws Exception {
		PaginatedResults<TeamMember> teamMembersPage1 = new PaginatedResults<TeamMember>();
		String trustedUserId = "876543";
		UserGroupHeader mockHeader = mock(UserGroupHeader.class);
		when(mockHeader.getOwnerId()).thenReturn(trustedUserId);
		TeamMember mockTeamMember = mock(TeamMember.class);
		when(mockTeamMember.getMember()).thenReturn(mockHeader);
		teamMembersPage1.setResults(Collections.singletonList(mockTeamMember));
		PaginatedResults<TeamMember> teamMembersPage2 = new PaginatedResults<TeamMember>();
		teamMembersPage2.setResults(new ArrayList());
		when(mockSynapse.getTeamMembers(anyString(), anyString(), anyInt(), anyInt()))
				.thenReturn(teamMembersPage1, teamMembersPage2);
		
		assertFalse(synapseClient.isUserAllowedToRenderHTML("untrustedId"));
		
		//get both pages
		verify(mockSynapse, times(2)).getTeamMembers(anyString(), anyString(), anyInt(), anyInt());
		
		// the cache should be ready, it should not ask for the team members from the synapse client again for an hour
		assertTrue(synapseClient.isUserAllowedToRenderHTML(trustedUserId));
		
		//only the original 2 calls
		verify(mockSynapse, times(2)).getTeamMembers(anyString(), anyString(), anyInt(), anyInt());
	}

	private void resetUpdateExternalFileHandleMocks(String testId,
			FileEntity file, ExternalFileHandle handle)
			throws SynapseException, JSONObjectAdapterException {
		reset(mockSynapse);
		when(mockSynapse.getEntityById(testId)).thenReturn(file);
		when(
				mockSynapse
						.createExternalFileHandle(any(ExternalFileHandle.class)))
				.thenReturn(handle);
		when(mockSynapse.putEntity(any(FileEntity.class))).thenReturn(file);
	}

	@Test
	public void testUpdateExternalFileHandle() throws Exception {
		// verify call is directly calling the synapse client provider, and it
		// tries to rename the entity to the filename
		String myFileName = "testFileName.csv";
		String testUrl = "  http://mytesturl/" + myFileName;
		String testId = "myTestId";
		String md5 = "e10e3f4491440ce7b48edc97f03307bb";
		Long fileSize=2048L;
		FileEntity file = new FileEntity();
		String originalFileEntityName = "syn1223";
		String contentType = "text/plain";
		file.setName(originalFileEntityName);
		file.setId(testId);
		file.setDataFileHandleId("handle1");
		ExternalFileHandle handle = new ExternalFileHandle();
		handle.setExternalURL(testUrl);

		resetUpdateExternalFileHandleMocks(testId, file, handle);
		synapseClient.updateExternalFile(testId, testUrl, myFileName, contentType, fileSize, md5, storageLocationId);

		verify(mockSynapse).getEntityById(testId);
		
		ArgumentCaptor<ExternalFileHandle> captor = ArgumentCaptor.forClass(ExternalFileHandle.class);
		verify(mockSynapse).createExternalFileHandle(captor.capture());
		ExternalFileHandle capturedValue = captor.getValue();
		assertEquals(testUrl.trim(), capturedValue.getExternalURL());
		assertEquals(md5, capturedValue.getContentMd5());
		assertEquals(contentType, capturedValue.getContentType());
		assertEquals(myFileName, capturedValue.getFileName());
//		assertEquals(fileSize, capturedValue.getContentSize());
		
		verify(mockSynapse).putEntity(any(FileEntity.class));

		// and if rename fails, verify all is well (but the FileEntity name is
		// not updated)
		resetUpdateExternalFileHandleMocks(testId, file, handle);
		file.setName(originalFileEntityName);
		// first call should return file, second call to putEntity should throw
		// an exception
		when(mockSynapse.putEntity(any(FileEntity.class))).thenReturn(file)
				.thenThrow(
						new IllegalArgumentException(
								"invalid name for some reason"));
		synapseClient.updateExternalFile(testId, testUrl, myFileName, contentType, fileSize, md5, storageLocationId);

		// called createExternalFileHandle
		verify(mockSynapse).createExternalFileHandle(
				any(ExternalFileHandle.class));
		// and it should have called putEntity again
		verify(mockSynapse).putEntity(any(FileEntity.class));
	}

	@Test
	public void testCreateExternalFile() throws Exception {
		// test setting file handle name
		String parentEntityId = "syn123333";
		String externalUrl = "  sftp://foobar.edu/b/test.txt";
		String fileName = "testing.txt";
		String md5 = "e10e3f4491440ce7b48edc97f03307bb";
		String contentType = "text/plain";
		Long fileSize = 1024L;
		when(
				mockSynapse
						.createExternalFileHandle(any(ExternalFileHandle.class)))
				.thenReturn(new ExternalFileHandle());
		when(mockSynapse.createEntity(any(FileEntity.class))).thenReturn(
				new FileEntity());
		synapseClient.createExternalFile(parentEntityId, externalUrl, fileName, contentType, fileSize, md5, storageLocationId);
		ArgumentCaptor<ExternalFileHandle> captor = ArgumentCaptor
				.forClass(ExternalFileHandle.class);
		verify(mockSynapse).createExternalFileHandle(captor.capture());
		ExternalFileHandle handle = captor.getValue();
		// verify name is set
		assertEquals(fileName, handle.getFileName());
		assertEquals(externalUrl.trim(), handle.getExternalURL());
		assertEquals(storageLocationId, handle.getStorageLocationId());
		assertEquals(md5, handle.getContentMd5());
//		assertEquals(fileSize, handle.getContentSize());
	}
	
	@Test
	public void testCreateExternalFileAutoname() throws Exception {
		// test setting file handle name
		String parentEntityId = "syn123333";
		String externalUrl = "sftp://foobar.edu/b/test.txt";
		String expectedAutoFilename = "test.txt";
		String fileName = null;
		String md5 = "e10e3f4491440ce7b48edc97f03307bb";
		String contentType = "text/plain";
		Long fileSize = 1024L;
		when(mockExternalFileHandle.getFileName()).thenReturn(expectedAutoFilename);
		when(
			mockSynapse.createExternalFileHandle(any(ExternalFileHandle.class)))
				.thenReturn(mockExternalFileHandle);
		when(mockSynapse.createEntity(any(FileEntity.class))).thenReturn(
				new FileEntity());
		synapseClient.createExternalFile(parentEntityId, externalUrl, fileName, contentType, fileSize, md5, storageLocationId);
		ArgumentCaptor<ExternalFileHandle> captor = ArgumentCaptor
				.forClass(ExternalFileHandle.class);
		verify(mockSynapse).createExternalFileHandle(captor.capture());
		ExternalFileHandle handle = captor.getValue();
		// verify name is set
		assertEquals(expectedAutoFilename, handle.getFileName());
		assertEquals(externalUrl, handle.getExternalURL());
		assertEquals(storageLocationId, handle.getStorageLocationId());
		assertEquals(contentType, handle.getContentType());
		assertEquals(md5, handle.getContentMd5());
		
		//also check the entity name
		ArgumentCaptor<Entity> entityCaptor = ArgumentCaptor.forClass(Entity.class);
		verify(mockSynapse).createEntity(entityCaptor.capture());
		assertEquals(expectedAutoFilename, entityCaptor.getValue().getName());
	}

	@Test
	public void testGetEntityDoi() throws Exception {
		// wiring test
		Doi testDoi = new Doi();
		testDoi.setDoiStatus(DoiStatus.CREATED);
		testDoi.setId("test doi id");
		testDoi.setCreatedBy("Test User");
		testDoi.setCreatedOn(new Date());
		testDoi.setObjectId("syn1234");
		Mockito.when(mockSynapse.getEntityDoi(anyString(), anyLong()))
				.thenReturn(testDoi);
		synapseClient.getEntityDoi("test entity id", null);
		verify(mockSynapse).getEntityDoi(anyString(), anyLong());
	}

	private FileEntity getTestFileEntity() {
		FileEntity testFileEntity = new FileEntity();
		testFileEntity.setId("5544");
		testFileEntity.setName(testFileName);
		return testFileEntity;
	}

	@Test(expected = NotFoundException.class)
	public void testGetEntityDoiNotFound() throws Exception {
		// wiring test
		Mockito.when(mockSynapse.getEntityDoi(anyString(), anyLong()))
				.thenThrow(new SynapseNotFoundException());
		synapseClient.getEntityDoi("test entity id", null);
	}

	@Test
	public void testCreateDoi() throws Exception {
		// wiring test
		synapseClient.createDoi("test entity id", null);
		verify(mockSynapse).createEntityDoi(anyString(), anyLong());
	}

	/**
	 * Direct upload tests. Most of the methods are simple pass-throughs to the
	 * Java Synapse client, but completeUpload has additional logic
	 * 
	 * @throws JSONObjectAdapterException
	 * @throws SynapseException
	 * @throws RestServiceException
	 */
	@Test
	public void testCompleteUpload() throws JSONObjectAdapterException,
			SynapseException, RestServiceException {
		FileEntity testFileEntity = getTestFileEntity();
		when(mockSynapse.createEntity(any(FileEntity.class))).thenReturn(
				testFileEntity);
		when(mockSynapse.putEntity(any(FileEntity.class))).thenReturn(
				testFileEntity);

		// parent entity has no immediate children
		EntityIdList childEntities = new EntityIdList();
		childEntities.setIdList(new ArrayList());

		synapseClient.setFileEntityFileHandle(null, null, "parentEntityId");

		// it should have tried to create a new entity (since entity id was
		// null)
		verify(mockSynapse).createEntity(any(FileEntity.class));
	}

	@Test(expected = NotFoundException.class)
	public void testGetFileEntityIdWithSameNameNotFound()
			throws JSONObjectAdapterException, SynapseException,
			RestServiceException, JSONException {
		JSONObject queryResult = new JSONObject();
		queryResult.put("totalNumberOfResults", (long) 0);
		when(mockSynapse.query(anyString())).thenReturn(queryResult); // TODO

		String fileEntityId = synapseClient.getFileEntityIdWithSameName(
				testFileName, "parentEntityId");
	}

	@Test(expected = ConflictException.class)
	public void testGetFileEntityIdWithSameNameConflict()
			throws JSONObjectAdapterException, SynapseException,
			RestServiceException, JSONException {
		Folder folder = new Folder();
		folder.setName(testFileName);
		JSONObject queryResult = new JSONObject();
		JSONArray results = new JSONArray();

		// Set up results.
		JSONObject objectResult = EntityFactory
				.createJSONObjectForEntity(folder);
		JSONArray typeArray = new JSONArray();
		typeArray.put("Folder");
		objectResult.put("entity.concreteType", typeArray);
		results.put(objectResult);

		// Set up query result.
		queryResult.put("totalNumberOfResults", (long) 1);
		queryResult.put("results", results);

		// Have results returned in query.
		when(mockSynapse.query(anyString())).thenReturn(queryResult);

		String fileEntityId = synapseClient.getFileEntityIdWithSameName(
				testFileName, "parentEntityId");
	}

	@Test
	public void testGetFileEntityIdWithSameNameFound() throws JSONException,
			JSONObjectAdapterException, SynapseException, RestServiceException {
		FileEntity file = getTestFileEntity();
		JSONObject queryResult = new JSONObject();
		JSONArray results = new JSONArray();

		// Set up results.
		JSONObject objectResult = EntityFactory.createJSONObjectForEntity(file);
		JSONArray typeArray = new JSONArray();
		typeArray.put(FileEntity.class.getName());
		objectResult.put("entity.concreteType", typeArray);
		objectResult.put("entity.id", file.getId());
		results.put(objectResult);
		queryResult.put("totalNumberOfResults", (long) 1);
		queryResult.put("results", results);

		// Have results returned in query.
		when(mockSynapse.query(anyString())).thenReturn(queryResult);

		String fileEntityId = synapseClient.getFileEntityIdWithSameName(
				testFileName, "parentEntityId");
		assertEquals(fileEntityId, file.getId());
	}

	@Test
	public void testInviteMemberOpenInvitations() throws SynapseException,
			RestServiceException, JSONObjectAdapterException {
		membershipStatus.setHasOpenInvitation(true);
		// verify it does not create a new invitation since one is already open
		synapseClient.inviteMember("123", "a team", "", "");
		verify(mockSynapse, Mockito.times(0)).addTeamMember(anyString(),
				anyString(), anyString(), anyString());
		verify(mockSynapse, Mockito.times(0)).createMembershipInvitation(
				any(MembershipInvtnSubmission.class), anyString(), anyString());

	}

	@Test
	public void testRequestMemberOpenRequests() throws SynapseException,
			RestServiceException, JSONObjectAdapterException {
		membershipStatus.setHasOpenRequest(true);
		// verify it does not create a new request since one is already open
		synapseClient.requestMembership("123", "a team", "let me join", TEST_HOME_PAGE_BASE, null);
		verify(mockSynapse, Mockito.times(0)).addTeamMember(anyString(),
				anyString(), eq(TEST_HOME_PAGE_BASE+"#!Team:"), eq(TEST_HOME_PAGE_BASE+"#!SignedToken:Settings/"));
		ArgumentCaptor<MembershipRqstSubmission> captor = ArgumentCaptor.forClass(MembershipRqstSubmission.class);
		verify(mockSynapse, Mockito.times(0)).createMembershipRequest(
				captor.capture(), anyString(), anyString());
	}

	@Test
	public void testInviteMemberCanJoin() throws SynapseException,
			RestServiceException, JSONObjectAdapterException {
		membershipStatus.setCanJoin(true);
		synapseClient.inviteMember("123", "a team", "", TEST_HOME_PAGE_BASE);
		verify(mockSynapse).addTeamMember(anyString(), anyString(), eq(TEST_HOME_PAGE_BASE+"#!Team:"), eq(TEST_HOME_PAGE_BASE+"#!SignedToken:Settings/"));
	}

	@Test
	public void testRequestMembershipCanJoin() throws SynapseException,
			RestServiceException, JSONObjectAdapterException {
		membershipStatus.setCanJoin(true);
		synapseClient.requestMembership("123", "a team", "", TEST_HOME_PAGE_BASE, new Date());
		verify(mockSynapse).addTeamMember(anyString(), anyString(), eq(TEST_HOME_PAGE_BASE+"#!Team:"), eq(TEST_HOME_PAGE_BASE+"#!SignedToken:Settings/"));
	}

	@Test
	public void testInviteMember() throws SynapseException,
			RestServiceException, JSONObjectAdapterException {
		synapseClient.inviteMember("123", "a team", "", TEST_HOME_PAGE_BASE);
		verify(mockSynapse).createMembershipInvitation(
				any(MembershipInvtnSubmission.class), eq(TEST_HOME_PAGE_BASE+"#!SignedToken:JoinTeam/"), eq(TEST_HOME_PAGE_BASE+"#!SignedToken:Settings/"));
	}

	@Test
	public void testRequestMembership() throws SynapseException,
			RestServiceException, JSONObjectAdapterException {
		ArgumentCaptor<MembershipRqstSubmission> captor = ArgumentCaptor.forClass(MembershipRqstSubmission.class);
		verify(mockSynapse, Mockito.times(0)).createMembershipRequest(
				captor.capture(), anyString(), anyString());
		String teamId = "a team";
		String message=  "let me join";
		Date expiresOn = null;
		synapseClient.requestMembership("123", teamId, message, TEST_HOME_PAGE_BASE, expiresOn);
		verify(mockSynapse).createMembershipRequest(
				captor.capture(), eq(TEST_HOME_PAGE_BASE+"#!SignedToken:JoinTeam/"), eq(TEST_HOME_PAGE_BASE+"#!SignedToken:Settings/"));
		MembershipRqstSubmission request = captor.getValue();
		assertEquals(expiresOn, request.getExpiresOn());
		assertEquals(teamId, request.getTeamId());
		assertEquals(message, request.getMessage());
	}
	
	@Test
	public void testRequestMembershipWithExpiresOn() throws SynapseException,
			RestServiceException, JSONObjectAdapterException {
		ArgumentCaptor<MembershipRqstSubmission> captor = ArgumentCaptor.forClass(MembershipRqstSubmission.class);
		verify(mockSynapse, Mockito.times(0)).createMembershipRequest(
				captor.capture(), anyString(), anyString());
		String teamId = "a team";
		String message=  "let me join";
		Date expiresOn = new Date();
		synapseClient.requestMembership("123", teamId, message, TEST_HOME_PAGE_BASE, expiresOn);
		verify(mockSynapse).createMembershipRequest(
				captor.capture(), eq(TEST_HOME_PAGE_BASE+"#!SignedToken:JoinTeam/"), eq(TEST_HOME_PAGE_BASE+"#!SignedToken:Settings/"));
		MembershipRqstSubmission request = captor.getValue();
		assertEquals(expiresOn, request.getExpiresOn());
		assertEquals(teamId, request.getTeamId());
		assertEquals(message, request.getMessage());
	}


	@Test
	public void testGetOpenRequestCountUnauthorized() throws SynapseException,
			RestServiceException {
		// is not an admin
		TeamMember testTeamMember = new TeamMember();
		testTeamMember.setIsAdmin(false);
		when(mockSynapse.getTeamMember(anyString(), anyString())).thenReturn(
				testTeamMember);

		Long count = synapseClient.getOpenRequestCount("myUserId", "myTeamId");
		// should never ask for open request count
		verify(mockSynapse, Mockito.never()).getOpenMembershipRequests(
				anyString(), anyString(), anyLong(), anyLong());
		assertNull(count);
	}

	@Test
	public void testGetOpenRequestCount() throws SynapseException,
			RestServiceException, MalformedURLException,
			JSONObjectAdapterException {
		// is admin
		TeamMember testTeamMember = new TeamMember();
		testTeamMember.setIsAdmin(true);
		when(mockSynapse.getTeamMember(anyString(), anyString())).thenReturn(
				testTeamMember);

		Long testCount = 42L;
		PaginatedResults<MembershipRequest> testOpenRequests = new PaginatedResults<MembershipRequest>();
		testOpenRequests.setTotalNumberOfResults(testCount);
		when(
				mockSynapse.getOpenMembershipRequests(anyString(), anyString(),
						anyLong(), anyLong())).thenReturn(testOpenRequests);

		Long count = synapseClient.getOpenRequestCount("myUserId", "myTeamId");

		verify(mockSynapse, Mockito.times(1)).getOpenMembershipRequests(
				anyString(), anyString(), anyLong(), anyLong());
		assertEquals(testCount, count);
	}

	@Test
	public void testGetOpenTeamInvitations() throws SynapseException,
			RestServiceException, JSONObjectAdapterException {
		setupTeamInvitations();
		int limit = 55;
		int offset = 2;
		String teamId = "132";
		List<OpenTeamInvitationBundle> invitationBundles = synapseClient
				.getOpenTeamInvitations(teamId, limit, offset);
		verify(mockSynapse).getOpenMembershipInvitationSubmissions(eq(teamId),
				anyString(), eq((long) limit), eq((long) offset));
		// we set this up so that a single invite would be returned. Verify that
		// it is the one we're looking for
		assertEquals(1, invitationBundles.size());
		OpenTeamInvitationBundle invitationBundle = invitationBundles.get(0);
		assertEquals(inviteeUserProfile, invitationBundle.getUserProfile());
		assertEquals(testInvitation, invitationBundle.getMembershipInvtnSubmission());
	}

	@Test
	public void testGetTeamBundle() throws SynapseException,
			RestServiceException, MalformedURLException,
			JSONObjectAdapterException {
		//TODO: test team member count
		
		// set team
		Team team = new Team();
		team.setId("test team id");
		when(mockSynapse.getTeam(anyString())).thenReturn(team);
		
		// is member
		TeamMembershipStatus membershipStatus = new TeamMembershipStatus();
		membershipStatus.setIsMember(true);
		when(mockSynapse.getTeamMembershipStatus(anyString(), anyString()))
				.thenReturn(membershipStatus);
		// is admin
		TeamMember testTeamMember = new TeamMember();
		boolean isAdmin = true;
		testTeamMember.setIsAdmin(isAdmin);
		when(mockSynapse.getTeamMember(anyString(), anyString())).thenReturn(
				testTeamMember);

		// make the call
		TeamBundle bundle = synapseClient.getTeamBundle("myUserId", "myTeamId",
				true);

		// now verify round all values were returned in the bundle (based on the
		// mocked service calls)
		assertEquals(team, bundle.getTeam());
		assertEquals(membershipStatus, bundle.getTeamMembershipStatus());
		assertEquals(isAdmin, bundle.isUserAdmin());
	}

	@Test
	public void testGetTeamMembers() throws SynapseException,
			RestServiceException, MalformedURLException,
			JSONObjectAdapterException {
		// set team member count
		Long testMemberCount = 111L;
		PaginatedResults<TeamMember> allMembers = new PaginatedResults<TeamMember>();
		allMembers.setTotalNumberOfResults(testMemberCount);
		List<TeamMember> members = new ArrayList<TeamMember>();

		TeamMember member1 = new TeamMember();
		member1.setIsAdmin(true);
		UserGroupHeader header1 = new UserGroupHeader();
		Long member1Id = 123L;
		header1.setOwnerId(member1Id + "");
		member1.setMember(header1);
		members.add(member1);

		TeamMember member2 = new TeamMember();
		member2.setIsAdmin(false);
		UserGroupHeader header2 = new UserGroupHeader();
		Long member2Id = 456L;
		header2.setOwnerId(member2Id + "");
		member2.setMember(header2);
		members.add(member2);

		allMembers.setResults(members);
		when(
				mockSynapse.getTeamMembers(anyString(), anyString(), anyLong(),
						anyLong())).thenReturn(allMembers);

		List<UserProfile> profiles = new ArrayList<UserProfile>();
		UserProfile profile1 = new UserProfile();
		profile1.setOwnerId(member1Id + "");
		UserProfile profile2 = new UserProfile();
		profile2.setOwnerId(member2Id + "");
		profiles.add(profile1);
		profiles.add(profile2);
		when(mockSynapse.listUserProfiles(anyList())).thenReturn(profiles);

		// make the call
		TeamMemberPagedResults results = synapseClient.getTeamMembers(
				"myTeamId", "search term", 100, 0);

		// verify it results in the two team member bundles that we expect
		List<TeamMemberBundle> memberBundles = results.getResults();
		assertEquals(2, memberBundles.size());
		TeamMemberBundle bundle1 = memberBundles.get(0);
		assertTrue(bundle1.getIsTeamAdmin());
		assertEquals(profile1, bundle1.getUserProfile());
		TeamMemberBundle bundle2 = memberBundles.get(1);
		assertFalse(bundle2.getIsTeamAdmin());
		assertEquals(profile2, bundle2.getUserProfile());
	}
	
	@Test
	public void testIsTeamMember() throws NumberFormatException, RestServiceException, SynapseException {
		synapseClient.isTeamMember(entityId, Long.valueOf(teamA.getId()));
		verify(mockSynapse).getTeamMembershipStatus(teamA.getId(), entityId);
	}

	@Test
	public void testGetEntityHeaderBatch() throws SynapseException,
			RestServiceException, MalformedURLException,
			JSONObjectAdapterException {
		List<EntityHeader> headers = synapseClient
				.getEntityHeaderBatch(new ArrayList());
		// in the setup, we told the mockSynapse.getEntityHeaderBatch to return
		// batchHeaderResults
		for (int i = 0; i < batchHeaderResults.size(); i++) {
			assertEquals(batchHeaderResults.get(i), headers.get(i));
		}
	}

	@Test
	public void testSendMessage() throws SynapseException,
			RestServiceException, JSONObjectAdapterException {
		ArgumentCaptor<MessageToUser> arg = ArgumentCaptor
				.forClass(MessageToUser.class);
		Set<String> recipients = new HashSet<String>();
		recipients.add("333");
		String subject = "The Mathematics of Quantum Neutrino Fields";
		String messageBody = "Atoms are not to be trusted, they make up everything";
		String hostPageBaseURL = "http://localhost/Portal.html";
		when(mockSynapse.sendStringMessage(any(MessageToUser.class), eq(messageBody))).thenReturn(mockMessageToUser);
		when(mockMessageToUser.writeToJSONObject(any(JSONObjectAdapter.class))).thenReturn(mockJSONObjAd);
		synapseClient.sendMessage(recipients, subject, messageBody, hostPageBaseURL);
		verify(mockSynapse).sendStringMessage(arg.capture(), eq(messageBody));
		MessageToUser toSendMessage = arg.getValue();
		assertEquals(subject, toSendMessage.getSubject());
		assertEquals(recipients, toSendMessage.getRecipients());
		assertTrue(toSendMessage.getNotificationUnsubscribeEndpoint().startsWith(hostPageBaseURL));
	}
	
	@Test
	public void testSendMessageToEntityOwner() throws SynapseException,
			RestServiceException, JSONObjectAdapterException {
		ArgumentCaptor<MessageToUser> arg = ArgumentCaptor
				.forClass(MessageToUser.class);
		
		String subject = "The Mathematics of Quantum Neutrino Fields";
		String messageBody = "Atoms are not to be trusted, they make up everything";
		String hostPageBaseURL = "http://localhost/Portal.html";
		String entityId = "syn98765";
		when(mockSynapse.sendStringMessage(any(MessageToUser.class), eq(entityId), eq(messageBody))).thenReturn(mockMessageToUser);
		when(mockMessageToUser.writeToJSONObject(any(JSONObjectAdapter.class))).thenReturn(mockJSONObjAd);
		synapseClient.sendMessageToEntityOwner(entityId, subject, messageBody, hostPageBaseURL);
		verify(mockSynapse).sendStringMessage(arg.capture(), eq(entityId), eq(messageBody));
		MessageToUser toSendMessage = arg.getValue();
		assertEquals(subject, toSendMessage.getSubject());
		assertTrue(toSendMessage.getNotificationUnsubscribeEndpoint().startsWith(hostPageBaseURL));
	}

	@Test
	public void testGetCertifiedUserPassingRecord()
			throws RestServiceException, SynapseException,
			JSONObjectAdapterException {
		PassingRecord passingRecord = new PassingRecord();
		passingRecord.setPassed(true);
		passingRecord.setQuizId(1238L);
		String passingRecordJson = passingRecord.writeToJSONObject(
				adapterFactory.createNew()).toJSONString();
		when(mockSynapse.getCertifiedUserPassingRecord(anyString()))
				.thenReturn(passingRecord);
		String returnedPassingRecordJson = synapseClient
				.getCertifiedUserPassingRecord("123");
		verify(mockSynapse).getCertifiedUserPassingRecord(anyString());
		assertEquals(passingRecordJson, returnedPassingRecordJson);
	}

	@Test(expected = NotFoundException.class)
	public void testUserNeverAttemptedCertification()
			throws RestServiceException, SynapseException {
		when(mockSynapse.getCertifiedUserPassingRecord(anyString())).thenThrow(
				new SynapseNotFoundException("PassingRecord not found"));
		synapseClient.getCertifiedUserPassingRecord("123");
	}

	@Test(expected = NotFoundException.class)
	public void testUserFailedCertification() throws RestServiceException,
			SynapseException {
		PassingRecord passingRecord = new PassingRecord();
		passingRecord.setPassed(false);
		passingRecord.setQuizId(1238L);
		when(mockSynapse.getCertifiedUserPassingRecord(anyString()))
				.thenReturn(passingRecord);
		synapseClient.getCertifiedUserPassingRecord("123");
	}

	@Test
	public void testGetCertificationQuiz() throws RestServiceException,
			SynapseException {
		when(mockSynapse.getCertifiedUserTest()).thenReturn(new Quiz());
		synapseClient.getCertificationQuiz();
		verify(mockSynapse).getCertifiedUserTest();
	}

	@Test
	public void testSubmitCertificationQuizResponse()
			throws RestServiceException, SynapseException,
			JSONObjectAdapterException {
		PassingRecord mockPassingRecord = new PassingRecord();
		when(
				mockSynapse
						.submitCertifiedUserTestResponse(any(QuizResponse.class)))
				.thenReturn(mockPassingRecord);
		QuizResponse myResponse = new QuizResponse();
		myResponse.setId(837L);
		synapseClient.submitCertificationQuizResponse(myResponse);
		verify(mockSynapse).submitCertifiedUserTestResponse(eq(myResponse));
	}

	@Test
	public void testMarkdownCache() throws Exception {
		Cache<MarkdownCacheRequest, WikiPage> mockCache = Mockito
				.mock(Cache.class);
		synapseClient.setMarkdownCache(mockCache);
		WikiPage page = new WikiPage();
		when(mockCache.get(any(MarkdownCacheRequest.class))).thenReturn(page);
		Mockito.when(
				mockSynapse
						.getV2WikiPage(any(org.sagebionetworks.repo.model.dao.WikiPageKey.class)))
				.thenReturn(v2Page);
		WikiPage actualResult = synapseClient
				.getV2WikiPageAsV1(new WikiPageKey(entity.getId(),
						ObjectType.ENTITY.toString(), "12"));
		assertEquals(page, actualResult);
		verify(mockCache).get(any(MarkdownCacheRequest.class));
	}

	@Test
	public void testMarkdownCacheWithVersion() throws Exception {
		Cache<MarkdownCacheRequest, WikiPage> mockCache = Mockito
				.mock(Cache.class);
		synapseClient.setMarkdownCache(mockCache);
		WikiPage page = new WikiPage();
		when(mockCache.get(any(MarkdownCacheRequest.class))).thenReturn(page);
		Mockito.when(
				mockSynapse
						.getVersionOfV2WikiPage(
								any(org.sagebionetworks.repo.model.dao.WikiPageKey.class),
								anyLong())).thenReturn(v2Page);
		WikiPage actualResult = synapseClient.getVersionOfV2WikiPageAsV1(
				new WikiPageKey(entity.getId(), ObjectType.ENTITY.toString(),
						"12"), 5L);
		assertEquals(page, actualResult);
		verify(mockCache).get(any(MarkdownCacheRequest.class));
	}

	@Test
	public void testFilterAccessRequirements() throws Exception {
		List<AccessRequirement> unfilteredAccessRequirements = new ArrayList<AccessRequirement>();
		List<AccessRequirement> filteredAccessRequirements;
		// filter empty list should not result in failure
		filteredAccessRequirements = AccessRequirementUtils
				.filterAccessRequirements(unfilteredAccessRequirements,
						ACCESS_TYPE.UPDATE);
		assertTrue(filteredAccessRequirements.isEmpty());

		unfilteredAccessRequirements
				.add(createAccessRequirement(ACCESS_TYPE.DOWNLOAD));
		unfilteredAccessRequirements
				.add(createAccessRequirement(ACCESS_TYPE.SUBMIT));
		unfilteredAccessRequirements
				.add(createAccessRequirement(ACCESS_TYPE.SUBMIT));
		// no requirements of type UPDATE
		filteredAccessRequirements = AccessRequirementUtils
				.filterAccessRequirements(unfilteredAccessRequirements,
						ACCESS_TYPE.UPDATE);
		assertTrue(filteredAccessRequirements.isEmpty());
		// 1 download
		filteredAccessRequirements = AccessRequirementUtils
				.filterAccessRequirements(unfilteredAccessRequirements,
						ACCESS_TYPE.DOWNLOAD);
		assertEquals(1, filteredAccessRequirements.size());
		// 2 submit
		filteredAccessRequirements = AccessRequirementUtils
				.filterAccessRequirements(unfilteredAccessRequirements,
						ACCESS_TYPE.SUBMIT);
		assertEquals(2, filteredAccessRequirements.size());

		// finally, filter null list - result will be an empty list
		filteredAccessRequirements = AccessRequirementUtils
				.filterAccessRequirements(null, ACCESS_TYPE.SUBMIT);
		assertNotNull(filteredAccessRequirements);
		assertTrue(filteredAccessRequirements.isEmpty());
	}

	@Test
	public void testGetEntityUnmetAccessRequirements() throws Exception {
		// verify it calls getUnmetAccessRequirements when unmet is true
		synapseClient.getEntityAccessRequirements(entityId, true, null);
		verify(mockSynapse)
				.getUnmetAccessRequirements(
						any(RestrictableObjectDescriptor.class),
						any(ACCESS_TYPE.class),
						anyLong(),
						anyLong());
	}

	@Test
	public void testGetAllEntityAccessRequirements() throws Exception {
		// verify it calls getAccessRequirements when unmet is false
		synapseClient.getEntityAccessRequirements(entityId, false, null);
		verify(mockSynapse).getAccessRequirements(
				any(RestrictableObjectDescriptor.class),
				anyLong(),
				anyLong());
	}

	@Test
	public void testGetAllEntityAccessRequirementsTwoPage() throws Exception {
		PaginatedResults<AccessRequirement> page1 = Mockito.mock(PaginatedResults.class);
		PaginatedResults<AccessRequirement> page2 = Mockito.mock(PaginatedResults.class);
		when(mockSynapse.getUnmetAccessRequirements(any(RestrictableObjectDescriptor.class), any(ACCESS_TYPE.class), anyLong(), anyLong()))
				.thenReturn(page1, page2);
		List<AccessRequirement> page1Results = new ArrayList<AccessRequirement>();
		for (int i = 0; i < SynapseClientImpl.LIMIT_50; i++) {
			page1Results.add(Mockito.mock(AccessRequirement.class));
		}
		when(page1.getResults()).thenReturn(page1Results);
		// second page has a single result
		AccessRequirement singleAccessRequirement = Mockito.mock(AccessRequirement.class);
		when(page2.getResults()).thenReturn(Collections.singletonList(singleAccessRequirement));
		boolean unmetOnly = true;
		org.sagebionetworks.web.shared.PaginatedResults<AccessRequirement> results = synapseClient.getEntityAccessRequirements(entityId, unmetOnly, null);
		//1 full page of results
		assertEquals(SynapseClientImpl.LIMIT_50 + 1, results.getResults().size());
		verify(mockSynapse).getUnmetAccessRequirements(any(RestrictableObjectDescriptor.class), any(ACCESS_TYPE.class), eq(SynapseClientImpl.LIMIT_50), eq(SynapseClientImpl.ZERO_OFFSET.longValue()));
		verify(mockSynapse).getUnmetAccessRequirements(any(RestrictableObjectDescriptor.class), any(ACCESS_TYPE.class), eq(SynapseClientImpl.LIMIT_50), eq(SynapseClientImpl.LIMIT_50));
	}
	
	// pass through tests for email validation

	@Test
	public void testAdditionalEmailValidation() throws Exception {
		Long userId = 992843l;
		String emailAddress = "test@test.com";
		String callbackUrl = "http://www.synapse.org/#!Account:";
		synapseClient.additionalEmailValidation(userId.toString(),
				emailAddress, callbackUrl);
		verify(mockSynapse).additionalEmailValidation(eq(userId),
				eq(emailAddress), eq(callbackUrl));
	}

	@Test
	public void testAddEmail() throws Exception {
		String emailAddressToken = "long synapse email token";
		synapseClient.addEmail(emailAddressToken);
		verify(mockSynapse).addEmail(any(AddEmailInfo.class), anyBoolean());
	}

	@Test
	public void testGetNotificationEmail() throws Exception {
		synapseClient.getNotificationEmail();
		verify(mockSynapse).getNotificationEmail();
	}

	@Test
	public void testSetNotificationEmail() throws Exception {
		String emailAddress = "test@test.com";
		synapseClient.setNotificationEmail(emailAddress);
		verify(mockSynapse).setNotificationEmail(eq(emailAddress));
	}

	@Test
	public void testLogErrorToRepositoryServices() throws SynapseException,
			RestServiceException, JSONObjectAdapterException {
		String errorMessage = "error has occurred";
		String permutationStrongName="Chrome";
		synapseClient.logErrorToRepositoryServices(errorMessage, null, null, null, permutationStrongName);
		verify(mockSynapse).getMyProfile();
		verify(mockSynapse).logError(any(LogEntry.class));
	}

	@Test
	public void testLogErrorToRepositoryServicesTruncation()
			throws SynapseException, RestServiceException,
			JSONObjectAdapterException, ServletException {
		String exceptionMessage = "This exception brought to you by Sage Bionetworks";
		Exception e = new Exception(exceptionMessage, new IllegalArgumentException(new NullPointerException()));
		ServletContext mockServletContext = Mockito.mock(ServletContext.class);
		ServletConfig mockServletConfig = Mockito.mock(ServletConfig.class);
		when(mockServletConfig.getServletContext()).thenReturn(mockServletContext);
		synapseClient.init(mockServletConfig);
		String errorMessage = "error has occurred";
		String permutationStrongName="FF";
		synapseClient.logErrorToRepositoryServices(errorMessage, e.getClass().getSimpleName(), e.getMessage(), e.getStackTrace(), permutationStrongName);
		ArgumentCaptor<LogEntry> captor = ArgumentCaptor
				.forClass(LogEntry.class);
		verify(mockSynapse).logError(captor.capture());
		LogEntry logEntry = captor.getValue();
		assertTrue(logEntry.getLabel().length() < SynapseClientImpl.MAX_LOG_ENTRY_LABEL_SIZE + 100);
		assertTrue(logEntry.getMessage().contains(errorMessage));
		assertTrue(logEntry.getMessage().contains(MY_USER_PROFILE_OWNER_ID));
		assertTrue(logEntry.getMessage().contains(e.getClass().getSimpleName()));
		assertTrue(logEntry.getMessage().contains(exceptionMessage));
	}

	@Test
	public void testGetMyProjects() throws Exception {
		int limit = 11;
		int offset = 20;
		ProjectPagedResults results = synapseClient.getMyProjects(ProjectListType.MY_PROJECTS, limit, offset,
				ProjectListSortColumn.LAST_ACTIVITY, SortDirection.DESC);
		verify(mockSynapse).getMyProjects(eq(ProjectListType.MY_PROJECTS),
				eq(ProjectListSortColumn.LAST_ACTIVITY),
				eq(SortDirection.DESC), eq(limit), eq(offset));
		verify(mockSynapse).listUserProfiles(anyList());
	}

	@Test
	public void testGetUserProjects() throws Exception {
		int limit = 11;
		int offset = 20;
		Long userId = 133l;
		String userIdString = userId.toString();
		synapseClient.getUserProjects(userIdString, limit, offset,
				ProjectListSortColumn.LAST_ACTIVITY, SortDirection.DESC);
		verify(mockSynapse).getProjectsFromUser(eq(userId),
				eq(ProjectListSortColumn.LAST_ACTIVITY),
				eq(SortDirection.DESC), eq(limit), eq(offset));
		verify(mockSynapse).listUserProfiles(anyList());
	}

	@Test
	public void testGetProjectsForTeam() throws Exception {
		int limit = 13;
		int offset = 40;
		Long teamId = 144l;
		String teamIdString = teamId.toString();
		synapseClient.getProjectsForTeam(teamIdString, limit, offset,
				ProjectListSortColumn.LAST_ACTIVITY, SortDirection.DESC);
		verify(mockSynapse).getProjectsForTeam(eq(teamId),
				eq(ProjectListSortColumn.LAST_ACTIVITY),
				eq(SortDirection.DESC), eq(limit), eq(offset));
		verify(mockSynapse).listUserProfiles(anyList());
	}

	@Test
	public void testSafeLongToInt() {
		int inRangeInt = 500;
		int after = SynapseClientImpl.safeLongToInt(inRangeInt);
		assertEquals(inRangeInt, after);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSafeLongToIntPositive() {
		long testValue = Integer.MAX_VALUE;
		testValue++;
		SynapseClientImpl.safeLongToInt(testValue);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSafeLongToIntNegative() {
		long testValue = Integer.MIN_VALUE;
		testValue--;
		SynapseClientImpl.safeLongToInt(testValue);
	}

	@Test
	public void testGetHost() throws RestServiceException {
		assertEquals("mydomain.com",
				synapseClient.getHost("sfTp://mydomain.com/foo/bar"));
		assertEquals("mydomain.com",
				synapseClient.getHost("http://mydomain.com/foo/bar"));
		assertEquals("mydomain.com",
				synapseClient.getHost("http://mydomain.com"));
		assertEquals("mydomain.com",
				synapseClient.getHost("sftp://mydomain.com:22/foo/bar"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetHostNull() throws RestServiceException {
		synapseClient.getHost(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetHostEmpty() throws RestServiceException {
		synapseClient.getHost("");
	}

	@Test(expected = BadRequestException.class)
	public void testGetHostBadUrl() throws RestServiceException {
		synapseClient.getHost("foobar");
	}

	@Test
	public void testGetRootWikiId() throws JSONObjectAdapterException,
			SynapseException, RestServiceException {
		org.sagebionetworks.repo.model.dao.WikiPageKey key = new org.sagebionetworks.repo.model.dao.WikiPageKey();
		key.setOwnerObjectId("1");
		key.setOwnerObjectType(ObjectType.ENTITY);
		String expectedId = "123";
		key.setWikiPageId(expectedId);
		when(mockSynapse.getRootWikiPageKey(anyString(), any(ObjectType.class)))
				.thenReturn(key);

		String actualId = synapseClient.getRootWikiId("1",
				ObjectType.ENTITY.toString());
		assertEquals(expectedId, actualId);
	}

	@Test
	public void testGetFavorites() throws JSONObjectAdapterException,
			SynapseException, RestServiceException {
		PaginatedResults<EntityHeader> pagedResults = new PaginatedResults<EntityHeader>();
		List<EntityHeader> unsortedResults = new ArrayList<EntityHeader>();
		pagedResults.setResults(unsortedResults);
		when(mockSynapse.getFavorites(anyInt(), anyInt())).thenReturn(
				pagedResults);

		// test empty favorites
		List<EntityHeader> actualList = synapseClient.getFavorites();
		assertTrue(actualList.isEmpty());

		// test a few unsorted favorites
		EntityHeader favZ = new EntityHeader();
		favZ.setName("Z");
		unsortedResults.add(favZ);
		EntityHeader favA = new EntityHeader();
		favA.setName("A");
		unsortedResults.add(favA);
		EntityHeader favQ = new EntityHeader();
		favQ.setName("q");
		unsortedResults.add(favQ);

		actualList = synapseClient.getFavorites();
		assertEquals(3, actualList.size());
		assertEquals(favA, actualList.get(0));
		assertEquals(favQ, actualList.get(1));
		assertEquals(favZ, actualList.get(2));
	}

	@Test
	public void testGetTeamBundlesNotOwner() throws RestServiceException, SynapseException {
		// the paginated results were set up to return {teamZ, teamA}, but
		// servlet side we sort by name.
		List<TeamRequestBundle> results = synapseClient.getTeamsForUser("abba", false);
		verify(mockSynapse).getTeamsForUser(eq("abba"), anyInt(), anyInt());
		assertEquals(2, results.size());
		assertEquals(teamA, results.get(0).getTeam());
		assertEquals(teamZ, results.get(1).getTeam());
		verify(mockSynapse, Mockito.never()).getOpenMembershipRequests(anyString(), anyString(),
				anyLong(), anyLong());
	}
	
	@Test
	public void testGetTeamBundlesOwner() throws RestServiceException, SynapseException {
		TeamMember testTeamMember = new TeamMember();
		testTeamMember.setIsAdmin(true);
		when(mockSynapse.getTeamMember(anyString(), anyString())).thenReturn(
				testTeamMember);
		when(mockSynapse.getOpenMembershipRequests(anyString(), anyString(), anyLong(), anyLong())).thenReturn(mockPaginatedMembershipRequest);
		
		List<TeamRequestBundle> results = synapseClient.getTeamsForUser("abba", true);
		verify(mockSynapse).getTeamsForUser(eq("abba"), anyInt(), anyInt());
		assertEquals(2, results.size());
		assertEquals(teamA, results.get(0).getTeam());
		assertEquals(teamZ, results.get(1).getTeam());
		Long reqCount1 = results.get(0).getRequestCount();
		Long reqCount2 = results.get(1).getRequestCount();
		assertEquals(new Long(3L), results.get(0).getRequestCount());
		assertEquals(new Long(3L), results.get(1).getRequestCount());

	}
	
	@Test(expected = BadRequestException.class)
	public void testHandleSignedTokenNull() throws RestServiceException, SynapseException{
		String tokenTypeName = null;
		synapseClient.hexDecodeAndDeserialize(tokenTypeName, encodedJoinTeamToken);
	}
	
	@Test(expected = BadRequestException.class)
	public void testHandleSignedTokenEmpty() throws RestServiceException, SynapseException{
		String tokenTypeName = "";
		synapseClient.hexDecodeAndDeserialize(tokenTypeName, encodedJoinTeamToken);
	}
	
	@Test(expected = BadRequestException.class)
	public void testHandleSignedTokenUnrecognized() throws RestServiceException, SynapseException{
		String tokenTypeName = "InvalidTokenType";
		synapseClient.hexDecodeAndDeserialize(tokenTypeName, encodedJoinTeamToken);
	}
	
	@Test
	public void testHandleSignedTokenJoinTeam() throws RestServiceException, SynapseException{
		String tokenTypeName = NotificationTokenType.JoinTeam.name();
		SignedTokenInterface token = synapseClient.hexDecodeAndDeserialize(tokenTypeName, encodedJoinTeamToken);
		synapseClient.handleSignedToken(token,TEST_HOME_PAGE_BASE);
		verify(mockSynapse).addTeamMember(joinTeamToken, TEST_HOME_PAGE_BASE+"#!Team:", TEST_HOME_PAGE_BASE+"#!SignedToken:Settings/");
	}
	
	@Test(expected = BadRequestException.class)
	public void testHandleSignedTokenInvalidJoinTeam() throws RestServiceException, SynapseException{
		String tokenTypeName = NotificationTokenType.JoinTeam.name();
		SignedTokenInterface token = synapseClient.hexDecodeAndDeserialize(tokenTypeName, "invalid token");
	}
	
	@Test
	public void testHandleSignedTokenNotificationSettings() throws RestServiceException, SynapseException{
		String tokenTypeName = NotificationTokenType.Settings.name();
		SignedTokenInterface token = synapseClient.hexDecodeAndDeserialize(tokenTypeName, encodedNotificationSettingsToken);
		synapseClient.handleSignedToken(token, TEST_HOME_PAGE_BASE);
		verify(mockSynapse).updateNotificationSettings(notificationSettingsToken);
	}
	
	@Test(expected = BadRequestException.class)
	public void testHandleSignedTokenInvalidNotificationSettings() throws RestServiceException, SynapseException{
		String tokenTypeName = NotificationTokenType.Settings.name();
		SignedTokenInterface token = synapseClient.hexDecodeAndDeserialize(tokenTypeName, "invalid token");
	}
	
	@Test
	public void testGetOrCreateActivityForEntityVersionGet() throws SynapseException, RestServiceException {
		when(mockSynapse.getActivityForEntityVersion(anyString(), anyLong())).thenReturn(new Activity());
		synapseClient.getOrCreateActivityForEntityVersion(entityId, version);
		verify(mockSynapse).getActivityForEntityVersion(entityId, version);
	}
	
	@Test
	public void testGetOrCreateActivityForEntityVersionCreate() throws SynapseException, RestServiceException {
		when(mockSynapse.getActivityForEntityVersion(anyString(), anyLong())).thenThrow(new SynapseNotFoundException());
		when(mockSynapse.createActivity(any(Activity.class))).thenReturn(mockActivity);
		synapseClient.getOrCreateActivityForEntityVersion(entityId, version);
		verify(mockSynapse).getActivityForEntityVersion(entityId, version);
		verify(mockSynapse).createActivity(any(Activity.class));
		verify(mockSynapse).putEntity(mockSynapse.getEntityById(entityId), mockActivity.getId());
	}
	
	@Test(expected = Exception.class)
	public void testGetOrCreateActivityForEntityVersionFailure() throws SynapseException, RestServiceException {
		when(mockSynapse.getActivityForEntityVersion(anyString(), anyLong())).thenThrow(new Exception());
		synapseClient.getOrCreateActivityForEntityVersion(entityId, version);
	}
	
	private void setupGetMyLocationSettings() throws SynapseException, RestServiceException{
		List<StorageLocationSetting> existingStorageLocations = new ArrayList<StorageLocationSetting>();
		StorageLocationSetting storageLocation = new ExternalS3StorageLocationSetting();
		storageLocation.setStorageLocationId(1L);
		storageLocation.setBanner(BANNER_1);
		existingStorageLocations.add(storageLocation);
		
		storageLocation = new ExternalStorageLocationSetting();
		storageLocation.setStorageLocationId(2L);
		storageLocation.setBanner(BANNER_2);
		((ExternalStorageLocationSetting)storageLocation).setUrl("sftp://www.jayhodgson.com");
		existingStorageLocations.add(storageLocation);
		
		storageLocation = new ExternalStorageLocationSetting();
		storageLocation.setStorageLocationId(3L);
		storageLocation.setBanner(BANNER_1);
		existingStorageLocations.add(storageLocation);
		
		storageLocation = new ExternalStorageLocationSetting();
		storageLocation.setStorageLocationId(4L);
		storageLocation.setBanner(null);
		existingStorageLocations.add(storageLocation);
		
		when(mockSynapse.getMyStorageLocationSettings()).thenReturn(existingStorageLocations);
	}
	
	@Test
	public void testGetMyLocationSettingBanners() throws SynapseException, RestServiceException {
		setupGetMyLocationSettings();
		List<String> banners = synapseClient.getMyLocationSettingBanners();
		verify(mockSynapse).getMyStorageLocationSettings();
		//should be 2 (only returns unique values)
		assertEquals(2, banners.size());
		assertTrue(banners.contains(BANNER_1));
		assertTrue(banners.contains(BANNER_2));
	}
	
	@Test(expected = Exception.class)
	public void testGetMyLocationSettingBannersFailure() throws SynapseException, RestServiceException {
		when(mockSynapse.getMyStorageLocationSettings()).thenThrow(new Exception());
		synapseClient.getMyLocationSettingBanners();
	}
	
	@Test
	public void testGetStorageLocationSettingNullSetting() throws SynapseException, RestServiceException {
		when(mockSynapse.getProjectSetting(entityId, ProjectSettingsType.upload)).thenReturn(null);
		assertNull(synapseClient.getStorageLocationSetting(entityId));
	}
	
	@Test
	public void testGetStorageLocationSettingNullUploadDestination() throws SynapseException, RestServiceException {
		assertNull(synapseClient.getStorageLocationSetting(entityId));
	}
	
	@Test
	public void testGetStorageLocationSettingDefaultUploadDestination() throws SynapseException, RestServiceException {
		UploadDestination setting = Mockito.mock(UploadDestination.class);
		String defaultStorageId = synapseClient.getSynapseProperties().get(SynapseClientImpl.DEFAULT_STORAGE_ID_PROPERTY_KEY);
		when(setting.getStorageLocationId()).thenReturn(Long.parseLong(defaultStorageId));
		when(mockSynapse.getDefaultUploadDestination(entityId)).thenReturn(setting);
		StorageLocationSetting mockStorageLocationSetting = Mockito.mock(StorageLocationSetting.class);
		when(mockSynapse.getMyStorageLocationSetting(anyLong())).thenReturn(mockStorageLocationSetting);
		
		assertNull(synapseClient.getStorageLocationSetting(entityId));
	}
	
	@Test
	public void testGetStorageLocationSetting() throws SynapseException, RestServiceException {
		UploadDestination setting = Mockito.mock(UploadDestination.class);
		when(setting.getStorageLocationId()).thenReturn(42L);
		when(mockSynapse.getDefaultUploadDestination(entityId)).thenReturn(setting);
		StorageLocationSetting mockStorageLocationSetting = Mockito.mock(StorageLocationSetting.class);
		when(mockSynapse.getMyStorageLocationSetting(anyLong())).thenReturn(mockStorageLocationSetting);
		assertEquals(mockStorageLocationSetting, synapseClient.getStorageLocationSetting(entityId));
	}
	
	@Test(expected = Exception.class)
	public void testGetStorageLocationSettingFailure() throws SynapseException, RestServiceException {
		when(mockSynapse.getMyStorageLocationSetting(anyLong())).thenThrow(new Exception());
		synapseClient.getStorageLocationSetting(entityId);
	}
	
	@Test
	public void testCreateStorageLocationSettingFoundStorageAndProjectSetting() throws SynapseException, RestServiceException {
		setupGetMyLocationSettings();
		
		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setLocations(Collections.EMPTY_LIST);
		when(mockSynapse.getProjectSetting(entityId, ProjectSettingsType.upload)).thenReturn(projectSetting);
		
		//test the case when it finds a duplicate storage location.
		ExternalStorageLocationSetting setting = new ExternalStorageLocationSetting();
		setting.setBanner(BANNER_2);
		setting.setUrl("sftp://www.jayhodgson.com");
		
		synapseClient.createStorageLocationSetting(entityId, setting);
		//should have found the duplicate storage location, so this is never called
		verify(mockSynapse, Mockito.never()).createStorageLocationSetting(any(StorageLocationSetting.class));
		//verify updates project setting, and the new location list is a single value (id of existing storage location)
		ArgumentCaptor<ProjectSetting> captor = ArgumentCaptor.forClass(ProjectSetting.class);
		verify(mockSynapse).updateProjectSetting(captor.capture());
		UploadDestinationListSetting updatedProjectSetting = (UploadDestinationListSetting)captor.getValue();
		List<Long> locations = updatedProjectSetting.getLocations();
		assertEquals(new Long(2), locations.get(0));
	}
	
	@Test
	public void testSetDefaultStorageLocationSetting() throws SynapseException, RestServiceException {
		setupGetMyLocationSettings();
		
		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setLocations(Collections.EMPTY_LIST);
		when(mockSynapse.getProjectSetting(entityId, ProjectSettingsType.upload)).thenReturn(projectSetting);
		
		synapseClient.createStorageLocationSetting(entityId, null);
		// do not try to create a new storage location setting
		verify(mockSynapse, Mockito.never()).createStorageLocationSetting(any(StorageLocationSetting.class));
		//verify updates project setting, and the new location list is a single value (id of existing storage location)
		ArgumentCaptor<ProjectSetting> captor = ArgumentCaptor.forClass(ProjectSetting.class);
		verify(mockSynapse).updateProjectSetting(captor.capture());
		UploadDestinationListSetting updatedProjectSetting = (UploadDestinationListSetting)captor.getValue();
		List<Long> locations = updatedProjectSetting.getLocations();
		assertEquals(SynapseClientImpl.defaultStorageLocation, locations.get(0));
	}

	@Test
	public void testCreateStorageLocationSettingNewStorageAndProjectSetting() throws SynapseException, RestServiceException {
		setupGetMyLocationSettings();
		when(mockSynapse.getProjectSetting(entityId, ProjectSettingsType.upload)).thenReturn(null);
		
		//test the case when it does not find duplicate storage location setting.
		ExternalStorageLocationSetting setting = new ExternalStorageLocationSetting();
		setting.setBanner(BANNER_2);
		setting.setUrl("sftp://www.google.com");
		
		Long newStorageLocationId = 1007L;
		ExternalStorageLocationSetting createdSetting = new ExternalStorageLocationSetting();
		createdSetting.setStorageLocationId(newStorageLocationId);
		
		when(mockSynapse.createStorageLocationSetting(any(StorageLocationSetting.class))).thenReturn(createdSetting);
		
		synapseClient.createStorageLocationSetting(entityId, setting);
		//should not have found a duplicate storage location, so this should be called
		verify(mockSynapse).createStorageLocationSetting(any(StorageLocationSetting.class));
		//verify creates new project setting, and the new location list is a single value (id of the new storage location)
		ArgumentCaptor<ProjectSetting> captor = ArgumentCaptor.forClass(ProjectSetting.class);
		verify(mockSynapse).createProjectSetting(captor.capture());
		UploadDestinationListSetting updatedProjectSetting = (UploadDestinationListSetting)captor.getValue();
		List<Long> locations = updatedProjectSetting.getLocations();
		assertEquals(newStorageLocationId, locations.get(0));
		assertEquals(ProjectSettingsType.upload, updatedProjectSetting.getSettingsType());
		assertEquals(entityId, updatedProjectSetting.getProjectId());
	}
	
	@Test(expected = Exception.class)
	public void testCreateStorageLocationSettingFailure() throws SynapseException, RestServiceException {
		when(mockSynapse.getMyStorageLocationSetting(anyLong())).thenThrow(new Exception());
		synapseClient.createStorageLocationSetting(entityId, new ExternalStorageLocationSetting());
	}
	
	@Test
	public void testUpdateTeamAcl() throws SynapseException, RestServiceException {
		AccessControlList returnedAcl = synapseClient.updateTeamAcl(acl);
		verify(mockSynapse).updateTeamACL(acl);
		assertEquals(acl, returnedAcl);
	}
	@Test
	public void testGetTeamAcl() throws SynapseException, RestServiceException {
		String teamId = "14";
		AccessControlList returnedAcl = synapseClient.getTeamAcl(teamId);
		verify(mockSynapse).getTeamACL(teamId);
		assertEquals(acl, returnedAcl);
	}
	
	private void setupVersionedEntityBundle(String entityId, Long latestVersionNumber) throws SynapseException {
		EntityBundle eb = new EntityBundle();
		Entity file = new FileEntity();
		eb.setEntity(file);
		eb.getEntity().setId(entityId);
		when(mockSynapse.getEntityBundle(anyString(), anyInt())).thenReturn(eb);
		when(mockSynapse.getEntityBundle(anyString(), anyLong(), anyInt())).thenReturn(eb);
		PaginatedResults<VersionInfo> versionInfoPaginatedResults = new PaginatedResults<VersionInfo>();
		List<VersionInfo> versionInfoList = new LinkedList<VersionInfo>();
		VersionInfo versionInfo = new VersionInfo();
		versionInfo.setVersionNumber(latestVersionNumber);
		versionInfoList.add(versionInfo);
		versionInfoPaginatedResults.setResults(versionInfoList);
		when(mockSynapse.getEntityVersions(anyString(), anyInt(), anyInt())).thenReturn(versionInfoPaginatedResults);
		when(mockSynapse.getEntityById(anyString())).thenReturn(file);
	}
	@Test
	public void testGetEntityBundlePlusForVersionVersionable() throws RestServiceException, SynapseException {
		String entityId = "syn123";
		Long targetVersionNumber = 1L;
		Long latestVersionNumber = 2L;
		setupVersionedEntityBundle(entityId, latestVersionNumber);
		EntityBundlePlus returnedEntityBundle = synapseClient.getEntityBundlePlusForVersion(entityId, targetVersionNumber, 1);
		assertEquals(returnedEntityBundle.getLatestVersionNumber(), latestVersionNumber);
		verify(mockSynapse).getEntityBundle(anyString(), eq(targetVersionNumber), anyInt());
		assertEquals(returnedEntityBundle.getEntityBundle().getEntity().getId(), entityId);
	}
	
	@Test
	public void testGetEntityBundlePlusForNullVersionVersionable() throws RestServiceException, SynapseException {
		String entityId = "syn123";
		Long targetVersionNumber = null;
		Long latestVersionNumber = 2L;
		setupVersionedEntityBundle(entityId, latestVersionNumber);
		EntityBundlePlus returnedEntityBundle = synapseClient.getEntityBundlePlusForVersion(entityId, targetVersionNumber, 1);
		assertEquals(returnedEntityBundle.getLatestVersionNumber(), latestVersionNumber);
		verify(mockSynapse).getEntityBundle(anyString(), anyInt());
		assertEquals(returnedEntityBundle.getEntityBundle().getEntity().getId(), entityId);
	}
	
	@Test
	public void testGetEntityBundlePlusForVersionLatestVersion() throws RestServiceException, SynapseException {
		String entityId = "syn123";
		Long targetVersionNumber = 2L;
		Long latestVersionNumber = 2L;
		setupVersionedEntityBundle(entityId, latestVersionNumber);
		EntityBundlePlus returnedEntityBundle = synapseClient.getEntityBundlePlusForVersion(entityId, targetVersionNumber, 1);
		assertEquals(returnedEntityBundle.getLatestVersionNumber(), latestVersionNumber);
		verify(mockSynapse).getEntityBundle(anyString(), anyInt());
		assertEquals(returnedEntityBundle.getEntityBundle().getEntity().getId(), entityId);
	}
	
	@Test
	public void testGetEntityBundlePlusForVersionNonVersionable() throws RestServiceException, SynapseException {
		EntityBundle eb = new EntityBundle();
		eb.setEntity(new Folder());
		eb.getEntity().setId("syn123");
		when(mockSynapse.getEntityBundle(anyString(), anyInt())).thenReturn(eb);
		EntityBundlePlus returnedEntityBundle = synapseClient.getEntityBundlePlusForVersion("syn123", 123L, 1);
		assertNull(returnedEntityBundle.getLatestVersionNumber());
		assertEquals(returnedEntityBundle.getEntityBundle().getEntity().getId(), "syn123");
	}
	
	@Test
	public void testGetUserIdFromUsername() throws UnsupportedEncodingException, SynapseException, RestServiceException {
		//find the user id based on user name
		Long targetUserId = 4L;
		when(mockPrincipalAliasResponse.getPrincipalId()).thenReturn(targetUserId);
		when(mockSynapse.getPrincipalAlias(any(PrincipalAliasRequest.class))).thenReturn(mockPrincipalAliasResponse);
		String userId = synapseClient.getUserIdFromUsername("luke");
		assertEquals(targetUserId.toString(), userId);
	}
	
	@Test(expected = BadRequestException.class)
	public void testGetUserIdFromUsernameBackendError() throws UnsupportedEncodingException, SynapseException, RestServiceException {
		//test error from backend
		when(mockSynapse.getPrincipalAlias(any(PrincipalAliasRequest.class))).thenThrow(new SynapseBadRequestException());
		synapseClient.getUserIdFromUsername("bad-request");
	}
	
	@Test
	public void testGetTableUpdateTransactionRequestNoChange()  throws RestServiceException, SynapseException {
		String tableId = "syn93939";
		
		List<ColumnModel> oldColumnModels = Collections.singletonList(mockOldColumnModel);
		when(mockSynapse.createColumnModels(anyList())).thenReturn(oldColumnModels);
		when(mockSynapse.getColumnModelsForTableEntity(tableId)).thenReturn(oldColumnModels);
		assertEquals(0, synapseClient.getTableUpdateTransactionRequest(tableId, oldColumnModels, oldColumnModels).getChanges().size());
	}
	
	@Test
	public void testGetTableUpdateTransactionRequestNewColumn()  throws RestServiceException, SynapseException {
		String tableId = "syn93939";
		List<ColumnModel> oldColumnModels = new ArrayList<ColumnModel>();
		when(mockSynapse.createColumnModels(anyList())).thenReturn(Collections.singletonList(mockNewColumnModelAfterCreate));
		List<ColumnModel> newColumnModels = Collections.singletonList(mockNewColumnModel);
		TableUpdateTransactionRequest request = synapseClient.getTableUpdateTransactionRequest(tableId, oldColumnModels, newColumnModels);
		verify(mockSynapse).createColumnModels(anyList());
		assertEquals(tableId, request.getEntityId());
		List<TableUpdateRequest> tableUpdates = request.getChanges();
		assertEquals(1, tableUpdates.size());
		TableSchemaChangeRequest schemaChange = (TableSchemaChangeRequest)tableUpdates.get(0);
		List<ColumnChange> changes = schemaChange.getChanges();
		assertEquals(1, changes.size());
		ColumnChange columnChange = changes.get(0);
		assertNull(columnChange.getOldColumnId());
		assertEquals(NEW_COLUMN_MODEL_ID, columnChange.getNewColumnId());
	}
	
	private ColumnModel getColumnModel(String id, ColumnType columnType) {
		ColumnModel cm = new ColumnModel();
		cm.setId(id);
		cm.setColumnType(columnType);
		return cm;
	}
	
	private ColumnChange getColumnChange(String oldColumnId, List<ColumnChange> changes) {
		for (ColumnChange columnChange : changes) {
			if (Objects.equal(oldColumnId, columnChange.getOldColumnId())) {
				return columnChange;
			}
		}
		throw new NoSuchElementException();
	}
	
	@Test
	public void testGetTableUpdateTransactionRequestFullTest()  throws RestServiceException, SynapseException {
		//In this test, we will change a column, delete a column, and add a column (with appropriately mocked responses)
		// Modify colA, delete colB, no change to colC, and add colD
		ColumnModel colA, colB, colC, colD, colAModified, colAAfterSave, colDAfterSave;
		String tableId = "syn93939";
		colA = getColumnModel("1", ColumnType.STRING);
		colB = getColumnModel("2", ColumnType.STRING);
		colC = getColumnModel("3", ColumnType.STRING);
		colD = getColumnModel(null, ColumnType.STRING);
		colAModified = getColumnModel("1", ColumnType.INTEGER);
		colAAfterSave = getColumnModel("4", ColumnType.INTEGER);
		colDAfterSave = getColumnModel("5", ColumnType.STRING);
		
		List<ColumnModel> oldSchema = Arrays.asList(colA, colB, colC);
		List<ColumnModel> proposedNewSchema = Arrays.asList(colAModified, colC, colD);
		List<ColumnModel> newSchemaAfterUpdate = Arrays.asList(colAAfterSave, colC, colDAfterSave);
		when(mockSynapse.createColumnModels(anyList())).thenReturn(newSchemaAfterUpdate);
		
		TableUpdateTransactionRequest request = synapseClient.getTableUpdateTransactionRequest(tableId, oldSchema, proposedNewSchema);
		verify(mockSynapse).createColumnModels(anyList());
		assertEquals(tableId, request.getEntityId());
		List<TableUpdateRequest> tableUpdates = request.getChanges();
		assertEquals(1, tableUpdates.size());
		TableSchemaChangeRequest schemaChange = (TableSchemaChangeRequest)tableUpdates.get(0);
		
		//changes should consist of a create, an update, and a delete
		List<ColumnChange> changes = schemaChange.getChanges();
		assertEquals(3, changes.size());
		
		// colB should be deleted
		ColumnChange columnChange = getColumnChange("2", changes);
		assertNull(columnChange.getNewColumnId());
		// colA should be modified
		columnChange = getColumnChange("1", changes);
		assertEquals("4", columnChange.getNewColumnId());
		// colD should be new
		columnChange = getColumnChange(null, changes);
		assertEquals("5", columnChange.getNewColumnId());
	}
	
	@Test
	public void testGetDefaultColumnsForView()  throws RestServiceException, SynapseException{
		ColumnModel colA, colB;
		colA = getColumnModel("1", ColumnType.STRING);
		colB = getColumnModel("2", ColumnType.STRING);
		
		List<ColumnModel> defaultColumns = Arrays.asList(colA, colB);
		when(mockSynapse.getDefaultColumnsForView(any(ViewType.class))).thenReturn(defaultColumns);
		List<ColumnModel> returnedColumns = synapseClient.getDefaultColumnsForView(ViewType.file);
		
		assertEquals(2, returnedColumns.size());
		assertEquals("1", returnedColumns.get(0).getId());
		assertEquals("2", returnedColumns.get(1).getId());
	}
	

	@Test(expected = UnknownErrorException.class)
	public void testUpdateFileEntityWrongResponseSize() throws RestServiceException, SynapseException {
		synapseClient.updateFileEntity(mockFileEntity, mockFileHandleCopyRequest);
	}
	
	@Test(expected = UnknownErrorException.class)
	public void testUpdateFileEntityWrongResponseSizeTooMany() throws RestServiceException, SynapseException {
		batchCopyResultsList.add(mockFileHandleCopyResult);
		batchCopyResultsList.add(mockFileHandleCopyResult);
		synapseClient.updateFileEntity(mockFileEntity, mockFileHandleCopyRequest);
	}
	
	@Test
	public void testUpdateFileEntity() throws RestServiceException, SynapseException {
		batchCopyResultsList.add(mockFileHandleCopyResult);
		when(mockFileHandleCopyResult.getFailureCode()).thenReturn(null);
		when(mockFileHandleCopyResult.getNewFileHandle()).thenReturn(handle);
		
		synapseClient.updateFileEntity(mockFileEntity, mockFileHandleCopyRequest);
		
		verify(mockSynapse).copyFileHandles(isA(BatchFileHandleCopyRequest.class));
		verify(mockFileEntity).setDataFileHandleId(handle.getId());
		verify(mockSynapse).putEntity(mockFileEntity);
	}
	
	@Test(expected = NotFoundException.class)
	public void testUpdateFileEntityNotFound() throws RestServiceException, SynapseException {
		batchCopyResultsList.add(mockFileHandleCopyResult);
		when(mockFileHandleCopyResult.getFailureCode()).thenReturn(FileResultFailureCode.NOT_FOUND);
		synapseClient.updateFileEntity(mockFileEntity, mockFileHandleCopyRequest);
	}
	
	@Test(expected = UnauthorizedException.class)
	public void testUpdateFileEntityUnauthorized() throws RestServiceException, SynapseException {
		batchCopyResultsList.add(mockFileHandleCopyResult);
		when(mockFileHandleCopyResult.getFailureCode()).thenReturn(FileResultFailureCode.UNAUTHORIZED);
		synapseClient.updateFileEntity(mockFileEntity, mockFileHandleCopyRequest);
	}
	
	@Test(expected = BadRequestException.class)
	public void testGenerateSqlWithFacetsError() throws RestServiceException, SynapseException {
		synapseClient.generateSqlWithFacets(null, null, null);
	}
	
	@Test
	public void testGenerateSqlWithFacets() throws RestServiceException, SynapseException {
		String sql = "select * from syn123";
		FacetColumnRequest request = new FacetColumnValuesRequest();
		String columnName = "col1";
		String facetValue = "a";
		request.setColumnName(columnName);
		when(mockNewColumnModel.getName()).thenReturn(columnName);
		when(mockNewColumnModel.getFacetType()).thenReturn(FacetType.enumeration);
		when(mockNewColumnModel.getColumnType()).thenReturn(ColumnType.STRING);
		((FacetColumnValuesRequest)request).setFacetValues(Collections.singleton(facetValue));
		List<FacetColumnRequest> selectedFacets = Collections.singletonList(request);
		List<ColumnModel> schema = Collections.singletonList(mockNewColumnModel);
		String newSql = synapseClient.generateSqlWithFacets(sql, selectedFacets, schema);
		assertEquals("SELECT * FROM syn123 WHERE ( ( col1 = 'a' ) )", newSql);
	}

	@Test
	public void testGetFileNameFromLocationPath() {
		String name = "filename.txt";
		assertEquals(name, SynapseClientImpl.getFileNameFromExternalUrl("http://some.really.long.com/path/to/a/file/" + name));
		assertEquals(name, SynapseClientImpl.getFileNameFromExternalUrl("http://some.really.long.com/path/to/a/file/" + name + "?param1=value&param2=value"));
		assertEquals(name, SynapseClientImpl.getFileNameFromExternalUrl("/root/" + name));
		assertEquals(name, SynapseClientImpl.getFileNameFromExternalUrl("http://google.com/" + name));
	}
	
	@Test
	public void testIsWiki() throws RestServiceException, SynapseException {
		org.sagebionetworks.repo.model.dao.WikiPageKey key = new org.sagebionetworks.repo.model.dao.WikiPageKey();
		key.setOwnerObjectId("2");
		key.setOwnerObjectType(ObjectType.ENTITY);
		key.setWikiPageId("456");
		when(mockSynapse.getRootWikiPageKey(anyString(), any(ObjectType.class))).thenReturn(key);

		assertTrue(synapseClient.isWiki("2"));
		when(mockSynapse.getRootWikiPageKey(anyString(), any(ObjectType.class))).thenThrow(new SynapseNotFoundException());
		assertFalse(synapseClient.isWiki("3"));
	}
	
	@Test
	public void testIsChallenge() throws RestServiceException, SynapseException {
		org.sagebionetworks.reflection.model.PaginatedResults<Evaluation> testResults = new org.sagebionetworks.reflection.model.PaginatedResults<Evaluation>();
		Evaluation e = new Evaluation();
		e.setId(EVAL_ID_1);
		e.setContentSource("syn123");
		testResults.setTotalNumberOfResults(1);
		testResults.setResults(Collections.singletonList(e));
		
		when(mockSynapse.getEvaluationByContentSource(anyString(),anyInt(),anyInt())).thenReturn(testResults);
		
		userEvaluationPermissions = new UserEvaluationPermissions();
		userEvaluationPermissions.setCanChangePermissions(true);
		when(mockSynapse.getUserEvaluationPermissions(EVAL_ID_1)).thenReturn(userEvaluationPermissions);
		
		//"Before" junit test setup configured so this user to have the ability to change permissions on eval 2, but not on eval 1
		assertTrue(synapseClient.isChallenge("syn123"));
		
		testResults = new org.sagebionetworks.reflection.model.PaginatedResults<Evaluation>();
		testResults.setResults(Collections.EMPTY_LIST);
		when(mockSynapse.getEvaluationByContentSource(anyString(),anyInt(),anyInt())).thenReturn(testResults);
		
		//and verify that no evaluations are returned for a different entity id
		assertFalse(synapseClient.isChallenge("syn987"));
	}
	
	@Test
	public void testIsQueryResult() throws RestServiceException, SynapseException {
		// empty results are returned by default.
		// entity query is run for Files tab, Tables tab, and Docker tab.
		assertFalse(synapseClient.isFileOrFolder("syn987"));
		assertFalse(synapseClient.isTable("syn987"));
		assertFalse(synapseClient.isDocker("syn987"));
		
		entityQueryResultsList.add(mockEntityQueryResult);
		assertTrue(synapseClient.isFileOrFolder("syn987"));
		assertTrue(synapseClient.isTable("syn987"));
		assertTrue(synapseClient.isDocker("syn987"));
	}
	
	@Test
	public void testIsForum() throws RestServiceException, SynapseException {
		Forum mockForum = mock(Forum.class);
		when(mockSynapse.getForumByProjectId(anyString())).thenReturn(mockForum);
		ThreadCount mockThreadCount = mock(ThreadCount.class);
		when(mockThreadCount.getCount()).thenReturn(0L);
		when(mockSynapse.getThreadCountForForum(anyString(), any(DiscussionFilter.class))).thenReturn(mockThreadCount);
		assertFalse(synapseClient.isForum("65"));
		
		when(mockThreadCount.getCount()).thenReturn(1L);
		assertTrue(synapseClient.isForum("65"));
	}
}

package org.sagebionetworks.web.unitclient.widget.entity;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.file.ExternalS3UploadDestination;
import org.sagebionetworks.repo.model.file.ExternalUploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestination;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.PortalGinInjector;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.SynapseJSNIUtils;
import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.client.events.EntityUpdatedHandler;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.utils.Callback;
import org.sagebionetworks.web.client.widget.entity.DoiWidget;
import org.sagebionetworks.web.client.widget.entity.EntityMetadata;
import org.sagebionetworks.web.client.widget.entity.EntityMetadataView;
import org.sagebionetworks.web.client.widget.entity.FileHistoryWidget;
import org.sagebionetworks.web.client.widget.entity.annotation.AnnotationsRendererWidget;
import org.sagebionetworks.web.client.widget.entity.restriction.v2.RestrictionWidget;
import org.sagebionetworks.web.test.helper.AsyncMockStubber;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;

public class EntityMetadataTest {
	@Mock
	EntityMetadataView mockView;
	@Mock
	PortalGinInjector mockInjector;
	@Mock
	AuthenticationController mockAuthenticationController;
	@Mock
	AnnotationsRendererWidget mockAnnotationsWidget;
	@Mock
	DoiWidget mockDoiWidget;
	@Mock
	org.sagebionetworks.web.client.widget.entity.RestrictionWidget mockRestrictionWidget;
	@Mock
	RestrictionWidget mockRestrictionWidgetV2;
	@Mock
	FileHistoryWidget mockFileHistoryWidget;
	@Mock
	Doi mockDoi;
	@Mock
	SynapseClientAsync mockSynapseClient;
	@Mock
	SynapseJSNIUtils mockJSNI;
	@Mock
	CookieProvider mockCookies;
	String entityId = "syn123";
	String entityName = "testEntity";
	Entity en = new Folder();
	EntityMetadata widget;
	
	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		widget = new EntityMetadata(mockView, mockDoiWidget, mockAnnotationsWidget, mockRestrictionWidget, 
				mockFileHistoryWidget, mockSynapseClient, mockJSNI, mockRestrictionWidgetV2, mockCookies);
		when(mockInjector.getFileHistoryWidget()).thenReturn(mockFileHistoryWidget);
	}
	
	@Test
	public void testConstruction() {
		verify(mockView).setDoiWidget(any(IsWidget.class));
		verify(mockView).setAnnotationsRendererWidget(any(IsWidget.class));
		verify(mockView).setFileHistoryWidget(any(IsWidget.class));
		verify(mockView).setRestrictionWidget(any(IsWidget.class));
		verify(mockView).setRestrictionWidgetV2(any(IsWidget.class));
		verify(mockRestrictionWidgetV2).setShowChangeLink(true);
		verify(mockRestrictionWidgetV2).setShowIfProject(false);
		verify(mockRestrictionWidgetV2).setShowFlagLink(true);
		verify(mockView).setRestrictionWidgetV2Visible(false);
	}
	
	@Test
	public void testSetEntityBundleProject() {
		UserEntityPermissions permissions = mock(UserEntityPermissions.class);
		boolean canChangePermissions = false;
		boolean canCertifiedUserEdit = true;
		boolean isCurrentVersion = true;
		when(permissions.getCanChangePermissions()).thenReturn(canChangePermissions);
		when(permissions.getCanCertifiedUserEdit()).thenReturn(canCertifiedUserEdit);
		when(mockRestrictionWidget.asWidget()).thenReturn(null);
		Project project = new Project();
		project.setName(entityName);
		project.setId(entityId);
		EntityBundle bundle = new EntityBundle();
		bundle.setEntity(project);
		bundle.setPermissions(permissions);
		bundle.setDoi(mockDoi);
		en.setId(entityId);
		widget.setEntityBundle(bundle, null);
		verify(mockView).setDetailedMetadataVisible(true);
		verify(mockView).setRestrictionPanelVisible(false);
		verify(mockDoiWidget).configure(mockDoi, entityId);
		verify(mockAnnotationsWidget).configure(bundle, canCertifiedUserEdit, isCurrentVersion);
		verify(mockRestrictionWidget).configure(Mockito.eq(bundle), Mockito.anyBoolean(), Mockito.anyBoolean(),
				Mockito.anyBoolean(), any(Callback.class));
		verify(mockRestrictionWidgetV2).configure(project, canChangePermissions);
		
		verify(mockView).setRestrictionWidgetVisible(true);
		verify(mockView, atLeastOnce()).setRestrictionWidgetV2Visible(false);
	}
	
	@Test
	public void testSetEntityBundleProjectAlphaMode() {
		when(mockCookies.getCookie(eq(DisplayUtils.SYNAPSE_TEST_WEBSITE_COOKIE_KEY))).thenReturn("true");
		UserEntityPermissions permissions = mock(UserEntityPermissions.class);
		EntityBundle bundle = new EntityBundle();
		bundle.setEntity(new Project());
		bundle.setPermissions(permissions);
		bundle.setDoi(mockDoi);
		widget.setEntityBundle(bundle, null);
		verify(mockView).setRestrictionWidgetVisible(false);
		verify(mockView).setRestrictionWidgetV2Visible(true);
	}

	@Test
	public void testSetEntityBundleDockerRepo() {
		UserEntityPermissions permissions = mock(UserEntityPermissions.class);
		boolean canChangePermissions = true;
		boolean canCertifiedUserEdit = false;
		boolean isCurrentVersion = true;
		when(permissions.getCanChangePermissions()).thenReturn(canChangePermissions);
		when(permissions.getCanCertifiedUserEdit()).thenReturn(canCertifiedUserEdit);
		DockerRepository dockerRepo = new DockerRepository();
		dockerRepo.setName(entityName);
		dockerRepo.setId(entityId);
		EntityBundle bundle = new EntityBundle();
		bundle.setEntity(dockerRepo);
		bundle.setPermissions(permissions);
		bundle.setDoi(mockDoi);
		Long versionNumber = null;
		widget.setEntityBundle(bundle, versionNumber);
		verify(mockView).setDetailedMetadataVisible(true);
		verify(mockFileHistoryWidget, never()).setEntityBundle(bundle, versionNumber);
		verify(mockFileHistoryWidget, never()).setEntityUpdatedHandler(any(EntityUpdatedHandler.class));
		verify(mockDoiWidget).configure(mockDoi, entityId);
		verify(mockAnnotationsWidget).configure(bundle, canCertifiedUserEdit, isCurrentVersion);
		verify(mockRestrictionWidget).configure(Mockito.eq(bundle), Mockito.anyBoolean(), Mockito.anyBoolean(),
				Mockito.anyBoolean(), any(Callback.class));
	}
	
	@Test
	public void testSetEntityBundleFileEntityMostRecent() {
		UserEntityPermissions permissions = mock(UserEntityPermissions.class);
		boolean canChangePermissions = true;
		boolean canCertifiedUserEdit = false;
		boolean isCurrentVersion = true;
		when(permissions.getCanChangePermissions()).thenReturn(canChangePermissions);
		when(permissions.getCanCertifiedUserEdit()).thenReturn(canCertifiedUserEdit);
		FileEntity fileEntity = new FileEntity();
		fileEntity.setName(entityName);
		fileEntity.setId(entityId);
		EntityBundle bundle = new EntityBundle();
		bundle.setEntity(fileEntity);
		bundle.setPermissions(permissions);
		bundle.setDoi(mockDoi);
		Long versionNumber = null;
		widget.setEntityBundle(bundle, versionNumber);
		verify(mockView).setDetailedMetadataVisible(true);
		verify(mockFileHistoryWidget).setEntityBundle(bundle, versionNumber);
		verify(mockFileHistoryWidget).setEntityUpdatedHandler(any(EntityUpdatedHandler.class));
		verify(mockDoiWidget).configure(mockDoi, entityId);
		verify(mockAnnotationsWidget).configure(bundle, canCertifiedUserEdit, isCurrentVersion);
		verify(mockRestrictionWidget).configure(Mockito.eq(bundle), Mockito.anyBoolean(), Mockito.anyBoolean(),
				Mockito.anyBoolean(), any(Callback.class));
	}
	
	@Test
	public void testSetEntityBundleFileEntityNotMostRecentVersion() {
		UserEntityPermissions permissions = mock(UserEntityPermissions.class);
		boolean canChangePermissions = true;
		boolean canCertifiedUserEdit = false;
		boolean isCurrentVersion = false;
		when(permissions.getCanChangePermissions()).thenReturn(canChangePermissions);
		when(permissions.getCanCertifiedUserEdit()).thenReturn(canCertifiedUserEdit);
		Long versionNumber = -122L;
		FileEntity fileEntity = new FileEntity();
		fileEntity.setName(entityName);
		fileEntity.setId(entityId);
		fileEntity.setVersionNumber(versionNumber);
		EntityBundle bundle = new EntityBundle();
		bundle.setEntity(fileEntity);
		bundle.setPermissions(permissions);
		bundle.setDoi(mockDoi);
		widget.setEntityBundle(bundle, versionNumber);
		verify(mockView).setDetailedMetadataVisible(true);
		verify(mockFileHistoryWidget).setEntityBundle(bundle, versionNumber);
		verify(mockFileHistoryWidget).setEntityUpdatedHandler(any(EntityUpdatedHandler.class));
		verify(mockDoiWidget).configure(mockDoi, entityId);
		verify(mockAnnotationsWidget).configure(bundle, canCertifiedUserEdit, isCurrentVersion);
		verify(mockRestrictionWidget).configure(Mockito.eq(bundle), Mockito.anyBoolean(), Mockito.anyBoolean(),
				Mockito.anyBoolean(), any(Callback.class));
	}
	
	@Test
	public void testConfigureStorageLocationExternalS3() {
		List<UploadDestination> uploadDestinations = new ArrayList<UploadDestination>();
		ExternalS3UploadDestination exS3Destination = new ExternalS3UploadDestination();
		exS3Destination.setBucket("testBucket");
		exS3Destination.setBaseKey("testBaseKey");
		uploadDestinations.add(exS3Destination);
		AsyncMockStubber.callSuccessWith(uploadDestinations).when(mockSynapseClient).getUploadDestinations(anyString(), any(AsyncCallback.class));
		widget.configureStorageLocation(en);
		verify(mockView).setUploadDestinationText("s3://testBucket/testBaseKey");
		verify(mockView).setUploadDestinationPanelVisible(false);
		verify(mockView).setUploadDestinationPanelVisible(true);
	}
	
	@Test
	public void testConfigureStorageLocationExternalSftp() {
		List<UploadDestination> uploadDestinations = new ArrayList<UploadDestination>();
		ExternalUploadDestination exS3Destination = new ExternalUploadDestination();
		exS3Destination.setUrl("sftp://testUrl.com/abcdef");
		exS3Destination.setUploadType(UploadType.SFTP);
		uploadDestinations.add(exS3Destination);
		AsyncMockStubber.callSuccessWith(uploadDestinations).when(mockSynapseClient).getUploadDestinations(anyString(), any(AsyncCallback.class));
		widget.configureStorageLocation(en);
		verify(mockView).setUploadDestinationText("sftp://testUrl.com");
		verify(mockView).setUploadDestinationPanelVisible(false);
		verify(mockView).setUploadDestinationPanelVisible(true);
	}
	
	@Test
	public void testConfigureStorageLocationExternal() {
		List<UploadDestination> uploadDestinations = new ArrayList<UploadDestination>();
		ExternalUploadDestination exS3Destination = new ExternalUploadDestination();
		exS3Destination.setUploadType(UploadType.HTTPS);
		exS3Destination.setUrl("testUrl.com");
		uploadDestinations.add(exS3Destination);
		AsyncMockStubber.callSuccessWith(uploadDestinations).when(mockSynapseClient).getUploadDestinations(anyString(), any(AsyncCallback.class));
		widget.configureStorageLocation(en);
		verify(mockView).setUploadDestinationText("testUrl.com");
		verify(mockView).setUploadDestinationPanelVisible(false);
		verify(mockView).setUploadDestinationPanelVisible(true);
	}
	
	@Test
	public void testConfigureStorageLocationSynapseStorage() {
		AsyncMockStubber.callSuccessWith(null).when(mockSynapseClient).getUploadDestinations(anyString(), any(AsyncCallback.class));
		widget.configureStorageLocation(en);
		verify(mockView).setUploadDestinationText("Synapse Storage");
		verify(mockView).setUploadDestinationPanelVisible(false);
		verify(mockView).setUploadDestinationPanelVisible(true);
	}
	
	@Test
	public void testConfigureStorageLocationFailure() {
		AsyncMockStubber.callFailureWith(new Exception("This is an exception!")).when(mockSynapseClient).getUploadDestinations(anyString(), any(AsyncCallback.class));
		widget.configureStorageLocation(en);
		verify(mockJSNI).consoleLog("This is an exception!");
		verify(mockView).setUploadDestinationPanelVisible(false);
		verify(mockView, Mockito.never()).setUploadDestinationPanelVisible(true);
	}
	
	@Test
	public void testConfigureStorageLocationFile() {
		AsyncMockStubber.callSuccessWith(null).when(mockSynapseClient).getUploadDestinations(anyString(), any(AsyncCallback.class));
		widget.configureStorageLocation(new FileEntity());
		verify(mockView).setUploadDestinationPanelVisible(false);
		verify(mockView, Mockito.never()).setUploadDestinationPanelVisible(true);
	}
	
}

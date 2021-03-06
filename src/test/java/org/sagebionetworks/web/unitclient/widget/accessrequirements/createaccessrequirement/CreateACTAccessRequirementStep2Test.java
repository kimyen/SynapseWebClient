package org.sagebionetworks.web.unitclient.widget.accessrequirements.createaccessrequirement;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.utils.CallbackP;
import org.sagebionetworks.web.client.widget.FileHandleWidget;
import org.sagebionetworks.web.client.widget.accessrequirements.createaccessrequirement.CreateACTAccessRequirementStep2;
import org.sagebionetworks.web.client.widget.accessrequirements.createaccessrequirement.CreateACTAccessRequirementStep2View;
import org.sagebionetworks.web.client.widget.entity.WikiMarkdownEditor;
import org.sagebionetworks.web.client.widget.entity.WikiPageWidget;
import org.sagebionetworks.web.client.widget.table.modal.wizard.ModalPage.ModalPresenter;
import org.sagebionetworks.web.client.widget.upload.FileHandleUploadWidget;
import org.sagebionetworks.web.client.widget.upload.FileMetadata;
import org.sagebionetworks.web.client.widget.upload.FileUpload;
import org.sagebionetworks.web.shared.WikiPageKey;
import org.sagebionetworks.web.test.helper.AsyncMockStubber;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;

public class CreateACTAccessRequirementStep2Test {
	
	CreateACTAccessRequirementStep2 widget;
	@Mock
	ModalPresenter mockModalPresenter;
	
	@Mock
	CreateACTAccessRequirementStep2View mockView;
	@Mock
	SynapseClientAsync mockSynapseClient;
	@Mock
	WikiMarkdownEditor mockWikiMarkdownEditor;
	@Mock
	WikiPageWidget mockWikiPageRenderer;
	@Mock
	ACTAccessRequirement mockACTAccessRequirement;
	@Mock
	FileHandleUploadWidget mockDucTemplateUploader;
	@Mock
	FileHandleWidget mockDucTemplateFileHandleWidget;
	
	@Captor
	ArgumentCaptor<CallbackP> callbackPCaptor;
	@Captor
	ArgumentCaptor<WikiPageKey> wikiPageKeyCaptor;
	@Captor
	ArgumentCaptor<FileHandleAssociation> fhaCaptor;
	@Mock
	FileUpload mockFileUpload;
	@Mock
	FileMetadata mockFileMetadata;
	
	public static final Long AR_ID = 8765L;
	public static final String FILENAME = "templatefile.pdf";
	public static final String FILE_HANDLE_ID = "9999";
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		widget = new CreateACTAccessRequirementStep2(mockView, mockSynapseClient, mockWikiMarkdownEditor, mockWikiPageRenderer, mockDucTemplateUploader, mockDucTemplateFileHandleWidget);
		widget.setModalPresenter(mockModalPresenter);
		when(mockACTAccessRequirement.getId()).thenReturn(AR_ID);
		AsyncMockStubber.callSuccessWith(mockACTAccessRequirement).when(mockSynapseClient).createOrUpdateAccessRequirement(any(AccessRequirement.class), any(AsyncCallback.class));
		when(mockFileUpload.getFileMeta()).thenReturn(mockFileMetadata);
		when(mockFileUpload.getFileHandleId()).thenReturn(FILE_HANDLE_ID);
		when(mockFileMetadata.getFileName()).thenReturn(FILENAME);
		
		when(mockView.areOtherAttachmentsRequired()).thenReturn(false);
		when(mockView.isAnnualReviewRequired()).thenReturn(false);
		when(mockView.isCertifiedUserRequired()).thenReturn(false);
		when(mockView.isDUCRequired()).thenReturn(false);
		when(mockView.isIDUPublic()).thenReturn(false);
		when(mockView.isIRBApprovalRequired()).thenReturn(false);
		when(mockView.isValidatedProfileRequired()).thenReturn(false);
		
	}
	
	@Test
	public void testConstruction() {
		verify(mockView).setWikiPageRenderer(any(IsWidget.class));
		verify(mockView).setDUCTemplateUploadWidget(any(IsWidget.class));
		verify(mockView).setDUCTemplateWidget(any(IsWidget.class));
		verify(mockView).setPresenter(widget);
		verify(mockWikiPageRenderer).setModifiedCreatedByHistoryVisible(false);
	}
	
	@Test
	public void testDucTemplateUploader() {
		verify(mockDucTemplateUploader).configure(anyString(), callbackPCaptor.capture());
		CallbackP<FileUpload> onUploadCallback = callbackPCaptor.getValue();
		widget.configure(mockACTAccessRequirement);
		onUploadCallback.invoke(mockFileUpload);
		verify(mockACTAccessRequirement).setDucTemplateFileHandleId(FILE_HANDLE_ID);
		verify(mockDucTemplateFileHandleWidget).configure(FILENAME, FILE_HANDLE_ID);
		verify(mockDucTemplateFileHandleWidget).setVisible(true);
	}
	
	@Test
	public void testConfigureWithWiki() {
		when(mockACTAccessRequirement.getDucTemplateFileHandleId()).thenReturn(FILE_HANDLE_ID);
		
		when(mockACTAccessRequirement.getAreOtherAttachmentsRequired()).thenReturn(true);
		when(mockACTAccessRequirement.getIsAnnualReviewRequired()).thenReturn(false);
		when(mockACTAccessRequirement.getIsCertifiedUserRequired()).thenReturn(true);
		when(mockACTAccessRequirement.getIsDUCRequired()).thenReturn(false);
		when(mockACTAccessRequirement.getIsIDUPublic()).thenReturn(true);
		when(mockACTAccessRequirement.getIsIRBApprovalRequired()).thenReturn(false);
		when(mockACTAccessRequirement.getIsValidatedProfileRequired()).thenReturn(true);
		
		widget.configure(mockACTAccessRequirement);
		verify(mockView).setOldTermsVisible(false);
		verify(mockView).setOldTerms("");
		verify(mockWikiPageRenderer).configure(wikiPageKeyCaptor.capture(), eq(false), eq((WikiPageWidget.Callback)null), eq(false));
		WikiPageKey key = wikiPageKeyCaptor.getValue();
		assertEquals(AR_ID.toString(), key.getOwnerObjectId());
		assertEquals(ObjectType.ACCESS_REQUIREMENT.toString(), key.getOwnerObjectType());
		
		//verify duc template file handle widget is configured properly (basd on act duc file handle id)
		verify(mockDucTemplateFileHandleWidget).configure(fhaCaptor.capture());
		FileHandleAssociation fha = fhaCaptor.getValue();
		assertEquals(FileHandleAssociateType.AccessRequirementAttachment, fha.getAssociateObjectType());
		assertEquals(AR_ID.toString(), fha.getAssociateObjectId());
		assertEquals(FILE_HANDLE_ID, fha.getFileHandleId());
		verify(mockDucTemplateFileHandleWidget).setVisible(true);
		
		//validate view is set according to AR values
		verify(mockView).setAreOtherAttachmentsRequired(true);
		verify(mockView).setIsAnnualReviewRequired(false);
		verify(mockView).setIsCertifiedUserRequired(true);
		verify(mockView).setIsDUCRequired(false);
		verify(mockView).setIsIDUPublic(true);
		verify(mockView).setIsIRBApprovalRequired(false);
		verify(mockView).setIsValidatedProfileRequired(true);
		
		// on edit of wiki
		widget.onEditWiki();
		verify(mockWikiMarkdownEditor).configure(eq(key), any(CallbackP.class));
		
		//on finish
		widget.onPrimary();

		// verify access requirement was updated from the view (view value responses configured in the the test setUp()
		verify(mockACTAccessRequirement).setAreOtherAttachmentsRequired(false);
		verify(mockACTAccessRequirement).setIsAnnualReviewRequired(false);
		verify(mockACTAccessRequirement).setIsCertifiedUserRequired(false);
		verify(mockACTAccessRequirement).setIsDUCRequired(false);
		verify(mockACTAccessRequirement).setIsIDUPublic(false);
		verify(mockACTAccessRequirement).setIsIRBApprovalRequired(false);
		verify(mockACTAccessRequirement).setIsValidatedProfileRequired(false);
		verify(mockModalPresenter).onFinished();
	}
	
	@Test
	public void testConfigureWithOldTermsOfUse() {
		String tou = "these are the old conditions";
		when(mockACTAccessRequirement.getActContactInfo()).thenReturn(tou);
		widget.configure(mockACTAccessRequirement);
		verify(mockView).setOldTermsVisible(true);
		verify(mockView).setOldTerms(tou);
		verify(mockWikiPageRenderer).configure(wikiPageKeyCaptor.capture(), eq(false), eq((WikiPageWidget.Callback)null), eq(false));
		WikiPageKey key = wikiPageKeyCaptor.getValue();
		assertEquals(AR_ID.toString(), key.getOwnerObjectId());
		assertEquals(ObjectType.ACCESS_REQUIREMENT.toString(), key.getOwnerObjectType());
		
		// on edit of wiki
		widget.onEditWiki();
		verify(mockWikiMarkdownEditor).configure(eq(key), any(CallbackP.class));
		
		//on finish, it should clear out the old terms of use
		widget.onPrimary();
		verify(mockModalPresenter).setLoading(true);
		verify(mockModalPresenter).setLoading(false);
		verify(mockModalPresenter, never()).setErrorMessage(anyString());
		verify(mockModalPresenter).onFinished();
	}
	
	@Test
	public void testConfigureWithOldTermsOfUseFailureToSave() {
		String tou = "these are the old conditions";
		when(mockACTAccessRequirement.getActContactInfo()).thenReturn(tou);
		widget.configure(mockACTAccessRequirement);
		
		//on finish, it should try to clear out the old terms of use
		String error = "error message";
		AsyncMockStubber.callFailureWith(new Exception(error)).when(mockSynapseClient).createOrUpdateAccessRequirement(any(AccessRequirement.class), any(AsyncCallback.class));
		widget.onPrimary();
		verify(mockModalPresenter).setLoading(true);
		verify(mockModalPresenter).setLoading(false);
		verify(mockModalPresenter).setErrorMessage(error);
		verify(mockModalPresenter, never()).onFinished();
	}
	
}

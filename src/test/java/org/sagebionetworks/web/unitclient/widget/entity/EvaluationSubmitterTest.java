package org.sagebionetworks.web.unitclient.widget.entity;

import static org.mockito.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.RestResourceList;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserSessionData;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.model.EntityBundle;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.utils.Callback;
import org.sagebionetworks.web.client.widget.entity.EvaluationSubmitter;
import org.sagebionetworks.web.client.widget.entity.EvaluationSubmitterView;
import org.sagebionetworks.web.shared.AccessRequirementsTransport;
import org.sagebionetworks.web.shared.EntityWrapper;
import org.sagebionetworks.web.shared.PaginatedResults;
import org.sagebionetworks.web.shared.exceptions.ForbiddenException;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;
import org.sagebionetworks.web.test.helper.AsyncMockStubber;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class EvaluationSubmitterTest {
		
	private static final String EVALUATION_2_SUBMISSION_RECEIPT_MESSAGE = "Evaluation 2 Submission Receipt Message";
	private static final String EVALUATION_1_SUBMISSION_RECEIPT_MESSAGE = "Evaluation 1 Submission Receipt Message";
	EvaluationSubmitter submitter;
	EvaluationSubmitterView mockView;
	AuthenticationController mockAuthenticationController;
	NodeModelCreator mockNodeModelCreator;
	SynapseClientAsync mockSynapseClient;
	GlobalApplicationState mockGlobalApplicationState;
	JSONObjectAdapter jSONObjectAdapter = new JSONObjectAdapterImpl();
	EvaluationSubmitter mockEvaluationSubmitter;
	FileEntity entity;
	EntityBundle bundle;
	String submitterAlias = "MyAlias";
	List<Evaluation> evaluationList;
	PaginatedResults<TermsOfUseAccessRequirement> requirements;
	Reference selectedReference;
	AccessRequirementsTransport art;
	
	@Before
	public void setup() throws RestServiceException, JSONObjectAdapterException{	
		mockView = mock(EvaluationSubmitterView.class);
		mockGlobalApplicationState = mock(GlobalApplicationState.class);
		mockNodeModelCreator = mock(NodeModelCreator.class);
		mockAuthenticationController = mock(AuthenticationController.class);
		mockSynapseClient = mock(SynapseClientAsync.class);
		mockEvaluationSubmitter = mock(EvaluationSubmitter.class);
		submitter = new EvaluationSubmitter(mockView, mockSynapseClient, mockNodeModelCreator, jSONObjectAdapter, mockGlobalApplicationState, mockAuthenticationController);
		UserSessionData usd = new UserSessionData();
		UserProfile profile = new UserProfile();
		profile.setOwnerId("test owner ID");
		usd.setProfile(profile);
		
		when(mockAuthenticationController.getCurrentUserSessionData()).thenReturn(usd);
		when(mockAuthenticationController.isLoggedIn()).thenReturn(true);
		
		AsyncMockStubber.callSuccessWith("fake submission result json").when(mockSynapseClient).createSubmission(anyString(), anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith("fake submitter alias results json").when(mockSynapseClient).getAvailableEvaluationsSubmitterAliases(any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith("fake evaluation results json").when(mockSynapseClient).getAvailableEvaluations(any(AsyncCallback.class));
		
		PaginatedResults<Evaluation> availableEvaluations = new PaginatedResults<Evaluation>();
		availableEvaluations.setTotalNumberOfResults(2);
		evaluationList = new ArrayList<Evaluation>();
		Evaluation e1 = new Evaluation();
		e1.setId("1");
		e1.setName("Test Evaluation 1");
		e1.setSubmissionReceiptMessage(EVALUATION_1_SUBMISSION_RECEIPT_MESSAGE);
		evaluationList.add(e1);
		Evaluation e2 = new Evaluation();
		e2.setId("2");
		e2.setName("Test Evaluation 2");
		e2.setSubmissionReceiptMessage(EVALUATION_2_SUBMISSION_RECEIPT_MESSAGE);
		evaluationList.add(e2);
		availableEvaluations.setResults(evaluationList);

		when(mockNodeModelCreator.createPaginatedResults(anyString(), any(Class.class))).thenReturn(availableEvaluations);
		
		RestResourceList submitterAliases = new RestResourceList();
		List<String> submitterAliasList = new ArrayList<String>();
		submitterAliasList.add("Mr. F");
		submitterAliases.setList(submitterAliasList);
		when(mockNodeModelCreator.createJSONEntity(anyString(), eq(RestResourceList.class))).thenReturn(submitterAliases);
		
		entity = new FileEntity();
		entity.setVersionNumber(5l);
		entity.setId("file entity test id");
		bundle = new EntityBundle(entity, null, null, null, null, null, null, null);
		
		AsyncMockStubber.callSuccessWith(null).when(mockSynapseClient).getEntity(anyString(), any(AsyncCallback.class));
		when(mockNodeModelCreator.createEntity(any(EntityWrapper.class))).thenReturn(entity);
	}
	
	@Test
	public void testSubmitToEvaluations() throws RestServiceException {
		submitter.configure(entity, null);
		
		
		submitter.submitToEvaluations(null, new ArrayList<Evaluation>(), submitterAlias);
		verify(mockSynapseClient, times(0)).createSubmission(anyString(), anyString(), any(AsyncCallback.class));
		submitter.submitToEvaluations(null, evaluationList, submitterAlias);
		
		//should invoke twice (once per evaluation)
		verify(mockSynapseClient, times(2)).createSubmission(anyString(), anyString(), any(AsyncCallback.class));
		
		ArgumentCaptor<HashSet> captor = ArgumentCaptor.forClass(HashSet.class);
		//submitted status shown
		verify(mockView).showSubmissionAcceptedDialogs(captor.capture());
		//verify both evaluation receipt messages are in the map to display
		HashSet receiptMessage = captor.getValue();
		assertTrue(receiptMessage.contains(EVALUATION_1_SUBMISSION_RECEIPT_MESSAGE));
		assertTrue(receiptMessage.contains(EVALUATION_2_SUBMISSION_RECEIPT_MESSAGE));
	}
	
	@Test
	public void testSubmitToEvaluationsFailure() throws RestServiceException {
		submitter.configure(entity, null);
		List<Evaluation> evals = new ArrayList<Evaluation>();
		AsyncMockStubber.callFailureWith(new ForbiddenException()).when(mockSynapseClient).createSubmission(anyString(), anyString(), any(AsyncCallback.class));
		evals.add(new Evaluation());
		submitter.submitToEvaluations(null, evals, submitterAlias);
		verify(mockSynapseClient).createSubmission(anyString(), anyString(), any(AsyncCallback.class));
		//submitted status shown
		verify(mockView).showErrorMessage(anyString());
	}
	
	@Test
	public void testSubmitToEvaluationsWithTermsOfUse() throws RestServiceException, JSONObjectAdapterException{
		requirements = new PaginatedResults<TermsOfUseAccessRequirement>();		
		requirements.setTotalNumberOfResults(1);
		TermsOfUseAccessRequirement requirement = new TermsOfUseAccessRequirement();
		requirement.setId(2l);
		requirement.setTermsOfUse("My test ToU");
		List<TermsOfUseAccessRequirement> ars = new ArrayList<TermsOfUseAccessRequirement>();
		ars.add(requirement);
		requirements.setResults(ars);

		selectedReference = new Reference();
		art = new AccessRequirementsTransport();
		AsyncMockStubber.callSuccessWith(art).when(mockSynapseClient).getUnmetAccessRequirements(anyString(), any(AsyncCallback.class));
		when(mockNodeModelCreator.createPaginatedResults(anyString(), any(Class.class))).thenReturn(requirements);
		
		submitter.configure(entity, null);
		submitter.submitToEvaluations(selectedReference, evaluationList, submitterAlias);

		//should show terms of use
		verify(mockView).showAccessRequirement(anyString(), any(Callback.class));
	}
	
	@Test
	public void testShowAvailableEvaluations() throws RestServiceException {
		submitter.configure(entity, null);
		verify(mockSynapseClient).getAvailableEvaluations(any(AsyncCallback.class));
		verify(mockSynapseClient).getAvailableEvaluationsSubmitterAliases(any(AsyncCallback.class));
		verify(mockView).popupSelector(anyBoolean(), any(List.class), any(List.class));
	}
	
	@Test
	public void testShowAvailableEvaluationsNoResults() throws RestServiceException, JSONObjectAdapterException {
		//mock empty evaluation list
		PaginatedResults<Evaluation> availableEvaluations = new PaginatedResults<Evaluation>();
		availableEvaluations.setTotalNumberOfResults(0);
		List<Evaluation> evaluationList = new ArrayList<Evaluation>();
		availableEvaluations.setResults(evaluationList);
		when(mockNodeModelCreator.createPaginatedResults(anyString(), any(Class.class))).thenReturn(availableEvaluations);
		submitter.configure(entity, null);
		verify(mockSynapseClient).getAvailableEvaluations(any(AsyncCallback.class));
		//no evaluations to join error message
		verify(mockView).showErrorMessage(anyString());
	}
	
	@Test
	public void testShowAvailableEvaluationsFailure1() throws RestServiceException, JSONObjectAdapterException {
		AsyncMockStubber.callFailureWith(new ForbiddenException()).when(mockSynapseClient).getAvailableEvaluations(any(AsyncCallback.class));
		submitter.configure(entity, null);
		verify(mockSynapseClient).getAvailableEvaluations(any(AsyncCallback.class));
		//no evaluations to join error message
		verify(mockView).showErrorMessage(anyString());
	}
	
	@Test
	public void testShowAvailableEvaluationsFailure2() throws RestServiceException, JSONObjectAdapterException {
		AsyncMockStubber.callFailureWith(new Exception()).when(mockSynapseClient).getAvailableEvaluationsSubmitterAliases(any(AsyncCallback.class));
		submitter.configure(entity, null);
		verify(mockSynapseClient).getAvailableEvaluationsSubmitterAliases(any(AsyncCallback.class));
		//Failure when asking for submitter aliases
		verify(mockView).showErrorMessage(anyString());
	}
	
}

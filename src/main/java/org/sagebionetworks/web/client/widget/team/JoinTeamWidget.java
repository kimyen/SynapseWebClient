package org.sagebionetworks.web.client.widget.team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PostMessageContentAccessRequirement;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMembershipStatus;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserSessionData;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.GWTWrapper;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.place.LoginPlace;
import org.sagebionetworks.web.client.presenter.TeamSearchPresenter;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.utils.Callback;
import org.sagebionetworks.web.client.utils.CallbackP;
import org.sagebionetworks.web.client.utils.GovernanceServiceHelper;
import org.sagebionetworks.web.client.widget.WidgetRendererPresenter;
import org.sagebionetworks.web.client.widget.entity.WikiPageWidget;
import org.sagebionetworks.web.shared.TeamBundle;
import org.sagebionetworks.web.shared.WebConstants;
import org.sagebionetworks.web.shared.WidgetConstants;
import org.sagebionetworks.web.shared.WikiPageKey;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class JoinTeamWidget implements JoinTeamWidgetView.Presenter, WidgetRendererPresenter {
	private JoinTeamWidgetView view;
	private GlobalApplicationState globalApplicationState;
	private SynapseClientAsync synapseClient;
	private WikiPageWidget wikiPageWidget;
	private GWTWrapper gwt;
	private String teamId;
	private boolean isChallengeSignup;
	private AuthenticationController authenticationController;
	private NodeModelCreator nodeModelCreator;
	private JSONObjectAdapter jsonObjectAdapter;
	private Callback teamUpdatedCallback;
	private String message, isMemberMessage, successMessage, buttonText;
	private boolean isAcceptingInvite, canPublicJoin;
	private Callback widgetRefreshRequired;
	private List<AccessRequirement> accessRequirements;
	private int currentPage;
	private int currentAccessRequirement;
	
	public static final String[] EXTRA_INFO_URL_WHITELIST = { 
		"https://www.projectdatasphere.org/projectdatasphere/",
		"https://mpmdev.ondemand.sas.com/projectdatasphere/"
	};

	
	@Inject
	public JoinTeamWidget(JoinTeamWidgetView view, 
			SynapseClientAsync synapseClient, 
			GlobalApplicationState globalApplicationState, 
			AuthenticationController authenticationController, 
			NodeModelCreator nodeModelCreator,
			JSONObjectAdapter jsonObjectAdapter,
			GWTWrapper gwt,
			WikiPageWidget wikiPageWidget
			) {
		this.view = view;
		view.setPresenter(this);
		this.synapseClient = synapseClient;
		this.globalApplicationState = globalApplicationState;
		this.authenticationController = authenticationController;
		this.nodeModelCreator = nodeModelCreator;
		this.jsonObjectAdapter = jsonObjectAdapter;
		this.gwt = gwt;
		this.wikiPageWidget = wikiPageWidget;
		wikiPageWidget.showCreatedBy(false);
		wikiPageWidget.showModifiedBy(false);
		wikiPageWidget.showWikiHistory(false);
	}

	public void configure(String teamId, boolean canPublicJoin, boolean isChallengeSignup, TeamMembershipStatus teamMembershipStatus, Callback teamUpdatedCallback, String isMemberMessage, String successMessage, String buttonText) {
		//set team id
		this.teamId = teamId;
		this.canPublicJoin = canPublicJoin;
		this.isChallengeSignup = isChallengeSignup;
		this.teamUpdatedCallback = teamUpdatedCallback;
		this.isMemberMessage = isMemberMessage;
		this.successMessage = successMessage;
		this.buttonText = buttonText;
		view.configure(authenticationController.isLoggedIn(), canPublicJoin, teamMembershipStatus, isMemberMessage, buttonText);
	};

	@Override
	public void configure(WikiPageKey wikiKey, Map<String, String> descriptor, Callback widgetRefreshRequired, Long wikiVersionInView) {
		this.widgetRefreshRequired = widgetRefreshRequired;
		this.teamId = null;
		if (descriptor.containsKey(WidgetConstants.JOIN_WIDGET_TEAM_ID_KEY)) 
			this.teamId = descriptor.get(WidgetConstants.JOIN_WIDGET_TEAM_ID_KEY);
		
		//is the team associated with joining a challenge?
		if (descriptor.containsKey(WebConstants.JOIN_WIDGET_IS_CHALLENGE_KEY)) {
			this.isChallengeSignup = Boolean.parseBoolean(descriptor.get(WebConstants.JOIN_WIDGET_IS_CHALLENGE_KEY));
		} else {
			//check for old param
			this.isChallengeSignup = descriptor.containsKey(WidgetConstants.JOIN_WIDGET_SHOW_PROFILE_FORM_KEY) ? 
					Boolean.parseBoolean(descriptor.get(WidgetConstants.JOIN_WIDGET_SHOW_PROFILE_FORM_KEY)) : 
					false;
		}
		
		this.isMemberMessage = descriptor.get(WidgetConstants.IS_MEMBER_MESSAGE);
		
		this.successMessage = descriptor.get(WidgetConstants.JOIN_TEAM_SUCCESS_MESSAGE);
		this.buttonText = descriptor.get(WidgetConstants.JOIN_TEAM_BUTTON_TEXT);
		
		refresh();
	}
	
	private void refresh() {
		boolean isLoggedIn = authenticationController.isLoggedIn();
		if (isLoggedIn) {
			synapseClient.getTeamBundle(authenticationController.getCurrentUserPrincipalId(), teamId, isLoggedIn, new AsyncCallback<TeamBundle>() {
				@Override
				public void onSuccess(TeamBundle result) {
					try {
						Team team = nodeModelCreator.createJSONEntity(result.getTeamJson(), Team.class);
						TeamMembershipStatus teamMembershipStatus = null;
						if (result.getTeamMembershipStatusJson() != null)
							teamMembershipStatus = nodeModelCreator.createJSONEntity(result.getTeamMembershipStatusJson(), TeamMembershipStatus.class);
						configure(team.getId(), TeamSearchPresenter.getCanPublicJoin(team), isChallengeSignup, teamMembershipStatus, null, isMemberMessage, successMessage, buttonText);
					} catch (JSONObjectAdapterException e) {
						onFailure(e);
					}
				}
				@Override
				public void onFailure(Throwable caught) {
					view.showErrorMessage(caught.getMessage());
				}
			});
		} else {
			configure(teamId, canPublicJoin, isChallengeSignup, null, null, isMemberMessage, successMessage, buttonText);
		}
	}
	
	@Override
	public void sendJoinRequest(String message, boolean isAcceptingInvite) {
		this.message = message;
		this.isAcceptingInvite = isAcceptingInvite;
		sendJoinRequestStep0();
	}
	
	public void sendJoinRequestStep0() {
		view.setButtonsEnabled(false);
		currentPage = 0;
		currentAccessRequirement = 0;
		//initialize the access requirements
		accessRequirements = new ArrayList<AccessRequirement>();
		synapseClient.getTeamAccessRequirements(teamId, new AsyncCallback<List<AccessRequirement>>() {
			@Override
			public void onSuccess(List<AccessRequirement> results) {
				//are there access restrictions?
				accessRequirements = results;
				view.setButtonsEnabled(true);
				//access requirements initialized, show the join wizard if challenge signup, or if there are AR to show
				if (isChallengeSignup) {
					startChallengeSignup();
				}
				else { //skip to step 2
					if (accessRequirements.size() > 0)
						view.showJoinWizard();
					sendJoinRequestStep2();
				}
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.setButtonsEnabled(true);
				view.showErrorMessage(DisplayConstants.JOIN_TEAM_ERROR + caught.getMessage());
			}
		});
	}
	
	public void startChallengeSignup() {
		//first, get the challenge participation info wiki key, then show the join wizard
		CallbackP<WikiPageKey> callback = new CallbackP<WikiPageKey>(){
			@Override
			public void invoke(WikiPageKey key) {
				view.showJoinWizard();
				sendJoinRequestStep1(key);
			}
		};
		getChallengeParticipationInfoWikiKey(callback);
	}
	
	public void getChallengeParticipationInfoWikiKey(final CallbackP<WikiPageKey> callback) {
		synapseClient.getHelpPages(new AsyncCallback<HashMap<String,WikiPageKey>>() {
			@Override
			public void onSuccess(HashMap<String,WikiPageKey> result) {
				callback.invoke(result.get(WebConstants.CHALLENGE_PARTICIPATION_INFO));
			};
			@Override
			public void onFailure(Throwable caught) {
				view.showErrorMessage(caught.getMessage());
				callback.invoke(null);
			}
		});
	}

	public int getTotalPageCount() {
		int challengeSignupPage = isChallengeSignup ? 1 : 0;
		return accessRequirements.size() + challengeSignupPage;
	}
	
	/**
	 * Gather additional info about the logged in user
	 */
	public void sendJoinRequestStep1(WikiPageKey challengeInfoWikiPageKey) {
		UserSessionData sessionData = authenticationController.getCurrentUserSessionData();
		UserProfile profile = sessionData.getProfile();
		view.updateWizardProgress(currentPage, getTotalPageCount());
		view.showChallengeInfoPage(profile, challengeInfoWikiPageKey, new Callback() {
			@Override
			public void invoke() {
				currentPage++;
				sendJoinRequestStep2();
			}
		});
	}
	
	/**
	 * Check for unmet access restrictions. As long as more exist, it will keep calling itself until all restrictions are approved.
	 * Will not proceed to step3 (joining the team) until all have been approved.
	 * @throws RestServiceException
	 */
	public void sendJoinRequestStep2() {
		if (currentAccessRequirement >= accessRequirements.size()) {
			//done showing access requirements (and challenge info)
			view.hideJoinWizard();
			sendJoinRequestStep3();
		} else {
			final AccessRequirement accessRequirement = accessRequirements.get(currentAccessRequirement);
			Callback termsOfUseCallback = new Callback() {
				@Override
				public void invoke() {
					//agreed to terms of use.
					currentAccessRequirement++;
					currentPage++;
					setLicenseAccepted(accessRequirement);
				}
			};
			//pop up the requirement
			view.updateWizardProgress(currentPage, getTotalPageCount());
			if (accessRequirement instanceof TermsOfUseAccessRequirement) {
				String text = GovernanceServiceHelper.getAccessRequirementText(accessRequirement);
				if (!DisplayUtils.isDefined(text)) {
					WikiPageKey wikiKey = new WikiPageKey(accessRequirement.getId().toString(), ObjectType.ACCESS_REQUIREMENT.toString(), null);
					wikiPageWidget.configure(wikiKey, false, null, false);
					view.showWikiAccessRequirement(wikiPageWidget.asWidget(), termsOfUseCallback);
				} else {
					view.showTermsOfUseAccessRequirement(text, termsOfUseCallback);	
				}
				
			} else if (accessRequirement instanceof ACTAccessRequirement) {
				String text = GovernanceServiceHelper.getAccessRequirementText(accessRequirement);
				view.showACTAccessRequirement(text, termsOfUseCallback);
			} else if (accessRequirement instanceof PostMessageContentAccessRequirement) {
				String url = ((PostMessageContentAccessRequirement) accessRequirement).getUrl();
				view.showPostMessageContentAccessRequirement(enhancePostMessageUrl(url), termsOfUseCallback);
			} else {
				view.showErrorMessage("Unsupported access restriction type - " + accessRequirement.getClass().getName());
			}
		}
	}
	
	public String enhancePostMessageUrl(String url) {
		if (authenticationController.isLoggedIn() && isRecognizedSite(url)) {
			//include other information from the profile
			UserProfile profile = authenticationController.getCurrentUserSessionData().getProfile();
			return url + "?" + 
					getEncodedParamValue(WebConstants.FIRST_NAME_PARAM, profile.getFirstName(), "&") + 
					getEncodedParamValue(WebConstants.LAST_NAME_PARAM, profile.getLastName(), "&") + 
					getEncodedParamValue(WebConstants.EMAIL_PARAM, profile.getEmails().get(0), "&") + 
					getEncodedParamValue(WebConstants.USER_ID_PARAM, profile.getOwnerId(), ""); 
		} else
			return url;
	}
	
	public String getEncodedParamValue(String paramKey, String value, String suffix) {
		String param = paramKey+"=";
		if (DisplayUtils.isDefined(value)) {
			param += gwt.encodeQueryString(value);
		}
		param += suffix;
		return param;
	}
	
	/**
	 * return true if it is on a whitelist that allows Synapse to send additional information in the query params
	 * @param siteUrl
	 * @return
	 */
	public static boolean isRecognizedSite(String siteUrl) {
		if(siteUrl != null) {
			for(String base : EXTRA_INFO_URL_WHITELIST) {
				// starts with one of the valid url bases?				
				if(siteUrl.toLowerCase().startsWith(base)) return true;
			}
		}
		return false;
	}

	
	public void setLicenseAccepted(AccessRequirement ar) {	
		final CallbackP<Throwable> onFailure = new CallbackP<Throwable>() {
			@Override
			public void invoke(Throwable t) {
				view.showErrorMessage(DisplayConstants.JOIN_TEAM_ERROR + t.getMessage());
			}
		};
		
		Callback onSuccess = new Callback() {
			@Override
			public void invoke() {
				//ToU signed, now try to register for the challenge (will check for other access restrictions before join)
				sendJoinRequestStep2();
			}
		};
		if (ar instanceof ACTAccessRequirement) {
			//no need to sign, just continue
			onSuccess.invoke();
		} else {
			GovernanceServiceHelper.signTermsOfUse(
					authenticationController.getCurrentUserPrincipalId(), 
					ar, 
					onSuccess, 
					onFailure, 
					synapseClient, 
					jsonObjectAdapter);
		}
	}
	
	public void sendJoinRequestStep3() {
		synapseClient.requestMembership(authenticationController.getCurrentUserPrincipalId(), teamId, message, new AsyncCallback<Void>() {
			@Override
			public void onSuccess(Void result) {
				String successJoinMessage = successMessage == null ? WidgetConstants.JOIN_TEAM_DEFAULT_SUCCESS_MESSAGE : successMessage;
				String message = isAcceptingInvite ? successJoinMessage : "Request Sent";
				view.showInfo(message, "");
				refresh();
				if (teamUpdatedCallback != null)
					teamUpdatedCallback.invoke();
				if (widgetRefreshRequired != null)
					widgetRefreshRequired.invoke();
				
			}
			@Override
			public void onFailure(Throwable caught) {
				view.showErrorMessage(caught.getMessage());
			}
		});
	}
	
	@Override
	public void gotoLoginPage() {
		goTo(new LoginPlace(LoginPlace.LOGIN_TOKEN));
	}
	
	public void goTo(Place place) {
		globalApplicationState.getPlaceChanger().goTo(place);
	}
	
	public void clear() {
		view.clear();
	}
	
	public Widget asWidget() {
		view.setPresenter(this);
		return view.asWidget();
	}
	
	public boolean isChallengeSignup() {
		return isChallengeSignup;
	}
}

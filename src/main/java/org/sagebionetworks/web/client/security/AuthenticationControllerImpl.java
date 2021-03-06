package org.sagebionetworks.web.client.security;

import java.util.Date;
import java.util.HashMap;

import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserSessionData;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.schema.adapter.AdapterFactory;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.web.client.DateUtils;
import org.sagebionetworks.web.client.GWTWrapper;
import org.sagebionetworks.web.client.GlobalApplicationStateImpl;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.UserAccountServiceAsync;
import org.sagebionetworks.web.client.cache.ClientCache;
import org.sagebionetworks.web.client.cache.SessionStorage;
import org.sagebionetworks.web.client.cookie.CookieKeys;
import org.sagebionetworks.web.client.cookie.CookieProvider;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.XsrfToken;
import com.google.gwt.user.client.rpc.XsrfTokenServiceAsync;
import com.google.inject.Inject;

/**
 * A util class for authentication
 * 
 * CODE SPLITTING NOTE: this class should be kept small
 * 
 * @author dburdick
 *
 */
public class AuthenticationControllerImpl implements AuthenticationController {
	public static final String XSRF_TOKEN_KEY = "org.sagebionetworks.XSRFToken";
	public static final String USER_SESSION_DATA_CACHE_KEY = "org.sagebionetworks.UserSessionData";
	public static final String USER_AUTHENTICATION_RECEIPT = "_authentication_receipt";
	private static final String AUTHENTICATION_MESSAGE = "Invalid usename or password.";
	private static UserSessionData currentUser;
	private CookieProvider cookies;
	private UserAccountServiceAsync userAccountService;	
	private SessionStorage sessionStorage;
	private ClientCache localStorage;
	private AdapterFactory adapterFactory;
	private SynapseClientAsync synapseClient;
	private XsrfTokenServiceAsync xsrfTokenService;
	private GWTWrapper gwt;
	
	@Inject
	public AuthenticationControllerImpl(
			CookieProvider cookies, 
			UserAccountServiceAsync userAccountService, 
			SessionStorage sessionStorage, 
			ClientCache localStorage, 
			AdapterFactory adapterFactory,
			XsrfTokenServiceAsync xsrfTokenService,
			SynapseClientAsync synapseClient,
			GWTWrapper gwt){
		this.cookies = cookies;
		this.userAccountService = userAccountService;
		this.sessionStorage = sessionStorage;
		this.localStorage = localStorage;
		this.adapterFactory = adapterFactory;
		this.synapseClient = synapseClient;
		this.xsrfTokenService = xsrfTokenService;
		this.gwt = gwt;
		gwt.asServiceDefTarget(xsrfTokenService).setServiceEntryPoint(gwt.getModuleBaseURL() + "xsrf");
	}

	@Override
	public void loginUser(final String username, String password, final AsyncCallback<UserSessionData> callback) {
		if(username == null || password == null) callback.onFailure(new AuthenticationException(AUTHENTICATION_MESSAGE));
		LoginRequest loginRequest = getLoginRequest(username, password);
		userAccountService.initiateSession(loginRequest, new AsyncCallback<LoginResponse>() {		
			@Override
			public void onSuccess(LoginResponse session) {
				storeAuthenticationReceipt(username, session.getAuthenticationReceipt());
				revalidateSession(session.getSessionToken(), callback);
			}
			
			
			@Override
			public void onFailure(Throwable caught) {
				callback.onFailure(caught);
			}
		});
	}
	
	public void storeAuthenticationReceipt(String username, String receipt) {
		localStorage.put(username + USER_AUTHENTICATION_RECEIPT, receipt, DateUtils.getYearFromNow().getTime());
	}
	
	public LoginRequest getLoginRequest(String username, String password) {
		LoginRequest request = new LoginRequest();
		request.setUsername(username);
		request.setPassword(password);
		String authenticationReceipt = localStorage.get(username + USER_AUTHENTICATION_RECEIPT);
		request.setAuthenticationReceipt(authenticationReceipt);
		return request;
	}
	
	@Override
	public void revalidateSession(final String token, final AsyncCallback<UserSessionData> callback) {
		setUser(token, callback);
	}

	@Override
	public void logoutUser() {
		// don't actually terminate session, just remove the cookie
		cookies.removeCookie(CookieKeys.USER_LOGIN_TOKEN);
		localStorage.clear();
		initSynapsePropertiesFromServer();
		sessionStorage.clear();
		currentUser = null;
	}

	public void initSynapsePropertiesFromServer() {
		synapseClient.getSynapseProperties(new AsyncCallback<HashMap<String, String>>() {			
			@Override
			public void onSuccess(HashMap<String, String> properties) {
				for (String key : properties.keySet()) {
					localStorage.put(key, properties.get(key), DateUtils.getYearFromNow().getTime());
				}
				localStorage.put(GlobalApplicationStateImpl.PROPERTIES_LOADED_KEY, Boolean.TRUE.toString(), DateUtils.getWeekFromNow().getTime());
			}
			
			@Override
			public void onFailure(Throwable caught) {
			}
		});
	}

	
	private void setUser(String token, final AsyncCallback<UserSessionData> callback) {
		if(token == null) {
			sessionStorage.clear();
			callback.onFailure(new AuthenticationException(AUTHENTICATION_MESSAGE));
			return;
		}
		
		userAccountService.getUserSessionData(token, new AsyncCallback<UserSessionData>() {
			@Override
			public void onSuccess(UserSessionData userSessionData) {
				Date tomorrow = DateUtils.getDayFromNow();
				cookies.setCookie(CookieKeys.USER_LOGGED_IN_RECENTLY, "true", DateUtils.getWeekFromNow());
				cookies.setCookie(CookieKeys.USER_LOGIN_TOKEN, userSessionData.getSession().getSessionToken(), tomorrow);
				currentUser = userSessionData;
				localStorage.put(USER_SESSION_DATA_CACHE_KEY, getUserSessionDataString(currentUser), tomorrow.getTime());
				updateXsrfToken(userSessionData, callback);
			}
			@Override
			public void onFailure(Throwable caught) {
				logoutUser();
				callback.onFailure(new AuthenticationException(AUTHENTICATION_MESSAGE + " " + caught.getMessage()));
			}
		});
	}

	private void updateXsrfToken(final UserSessionData userSessionData, final AsyncCallback<UserSessionData> callback) {
		xsrfTokenService.getNewXsrfToken(new AsyncCallback<XsrfToken>() {
			public void onSuccess(XsrfToken token) {
				gwt.asHasRpcToken(synapseClient).setRpcToken(token);
				localStorage.put(XSRF_TOKEN_KEY, token.getToken(), DateUtils.getDayFromNow().getTime());
				callback.onSuccess(userSessionData);
			}

			public void onFailure(Throwable caught) {
				callback.onFailure(caught);
			}
		});
	}

	public String getUserSessionDataString(UserSessionData session) {
		JSONObjectAdapter adapter = adapterFactory.createNew();
		try {
			session.writeToJSONObject(adapter);
			return adapter.toJSONString();
		} catch (JSONObjectAdapterException e) {
			return null;
		}
	}
	
	public UserSessionData getUserSessionData(String sessionString) {
		try {
			return new UserSessionData(adapterFactory.createNew(sessionString));
		} catch (JSONObjectAdapterException e) {
			return null;
		}
	}
	
	
	@Override
	public void updateCachedProfile(UserProfile updatedProfile){
		if(currentUser != null) {
			currentUser.setProfile(updatedProfile);
			Date tomorrow = DateUtils.getDayFromNow();
			localStorage.put(USER_SESSION_DATA_CACHE_KEY, getUserSessionDataString(currentUser), tomorrow.getTime());
		}
	}
	
	@Override
	public void getTermsOfUse(AsyncCallback<String> callback) {
		userAccountService.getTermsOfUse(callback);
	}

	@Override
	public boolean isLoggedIn() {
		String token = cookies.getCookie(CookieKeys.USER_LOGIN_TOKEN);
		return token != null && !token.isEmpty() && currentUser != null;
	}

	@Override
	public String getCurrentUserPrincipalId() {
		if(currentUser != null) {		
			UserProfile profileObj = currentUser.getProfile();
			if(profileObj != null && profileObj.getOwnerId() != null) {							
				return profileObj.getOwnerId();						
			}
		} 
		return null;
	}
	
	@Override
	public void reloadUserSessionData() {
		String sessionToken = cookies.getCookie(CookieKeys.USER_LOGIN_TOKEN);
		// try to set current user and bundle from session cache
		if (sessionToken != null) {
			// load user session data from session storage
			String sessionStorageString = localStorage.get(USER_SESSION_DATA_CACHE_KEY);
			if (sessionStorageString != null) {
				currentUser = getUserSessionData(sessionStorageString);
			} else {
				logoutUser();
			}
		}
	}

	@Override
	public UserSessionData getCurrentUserSessionData() {
		if (isLoggedIn()) {
			return currentUser;
		} else
			return null;
	}

	@Override
	public String getCurrentUserSessionToken() {
		if(currentUser != null) return currentUser.getSession().getSessionToken();
		else return null;
	}
	
	@Override
	public void signTermsOfUse(boolean accepted, AsyncCallback<Void> callback) {
		userAccountService.signTermsOfUse(getCurrentUserSessionToken(), accepted, callback);
	}
	@Override
	public String getCurrentXsrfToken() {
		return localStorage.get(XSRF_TOKEN_KEY);
	}
}

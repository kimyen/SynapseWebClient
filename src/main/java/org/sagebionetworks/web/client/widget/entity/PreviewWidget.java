package org.sagebionetworks.web.client.widget.entity;

import static org.sagebionetworks.repo.model.EntityBundle.ENTITY;
import static org.sagebionetworks.repo.model.EntityBundle.FILE_HANDLES;

import java.util.Map;

import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Link;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.Versionable;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.util.ContentTypeUtils;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.RequestBuilderWrapper;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.SynapseJSNIUtils;
import org.sagebionetworks.web.client.presenter.EntityPresenter;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.utils.Callback;
import org.sagebionetworks.web.client.widget.WidgetRendererPresenter;
import org.sagebionetworks.web.client.widget.entity.controller.SynapseAlert;
import org.sagebionetworks.web.client.widget.entity.editor.VideoConfigEditor;
import org.sagebionetworks.web.client.widget.entity.renderer.VideoWidget;
import org.sagebionetworks.web.shared.WebConstants;
import org.sagebionetworks.web.shared.WidgetConstants;
import org.sagebionetworks.web.shared.WikiPageKey;

import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class PreviewWidget implements PreviewWidgetView.Presenter, WidgetRendererPresenter {
	public static final String APPLICATION_ZIP = "application/zip";	
	public static final int MAX_LENGTH = 100000;
	public static final int VIDEO_WIDTH = 320;
	public static final int VIDEO_HEIGHT = 180;
	public enum PreviewFileType {
		PLAINTEXT, CODE, ZIP, CSV, IMAGE, NONE, TAB, HTML
	}

	
	PreviewWidgetView view;
	RequestBuilderWrapper requestBuilder;
	SynapseJSNIUtils synapseJSNIUtils;
	SynapseAlert synapseAlert;
	SynapseClientAsync synapseClient;
	AuthenticationController authController;
	VideoWidget videoWidget;
	EntityBundle bundle;
	boolean isFullSize = false;
	@Inject
	public PreviewWidget(PreviewWidgetView view, 
			RequestBuilderWrapper requestBuilder,
			SynapseJSNIUtils synapseJSNIUtils,
			SynapseAlert synapseAlert,
			SynapseClientAsync synapseClient,
			AuthenticationController authController,
			VideoWidget videoWidget) {
		this.view = view;
		this.requestBuilder = requestBuilder;
		this.synapseJSNIUtils = synapseJSNIUtils;
		this.synapseAlert = synapseAlert;
		this.synapseClient = synapseClient;
		this.authController = authController;
		this.videoWidget = videoWidget;
	}
	
	public PreviewFileType getPreviewFileType(PreviewFileHandle previewHandle, FileHandle originalFileHandle) {
		PreviewFileType previewFileType = PreviewFileType.NONE;
		if (previewHandle == null && originalFileHandle != null) {
			String contentType = originalFileHandle.getContentType();
			if (contentType != null) {
				if (DisplayUtils.isRecognizedImageContentType(contentType)) {
					previewFileType = PreviewFileType.IMAGE;	
				} else if (DisplayUtils.isHTML(contentType)) {
					previewFileType = PreviewFileType.HTML;	
				}
			}
		} else if (previewHandle != null && originalFileHandle != null) {
			String contentType = previewHandle.getContentType();
			if (contentType != null) {
				if (DisplayUtils.isRecognizedImageContentType(contentType)) {
					previewFileType = PreviewFileType.IMAGE;
				}
				else if (DisplayUtils.isTextType(contentType)) {
					//some kind of text
					if (DisplayUtils.isHTML(originalFileHandle.getContentType())) {
						 previewFileType = PreviewFileType.HTML;
					}
					else if (ContentTypeUtils.isRecognizedCodeFileName(originalFileHandle.getFileName())){
						previewFileType = PreviewFileType.CODE;
					}
					else if (DisplayUtils.isCSV(contentType)) {
						if (APPLICATION_ZIP.equals(originalFileHandle.getContentType()))
							previewFileType = PreviewFileType.ZIP;
						else
							previewFileType = PreviewFileType.CSV;
					}
					else if (DisplayUtils.isCSV(originalFileHandle.getContentType())){
						previewFileType = PreviewFileType.CSV;
					}
					else if (DisplayUtils.isTAB(contentType) || DisplayUtils.isTAB(originalFileHandle.getContentType())) {
						previewFileType = PreviewFileType.TAB;
					}
					else {
						previewFileType = PreviewFileType.PLAINTEXT;
					}
				}
			}
		}
		return previewFileType;
	}
	
	@Override
	public void configure(WikiPageKey wikiKey,
			Map<String, String> widgetDescriptor,
			Callback widgetRefreshRequired, 
			Long wikiVersionInView) {
		//get the entity id and version from the wiki widget parameters
		view.clear();
		String entityId = widgetDescriptor.get(WidgetConstants.WIDGET_ENTITY_ID_KEY);
		String version = widgetDescriptor.get(WidgetConstants.WIDGET_ENTITY_VERSION_KEY);
		isFullSize = true;
		configure(entityId, version);
	}
	
	public void configure(String entityId, String version) {
		if (version == null && entityId.contains(".")) {
			String[] tokens = entityId.split("\\.");
			entityId = tokens[0];
			version = tokens[1];
		}
		
		int mask = ENTITY  | FILE_HANDLES;
		AsyncCallback<EntityBundle> entityBundleCallback = new AsyncCallback<EntityBundle>() {
			@Override
			public void onFailure(Throwable caught) {
				view.addSynapseAlertWidget(synapseAlert.asWidget());
				synapseAlert.handleException(caught);
			}
			@Override
			public void onSuccess(EntityBundle bundle) {
				configure(bundle);
			}
		};
		if (EntityPresenter.isValidEntityId(entityId)) {
			if (version == null) {
				synapseClient.getEntityBundle(entityId, mask, entityBundleCallback);
			} else {
				synapseClient.getEntityBundleForVersion(entityId, Long.parseLong(version), mask, entityBundleCallback);	
			}
		} else {
			view.addSynapseAlertWidget(synapseAlert.asWidget());
			synapseAlert.showError("Preview error: " + entityId + " does not appear to be a valid Synapse identifier.");
		}
	}
	
	public void configure(EntityBundle bundle) {
		this.bundle = bundle;
		view.clear();
		//if not logged in, don't even try to load the preview.  Just direct user to log in.
		if (!synapseAlert.isUserLoggedIn()) {
			view.addSynapseAlertWidget(synapseAlert.asWidget());
			synapseAlert.showLogin();
		} else if (bundle != null) {
			// SWC-2652: follow Link
			if (bundle.getEntity() instanceof Link) {
				// configure based on target
				Reference ref = ((Link)bundle.getEntity()).getLinksTo();
				String targetVersion = ref.getTargetVersionNumber() == null ? null : ref.getTargetVersionNumber() + "";
				configure(ref.getTargetId(), targetVersion);
				return;
			}
			if (!(bundle.getEntity() instanceof FileEntity)) {
				//not a file!
				view.addSynapseAlertWidget(synapseAlert.asWidget());
				synapseAlert.showError("Preview unavailable for \"" + bundle.getEntity().getName() + "\" ("+bundle.getEntity().getId()+")");
			} else {
				renderFilePreview(bundle);
			}
		}
	}
	
	public void renderHTML(String modifiedBy, final String content) {
		synapseClient.isUserAllowedToRenderHTML(modifiedBy, new AsyncCallback<Boolean>() {
			@Override
			public void onFailure(Throwable caught) {
				String escapedContent = SafeHtmlUtils.htmlEscapeAllowEntities(content);
				if (escapedContent.length() > 500000) {
					escapedContent = escapedContent.substring(0, 500000) + "\n...";
				}
				if (isFullSize) {
					view.setTextPreviewFull(escapedContent);
				} else {
					view.setTextPreview(escapedContent);
				}
			}
			
			@Override
			public void onSuccess(Boolean trustedUser) {
				if (trustedUser) {
					if (isFullSize) {
						view.setHTMLFull(content);
					} else {
						view.setHTML(content);
					}
				} else {
					// is the sanitized version the same as the original??
					String newHtml = synapseJSNIUtils.sanitizeHtml(content);
					if (content.equals(newHtml)) {
						if (isFullSize) {
							view.setHTMLFull(content);	
						} else {
							view.setHTML(content);
						}
						
					} else {
						onFailure(new Exception());	
					}
				}
			}
		});
	}
	
	private void renderFilePreview(EntityBundle bundle) {
		PreviewFileHandle handle = DisplayUtils.getPreviewFileHandle(bundle);
		FileHandle originalFileHandle = DisplayUtils.getFileHandle(bundle);
		final PreviewFileType previewType = getPreviewFileType(handle, originalFileHandle);
		String xsrfToken = authController.getCurrentXsrfToken();
		if (previewType != PreviewFileType.NONE) {
			final FileEntity fileEntity = (FileEntity)bundle.getEntity();
			if (previewType == PreviewFileType.IMAGE) {
				//add a html panel that contains the image src from the attachments server (to pull asynchronously)
				//create img
				boolean hasPreviewFileHandle = handle != null;
				String fullFileUrl = DisplayUtils.createFileEntityUrl(synapseJSNIUtils.getBaseFileHandleUrl(), fileEntity.getId(), ((Versionable)fileEntity).getVersionNumber(), false, xsrfToken);
				String previewFileUrl = DisplayUtils.createFileEntityUrl(synapseJSNIUtils.getBaseFileHandleUrl(), fileEntity.getId(),  ((Versionable)fileEntity).getVersionNumber(), hasPreviewFileHandle, xsrfToken);
				if (isFullSize) {
					view.setImagePreviewFull(fullFileUrl);
				} else {
					view.setImagePreview(fullFileUrl, previewFileUrl);	
				}
				
			} else {
				// if HTML, get the full file contents
				view.showLoading();
				boolean isGetPreviewFile = PreviewFileType.HTML != previewType;
				String contentType = isGetPreviewFile ? handle.getContentType() : originalFileHandle.getContentType();
				final String fileCreatedBy = originalFileHandle.getCreatedBy();
				//must be a text type of some kind
				//try to load the text of the preview, if available
				requestBuilder.configure(RequestBuilder.GET, 
						DisplayUtils.createFileEntityUrl(synapseJSNIUtils.getBaseFileHandleUrl(), fileEntity.getId(),  ((Versionable)fileEntity).getVersionNumber(), isGetPreviewFile, false, xsrfToken));
				requestBuilder.setHeader(WebConstants.CONTENT_TYPE, contentType);
				
				try {
					requestBuilder.sendRequest(null, new RequestCallback() {
						public void onError(final Request request, final Throwable e) {
							view.addSynapseAlertWidget(synapseAlert.asWidget());
							synapseAlert.handleException(e);
						}
						public void onResponseReceived(final Request request, final Response response) {
							//add the response text
						int statusCode = response.getStatusCode();
							if (statusCode == Response.SC_OK) {
								String responseText = response.getText();
								if (responseText != null && responseText.length() > 0) {
									if (previewType == PreviewFileType.HTML) {
										renderHTML(fileCreatedBy, responseText);
									} else {
										if (responseText.length() > MAX_LENGTH) {
											responseText = responseText.substring(0, MAX_LENGTH) + "...";
										}
										
										if (PreviewFileType.CODE == previewType) {
											String codePreview = SafeHtmlUtils.htmlEscapeAllowEntities(responseText);
											if (isFullSize) {
												view.setCodePreviewFull(codePreview);	
											} else {
												view.setCodePreview(codePreview);
											}
										} 
										else if (PreviewFileType.CSV == previewType) {
											if (isFullSize) {
												view.setTablePreviewFull(responseText, ",");	
											} else {
												view.setTablePreview(responseText, ",");
											}
										}
											
										else if (PreviewFileType.TAB == previewType) {
											if (isFullSize) {
												view.setTablePreviewFull(responseText, "\\t");
											} else {
												view.setTablePreview(responseText, "\\t");
											}
										}
											
										else if (PreviewFileType.PLAINTEXT == previewType || PreviewFileType.ZIP == previewType) {
											if (isFullSize) {
												view.setTextPreviewFull(SafeHtmlUtils.htmlEscapeAllowEntities(responseText));	
											} else {
												view.setTextPreview(SafeHtmlUtils.htmlEscapeAllowEntities(responseText));
											}
										}
									}
								}
							}
						}
					});
				} catch (final Exception e) {
					view.addSynapseAlertWidget(synapseAlert.asWidget());
					synapseAlert.handleException(e);
				}
			}
		} 
		else if (originalFileHandle != null && VideoConfigEditor.isRecognizedVideoFileName(originalFileHandle.getFileName())) {
			videoWidget.configure(bundle.getEntity().getId(), originalFileHandle.getFileName(), VIDEO_WIDTH, VIDEO_HEIGHT);
			view.setPreviewWidget(videoWidget.asWidget());
		}
	}
	
	
	
	public Widget asWidget() {
		return view.asWidget();
	}
	
	@Override
	public void imagePreviewLoadFailed(ErrorEvent e) {
		//show the load error
		view.addSynapseAlertWidget(synapseAlert.asWidget());
		synapseAlert.showError("Unable to load image preview");
	}
	
	public void addStyleName(String style) {
		view.addStyleName(style);
	}
}

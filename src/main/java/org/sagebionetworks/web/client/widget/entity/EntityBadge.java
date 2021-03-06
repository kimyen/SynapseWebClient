package org.sagebionetworks.web.client.widget.entity;

import static org.sagebionetworks.repo.model.EntityBundle.ANNOTATIONS;
import static org.sagebionetworks.repo.model.EntityBundle.BENEFACTOR_ACL;
import static org.sagebionetworks.repo.model.EntityBundle.ENTITY;
import static org.sagebionetworks.repo.model.EntityBundle.FILE_HANDLES;
import static org.sagebionetworks.repo.model.EntityBundle.PERMISSIONS;
import static org.sagebionetworks.repo.model.EntityBundle.ROOT_WIKI_ID;
import static org.sagebionetworks.repo.model.EntityBundle.THREAD_COUNT;

import java.util.List;

import org.gwtbootstrap3.client.ui.constants.ButtonSize;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.EntityTypeUtils;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.SynapseJSNIUtils;
import org.sagebionetworks.web.client.events.EntityUpdatedEvent;
import org.sagebionetworks.web.client.events.EntityUpdatedHandler;
import org.sagebionetworks.web.client.place.Synapse;
import org.sagebionetworks.web.client.utils.Callback;
import org.sagebionetworks.web.client.utils.CallbackP;
import org.sagebionetworks.web.client.widget.SynapseWidgetPresenter;
import org.sagebionetworks.web.client.widget.entity.annotation.AnnotationTransformer;
import org.sagebionetworks.web.client.widget.entity.dialog.Annotation;
import org.sagebionetworks.web.client.widget.entity.file.FileDownloadButton;
import org.sagebionetworks.web.client.widget.lazyload.LazyLoadHelper;
import org.sagebionetworks.web.client.widget.user.UserBadge;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class EntityBadge implements EntityBadgeView.Presenter, SynapseWidgetPresenter {
	
	private EntityBadgeView view;
	private GlobalApplicationState globalAppState;
	private EntityHeader entityHeader;
	private AnnotationTransformer transformer;
	private UserBadge modifiedByUserBadge;
	private SynapseJSNIUtils synapseJSNIUtils;
	private SynapseClientAsync synapseClient;
	private CallbackP<String> customEntityClickHandler;
	private FileDownloadButton fileDownloadButton;
	private LazyLoadHelper lazyLoadHelper;
	@Inject
	public EntityBadge(EntityBadgeView view, 
			GlobalApplicationState globalAppState,
			AnnotationTransformer transformer,
			UserBadge modifiedByUserBadge,
			SynapseJSNIUtils synapseJSNIUtils,
			SynapseClientAsync synapseClient,
			FileDownloadButton fileDownloadButton,
			LazyLoadHelper lazyLoadHelper) {
		this.view = view;
		this.globalAppState = globalAppState;
		this.transformer = transformer;
		this.modifiedByUserBadge = modifiedByUserBadge;
		this.synapseJSNIUtils = synapseJSNIUtils;
		this.synapseClient = synapseClient;
		this.fileDownloadButton = fileDownloadButton;
		this.lazyLoadHelper = lazyLoadHelper;
		view.setPresenter(this);
		view.setModifiedByWidget(modifiedByUserBadge.asWidget());
		Callback loadDataCallback = new Callback() {
			@Override
			public void invoke() {
				getEntityBundle();
			}
		};
		
		lazyLoadHelper.configure(loadDataCallback, view);
		fileDownloadButton.setSize(ButtonSize.EXTRA_SMALL);
		fileDownloadButton.setEntityUpdatedHandler(new EntityUpdatedHandler() {
			@Override
			public void onPersistSuccess(EntityUpdatedEvent event) {
				getEntityBundle();
			}
		});
	}
	
	public void getEntityBundle() {
		int partsMask = ENTITY | ANNOTATIONS | ROOT_WIKI_ID | FILE_HANDLES | PERMISSIONS | BENEFACTOR_ACL | THREAD_COUNT;
		synapseClient.getEntityBundle(entityHeader.getId(), partsMask, new AsyncCallback<EntityBundle>() {
			@Override
			public void onFailure(Throwable caught) {
				view.showErrorIcon();
				view.setError(caught.getMessage());
			}
			public void onSuccess(EntityBundle eb) {
				setEntityBundle(eb);
			};
		});
	}
	
	public void configure(EntityHeader header) {
		entityHeader = header;
		view.setEntity(header);
		view.setIcon(EntityTypeUtils.getIconTypeForEntityClassName(header.getType()));
		lazyLoadHelper.setIsConfigured();
	}
	
	public void clearState() {
	}

	@Override
	public Widget asWidget() {
		return view.asWidget();
	}
	
	public void setEntityBundle(EntityBundle eb) {
		Annotations annotations = eb.getAnnotations();
		String rootWikiId = eb.getRootWikiId();
		List<FileHandle> handles = eb.getFileHandles();
		List<Annotation> annotationList = transformer.annotationsToList(annotations);
		if (!annotationList.isEmpty()) {
			view.showAnnotationsIcon();
			view.setAnnotations(getAnnotationsHTML(annotationList));
		}
		
		view.setSize(getContentSize(handles));
		view.setMd5(getContentMd5(handles));
		if(eb.getPermissions().getCanPublicRead()) {
			view.showPublicIcon();
		} else {
			view.showPrivateIcon();
		}
		boolean hasLocalSharingSettings = eb.getBenefactorAcl().getId().equals(entityHeader.getId());
		if (hasLocalSharingSettings) {
			view.showSharingSetIcon();
		}
		
		if (DisplayUtils.isDefined(rootWikiId)) {
			view.showHasWikiIcon();
		}
		
		if (eb.getEntity() instanceof FileEntity) {
			fileDownloadButton.configure(eb);
			fileDownloadButton.setClientsHelpVisible(false);
			view.setFileDownloadButton(fileDownloadButton.asWidget());
		}
		
		view.setDiscussionThreadIconVisible(eb.getThreadCount() > 0);
		
		if (eb.getEntity().getModifiedBy() != null) {
			modifiedByUserBadge.configure(eb.getEntity().getModifiedBy());
			view.setModifiedByWidgetVisible(true);
		} else {
			view.setModifiedByWidgetVisible(false);
		}
		
		if (eb.getEntity().getModifiedOn() != null) {
			String modifiedOnString = synapseJSNIUtils.convertDateToSmallString(eb.getEntity().getModifiedOn());
			view.setModifiedOn(modifiedOnString);
		} else {
			view.setModifiedOn("");
		}
	}
	
	public String getContentSize(List<FileHandle> handles) {
		if (handles != null) {
			for (FileHandle handle: handles) {
				if (!(handle instanceof PreviewFileHandle)) {
					Long contentSize = handle.getContentSize();
					if (contentSize != null && contentSize > 0) {
						return view.getFriendlySize(contentSize, true);
					}
				}
			}
		}
		return "";
	}
	

	public String getContentMd5(List<FileHandle> handles) {
		if (handles != null) {
			for (FileHandle handle: handles) {
				if (!(handle instanceof PreviewFileHandle)) {
					return handle.getContentMd5();
				}
			}
		}
		return "";
	}
	
	/**
	 * Adds annotations and wiki status values to the given key value display
	 * @param keyValueDisplay
	 * @param annotations
	 */
	public String getAnnotationsHTML(List<Annotation> annotations) {
		StringBuilder sb = new StringBuilder();
		for (Annotation annotation : annotations) {
			String key = annotation.getKey();
			sb.append("<strong>");
			sb.append(SafeHtmlUtils.htmlEscapeAllowEntities(key));
			sb.append("</strong>&nbsp;");
			sb.append(SafeHtmlUtils.htmlEscapeAllowEntities(transformer.getFriendlyValues(annotation)));
			sb.append("<br />");
		}
		return sb.toString();
	}
	
	
	public void setEntityClickedHandler(CallbackP<String> callback) {
		customEntityClickHandler = callback;
	}
	
	@Override
	public void entityClicked(EntityHeader entityHeader) {
		showLoadingIcon();
		if (customEntityClickHandler == null) {
			globalAppState.getPlaceChanger().goTo(new Synapse(entityHeader.getId()));	
		} else {
			customEntityClickHandler.invoke(entityHeader.getId());
		}
	}
	
	public void hideLoadingIcon() {
		view.hideLoadingIcon();
	}

	public void showLoadingIcon() {
		view.showLoadingIcon();
	}
	
	public EntityHeader getHeader() {
		return entityHeader;
	}
	
	public void setClickHandler(ClickHandler handler) {
		modifiedByUserBadge.setCustomClickHandler(handler);
		view.setClickHandler(handler);
	}
	
	public String getEntityId() {
		return entityHeader.getId();
	}
}

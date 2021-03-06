package org.sagebionetworks.web.client.widget.entity.browse;

import java.util.ArrayList;
import java.util.List;

import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.DropDownMenu;
import org.gwtbootstrap3.client.ui.InlineRadio;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.Radio;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.html.Div;
import org.gwtbootstrap3.client.ui.html.Text;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.widget.entity.EntitySearchBox;
import org.sagebionetworks.web.client.widget.entity.browse.MyEntitiesBrowser.SelectedHandler;
import org.sagebionetworks.web.shared.PaginatedResults;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class EntityFinderViewImpl implements EntityFinderView {
	
	public interface Binder extends UiBinder<Widget, EntityFinderViewImpl> {}
	
	private Presenter presenter;
	private MyEntitiesBrowser myEntitiesBrowser;	
	private EntitySearchBox entitySearchBox;
	private EntityFinderArea currentArea;
	
	//the modal dialog
	private Modal modal;
	
	@UiField
	Button okButton;
	@UiField
	Button cancelButton;
	
	@UiField
	SimplePanel browseMyEntitiesContainer;
	@UiField
	SimplePanel searchContainer;
	@UiField
	SimplePanel enterSynapseIdContainer;
	@UiField
	SimplePanel enterSynapseMultiIdContainer;
	@UiField
	SimplePanel myEntitiesBrowserContainer;
	@UiField
	FlowPanel entitySearchWidgetContainer;
	@UiField
	SimplePanel entitySearchBoxContainer;
	
	@UiField
	FlowPanel enterIdWidgetContainer;
	@UiField
	FlowPanel enterMultiIdWidgetContainer;
	@UiField
	FlowPanel versionUI;
	@UiField
	Text selectedText;
	@UiField
	Button versionDropDownButton;
	@UiField
	DropDownMenu versionDropDownMenu;
	
	@UiField
	Radio currentVersionRadio;
	@UiField
	Radio currentVersionRadioShowingVersions;
	@UiField
	InlineRadio specificVersionRadio;
	
	@UiField
	TextBox synapseIdTextBox;
	@UiField
	Button lookupSynapseIdButton;
	@UiField
	TextBox synapseMultiIdTextBox;
	@UiField
	Button lookupSynapseMultiIdButton;
	@UiField
	Div synAlertContainer;
	
	private List<Reference> selectedRef; // DO NOT SET THIS DIRECTLY, use setSelected... methods
	private Long maxVersion = 0L;
	boolean isFinderComponentsInitialized;
	@Inject
	public EntityFinderViewImpl(Binder binder,
			SageImageBundle sageImageBundle,
			IconsImageBundle iconsImageBundle, 
			MyEntitiesBrowser myEntitiesBrowser, 
			EntitySearchBox entitySearchBox) {
		this.modal = (Modal)binder.createAndBindUi(this);
		this.myEntitiesBrowser = myEntitiesBrowser;
		this.entitySearchBox = entitySearchBox;
		selectedRef = new ArrayList<Reference>();
		okButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				presenter.okClicked();
			}
		});
		cancelButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				modal.hide();
			}
		});
		
		ClickHandler currentVersionClickHandler = new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				versionDropDownButton.setEnabled(false);
    			setSelectedVersion(null);
    			updateSelectedView();		    			
	    	}
		};
		currentVersionRadio.addClickHandler(currentVersionClickHandler);
		currentVersionRadioShowingVersions.addClickHandler(currentVersionClickHandler);
		specificVersionRadio.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				versionDropDownButton.setEnabled(true);
				setSelectedVersion(maxVersion);
				updateSelectedView();
			}
		});
		isFinderComponentsInitialized = false;
	}
	
	@Override
	public void initFinderComponents(EntityFilter filter) {
		if (!isFinderComponentsInitialized || !filter.equals(myEntitiesBrowser.getEntityFilter())) {
			isFinderComponentsInitialized = true;
			createMyEntityBrowserWidget();		
			createSearchBoxWidget();			
			createEnterIdWidget();
			createEnterMultiIdWidget();
			myEntitiesBrowser.setEntityFilter(filter);
		}
	}
	
	private void hideAllRightTopWidgets() {
		myEntitiesBrowserContainer.setVisible(false);
		entitySearchWidgetContainer.setVisible(false);
		enterIdWidgetContainer.setVisible(false);
		enterMultiIdWidgetContainer.setVisible(false);
	}
	
	private void showTopRightContainer(Widget container, EntityFinderArea newArea) {
		versionUI.setVisible(false);
		versionDropDownMenu.clear();
		hideAllRightTopWidgets();
		container.setVisible(true);
		currentArea = newArea;
	}
	
	@Override 
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}
		
	@Override
	public void showErrorMessage(String message) {
		DisplayUtils.showErrorMessage(message);
	}

	@Override
	public void showLoading() {
	}

	@Override
	public void showInfo(String title, String message) {
		DisplayUtils.showInfo(title, message);
	}

	@Override
	public void clear() {
		presenter.clearSelectedEntities();
		updateSelectedView();
		selectedRef.clear();
		myEntitiesBrowser.clearState();
		myEntitiesBrowser.refresh();
		synapseIdTextBox.clear();
		entitySearchBox.clearSelection();
		synapseMultiIdTextBox.clear();
	}

	/*
	 * Private Methods
	 */
	private void createMyEntityBrowserWidget() {
		// configure tree browsers
		EntityTreeBrowser tree;
		tree = myEntitiesBrowser.getEntityTreeBrowser();
		tree.makeSelectable();
		
		tree = myEntitiesBrowser.getFavoritesTreeBrowser();
		tree.makeSelectable();
		
		myEntitiesBrowser.setEntitySelectedHandler(new SelectedHandler() {					
			@Override
			public void onSelection(String selectedEntityId) {
				setSelectedId(selectedEntityId);
				updateSelectedView();
				createVersionChooser(selectedEntityId);
			}
		});
		myEntitiesBrowserContainer.clear();
		myEntitiesBrowserContainer.setWidget(myEntitiesBrowser.asWidget());

		// list entry
		Widget entry = createNewLeftEntry(DisplayConstants.BROWSE_MY_ENTITIES, new ClickHandler(){
	        @Override
	        public void onClick(ClickEvent event) {
	        	setBrowseAreaVisible();
	        }
	    });
		browseMyEntitiesContainer.clear();
		browseMyEntitiesContainer.setWidget(entry);
	}
	@Override
	public void setBrowseAreaVisible() {
		showTopRightContainer(myEntitiesBrowserContainer, EntityFinderArea.BROWSE);
	}
	@Override
	public void setSearchAreaVisible() {
		showTopRightContainer(entitySearchWidgetContainer, EntityFinderArea.SEARCH);
	}
	@Override
	public void setSynapseIdAreaVisible() {
		showTopRightContainer(enterIdWidgetContainer, EntityFinderArea.SYNAPSE_ID);
	}
	@Override
	public void setSynapseMultiIdAreaVisible() {
		showTopRightContainer(enterMultiIdWidgetContainer, EntityFinderArea.SYNAPSE_MULTI_ID);
	}
	@Override
	public EntityFinderArea getCurrentArea() {
		return currentArea;
	}
	private void createSearchBoxWidget() {
		// Search Widget
		entitySearchBox.setEntitySelectedHandler(new EntitySearchBox.EntitySelectedHandler() {			
			@Override
			public void onSelected(String entityId, String name, List<VersionInfo> versions) {
				setSelectedId(entityId);
				updateSelectedView();
				createVersionChooser(entityId);
			}
		}, false);
		entitySearchBoxContainer.add(entitySearchBox.asWidget());
		
		// list entry
		Widget entry = createNewLeftEntry(DisplayConstants.LABEL_SEARCH, new ClickHandler(){
	        @Override
	        public void onClick(ClickEvent event) {
	        	setSearchAreaVisible();
	        }
	    });
		searchContainer.clear();
		searchContainer.setWidget(entry);
	}	

	private void createEnterIdWidget() {
		synapseIdTextBox.addKeyDownHandler(new KeyDownHandler() {
			@Override
			public void onKeyDown(KeyDownEvent event) {
				if(event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
					lookupSynapseIdButton.click();
				}
			}
		});
		lookupSynapseIdButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				presenter.lookupEntity(synapseIdTextBox.getValue(), new AsyncCallback<List<EntityHeader>>() {
					@Override
					public void onFailure(Throwable caught) {
						
					}
					@Override
					public void onSuccess(List<EntityHeader> result) {
						String entityId = result.get(0).getId();
						setSelectedId(entityId);
						updateSelectedView();
						createVersionChooser(entityId);
						
					}
				});
			}
		});
		
		// list entry		
		final Widget entry = createNewLeftEntry(DisplayConstants.ENTER_SYNAPSE_ID, new ClickHandler(){
	        @Override
	        public void onClick(ClickEvent event) {
	        	setSynapseIdAreaVisible();
	        }
	    });
		enterSynapseIdContainer.clear();
		enterSynapseIdContainer.setWidget(entry);
	}
	
	private void createEnterMultiIdWidget() {
		synapseMultiIdTextBox.addKeyDownHandler(new KeyDownHandler() {
			@Override
			public void onKeyDown(KeyDownEvent event) {
				if(event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
					lookupSynapseIdButton.click();
				}
			}
		});
		lookupSynapseMultiIdButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				presenter.lookupEntity(synapseMultiIdTextBox.getValue(), new AsyncCallback<List<EntityHeader>>() {
					@Override
					public void onFailure(Throwable caught) {
						
					}
					@Override
					public void onSuccess(List<EntityHeader> result) {
						setSelectedId(result);
						updateSelectedView();
					}
				});
			}
		});
		
		// list entry		
		final Widget entry = createNewLeftEntry("Enter List of Synapse Ids", new ClickHandler(){
	        @Override
	        public void onClick(ClickEvent event) {
	        	setSynapseMultiIdAreaVisible();
	        }
	    });
		
		enterSynapseMultiIdContainer.clear();
		enterSynapseMultiIdContainer.setWidget(entry);
	}
				
	private Widget createNewLeftEntry(String name, ClickHandler handler) {
		SimplePanel p = new SimplePanel();
	    p.sinkEvents(Event.ONCLICK);	    
	    p.addHandler(handler, ClickEvent.getType());
		
		p.addStyleName("last sidebarMenu");		
		p.setWidget(new HTML(name));
		return p;
	}
	
	private void updateSelectedView() {
		String display = "";
		for (Reference ref : selectedRef) {
			display += DisplayUtils.createEntityVersionString(ref) + ", ";
		}
		selectedText.setText(display.substring(0, display.length() - 2));
	}

	private void createVersionChooser(String entityId) {
		versionUI.setVisible(false);
		boolean showVersions = presenter.showVersions();
		specificVersionRadio.setEnabled(showVersions);
		if (showVersions) {
			currentVersionRadio.setVisible(false);
			currentVersionRadioShowingVersions.setVisible(true);
			currentVersionRadioShowingVersions.setValue(true);
		} else {
			currentVersionRadioShowingVersions.setVisible(false);
			currentVersionRadio.setVisible(true);
			currentVersionRadio.setValue(true);
		}
		
		presenter.loadVersions(entityId);
		versionUI.setVisible(true);
	}

	private String getVersionLabel(VersionInfo info, boolean isCurrent) {
		String current = isCurrent ? " [" + DisplayConstants.CURRENT + "]" : "";
		if(info.getVersionLabel().equals(info.getVersionNumber().toString())) {
			return info.getVersionNumber().toString() + current;
		} else {
			return info.getVersionLabel() + " (" + info.getVersionNumber() + ")" + current;
		}  
	}
	
	@Override
	public void setVersions(List<VersionInfo> versions) {
		if(versions == null) return;
		versionDropDownMenu.clear();
		versionDropDownButton.setEnabled(false);
		maxVersion = 0L;
		//find maxVersion
		for (VersionInfo info : versions) {
			if(info.getVersionNumber() > maxVersion) {
				maxVersion = info.getVersionNumber();
			}
		}
		//now add items to the drop down menu
		for (final VersionInfo info : versions) {
			boolean isCurrent = info.getVersionNumber() == maxVersion;
			final String text = getVersionLabel(info, isCurrent);
			AnchorListItem item = new AnchorListItem(text);
			item.setMarginLeft(10);
			ClickHandler itemClicked = new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					versionDropDownButton.setText(text);
					setSelectedVersion(info.getVersionNumber());
					updateSelectedView();
				}
			};
			item.addClickHandler(itemClicked);
			versionDropDownMenu.add(item);
			if (isCurrent) {
				//set text to the current version by default
				versionDropDownButton.setText(text);
			}
		}
	}

	private void setSelectedId(String entityId) {
		// clear out selection and set new id
		selectedRef.clear();
		Reference ref = new Reference();
		ref.setTargetId(entityId);
		ref.setTargetVersionNumber(null);
		selectedRef.add(ref);
		presenter.setSelectedEntity(ref);	
	}
	
	private void setSelectedId(List<EntityHeader> result) {
		selectedRef.clear();
		for (EntityHeader eh : result) {
			Reference ref = new Reference();
			ref.setTargetId(eh.getId());
			ref.setTargetVersionNumber(eh.getVersionNumber());
			selectedRef.add(ref);
		}
		presenter.setSelectedEntities(selectedRef);
	}
	
	private void setSelectedVersion(Long versionNumber) {
		selectedRef.get(0).setTargetVersionNumber(versionNumber);
		presenter.setSelectedEntity(selectedRef.get(0));
	}
	
	
	@Override
	public void show() {
		//show modal
		modal.show();
	}
	
	@Override
	public void hide() {
		modal.hide();
	}
	
	@Override
	public Widget asWidget() {
		return modal;
	}
	
	@Override
	public boolean isShowing() {
		return modal.isAttached() && modal.isVisible();
	}
	
	@Override
	public void setSynAlert(Widget w) {
		synAlertContainer.clear();
		synAlertContainer.add(w);
	}

	public void setMultiVisible(boolean b) {
		createEnterMultiIdWidget();
	}
}








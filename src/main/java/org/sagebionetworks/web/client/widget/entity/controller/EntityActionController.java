package org.sagebionetworks.web.client.widget.entity.controller;

import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.web.client.events.EntityUpdatedHandler;
import org.sagebionetworks.web.client.widget.entity.menu.v2.ActionMenuWidget;

import com.google.gwt.user.client.ui.IsWidget;

/**
 * Abstraction for a controller that executes basic entity actions from events
 * generated by a shared action menu.
 * 
 * @author jhill
 *
 */
public interface EntityActionController extends IsWidget {

	/**
	 * Configure this controller for a given entity
	 * 
	 * @param actionMenu
	 * @param entityBundle
	 * @param isCurrentVersion TODO
	 */
	public void configure(ActionMenuWidget actionMenu, EntityBundle entityBundle, boolean isCurrentVersion, String wikiPageKey, EntityUpdatedHandler handler);

	/**
	 * Delete action selected
	 */
	public void onDeleteEntity();
	
	/**
	 * Share dialog selected.
	 */
	public void onShare();
	
	/**
	 * Annotations toggled
	 * @param shown
	 */
	void onAnnotationsToggled(boolean shown);

	void onFileHistoryToggled(boolean shown);

}

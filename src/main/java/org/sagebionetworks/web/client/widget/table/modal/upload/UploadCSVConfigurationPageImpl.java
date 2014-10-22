package org.sagebionetworks.web.client.widget.table.modal.upload;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewResult;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.widget.asynch.AsynchronousProgressHandler;
import org.sagebionetworks.web.client.widget.asynch.AsynchronousProgressWidget;
import org.sagebionetworks.web.client.widget.asynch.JobTrackingWidget;
import org.sagebionetworks.web.shared.asynch.AsynchType;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class UploadCSVConfigurationPageImpl implements UploadCSVConfigurationPage, UploadCSVConfigurationView.Presenter{
	
	public static final String CREATING_TABLE_COLUMNS = "Creating table columns...";
	public static final String CREATING_THE_TABLE = "Creating the table...";
	public static final String ANALYZING_FILE = "Analyzing file...";
	public static final String APPLYING_CSV_TO_THE_TABLE = "Applying CSV to the Table...";
	public static final String PREPARING_A_PREVIEW = "Preparing a preview...";
	public static final String CREATE = "Create";
	// Injected dependencies.
	UploadCSVConfigurationView view;
	SynapseClientAsync synapseClient;
	UploadPreviewWidget uploadPreviewWidget;
	JobTrackingWidget jobTrackingWidget;

	// dynamic data fields
	ContentTypeDelimiter type;
	String fileName;
	String parentId;
	String fileHandleId;
	ModalPresenter presenter;
	
	@Inject
	public UploadCSVConfigurationPageImpl(UploadCSVConfigurationView view, SynapseClientAsync synapseClient, UploadPreviewWidget uploadPreviewWidget, JobTrackingWidget jobTrackingWidget){
		this.view = view;
		this.synapseClient = synapseClient;
		this.uploadPreviewWidget = uploadPreviewWidget;
		this.jobTrackingWidget = jobTrackingWidget;
		view.setPresenter(this);
		this.view.setPreviewWidget(this.uploadPreviewWidget.asWidget());
		this.view.setTrackingWidget(this.jobTrackingWidget.asWidget());
	}

	@Override
	public Widget asWidget() {
		return this.view.asWidget();
	}

	@Override
	public void configure(ContentTypeDelimiter type, String fileName, String parentId, String fileHandleId) {
		this.type = type;
		this.fileName = fileName;
		this.parentId = parentId;
		this.fileHandleId = fileHandleId;
	}

	@Override
	public void onPrimary() {
		// Get the columns and create them
		createColumns();
	}

	private void createColumns() {
		try{
			presenter.setLoading(true);
			view.showSpinner(CREATING_TABLE_COLUMNS);
			List<ColumnModel> value = this.uploadPreviewWidget.getCurrentModel();
			// Create the columns
			synapseClient.createTableColumns(value, new AsyncCallback<List<ColumnModel>>(){

				@Override
				public void onFailure(Throwable caught) {
					view.hideSpinner();
					presenter.setErrorMessage(caught.getMessage());
				}

				@Override
				public void onSuccess(List<ColumnModel> schema) {
					createTable(schema);
				}} );
		}catch(IllegalArgumentException e){
			view.hideSpinner();
			presenter.setErrorMessage(e.getMessage());
		}
	}
	
	public void createTable(List<ColumnModel> schema){
		view.showSpinner(CREATING_THE_TABLE);
		// Get the column model ids.
		List<String> columnIds = new ArrayList<String>(schema.size());
		for(ColumnModel cm: schema){
			columnIds.add(cm.getId());
		}
		TableEntity table = new TableEntity();
		table.setColumnIds(columnIds);
		table.setParentId(this.parentId);
		table.setName(this.view.getTableName());
		// Create the table
		synapseClient.createTableEntity(table, new AsyncCallback<TableEntity>() {
			
			@Override
			public void onSuccess(TableEntity result) {
				view.hideSpinner();
				applyCSVToTable(result);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.hideSpinner();
				presenter.setErrorMessage(caught.getMessage());
			}
		});
	}
	
	/**
	 * Apply the CSV to the table.
	 * @param table
	 */
	public void applyCSVToTable(final TableEntity table){
		// Get the preview request.
		UploadToTableRequest request = uploadPreviewWidget.getUploadRequest();
		request.setTableId(table.getId());
		this.view.setTrackerVisible(true);
		jobTrackingWidget.startAndTrackJob(APPLYING_CSV_TO_THE_TABLE, false, AsynchType.TableCSVUpload, request, new AsynchronousProgressHandler(){

			@Override
			public void onCancel() {
				presenter.onCancel();
			}

			@Override
			public void onComplete(AsynchronousResponseBody response) {
				// At this point the table should be created with CSV applied.
				presenter.onTableCreated(table);
			}

			@Override
			public void onFailure(Throwable failure) {
				presenter.setErrorMessage(failure.getMessage());
			}});
	}

	@Override
	public void setModalPresenter(final ModalPresenter presenter) {
		this.presenter = presenter;
		this.view.setTableName(fileName);
		this.view.setPreviewVisible(false);
		this.view.setTrackerVisible(true);
		this.presenter.setPrimaryButtonText(CREATE);
		this.presenter.setInstructionMessage(PREPARING_A_PREVIEW);
		this.presenter.setLoading(true);
		// Setup the preview request
		final UploadToTablePreviewRequest previewRequest = new UploadToTablePreviewRequest();
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setSeparator(type.getDelimiter());
		previewRequest.setCsvTableDescriptor(descriptor);
		previewRequest.setUploadFileHandleId(fileHandleId);
		previewRequest.setDoFullFileScan(true);
		// Start the job
		jobTrackingWidget.startAndTrackJob(ANALYZING_FILE, false, AsynchType.TableCSVUploadPreview, previewRequest, new AsynchronousProgressHandler() {
			
			@Override
			public void onFailure(Throwable failure) {
				presenter.setErrorMessage(failure.getMessage());
			}
			
			@Override
			public void onComplete(AsynchronousResponseBody response) {
				previewCreated(previewRequest, (UploadToTablePreviewResult) response);
			}
			
			@Override
			public void onCancel() {
				presenter.onCancel();
			}
		});
	}
	
	private void previewCreated(UploadToTablePreviewRequest previewRequest, UploadToTablePreviewResult results){
		this.presenter.setInstructionMessage("");
		this.view.setTrackerVisible(false);
		this.uploadPreviewWidget.configure(previewRequest, results);
		this.view.setPreviewVisible(true);
		this.presenter.setLoading(false);
	}
	

}
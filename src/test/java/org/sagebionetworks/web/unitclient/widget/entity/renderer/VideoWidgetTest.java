package org.sagebionetworks.web.unitclient.widget.entity.renderer;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.widget.entity.renderer.VideoWidget;
import org.sagebionetworks.web.client.widget.entity.renderer.VideoWidgetView;
import org.sagebionetworks.web.shared.WidgetConstants;
import org.sagebionetworks.web.shared.WikiPageKey;

public class VideoWidgetTest {
		
	VideoWidget widget;
	VideoWidgetView mockView;
	WikiPageKey wikiKey = new WikiPageKey("", ObjectType.ENTITY.toString(), null);
	AuthenticationController mockAuthController;
	String xsrfToken = "12345";
	@Before
	public void setup(){
		mockView = mock(VideoWidgetView.class);
		mockAuthController = mock(AuthenticationController.class);
		widget = new VideoWidget(mockView, mockAuthController);
		when(mockAuthController.isLoggedIn()).thenReturn(true);
		when(mockAuthController.getCurrentXsrfToken()).thenReturn(xsrfToken);
	}
	
	@Test
	public void testAsWidget() {
		widget.asWidget();
		verify(mockView).asWidget();
	}
	
	@Test
	public void testConfigure() {
		Map<String, String> descriptor = new HashMap<String, String>();
		String mp4VideoId = "syn123";
		String webMVideoId = "syn456";
		String oggVideoId = "syn789";
		String width = "400px";
		String height="600px";
		descriptor.put(WidgetConstants.VIDEO_WIDGET_MP4_SYNAPSE_ID_KEY, mp4VideoId);
		descriptor.put(WidgetConstants.VIDEO_WIDGET_WEBM_SYNAPSE_ID_KEY, webMVideoId);
		descriptor.put(WidgetConstants.VIDEO_WIDGET_OGG_SYNAPSE_ID_KEY, oggVideoId);
		descriptor.put(WidgetConstants.VIDEO_WIDGET_WIDTH_KEY, width);
		descriptor.put(WidgetConstants.HEIGHT_KEY, height);
		
		widget.configure(wikiKey, descriptor, null, null);
		verify(mockView).configure(eq(mp4VideoId), eq(oggVideoId), eq(webMVideoId), eq(width), eq(height), eq(xsrfToken));
	}
	
	@Test
	public void testShortConfigureMP4() {
		String mp4VideoId = "syn123";
		String oggVideoId = null;
		String webMVideoId = null;
		String width = "400";
		String height="600";
		widget.configure(mp4VideoId, "filename.mp4", 400, 600);
		verify(mockView).configure(eq(mp4VideoId), eq(oggVideoId), eq(webMVideoId), eq(width), eq(height), eq(xsrfToken));
	}
	
	@Test
	public void testShortConfigureWebMV() {
		String mp4VideoId = null;
		String oggVideoId = null;
		String webMVideoId = "syn456";
		String width = "400";
		String height="600";
		widget.configure(webMVideoId, "filename.webm", 400, 600);
		verify(mockView).configure(eq(mp4VideoId), eq(oggVideoId), eq(webMVideoId), eq(width), eq(height), eq(xsrfToken));
	}
	
	@Test
	public void testShortConfigureOgg() {
		String mp4VideoId = null;
		String oggVideoId = "syn789";
		String webMVideoId = null;
		String width = "400";
		String height="600";
		widget.configure(oggVideoId, "filename.ogg", 400, 600);
		verify(mockView).configure(eq(mp4VideoId), eq(oggVideoId), eq(webMVideoId), eq(width), eq(height), eq(xsrfToken));
	}
}

package org.sagebionetworks.web.unitclient.widget.discussion;

import static org.junit.Assert.*;
import static org.sagebionetworks.web.client.widget.discussion.DiscussionMessageURLUtil.*;
import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.sagebionetworks.web.client.widget.discussion.DiscussionMessageURLUtil;
import org.sagebionetworks.web.shared.WebConstants;

public class DiscussionMessageURLUtilTest {

	@Test (expected=IllegalArgumentException.class)
	public void testBuildMessageUrlWithNullKey() throws UnsupportedEncodingException {
		DiscussionMessageURLUtil.buildMessageUrl(null, "type");
	}

	@Test (expected=IllegalArgumentException.class)
	public void testBuildMessageUrlWithNullType() throws UnsupportedEncodingException {
		DiscussionMessageURLUtil.buildMessageUrl("key", null);
	}

	@Test
	public void testBuildMessageUrlWithThreadType() throws UnsupportedEncodingException {
		String messageKey = "key";
		String url = DiscussionMessageURLUtil.buildMessageUrl(messageKey, WebConstants.THREAD_TYPE);
		assertNotNull(url);
		assertEquals(url, "/Portal"+WebConstants.DISCUSSION_MESSAGE_SERVLET+"?"
				+WebConstants.MESSAGE_KEY_PARAM+"="
				+ new String(Base64.encodeBase64(messageKey.getBytes(UTF_8)), UTF_8)
				+"&"+WebConstants.TYPE_PARAM
				+"="+WebConstants.THREAD_TYPE);
	}

	@Test
	public void testBuildMessageUrlWithReplyType() throws UnsupportedEncodingException {
		String messageKey = "key";
		String url = DiscussionMessageURLUtil.buildMessageUrl(messageKey, WebConstants.REPLY_TYPE);
		assertNotNull(url);
		assertEquals(url, "/Portal"+WebConstants.DISCUSSION_MESSAGE_SERVLET+"?"
				+WebConstants.MESSAGE_KEY_PARAM+"="
				+ new String(Base64.encodeBase64(messageKey.getBytes(UTF_8)), UTF_8)
				+"&"+WebConstants.TYPE_PARAM
				+"="+WebConstants.REPLY_TYPE);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testBuildMessageUrlWithUnsupportedType() throws UnsupportedEncodingException {
		DiscussionMessageURLUtil.buildMessageUrl("key", "type");
	}
}

package org.sagebionetworks.web.client.widget.discussion;

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;
import org.sagebionetworks.web.shared.WebConstants;

public class DiscussionMessageURLUtil {
	public static final String UTF_8 = "UTF-8";

	public static String buildMessageUrl(String messageKey, String type) throws UnsupportedEncodingException{
		if (messageKey == null) {
			throw new IllegalArgumentException("messageKey is required.");
		}
		if (type == null) {
			throw new IllegalArgumentException("type is required.");
		}
		if (!type.equals(WebConstants.THREAD_TYPE) && !type.equals(WebConstants.REPLY_TYPE)) {
			throw new IllegalArgumentException("type "+type+" is not supported.");
		}
		StringBuilder builder = new StringBuilder();
		builder.append("/Portal");
		builder.append(WebConstants.DISCUSSION_MESSAGE_SERVLET);
		builder.append("?");
		builder.append(WebConstants.MESSAGE_KEY_PARAM);
		builder.append("=");
		String encodedMessageKey = new String(Base64.encodeBase64(messageKey.getBytes(UTF_8)), UTF_8);
		builder.append(encodedMessageKey);
		builder.append("&");
		builder.append(WebConstants.TYPE_PARAM);
		builder.append("=");
		builder.append(type);
		return builder.toString();
	}
}

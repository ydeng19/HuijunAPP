package edu.asu.mobicloud.mcosgi.IQ;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.StringUtils;

public class IQServiceRequest extends IQ {
	public static final String ELEMENT = IQServiceRequest.class.getName();
	public static final String NAMESPACE = IQServiceRequest.class.getName();
	public static final String HEAD = "service_interface";
	public static final String BODY = "service_parameter";
	private String head = null;
	private String body = null;

	@Override
	public String getChildElementXML() {
		StringBuffer sb = new StringBuffer();
		sb.append("<").append(ELEMENT).append(" xmlns=\"").append(NAMESPACE).append("\">");
		sb.append("<" + HEAD + ">").append(getHead()).append("</" + HEAD + ">");
		sb.append("<" + BODY + ">").append(getBody()).append("</" + BODY + ">");
		sb.append("</").append(ELEMENT).append(">");
		return sb.toString();
	}

	public IQServiceRequest() {
		super();
		this.setPacketID(StringUtils.randomString(9));
	}

	public String getHead() {
		return head;
	}

	public void setHead(String head) {
		this.head = head;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
}

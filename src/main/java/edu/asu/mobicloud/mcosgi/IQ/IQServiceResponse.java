package edu.asu.mobicloud.mcosgi.IQ;

import org.jivesoftware.smack.packet.IQ;

public class IQServiceResponse extends IQ {
	public static final String ELEMENT = IQServiceResponse.class.getName();
	public static final String NAMESPACE = IQServiceResponse.class.getName();
	private String head = null;
	private String body = null;

	@Override
	public String getChildElementXML() {
		StringBuffer sb = new StringBuffer();
		sb.append("<").append(ELEMENT).append(" xmlns=\"")
				.append(NAMESPACE).append("\">");
		sb.append("<" + IQServiceRequest.HEAD + ">").append(getHead())
				.append("</" + IQServiceRequest.HEAD + ">");
		sb.append("<" + IQServiceRequest.BODY + ">").append(getBody())
				.append("</" + IQServiceRequest.BODY + ">");
		sb.append("</").append(ELEMENT).append(">");
		return sb.toString();
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

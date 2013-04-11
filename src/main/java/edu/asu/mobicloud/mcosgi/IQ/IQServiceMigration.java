package edu.asu.mobicloud.mcosgi.IQ;

import org.jivesoftware.smack.packet.IQ;

public class IQServiceMigration extends IQ {

	public static final String ELEMENT = IQServiceMigration.class.getName();
	public static final String NAMESPACE = IQServiceMigration.class.getName();

	Long bundle_id;
	Integer on_off;

	public Long getBundle_id() {
		return bundle_id;
	}

	public void setBundle_id(Long bundle_id) {
		this.bundle_id = bundle_id;
	}

	public Integer getOn_off() {
		return on_off;
	}

	public void setOn_off(Integer on_off) {
		this.on_off = on_off;
	}

	@Override
	public String getChildElementXML() {
		StringBuffer sb = new StringBuffer();
		sb.append("<").append(ELEMENT).append(" xmlns=\"").append(NAMESPACE).append("\">");
		sb.append("<" + BUNDLE_ID + ">").append(getBundle_id()).append("</" + BUNDLE_ID + ">");
		sb.append("<" + ON_OFF + ">").append(getOn_off()).append("</" + ON_OFF + ">");
		sb.append("</").append(ELEMENT).append(">");
		return sb.toString();
	}

	public static final String BUNDLE_ID = "BUNDLE_ID";
	public static final String ON_OFF = "ON_OFF";
}

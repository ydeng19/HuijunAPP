package edu.asu.mobicloud.mcosgi.IQ;

import org.jivesoftware.smack.packet.IQ;

public class IQPullFile extends IQ {

	public static final String ELEMENT = IQPullFile.class.getName();
	public static final String NAMESPACE = IQPullFile.class.getName();

	Long remote_bunlde_id;

	public Long getRemote_bunlde_id() {
		return remote_bunlde_id;
	}

	public void setRemote_bunlde_id(Long remote_bunlde_id) {
		this.remote_bunlde_id = remote_bunlde_id;
	}

	@Override
	public String getChildElementXML() {
		String head = "<" + ELEMENT + " xmlns=\"" + NAMESPACE + "\">";
		String bundle_id = "<" + BUNDLE_ID + ">" + getRemote_bunlde_id().toString() + "</" + BUNDLE_ID + ">";
		String foot = "</" + ELEMENT + ">";
		return head + bundle_id + foot;
	}

	public static final String BUNDLE_ID = "bundle_id";

}

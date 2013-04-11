package edu.asu.mobicloud.mcosgi.IQ;

public class MyBundleUpdate {

	public static final String ELEMENT = IQPullFile.class.getName();
	public static final String NAMESPACE = IQPullFile.class.getName();

	public static final String PAYLOAD = "PAYLOAD";

	Long id;
	Integer onOff;

	public Integer getOnOff() {
		return onOff;
	}

	public void setOnOff(Integer onOff) {
		this.onOff = onOff;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String toXML() {
		StringBuffer sb = new StringBuffer();
		sb.append("<" + PAYLOAD + ">").append(getId() + ":" + getOnOff()).append("</" + PAYLOAD + ">");
		return sb.toString();
	}

}

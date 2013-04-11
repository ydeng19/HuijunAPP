package edu.asu.mobicloud.mcosgi.IQProvider;

import edu.asu.mobicloud.mcosgi.IQ.MyBundleUpdate;

public class MyProviderBundleUpdate {

	public MyBundleUpdate fromXML(String xml) throws Exception {
		MyBundleUpdate su = new MyBundleUpdate();
//		System.out.println(xml);
		int start = xml.indexOf(">");
		int end = xml.indexOf("<", start);
		xml = xml.substring(start + 1, end);
//		System.out.println(xml);
		String[] s = xml.split(":");
		su.setId(Long.valueOf(s[0]));
		su.setOnOff(Integer.valueOf(s[1]));
		return su;
	}
}

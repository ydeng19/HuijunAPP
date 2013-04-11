package edu.asu.mobicloud.mcosgi.IQProvider;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

import edu.asu.mobicloud.mcosgi.IQ.IQServiceRequest;

public class ProviderServiceRequest implements IQProvider {
	public IQ parseIQ(XmlPullParser xpp) throws Exception {
		IQServiceRequest iq = new IQServiceRequest();
		while (true) {
			int n = xpp.next();
			if (n == XmlPullParser.START_TAG) {
				if (IQServiceRequest.HEAD.equals(xpp.getName())) {
					iq.setHead(xpp.nextText());
				} else if (IQServiceRequest.BODY.equals(xpp.getName())) {
					iq.setBody(xpp.nextText());
				}
			} else if (n == XmlPullParser.END_TAG) {
				if (IQServiceRequest.ELEMENT.equals(xpp.getName())) {
					break;
				}
			}
		}
		return iq;
	}

}

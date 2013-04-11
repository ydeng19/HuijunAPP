package edu.asu.mobicloud.mcosgi.IQProvider;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

import edu.asu.mobicloud.mcosgi.IQ.IQPullFile;

public class ProviderPullFile implements IQProvider {

	public IQ parseIQ(XmlPullParser xp) throws Exception {
		IQPullFile iq = new IQPullFile();
		while (true) {
			int n = xp.next();
			if (n == XmlPullParser.START_TAG) {
				if (IQPullFile.BUNDLE_ID.equals(xp.getName())) {
					iq.setRemote_bunlde_id(Long.valueOf(xp.nextText()));
				}
			} else if (n == XmlPullParser.END_TAG) {
				if (IQPullFile.ELEMENT.equals(xp.getName())) {
					break;
				}
			}
		}
		return iq;
	}

}

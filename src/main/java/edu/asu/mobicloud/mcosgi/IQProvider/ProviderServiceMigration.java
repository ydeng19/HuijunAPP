package edu.asu.mobicloud.mcosgi.IQProvider;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

import edu.asu.mobicloud.mcosgi.IQ.IQServiceMigration;

public class ProviderServiceMigration implements IQProvider {

	public IQ parseIQ(XmlPullParser xp) throws Exception {
		IQServiceMigration iq = new IQServiceMigration();
		while (true) {
			int n = xp.next();
			if (n == XmlPullParser.START_TAG) {
				if (IQServiceMigration.BUNDLE_ID.equals(xp.getName())) {
					iq.setBundle_id(Long.valueOf(xp.nextText()));
				}
				if (IQServiceMigration.ON_OFF.equals(xp.getName())) {
					iq.setOn_off(Integer.valueOf(xp.nextText()));
				}
			} else if (n == XmlPullParser.END_TAG) {
				if (IQServiceMigration.ELEMENT.equals(xp.getName())) {
					break;
				}
			}
		}
		return iq;
	}

}

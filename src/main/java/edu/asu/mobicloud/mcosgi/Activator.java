package edu.asu.mobicloud.mcosgi;

import org.jivesoftware.smack.packet.Presence;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

	SQLJet db = new SQLJet();

	Smack smack = new Smack(db);
	Smackx smackx = new Smackx(db, smack);
	Smackxy smackxy = new Smackxy(db, smackx);

	Felix felix = new Felix(db);

	McOsgiImpl mcosgi = new McOsgiImpl(db);

	public void start(BundleContext context) throws Exception {
		felix.setContext(context);

		felix.setSmack(smack, smackx, smackxy);

		felix.setMcOsgi(mcosgi);

		smack.setFelix(felix);
		smackx.setFelix(felix);
		smackxy.setFelix(felix);

		smack.setSmack(smackx, smackxy);
		smackx.setSmack(smackxy);
		smackxy.setSmack(smack);

		smack.setMcOsgi(mcosgi);
		smackx.setMcOsgi(mcosgi);
		smackxy.setMcOsgi(mcosgi);

		mcosgi.setSmack(smack, smackx, smackxy);
		mcosgi.setFelix(felix);

		String dbfile = context.getProperty("edu.asu.mobicloud.mcosgi.dbfile");
		if (dbfile == null) {
			dbfile = "mcosgi.sqlite";
		}
		db.open(dbfile);
		felix.active.getProperty();

		smack.connect();
		smackx.connect();
		smackxy.connect();
		felix.passive.addBundleListener();
		smack.connection.sendPacket(new Presence(Presence.Type.available));

		sr = context.registerService(McOsgiService.class, mcosgi, null);
	}

	ServiceRegistration<McOsgiService> sr;

	public void stop(BundleContext context) throws Exception {
		felix.setContext(context);
		sr.unregister();

		smackxy.disconnect();
		smackx.disconnect();
		smack.disconnect();

		db.close();
	}

}

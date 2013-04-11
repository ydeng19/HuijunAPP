package edu.asu.mobicloud.mcosgi;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.log.LogService;
import org.tmatesoft.sqljet.core.SqlJetException;

import edu.asu.mobicloud.mcosgi.IQ.MyBundleUpdate;

public class Felix {
	BundleContext context = null;

	SQLJet db = null;

	public Felix(SQLJet db) {
		this.db = db;
	}

	void setContext(BundleContext context2) {
		this.context = context2;

	}

	Smack smack = null;
	Smackx smackx = null;
	Smackxy smackxy = null;

	void setSmack(Smack smack, Smackx smackx, Smackxy smackxy) {
		this.smack = smack;
		this.smackx = smackx;
		this.smackxy = smackxy;
	}

	McOsgiImpl mcosgi = null;

	void setMcOsgi(McOsgiImpl mcosgi) {
		this.mcosgi = mcosgi;
	}

	Active active = new Active();
	Passive passive = new Passive();

	class Active {
		void getProperty() throws Exception {
			String ip = context.getProperty("edu.asu.mobicloud.mcosgi.openfire");
			if (ip == null) {
				throw new Exception("fatal error : openfire ip is missing");
			}
			db.setProperty("openfire", ip);
			String user = context.getProperty("edu.asu.mobicloud.mcosgi.username");
			if (user == null) {
				throw new Exception("fatal error : user name is missing");
			}
			db.setProperty("username", user);
			String pass = context.getProperty("edu.asu.mobicloud.mcosgi.password");
			if (pass == null) {
				throw new Exception("fatal error : pass word is missing");
			}
			db.setProperty("password", pass);
			String res = context.getProperty("edu.asu.mobicloud.mcosgi.resource");
			if (res == null) {
				res = "mc-osgi";
			}
			db.setProperty("resource", res);
			String cache = context.getProperty("org.osgi.framework.storage");
			if (cache == null) {
				cache = "felix-cache";
			}
			db.setProperty("storage", cache);
			String preparation = context.getProperty("edu.asu.mobicloud.mcosgi.preparation");
			if (preparation == null) {
				preparation = "pull";
			}
			db.setProperty("preparation", preparation);
		}

		void DiscoverLocal() throws InvalidSyntaxException, SqlJetException {
			ServiceReference<?>[] sr = context.getAllServiceReferences(null, "(" + MC_OSGI_SERVICE_PROPERTY + "=*)");
			if (sr == null) {
				return;
			}
			for (int i = 0; i < sr.length; i++) {
				String export_service = (String) sr[i].getProperty(MC_OSGI_SERVICE_PROPERTY);
				Long local_bundle_id = sr[i].getBundle().getBundleId();
				db.setLocalDiscovery(export_service, local_bundle_id);
			}
		}

		long installBundle(File f, String jid, Long remote_bundle_id) throws SqlJetException, IOException,
				BundleException {
			long local_bundle_id = 0;
			try {
				Bundle b = context.installBundle("file:" + f.getAbsolutePath());
				db.setBundleMapping(b.getBundleId(), jid, remote_bundle_id);
				local_bundle_id = b.getBundleId();
			} catch (BundleException e) {
				if (e.getType() == BundleException.DUPLICATE_BUNDLE_ERROR) {
					ZipFile zf = new ZipFile(f);
					ZipEntry ze = zf.getEntry("META-INF/MANIFEST.MF");
					Properties prop = new Properties();
					prop.load(zf.getInputStream(ze));
					String name = prop.getProperty("Bundle-SymbolicName");
					Version version = Version.parseVersion(prop.getProperty("Bundle-Version"));
					Bundle[] bs = context.getBundles();
					for (int i = 0; i < bs.length; i++) {
						String n = bs[i].getSymbolicName();
						Version v = bs[i].getVersion();
						if (name.equals(n) && 0 == version.compareTo(v)) {
							long id = bs[i].getBundleId();
							db.setBundleMapping(id, jid, remote_bundle_id);
							local_bundle_id = id;
							break;
						}
					}
					zf.close();
				} else {
					throw e;
				}
			}
			return local_bundle_id;
		}

		String getJarPath(Long id) throws SqlJetException {
			String base_path = db.getProperty("storage");
			String version_path = base_path + "/bundle" + id;
			int refresh_count = 0;
			int revision_count = 0;
			File dir = new File(version_path);
			for (File child : dir.listFiles()) {
				String dir_name = child.getName();
				String start = "version";
				if (dir_name.startsWith(start)) {
					String sub_str = dir_name.substring(start.length());
					String[] tmp_str = sub_str.split("\\.");
					int cur_refresh_count = Integer.valueOf(tmp_str[0]);
					if (cur_refresh_count > refresh_count) {
						refresh_count = cur_refresh_count;
					}
					int cur_revision_count = Integer.valueOf(tmp_str[1]);
					if (cur_revision_count > revision_count) {
						revision_count = cur_revision_count;
					}
				}
			}
			String path = version_path + "/version" + refresh_count + "." + revision_count + "/bundle.jar";
			return path;
		}

		Object getService(String service) {
			ServiceReference<?> sref = context.getServiceReference(service);
			return context.getService(sref);
		}
	}

	class Passive {
		void addBundleListener() {
			context.addBundleListener(new BundleListener() {
				public void bundleChanged(BundleEvent event) {
					long id = event.getBundle().getBundleId();
					if (context.getBundle().getBundleId() == id) {
						return;
					}
					int t = event.getType();
					try {
						switch (t) {
						case BundleEvent.STARTED:
							ServiceReference<?>[] srefs = context.getBundle(id).getRegisteredServices();
							if (srefs == null) {
								return;
							}
							boolean no_export = true;
							for (int i = 0; i < srefs.length; i++) {
								if (srefs[i].getProperty(MC_OSGI_SERVICE_PROPERTY) != null) {
									no_export = false;
									break;
								}
							}
							if (no_export) {
								return;
							}
							Set<String> before = db.getAllLocalDiscoveryService();
							db.deleteFromLocalDiscovery();
							active.DiscoverLocal();
							Set<String> after = db.getAllLocalDiscoveryService();
							after.removeAll(before);
							for (Iterator<String> it = after.iterator(); it.hasNext();) {
								smackx.passive.ProvideNodeInformation(it.next());
							}
						case BundleEvent.STOPPED:
							if (!db.checkLocalDiscoveryBundle(id)) {
								return;
							}
							Set<String> before1 = db.getAllLocalDiscoveryService();
							db.deleteFromLocalDiscovery();
							active.DiscoverLocal();
							Set<String> after1 = db.getAllLocalDiscoveryService();
							before1.removeAll(after1);
							for (Iterator<String> it = before1.iterator(); it.hasNext();) {
								smackx.serviceDiscovery.removeNodeInformationProvider(it.next());
							}
							break;
						default:
							return;
						}
						MyBundleUpdate bu = new MyBundleUpdate();
						bu.setId(id);
						bu.setOnOff(t);
						SimplePayload payload = new SimplePayload(Smackx.MC_OSGI_PUBSUB_EMELENT_NAME,
								Smackx.MC_OSGI_PUBSUB_NAME_SPACE, bu.toXML());
						smackx.active.PublishingToANode(smackxy.leafNode, payload);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	void log(int level, String message) {
		System.out.println("[" + level + "] " + message);
		ServiceReference<LogService> sref = context.getServiceReference(LogService.class);
		if (sref != null) {
			LogService m_log = context.getService(sref);
			if (m_log != null) {
				m_log.log(level, message);
			}
		}
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		try {
//			FileWriter fw = new FileWriter(new File("f:/tmp/microLog.txt"), true);
//			FileWriter fw = new FileWriter(new File("/sdcard/microLog.txt"), true);
//			fw.append(sdf.format(cal.getTime()) + " : " + message + "\n");
//			fw.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	/*********
	 * constant
	 */

	public static final String MC_OSGI_SERVICE_PROPERTY = "MC_OSGi_ServiceInterface";

}

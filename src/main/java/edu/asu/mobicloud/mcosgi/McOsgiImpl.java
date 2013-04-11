package edu.asu.mobicloud.mcosgi;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.tmatesoft.sqljet.core.SqlJetException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import edu.asu.mobicloud.mcosgi.IQ.IQPullFile;
import edu.asu.mobicloud.mcosgi.IQ.IQServiceMigration;
import edu.asu.mobicloud.mcosgi.IQ.IQServiceRequest;

public class McOsgiImpl implements McOsgiService {

	Smack smack = null;
	Smackx smackx = null;
	Smackxy smackxy = null;

	SQLJet db = null;

	Felix felix = null;

	Long id = 0L; // period Integer.MAX_VALUE
	Map<Long, Object> id_proxy = new HashMap<Long, Object>();
	Map<Long, ServiceRegistration<?>> id_sreg = new HashMap<Long, ServiceRegistration<?>>();
	Map<Long, String> id_service = new HashMap<Long, String>();

	public McOsgiImpl(SQLJet db2) {
		this.db = db2;
	}

	ServiceRegistration<?> mapGetServiceRegistration(Long long1) {
		return id_sreg.get(long1);
	}

	Object mapGetProxy(Long id) {
		return id_proxy.get(id);
	}

	String mapGetSerivce(Long id) {
		return id_service.get(id);
	}

	Long mapPut(ServiceRegistration<?> sreg, Object proxy, String name) {
		id_proxy.put(id, proxy);
		id_sreg.put(id, sreg);
		id_service.put(id, name);
		return id++;
	}

	void mapReplace(Long id_, ServiceRegistration<?> sreg, Object proxy, String name) {
		id_proxy.put(id_, proxy);
		id_sreg.put(id_, sreg);
		id_service.put(id_, name);
	}

	/**********
	 * discover
	 */

	public void Discover(String remote_jid) throws SqlJetException, XMPPException, InvalidSyntaxException {
		db.deleteRemoteDiscoveryJid(remote_jid);
		felix.log(LogService.LOG_DEBUG, "Discover - deleteRemoteDiscoveryJid finish");
		DiscoverInfo info = smackx.active.DiscoverInformationAboutAnXmppEntity(remote_jid, null);
		felix.log(LogService.LOG_DEBUG, "Discover - DiscoverInformationAboutAnXmppEntity finish " + info);
		if (info.containsFeature(Smackx.MC_OSGI_JID_FEATURE)) {
			DiscoverItems items = smackx.active.DiscoverItemsAssociatedWithAnXmppEntity(remote_jid,
					Smackx.MC_OSGI_INDEX_NODE);
			felix.log(LogService.LOG_DEBUG, "Discover - DiscoverItemsAssociatedWithAnXmppEntity finish " + items);
			for (Iterator<DiscoverItems.Item> it_itm = items.getItems(); it_itm.hasNext();) {
				DiscoverItems.Item tiem = it_itm.next();
				String remote_service = tiem.getNode();
				DiscoverInfo info2 = smackx.active.DiscoverInformationAboutAnXmppEntity(remote_jid, remote_service);
				felix.log(LogService.LOG_DEBUG, "Discover - DiscoverInformationAboutAnXmppEntity finish " + info2);
				for (Iterator<DiscoverInfo.Identity> it_idey = info2.getIdentities(); it_idey.hasNext();) {
					DiscoverInfo.Identity idty = it_idey.next();
					Long remote_bundle_id = Long.valueOf(idty.getType());
					felix.log(LogService.LOG_DEBUG, "Discover - DiscoverInformationAboutAnXmppEntity type "
							+ remote_bundle_id);
					db.setRemoteDiscovery(remote_jid, remote_service, remote_bundle_id);
				}
			}
		}
	}

	public void Discover() throws SqlJetException, XMPPException, InvalidSyntaxException {
		Set<Presence> ps = smack.active.getAllPresences();
		for (Iterator<Presence> it = ps.iterator(); it.hasNext();) {
			Presence p = it.next();
			String remote_jid = p.getFrom();
			if (p.isAvailable()) {
				Discover(remote_jid);
			}
		}
		db.deleteFromLocalDiscovery();
		felix.active.DiscoverLocal();
	}

	/*******
	 * pull
	 */

	public void FilePull(String jid) throws SqlJetException {
		Set<Long> ids = db.getAllRemoteDiscoveryBundle(jid);
		for (Iterator<Long> it2 = ids.iterator(); it2.hasNext();) {
			Long id = it2.next();
			if (!db.checkBundleMappingRemote(jid, id)) {
				IQPullFile iq = new IQPullFile();
				iq.setRemote_bunlde_id(id);
				iq.setTo(jid);
				smack.connection.sendPacket(iq);
				felix.log(LogService.LOG_DEBUG, "send pull file request");
			}
		}
	}

	public void FilePull() throws SqlJetException {
		Set<String> jids = db.getAllRemoteDiscoveryJid();
		for (Iterator<String> it = jids.iterator(); it.hasNext();) {
			String jid = it.next();
			Set<Long> ids = db.getAllRemoteDiscoveryBundle(jid);
			for (Iterator<Long> it2 = ids.iterator(); it.hasNext();) {
				Long id = it2.next();
				if (!db.checkBundleMappingRemote(jid, id)) {
					IQPullFile iq = new IQPullFile();
					iq.setRemote_bunlde_id(id);
					smack.connection.sendPacket(iq);
				}
			}
		}
	}

	/**********
	 * push
	 */

	public void FilePush(String jid) throws SqlJetException, XMPPException {
		Set<Long> ids = db.getAllDisdinctLocalDiscoveryBundle();
		for (Iterator<Long> it = ids.iterator(); it.hasNext();) {
			Long id = it.next();
			if (!db.checkBundleMappingRemote(jid, id)) {
				String file = felix.active.getJarPath(id);
				smackx.active.SendAFileToAnotherUser(jid, file, id.toString());
			}
		}
	}

	public void FilePush() throws SqlJetException, XMPPException {
		Set<Presence> ps = smack.active.getAllPresences();
		for (Iterator<Presence> it2 = ps.iterator(); it2.hasNext();) {
			Presence p = it2.next();
			if (p.isAvailable()) {
				String jid = p.getFrom();
				FilePush(jid);
			}
		}
	}

	/**********
	 * proxy
	 */

	static class jsonUtil {
		/*
		 * client side
		 */
		public static String putRequest(Method m, Object[] p) throws JsonGenerationException, JsonMappingException,
				IOException {
			return putResponse(m, p, null);
		}

		public static Object getResponse(String json, Object[] p) throws JsonParseException, JsonMappingException,
				IOException {
			@SuppressWarnings("unchecked")
			LinkedHashMap<String, Object> decode = Jackson.getInstance().readValue(json, LinkedHashMap.class);
			@SuppressWarnings("unchecked")
			ArrayList<Object> recover = (ArrayList<Object>) decode.get("params");
			int idx = 0;
			for (Iterator<Object> it = recover.iterator(); it.hasNext();) {
				Object o = it.next();
				p[idx] = o;
				idx++;
			}
			Object r = decode.get("ret");
			return r;
		}

		/*
		 * server side
		 */
		public static Object[] getRequestParameters(String json) throws JsonParseException, JsonMappingException,
				IOException {
			@SuppressWarnings("unchecked")
			LinkedHashMap<String, Object> decode = Jackson.getInstance().readValue(json, LinkedHashMap.class);
			@SuppressWarnings("unchecked")
			ArrayList<Object> recover = (ArrayList<Object>) decode.get("params");
			Object[] p = new Object[recover.size()];
			int idx = 0;
			for (Iterator<Object> it = recover.iterator(); it.hasNext();) {
				Object o = it.next();
				p[idx] = o;
				idx++;
			}
			return p;
		}

		public static String getRequestName(String json) throws JsonParseException, JsonMappingException, IOException {
			@SuppressWarnings("unchecked")
			LinkedHashMap<String, Object> decode = Jackson.getInstance().readValue(json, LinkedHashMap.class);
			String recover = (String) decode.get("method");
			return recover;
		}

		public static String putResponse(Method m, Object[] p, Object r) throws JsonGenerationException,
				JsonMappingException, IOException {
			LinkedHashMap<String, Object> rmiPacket = new LinkedHashMap<String, Object>();
			rmiPacket.put("ret", r);
			rmiPacket.put("method", m.getName());
			ArrayList<Object> params = new ArrayList<Object>();
			for (int i = 0; i < p.length; i++) {
				params.add(p[i]);
			}
			rmiPacket.put("params", params);
			String result = Jackson.getInstance().writeValueAsString(rmiPacket);
			return result;
		}
	}

	void handleRmiRequest(Packet p) {
		long start = System.currentTimeMillis();
		IQServiceRequest iq = (IQServiceRequest) p;
		String service = iq.getHead();
		String json = iq.getBody();
		//
		Object[] req_params = null;
		String function_name = null;

		try {
			function_name = jsonUtil.getRequestName(json);
			req_params = jsonUtil.getRequestParameters(json);
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		//
		Object service_provider_object = felix.active.getService(service);
		assert service_provider_object == null : "service not found";
		Method[] all_m = service_provider_object.getClass().getMethods();
		Method m = null;
		for (int i = 0; i < all_m.length; i++) {
			if (all_m[i].getName().equals(function_name)) {
				// function name match
				Class<?>[] params = all_m[i].getParameterTypes();
				boolean match = true;
				for (int j = 0; j < params.length; j++) {
					if (!checkMatch(params[j], req_params[j].getClass())) {
						match = false;
						break;
					}
				}
				if (match) {
					// parameter order and type match
					m = all_m[i];
					break;
				}
			}
		}
		long end = System.currentTimeMillis();
		long start_call = System.currentTimeMillis();
		assert m == null : "object.method not found";
		Object r = null;
		try {

			r = m.invoke(service_provider_object, req_params);
		} catch (Exception e) {
			e.printStackTrace();
		}
		long end_call = System.currentTimeMillis();
		long start_ = System.currentTimeMillis();
		try {
			json = jsonUtil.putResponse(m, req_params, r);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		long end_ = System.currentTimeMillis();

		if (req_params.length >= 1) {
			felix.log(LogService.LOG_DEBUG, m.getName() + " : " + req_params[0].toString() + " marsh : "
					+ ((end - start) + (end_ - start_)) + ": real calc : " + (end_call - start_call));
			try {
				db.setCalledStatistics(service, m.getName(), req_params[0].toString(), (end - start) + (end_ - start_),
						end_call - start_call);
			} catch (SqlJetException e) {
				e.printStackTrace();
			}
		} else {
			felix.log(LogService.LOG_DEBUG, m.getName() + " marsh : " + ((end - start) + (end_ - start_))
					+ ": real calc : " + (end_call - start_call));
		}
		try {
			smack.active.rmiResponse(iq.getFrom(), iq.getPacketID(), service, json);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean checkMatch(Class<?> class1, Class<? extends Object> class2) {
		if (class1.equals(class2)) {
			return true;
		} else if (class1.equals(int.class) && class2.equals(Integer.class)) {
			return true;
		} else if (class1.equals(Integer.class) && class2.equals(int.class)) {
			return true;
		} else if (class1.equals(short.class) && class2.equals(Short.class)) {
			return true;
		} else if (class1.equals(Short.class) && class2.equals(short.class)) {
			return true;
		} else if (class1.equals(byte.class) && class2.equals(Byte.class)) {
			return true;
		} else if (class1.equals(Byte.class) && class2.equals(byte.class)) {
			return true;
		} else if (class1.equals(long.class) && class2.equals(Long.class)) {
			return true;
		} else if (class1.equals(Long.class) && class2.equals(int.class)) {
			return true;
		} else if (class1.equals(double.class) && class2.equals(Double.class)) {
			return true;
		} else if (class1.equals(Double.class) && class2.equals(double.class)) {
			return true;
		} else if (class1.equals(float.class) && class2.equals(Float.class)) {
			return true;
		} else if (class1.equals(Float.class) && class2.equals(float.class)) {
			return true;
		} else if (class1.equals(boolean.class) && class2.equals(Boolean.class)) {
			return true;
		} else if (class1.equals(Boolean.class) && class2.equals(boolean.class)) {
			return true;
		}
		return false;
	}

	class ClientDynamicProxyHandler implements InvocationHandler {
		String server_jid;
		String service_name;

		public ClientDynamicProxyHandler(String server_jid, String service_name) {
			this.server_jid = server_jid;
			this.service_name = service_name;
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

			// parameter to json
			long start = System.currentTimeMillis();
			String json = jsonUtil.putRequest(method, args);
			long end = System.currentTimeMillis();
			// send json to remote
			long start_net = System.currentTimeMillis();
			json = smack.active.rmiRequest(server_jid, service_name, json);
			long end_net = System.currentTimeMillis();
			// json to return
			long start_ = System.currentTimeMillis();
			Object r = jsonUtil.getResponse(json, args);
			long end_ = System.currentTimeMillis();
			if (args.length >= 1) {
				felix.log(LogService.LOG_DEBUG, method.getName() + "; " + args[0].toString() + "; local marsharl ; "
						+ ((end - start) + (end_ - start_)) + "; remote all ; " + (end_net - start_net));
				db.setCallingStatistics(service_name, method.getName(), args[0].toString(), (end - start)
						+ (end_ - start_), end_net - start_net);
			} else {

			}
			return r;
		}
	}

	// can add proxy
	public void ProxyGenerate() throws SqlJetException, ClassNotFoundException {
		// F_REMOTE_JID F_LOCAL_BUNDLE F_SERVICE
		Vector<Object[]> v = db.getProxyGenerate();
		for (Iterator<Object[]> it = v.iterator(); it.hasNext();) {
			Object[] row = it.next();
			String remot_jid = (String) row[0];
			Long local_bundle_id = (Long) row[1];
			String service = (String) row[2];
			// get interface meta data
			Class<?> service_interface = felix.context.getBundle(local_bundle_id).loadClass(service);
			Class<?>[] interfaces = new Class[] { service_interface };
			// create proxy
			Object p = Proxy.newProxyInstance(service_interface.getClassLoader(), interfaces,
					new ClientDynamicProxyHandler(remot_jid, service));
			// register proxy into OSGi framework
			Dictionary<String, Integer> props = new Hashtable<String, Integer>();
			props.put(Constants.SERVICE_RANKING, Integer.MIN_VALUE);
			ServiceRegistration<?> sr = felix.context.registerService(service, p, props);
			Long proxy_id = mapPut(sr, p, service);
			db.setProxy(proxy_id, row);
		}
	}

	// only remove proxy
	public void ProxyUnreg() throws SqlJetException {
		Set<Long> proxy_id = db.getProxyInactive();
		for (Iterator<Long> it = proxy_id.iterator(); it.hasNext();) {
			ServiceRegistration<?> sr = mapGetServiceRegistration(it.next());
			try {
				sr.unregister();
			} catch (IllegalStateException e) {
				felix.log(LogService.LOG_DEBUG, "proxy service already unregistered, ERROR, SHOULD PROXY RECOVER");
			}
		}
		db.setProxyInactive(proxy_id);
	}

	public void ProxyRecover() throws SqlJetException {
		Set<Long> proxy_id = db.getProxyRecover();
		for (Iterator<Long> it = proxy_id.iterator(); it.hasNext();) {
			Long id = it.next();
			String service = mapGetSerivce(id);
			Object p = mapGetProxy(id);
			Dictionary<String, Integer> props = new Hashtable<String, Integer>();
			props.put(Constants.SERVICE_RANKING, Integer.MIN_VALUE);
			ServiceRegistration<?> sr = felix.context.registerService(service, p, props);
			mapReplace(id, sr, p, service);
		}
		db.setProxyActive(proxy_id);
	}

	/*******
	 * offload
	 */

	void PushService(String target_jid, long local_bundle_id) throws SqlJetException {
		db.startMigrateService(target_jid, local_bundle_id, "push");
		IQServiceMigration iq = new IQServiceMigration();
		iq.setBundle_id(local_bundle_id);
		iq.setOn_off(McOsgiService.SERVICE_MIGRATION_PUSH);
		iq.setTo(target_jid);
		smack.connection.sendPacket(iq);
	}

	void PullService(String target_jid, long local_bundle_id) throws SqlJetException, BundleException {
		db.startMigrateService(target_jid, local_bundle_id, "pull");
		felix.context.getBundle(local_bundle_id).start();
		Long remote_bundle_id = db.getBundleMappingRemoteBundle(target_jid, local_bundle_id);
		IQServiceMigration iq = new IQServiceMigration();
		iq.setBundle_id(remote_bundle_id);
		iq.setOn_off(McOsgiService.SERVICE_MIGRATION_PULL);
		iq.setTo(target_jid);
		smack.connection.sendPacket(iq);
	}

	public void ServiceMigration(int push_pull, String target_jid, long local_bundle_id) throws SqlJetException,
			BundleException {
		switch (push_pull) {
		case McOsgiService.SERVICE_MIGRATION_PULL:
			PullService(target_jid, local_bundle_id);
			break;
		case McOsgiService.SERVICE_MIGRATION_PUSH:
			PushService(target_jid, local_bundle_id);
			break;
		default:
		}
	}

	/*********
	 * prepare
	 */
	public void Prepare(String remote_jid) throws SqlJetException, XMPPException {
		String prep = db.getProperty("preparation");
		if (prep.equals("push")) {
			FilePush(remote_jid);
		} else if (prep.equals("pull")) {
			FilePull(remote_jid);
		}
	}

	public void Prepare() throws SqlJetException, XMPPException {
		String prep = db.getProperty("preparation");
		if (prep.equals("push")) {
			FilePush();
		} else if (prep.equals("pull")) {
			FilePull();
		}
	}

	/***********
	 * set
	 */
	void setSmack(Smack smack2, Smackx smackx2, Smackxy smackxy2) {
		this.smack = smack2;
		this.smackx = smackx2;
		this.smackxy = smackxy2;
	}

	void setFelix(Felix felix2) {
		this.felix = felix2;
	}

}

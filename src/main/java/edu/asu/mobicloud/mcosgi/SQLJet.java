package edu.asu.mobicloud.mcosgi;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.ISqlJetTransaction;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

public class SQLJet {
	SqlJetDb db = null;

	/*********
	 * function
	 */
	void open(String dbfile) throws SqlJetException {
		File dbFile = new File(dbfile);
		dbFile.delete();
		db = SqlJetDb.open(dbFile, true);
		db.getOptions().setAutovacuum(true);
		db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				db.getOptions().setUserVersion(1);
				return true;
			}
		}, SqlJetTransactionMode.WRITE);

		createSchema();
	}

	void close() throws SqlJetException {
		db.close();
	}

	void setProperty(final String string, final String pass) throws SqlJetException {
		db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				ISqlJetTable table = db.getTable(T_PROPERTY);
				ISqlJetCursor c = table.lookup(I_PROPERTY_KEY, string);
				if (c.eof()) {
					table.insert(string, pass);
				} else {
					c.update(string, pass);
				}
				c.close();
				return true;
			}
		}, SqlJetTransactionMode.WRITE);
	}

	String getProperty(final String string) throws SqlJetException {
		Object result = db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				ISqlJetTable table = db.getTable(T_PROPERTY);
				ISqlJetCursor c = table.lookup(I_PROPERTY_KEY, string);
				String r = c.getString(F_VALUE);
				c.close();
				return r;
			}
		}, SqlJetTransactionMode.READ_ONLY);
		return (String) result;
	}

	void setLocalDiscovery(final String export_service, final Long local_bundle_id) throws SqlJetException {
		db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				ISqlJetTable table = db.getTable(T_LOCAL_DISCOVERY);
				ISqlJetCursor c = table.lookup(I_LOCAL_DISCOVERY_LOCAL_SERVICE, export_service);
				if (c.eof()) {
					table.insert(export_service, local_bundle_id);
				} else {
					c.update(export_service, local_bundle_id);
				}
				c.close();
				return true;
			}
		}, SqlJetTransactionMode.WRITE);
	}

	@SuppressWarnings("unchecked")
	Set<String> getAllLocalDiscoveryService() throws SqlJetException {
		Object result = db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				Set<String> r = new HashSet<String>();
				ISqlJetTable table = db.getTable(T_LOCAL_DISCOVERY);
				ISqlJetCursor c = table.open();
				if (!c.eof()) {
					do {
						r.add(c.getString(F_LOCAL_SERVICE));
					} while (c.next());
				}
				c.close();
				return r;
			}
		}, SqlJetTransactionMode.READ_ONLY);
		return (Set<String>) result;
	}

	Long getLocalDiscoveryBundle(final String export_serivce) throws SqlJetException {
		Object result = db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				Long r = 0L;
				ISqlJetTable table = db.getTable(T_LOCAL_DISCOVERY);
				ISqlJetCursor c = table.lookup(I_LOCAL_DISCOVERY_LOCAL_SERVICE, export_serivce);
				if (!c.eof()) {
					r = c.getInteger(F_LOCAL_BUNDLE);
				}
				c.close();
				return r;
			}
		}, SqlJetTransactionMode.READ_ONLY);
		return (Long) result;
	}

	void deleteFromLocalDiscovery() throws SqlJetException {
		db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				ISqlJetTable table = db.getTable(T_LOCAL_DISCOVERY);
				ISqlJetCursor c = table.open();
				while (!c.eof()) {
					c.delete();
				}
				c.close();
				return true;
			}
		}, SqlJetTransactionMode.WRITE);
	}

	boolean checkBundleMappingRemote(final String jid, final Long remote_bundle_id) throws SqlJetException {
		Object result = db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				boolean r = false;
				ISqlJetTable table = db.getTable(T_BUNDLE_MAPPING);
				ISqlJetCursor c = table.lookup(I_BUNDLE_MAPPING_REMOTE_JID_BUNDLE, jid, remote_bundle_id);
				if (!c.eof()) {
					r = true;
				}
				c.close();
				return r;
			}
		}, SqlJetTransactionMode.READ_ONLY);
		return (Boolean) result;
	}

	void setBundleMapping(final Long local_bundle_id, final String jid, final Long remote_bundle_id)
			throws SqlJetException {
		db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				ISqlJetTable table = db.getTable(T_BUNDLE_MAPPING);
				table.insert(local_bundle_id, jid, remote_bundle_id);
				return true;
			}
		}, SqlJetTransactionMode.WRITE);
	}

	void setRemoteDiscovery(final String remote_jid, final String remote_service, final Long remote_bundle_id)
			throws SqlJetException {
		db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				ISqlJetTable table = db.getTable(T_REMOTE_DISCOVERY);
				ISqlJetCursor c = table.lookup(I_REMOTE_DISCOVERY_REMOTE_JID_SERVICE, remote_jid, remote_service);
				if (c.eof()) {
					table.insert(remote_jid, remote_service, remote_bundle_id);
				} else {
					c.update(remote_jid, remote_service, remote_bundle_id);
				}
				c.close();
				return true;
			}
		}, SqlJetTransactionMode.WRITE);
	}

	void deleteFromRemoteDiscovery() throws SqlJetException {
		db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				ISqlJetTable table = db.getTable(T_REMOTE_DISCOVERY);
				ISqlJetCursor c = table.open();
				while (!c.eof()) {
					c.delete();
				}
				c.close();
				return true;
			}
		}, SqlJetTransactionMode.WRITE);
	}

	@SuppressWarnings("unchecked")
	Set<String> getAllRemoteDiscoveryJid() throws SqlJetException {
		Object result = db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				Set<String> r = new HashSet<String>();
				ISqlJetTable table = db.getTable(T_REMOTE_DISCOVERY);
				ISqlJetCursor c = table.open();
				if (!c.eof()) {
					do {
						r.add(c.getString(F_REMOTE_JID));
					} while (c.next());
				}
				c.close();
				return r;
			}
		}, SqlJetTransactionMode.READ_ONLY);
		return (Set<String>) result;
	}

	@SuppressWarnings("unchecked")
	Set<Long> getAllRemoteDiscoveryBundle(final String jid) throws SqlJetException {
		Object result = db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				Set<Long> r = new HashSet<Long>();
				ISqlJetTable table = db.getTable(T_REMOTE_DISCOVERY);
				ISqlJetCursor c = table.lookup(I_REMOTE_DISCOVERY_REMOTE_JID, jid);
				if (!c.eof()) {
					do {
						r.add(c.getInteger(F_REMOTE_BUNDLE));
					} while (c.next());
				}
				c.close();
				return r;
			}
		}, SqlJetTransactionMode.READ_ONLY);
		return (Set<Long>) result;
	}

	@SuppressWarnings("unchecked")
	Set<Long> getAllDisdinctLocalDiscoveryBundle() throws SqlJetException {
		Object result = db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				Set<Long> r = new HashSet<Long>();
				ISqlJetTable table = db.getTable(T_LOCAL_DISCOVERY);
				ISqlJetCursor c = table.order(I_LOCAL_DISCOVERY_LOCAL_BUNDLE);
				if (!c.eof()) {
					Long last_id = -1L;
					do {
						Long id = c.getInteger(F_LOCAL_BUNDLE);
						if (id != last_id) {
							r.add(id);
							last_id = id;
						}
					} while (c.next());
				}
				c.close();
				return r;
			}
		}, SqlJetTransactionMode.READ_ONLY);
		return (Set<Long>) result;
	}

	void deleteRemoteDiscoveryJid(final String remote_jid) throws SqlJetException {
		db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				ISqlJetTable table = db.getTable(T_REMOTE_DISCOVERY);
				ISqlJetCursor c = table.lookup(I_REMOTE_DISCOVERY_REMOTE_JID, remote_jid);
				while (!c.eof()) {
					c.delete();
				}
				c.close();
				return true;
			}
		}, SqlJetTransactionMode.WRITE);
	}

	// F_REMOTE_JID F_LOCAL_BUNDLE F_SERVICE
	@SuppressWarnings("unchecked")
	Vector<Object[]> getProxyGenerate() throws SqlJetException {
		Object result = db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				Vector<Object[]> r = new Vector<Object[]>();
				ISqlJetTable tbl_remote_discovery = db.getTable(T_REMOTE_DISCOVERY);
				ISqlJetTable tbl_bundle_mapping = db.getTable(T_BUNDLE_MAPPING);
				ISqlJetTable tbl_proxy = db.getTable(T_PROXY);
				ISqlJetCursor c_remote_discovert = tbl_remote_discovery.open();
				if (!c_remote_discovert.eof()) {
					do {
						String jid = c_remote_discovert.getString(F_REMOTE_JID);
						String service = c_remote_discovert.getString(F_REMOTE_SERVICE);
						Long remote_bundle_id = c_remote_discovert.getInteger(F_REMOTE_BUNDLE);
						ISqlJetCursor c2_bundle_mapping = tbl_bundle_mapping.lookup(I_BUNDLE_MAPPING_REMOTE_JID_BUNDLE,
								jid, remote_bundle_id);
						if (c2_bundle_mapping.eof()) {
							// wait for bundle
						} else {
							Long local_bundle_id = c2_bundle_mapping.getInteger(F_LOCAL_BUNDLE);
							ISqlJetCursor c3_proxy = tbl_proxy.lookup(I_PROXY_SERVICE, service);
							if (c3_proxy.eof()) {
								Object[] row = new Object[3];
								row[0] = jid;
								row[1] = local_bundle_id;
								row[2] = service;
								r.add(row);
							}
							c3_proxy.close();
						}
						c2_bundle_mapping.close();
					} while (c_remote_discovert.next());
				}
				c_remote_discovert.close();
				return r;
			}
		}, SqlJetTransactionMode.READ_ONLY);
		return (Vector<Object[]>) result;
	}

	void setProxy(final Long proxy_id, final Object[] row) throws SqlJetException {
		db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				ISqlJetTable table = db.getTable(T_PROXY);
				Object[] newrow = new Object[row.length + 2];
				newrow[0] = proxy_id;
				for (int i = 1; i < newrow.length - 1; i++) {
					newrow[i] = row[i - 1];
				}
				newrow[newrow.length - 1] = "active";
				table.insert(newrow);
				return true;
			}
		}, SqlJetTransactionMode.WRITE);
	}

	@SuppressWarnings("unchecked")
	Set<Long> getProxyInactive() throws SqlJetException {
		Object result = db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				Set<Long> r = new HashSet<Long>();
				ISqlJetTable tbl_remote_discovery = db.getTable(T_REMOTE_DISCOVERY);
				ISqlJetTable tbl_proxy = db.getTable(T_PROXY);
				ISqlJetCursor c3_proxy = tbl_proxy.open();
				if (!c3_proxy.eof()) {
					do {
						String jid = c3_proxy.getString(F_REMOTE_JID);
						String service = c3_proxy.getString(F_SERVICE);
						ISqlJetCursor c_remote_discovert = tbl_remote_discovery.lookup(
								I_REMOTE_DISCOVERY_REMOTE_JID_SERVICE, jid, service);
						if (c_remote_discovert.eof()) {
							Long proxy_id = c3_proxy.getInteger(F_PROXY_ID);
							r.add(proxy_id);
						}
						c_remote_discovert.close();
					} while (c3_proxy.next());
				}
				c3_proxy.close();
				return r;
			}
		}, SqlJetTransactionMode.READ_ONLY);
		return (Set<Long>) result;
	}

	boolean checkLocalDiscoveryBundle(final Long id) throws SqlJetException {
		Object result = db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				boolean r = false;
				ISqlJetTable table = db.getTable(T_LOCAL_DISCOVERY);
				ISqlJetCursor c = table.lookup(I_LOCAL_DISCOVERY_LOCAL_BUNDLE, id);
				if (!c.eof()) {
					r = true;
				}
				c.close();
				return r;
			}
		}, SqlJetTransactionMode.READ_ONLY);
		return (Boolean) result;
	}

	void setProxyInactive(final Set<Long> proxy_id) throws SqlJetException {
		db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				ISqlJetTable table = db.getTable(T_PROXY);
				for (Iterator<Long> it = proxy_id.iterator(); it.hasNext();) {
					Long id = it.next();
					ISqlJetCursor c = table.lookup(I_PROXY_PROXY_ID, id);
					Map<String, Object> m = new HashMap<String, Object>();
					m.put(F_ACTIVE, "gone");
					c.updateByFieldNames(m);
				}
				return true;
			}
		}, SqlJetTransactionMode.WRITE);
	}

	void setProxyActive(final Set<Long> proxy_id) throws SqlJetException {
		db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				ISqlJetTable table = db.getTable(T_PROXY);
				for (Iterator<Long> it = proxy_id.iterator(); it.hasNext();) {
					Long id = it.next();
					ISqlJetCursor c = table.lookup(I_PROXY_PROXY_ID, id);
					Map<String, Object> m = new HashMap<String, Object>();
					m.put(F_ACTIVE, "active");
					c.updateByFieldNames(m);
				}
				return true;
			}
		}, SqlJetTransactionMode.WRITE);
	}

	@SuppressWarnings("unchecked")
	Set<Long> getProxyRecover() throws SqlJetException {
		Object result = db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				Set<Long> r = new HashSet<Long>();
				ISqlJetTable tbl_remote_discovery = db.getTable(T_REMOTE_DISCOVERY);
				ISqlJetTable tbl_proxy = db.getTable(T_PROXY);
				ISqlJetCursor c3_proxy = tbl_proxy.lookup(I_ACTIVE, "gone");
				if (!c3_proxy.eof()) {
					do {
						String jid = c3_proxy.getString(F_REMOTE_JID);
						String service = c3_proxy.getString(F_SERVICE);
						ISqlJetCursor c_remote_discovert = tbl_remote_discovery.lookup(
								I_REMOTE_DISCOVERY_REMOTE_JID_SERVICE, jid, service);
						if (!c_remote_discovert.eof()) {
							Long proxy_id = c3_proxy.getInteger(F_PROXY_ID);
							r.add(proxy_id);
						}
						c_remote_discovert.close();
					} while (c3_proxy.next());
				}
				c3_proxy.close();
				return r;
			}
		}, SqlJetTransactionMode.READ_ONLY);
		return (Set<Long>) result;
	}

	Long getBundleMappingRemoteBundle(final String target_jid, final long local_bundle_id) throws SqlJetException {
		Object result = db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				Long r = null;
				ISqlJetTable tbl_bundle_mapping = db.getTable(T_BUNDLE_MAPPING);
				ISqlJetCursor c3_bm = tbl_bundle_mapping.lookup(I_BUNDLE_MAPPING_REMOTE_JID_LOCAL_BUNDLE, target_jid,
						local_bundle_id);
				if (!c3_bm.eof()) {
					r = c3_bm.getInteger(F_REMOTE_BUNDLE);
				}
				c3_bm.close();
				return r;
			}
		}, SqlJetTransactionMode.READ_ONLY);
		return (Long) result;
	}

	Long getBundleMappingLocalBundle(final String remote_jid, final Long remote_bundleid) throws SqlJetException {
		Object result = db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				Long r = null;
				ISqlJetTable tbl_bundle_mapping = db.getTable(T_BUNDLE_MAPPING);
				ISqlJetCursor c3_bm = tbl_bundle_mapping.lookup(I_BUNDLE_MAPPING_REMOTE_JID_BUNDLE, remote_jid,
						remote_bundleid);
				if (!c3_bm.eof()) {
					r = c3_bm.getInteger(F_LOCAL_BUNDLE);
				}
				c3_bm.close();
				return r;
			}
		}, SqlJetTransactionMode.READ_ONLY);
		return (Long) result;
	}

	void startMigrateService(final String target_jid, final Long local_bundle_id, final String string)
			throws SqlJetException {
		db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				ISqlJetTable table = db.getTable(T_SERVICE_MIGRATION);
				table.insert(target_jid, local_bundle_id, string);
				return true;
			}
		}, SqlJetTransactionMode.WRITE);
	}

	void endMigrateService(final String remote_jid, final Long local_bundle_id, final String string)
			throws SqlJetException {
		db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				ISqlJetTable table = db.getTable(T_SERVICE_MIGRATION);
				ISqlJetCursor c = table.lookup(I_SERVICE_MIGRATION_ALL, remote_jid, local_bundle_id, string);
				if (!c.eof()) {
					c.delete();
				}
				return true;
			}
		}, SqlJetTransactionMode.WRITE);
	}

	/**********
	 * schema
	 */

	public static final String T_PROPERTY = "T_PROPERTY";
	public static final String F_KEY = "F_KEY";
	public static final String F_VALUE = "F_VALUE";
	public static final String I_PROPERTY_KEY = "I_PROPERTY_KEY";

	public static final String T_LOCAL_DISCOVERY = "T_LOCAL_DISCOVERY";
	public static final String F_LOCAL_SERVICE = "F_LOCAL_SERVICE";
	public static final String F_LOCAL_BUNDLE = "F_LOCAL_BUNDLE";
	public static final String I_LOCAL_DISCOVERY_LOCAL_SERVICE = "I_LOCAL_DISCOVERY_LOCAL_SERVICE";
	public static final String I_LOCAL_DISCOVERY_LOCAL_BUNDLE = "I_LOCAL_DISCOVERY_LOCAL_BUNDLE";

	public static final String T_BUNDLE_MAPPING = "T_BUNDLE_MAPPING";
	// public static final String F_LOCAL_BUNDLE = "F_LOCAL_BUNDLE";
	public static final String F_REMOTE_JID = "F_REMOTE_JID";
	public static final String F_REMOTE_BUNDLE = "F_REMOTE_BUNDLE";
	public static final String I_BUNDLE_MAPPING_REMOTE_JID_BUNDLE = "I_BUNDLE_MAPPING_REMOTE_JID_BUNDLE";
	public static final String I_BUNDLE_MAPPING_LOCAL_BUNDLE = "I_BUNDLE_MAPPING_LOCAL_BUNDLE";
	public static final String I_BUNDLE_MAPPING_REMOTE_JID_LOCAL_BUNDLE = "I_BUNDLE_MAPPING_REMOTE_JID_LOCAL_BUNDLE";

	public static final String T_REMOTE_DISCOVERY = "T_REMOTE_DISCOVERY";
	// public static final String F_REMOTE_JID = "F_REMOTE_JID";
	public static final String F_REMOTE_SERVICE = "F_REMOTE_SERVICE";
	// public static final String F_REMOTE_BUNDLE = "F_REMOTE_BUNDLE";
	public static final String I_REMOTE_DISCOVERY_REMOTE_JID_SERVICE = "I_REMOTE_DISCOVERY_REMOTE_JID_SERVICE";
	public static final String I_REMOTE_DISCOVERY_REMOTE_JID = "I_REMOTE_DISCOVERY_REMOTE_JID";
	public static final String I_REMOTE_DISCOVERY_REMOTE_JID_BUNDLE = "I_REMOTE_DISCOVERY_REMOTE_JID_BUNDLE";

	public static final String T_PROXY = "T_PROXY";
	public static final String F_PROXY_ID = "F_PROXY_ID";
	// public static final String F_REMOTE_JID = "F_REMOTE_JID";
	// public static final String F_LOCAL_BUNDLE = "F_LOCAL_BUNDLE";
	public static final String F_SERVICE = "F_SERVICE";
	public static final String F_ACTIVE = "F_ACTIVE";
	public static final String I_PROXY_SERVICE = "I_PROXY_SERVICE";
	public static final String I_PROXY_LOCAL_BUNDLE = "I_PROXY_LOCAL_BUNDLE";
	public static final String I_PROXY_REMOTE_JID = "I_PROXY_REMOTE_JID";
	public static final String I_PROXY_PROXY_ID = "I_PROXY_PROXY_ID";
	public static final String I_ACTIVE = "I_ACTIVE";

	public static final String T_SERVICE_MIGRATION = "T_SERVICE_MIGRATION";
	public static final String F_TARGET_JID = "F_TARGET_JID";
	// public static final String F_LOCAL_BUNDLE = "F_LOCAL_BUNDLE";
	public static final String F_DIRECTION = "F_DIRECTION";
	public static final String I_SERVICE_MIGRATION_LOCAL_BUNDLE = "I_LOCAL_BUNDLE";
	public static final String I_SERVICE_MIGRATION_TARGET_JID = "I_TARGET_JID";
	public static final String I_SERVICE_MIGRATION_ALL = "I_SERVICE_MIGRATION_ALL";

	public static final String T_STATISTICS = "T_STATISTICS";
	// public static final String F_SERVICE = "F_SERVICE";
	public static final String F_METHOD = "F_METHOD";
	public static final String F_PARAMETER = "F_PARAMETER";
	public static final String F_REMOTE_TIME = "F_REMOTE_TIME";
	public static final String F_MARSHAL_TIME = "F_MARSHAL_TIME";// independent
	public static final String F_EXECUTION_TIME = "F_EXECUTION_TIME";
	public static final String F_TOTAL_COUNT = "F_TOTAL_COUNT";
	public static final String I_STATISTICS_SERVICE_METHOD_PARAMETER = "I_STATISTICS_SERVICE_METHOD_PARAMETER";

	private void createSchema() throws SqlJetException {

		db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				String sql = null;
				sql = "CREATE TABLE " + T_PROPERTY + "(" + F_KEY + " TEXT, " + F_VALUE + " TEXT)";
				db.createTable(sql);
				sql = "CREATE INDEX " + I_PROPERTY_KEY + " ON " + T_PROPERTY + "(" + F_KEY + ")";
				db.createIndex(sql);

				sql = "CREATE TABLE " + T_LOCAL_DISCOVERY + "(" + F_LOCAL_SERVICE + " TEXT, " + F_LOCAL_BUNDLE
						+ " INTEGER)";
				db.createTable(sql);
				sql = "CREATE INDEX " + I_LOCAL_DISCOVERY_LOCAL_SERVICE + " ON " + T_LOCAL_DISCOVERY + "("
						+ F_LOCAL_SERVICE + ")";
				db.createIndex(sql);
				sql = "CREATE INDEX " + I_LOCAL_DISCOVERY_LOCAL_BUNDLE + " ON " + T_LOCAL_DISCOVERY + "("
						+ F_LOCAL_BUNDLE + ")";
				db.createIndex(sql);

				sql = "CREATE TABLE " + T_BUNDLE_MAPPING + "(" + F_LOCAL_BUNDLE + " INTEGER, " + F_REMOTE_JID
						+ " TEXT, " + F_REMOTE_BUNDLE + " INTEGER)";
				db.createTable(sql);
				sql = "CREATE INDEX " + I_BUNDLE_MAPPING_REMOTE_JID_BUNDLE + " ON " + T_BUNDLE_MAPPING + "("
						+ F_REMOTE_JID + ", " + F_REMOTE_BUNDLE + ")";
				db.createIndex(sql);
				sql = "CREATE INDEX " + I_BUNDLE_MAPPING_LOCAL_BUNDLE + " ON " + T_BUNDLE_MAPPING + "("
						+ F_LOCAL_BUNDLE + ")";
				db.createIndex(sql);
				sql = "CREATE INDEX " + I_BUNDLE_MAPPING_REMOTE_JID_LOCAL_BUNDLE + " ON " + T_BUNDLE_MAPPING + "("
						+ F_REMOTE_JID + ", " + F_LOCAL_BUNDLE + ")";
				db.createIndex(sql);

				sql = "CREATE TABLE " + T_REMOTE_DISCOVERY + "(" + F_REMOTE_JID + " TEXT, " + F_REMOTE_SERVICE
						+ " TEXT, " + F_REMOTE_BUNDLE + " INTEGER)";
				db.createTable(sql);
				sql = "CREATE INDEX " + I_REMOTE_DISCOVERY_REMOTE_JID_SERVICE + " ON " + T_REMOTE_DISCOVERY + "("
						+ F_REMOTE_JID + ", " + F_REMOTE_SERVICE + ")";
				db.createIndex(sql);
				sql = "CREATE INDEX " + I_REMOTE_DISCOVERY_REMOTE_JID_BUNDLE + " ON " + T_REMOTE_DISCOVERY + "("
						+ F_REMOTE_JID + ", " + F_REMOTE_BUNDLE + ")";
				db.createIndex(sql);
				sql = "CREATE INDEX " + I_REMOTE_DISCOVERY_REMOTE_JID + " ON " + T_REMOTE_DISCOVERY + "("
						+ F_REMOTE_JID + ")";
				db.createIndex(sql);

				sql = "CREATE TABLE " + T_PROXY + "(" + F_PROXY_ID + " INTEGER, " + F_REMOTE_JID + " TEXT, "
						+ F_LOCAL_BUNDLE + " INTEGER, " + F_SERVICE + " TEXT, " + F_ACTIVE + " TEXT)";
				db.createTable(sql);
				sql = "CREATE INDEX " + I_PROXY_SERVICE + " ON " + T_PROXY + "(" + F_SERVICE + ")";
				db.createIndex(sql);
				sql = "CREATE INDEX " + I_PROXY_LOCAL_BUNDLE + " ON " + T_PROXY + "(" + F_LOCAL_BUNDLE + ")";
				db.createIndex(sql);
				sql = "CREATE INDEX " + I_PROXY_REMOTE_JID + " ON " + T_PROXY + "(" + F_REMOTE_JID + ")";
				db.createIndex(sql);
				sql = "CREATE INDEX " + I_PROXY_PROXY_ID + " ON " + T_PROXY + "(" + F_PROXY_ID + ")";
				db.createIndex(sql);
				sql = "CREATE INDEX " + I_ACTIVE + " ON " + T_PROXY + "(" + F_ACTIVE + ")";
				db.createIndex(sql);

				sql = "CREATE TABLE " + T_SERVICE_MIGRATION + "(" + F_TARGET_JID + " TEXT, " + F_LOCAL_BUNDLE
						+ " INTEGER, " + F_DIRECTION + " TEXT)";
				db.createTable(sql);
				sql = "CREATE INDEX " + I_SERVICE_MIGRATION_LOCAL_BUNDLE + " ON " + T_SERVICE_MIGRATION + "("
						+ F_LOCAL_BUNDLE + ")";
				db.createIndex(sql);
				sql = "CREATE INDEX " + I_SERVICE_MIGRATION_TARGET_JID + " ON " + T_SERVICE_MIGRATION + "("
						+ F_TARGET_JID + ")";
				db.createIndex(sql);
				sql = "CREATE INDEX " + I_SERVICE_MIGRATION_ALL + " ON " + T_SERVICE_MIGRATION + "(" + F_TARGET_JID
						+ ", " + F_LOCAL_BUNDLE + ", " + F_DIRECTION + ")";
				db.createIndex(sql);

				sql = "CREATE TABLE " + T_STATISTICS + "(" + F_SERVICE + " TEXT, " + F_METHOD + " TEXT, " + F_PARAMETER
						+ " TEXT, " + F_REMOTE_TIME + " INTEGER, " + F_MARSHAL_TIME + " INTEGER, " + F_EXECUTION_TIME
						+ " INTEGER, " + F_TOTAL_COUNT + " INTEGER)";
				db.createTable(sql);
				sql = "CREATE INDEX " + I_STATISTICS_SERVICE_METHOD_PARAMETER + " ON " + T_STATISTICS + "(" + F_SERVICE
						+ ", " + F_METHOD + ", " + F_PARAMETER + ")";
				db.createIndex(sql);
				return true;
			}
		}, SqlJetTransactionMode.WRITE);
	}

	void setCalledStatistics(final String object, final String method, final String param, final Long marsharl,
			final Long execution) throws SqlJetException {
		db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				ISqlJetTable table = db.getTable(T_STATISTICS);
				ISqlJetCursor cursor = table.lookup(I_STATISTICS_SERVICE_METHOD_PARAMETER, object, method, param);
				if (cursor.eof()) {
					table.insert(object, method, param, 0, marsharl, execution, 1);
				} else {
					Long mars = cursor.getInteger(F_MARSHAL_TIME);
					Long exec = cursor.getInteger(F_EXECUTION_TIME);
					Long coun = cursor.getInteger(F_TOTAL_COUNT);
					Map<String, Object> m = new HashMap<String, Object>();
					m.put(F_MARSHAL_TIME, mars + marsharl);
					m.put(F_EXECUTION_TIME, exec + execution);
					m.put(F_TOTAL_COUNT, coun + 1);
					cursor.updateByFieldNames(m);
				}
				cursor.close();
				return true;
			}
		}, SqlJetTransactionMode.WRITE);
	}

	void setCallingStatistics(final String object, final String method, final String param, final Long marsharl,
			final Long remote) throws SqlJetException {
		db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				ISqlJetTable table = db.getTable(T_STATISTICS);
				ISqlJetCursor cursor = table.lookup(I_STATISTICS_SERVICE_METHOD_PARAMETER, object, method, param);
				if (cursor.eof()) {
					table.insert(object, method, param, remote, marsharl, 0, 1);
				} else {
					Long mars = cursor.getInteger(F_MARSHAL_TIME);
					Long exec = cursor.getInteger(F_REMOTE_TIME);
					Long coun = cursor.getInteger(F_TOTAL_COUNT);
					Map<String, Object> m = new HashMap<String, Object>();
					m.put(F_MARSHAL_TIME, mars + marsharl);
					m.put(F_REMOTE_TIME, exec + remote);
					m.put(F_TOTAL_COUNT, coun + 1);
					cursor.updateByFieldNames(m);
				}
				cursor.close();
				return true;
			}
		}, SqlJetTransactionMode.WRITE);
	}

	boolean checkMigrateService(final String remote_jid, final long local_bundle_id, final String string)
			throws SqlJetException {
		Boolean ret = (Boolean) db.runTransaction(new ISqlJetTransaction() {
			public Object run(SqlJetDb db) throws SqlJetException {
				ISqlJetTable table = db.getTable(T_SERVICE_MIGRATION);
				ISqlJetCursor cursor = table.lookup(I_SERVICE_MIGRATION_ALL, remote_jid, local_bundle_id, string);
				Boolean ret;
				if (cursor.eof()) {
					ret = false;
				} else {
					ret = true;
				}
				cursor.close();
				return ret;
			}
		}, SqlJetTransactionMode.WRITE);
		return ret;
	}

}

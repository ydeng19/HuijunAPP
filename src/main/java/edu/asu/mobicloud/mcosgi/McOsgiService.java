package edu.asu.mobicloud.mcosgi;

import org.osgi.framework.BundleException;
import org.tmatesoft.sqljet.core.SqlJetException;

public interface McOsgiService {

	/****
	 * offload service to remote
	 * 
	 * @category service offload #3 (final)
	 */
	void ServiceMigration(int push_pull, String target_jid, long local_bundle_id) throws SqlJetException,
			BundleException;

	int SERVICE_MIGRATION_PUSH = 0;
	int SERVICE_MIGRATION_PULL = 1;
}

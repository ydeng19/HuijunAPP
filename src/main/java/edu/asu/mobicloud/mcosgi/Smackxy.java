package edu.asu.mobicloud.mcosgi;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.osgi.service.log.LogService;

public class Smackxy {
	LeafNode leafNode = null;

	Smackx smackx = null;
	SQLJet db = null;

	public Smackxy(SQLJet db, Smackx smackx) {
		this.db = db;
		this.smackx = smackx;
	}

	Felix felix = null;

	void setFelix(Felix felix) {
		this.felix = felix;
	}

	McOsgiImpl mcosgi = null;

	void setMcOsgi(McOsgiImpl mcosgi) {
		this.mcosgi = mcosgi;
	}

	Smack smack = null;

	void setSmack(Smack smack) {
		this.smack = smack;
	}

	/*********
	 * function
	 */
	void connect() throws XMPPException {
		try {
			smackx.pubSub.deleteNode(smack.connection.getUser());
		} catch (Exception e) {
			felix.log(LogService.LOG_DEBUG, "delete self pubsub node fail");
		}
		leafNode = smackx.active.NodeCreationAndConfiguration(smack.connection.getUser());
		leafNode.deleteAllItems();
	}

	void disconnect() throws XMPPException {
		leafNode.deleteAllItems();
		// smackx.pubSub.deleteNode(smack.connection.getUser());
	}
}

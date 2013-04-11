package edu.asu.mobicloud.mcosgi;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.NodeInformationProvider;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.filetransfer.FileTransfer;
import org.jivesoftware.smackx.filetransfer.FileTransfer.Status;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverInfo.Identity;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.packet.DiscoverItems.Item;
import org.jivesoftware.smackx.pubsub.AccessModel;
import org.jivesoftware.smackx.pubsub.ConfigureForm;
import org.jivesoftware.smackx.pubsub.FormType;
import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.PublishModel;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.log.LogService;
import org.tmatesoft.sqljet.core.SqlJetException;

import edu.asu.mobicloud.mcosgi.IQ.MyBundleUpdate;
import edu.asu.mobicloud.mcosgi.IQProvider.MyProviderBundleUpdate;

public class Smackx {
	ServiceDiscoveryManager serviceDiscovery = null;
	PubSubManager pubSub = null;
	FileTransferManager fileTransfer = null;

	Smack smack = null;
	SQLJet db = null;

	public Smackx(SQLJet db, Smack smack) {
		this.db = db;
		this.smack = smack;
	}

	Felix felix = null;

	void setFelix(Felix felix) {
		this.felix = felix;
	}

	McOsgiImpl mcosgi = null;

	void setMcOsgi(McOsgiImpl mcosgi) {
		this.mcosgi = mcosgi;
	}

	Active active = new Active();
	Passive passive = new Passive();

	class Active {

		DiscoverItems DiscoverItemsAssociatedWithAnXmppEntity(String jid, String node) throws XMPPException {
			DiscoverItems items = null;
			if (node == null) {
				items = serviceDiscovery.discoverItems(jid);
			} else {
				items = serviceDiscovery.discoverItems(jid, node);
			}
			return items;
		}

		DiscoverInfo DiscoverInformationAboutAnXmppEntity(String jid, String node) throws XMPPException {
			DiscoverInfo info = null;
			if (node == null) {
				info = serviceDiscovery.discoverInfo(jid);
			} else {
				info = serviceDiscovery.discoverInfo(jid, node);
			}
			return info;
		}

		void SendAFileToAnotherUser(String jid, String file, String description) throws XMPPException {
			OutgoingFileTransfer transfer = fileTransfer.createOutgoingFileTransfer(jid);
			transfer.sendFile(new File(file), description);
		}

		LeafNode NodeCreationAndConfiguration(String node) throws XMPPException {
			ConfigureForm form = new ConfigureForm(FormType.submit);
			form.setAccessModel(AccessModel.open);
			form.setDeliverPayloads(true);
			form.setMaxItems(1);
			form.setPersistentItems(true);
			form.setPublishModel(PublishModel.open);
			LeafNode leaf = (LeafNode) pubSub.createNode(node, form);
			return leaf;
		}

		void PublishingToANode(LeafNode node, SimplePayload payload) throws XMPPException {
			node.send(new PayloadItem<SimplePayload>(StringUtils.randomString(7), payload));
		}

	}

	class Passive {
		void ManageXmppEntityFeatures() {
			serviceDiscovery.addFeature("http://jabber.org/protocol/disco#info");
			serviceDiscovery.addFeature("http://jabber.org/protocol/disco#items");
			serviceDiscovery.addFeature("http://jabber.org/protocol/pubsub");
			serviceDiscovery.addFeature(MC_OSGI_JID_FEATURE);
		}

		void ProvideNodeInformation(final String export_serivce) {
			serviceDiscovery.setNodeInformationProvider(export_serivce, new NodeInformationProvider() {
				public List<String> getNodeFeatures() {
					ArrayList<String> answer = new ArrayList<String>();
					answer.add("http://jabber.org/protocol/disco#info");
					return answer;
				}

				public List<Identity> getNodeIdentities() {
					ArrayList<Identity> answer = new ArrayList<Identity>();
					Identity idty = new Identity(MC_OSGI_SERVICE_NODE_IDENTITY_CATEGORY, null);
					try {
						idty.setType(db.getLocalDiscoveryBundle(export_serivce).toString());
					} catch (SqlJetException e) {
						e.printStackTrace();
					}
					answer.add(idty);
					return answer;
				}

				public List<Item> getNodeItems() {
					return new Vector<Item>();
				}
			});
		}

		void ProvideNodeInformation() throws InvalidSyntaxException, SqlJetException {
			db.deleteFromLocalDiscovery();
			felix.active.DiscoverLocal();

			for (Iterator<String> it = db.getAllLocalDiscoveryService().iterator(); it.hasNext();) {
				String export_serivce = it.next();
				ProvideNodeInformation(export_serivce);
			}

			serviceDiscovery.setNodeInformationProvider(MC_OSGI_INDEX_NODE, new NodeInformationProvider() {
				public List<String> getNodeFeatures() {
					ArrayList<String> answer = new ArrayList<String>();
					answer.add("http://jabber.org/protocol/disco#info");
					answer.add("http://jabber.org/protocol/disco#items");
					return answer;
				}

				public List<Identity> getNodeIdentities() {
					List<Identity> identity = new Vector<Identity>();
					Identity idty_ = new Identity(MC_OSGI_INDEX_NODE_IDENTITY_CATEGORY, null);
					idty_.setType(MC_OSGI_INDEX_NODE_IDENTITY_TYPE);
					identity.add(idty_);
					return identity;
				}

				public List<Item> getNodeItems() {
					Set<String> export_services = null;
					try {
						export_services = db.getAllLocalDiscoveryService();
					} catch (SqlJetException e) {
						e.printStackTrace();
					}
					ArrayList<Item> answer = new ArrayList<Item>();
					for (Iterator<String> it = export_services.iterator(); it.hasNext();) {
						DiscoverItems.Item item = new DiscoverItems.Item(smack.connection.getUser());
						item.setNode(it.next());
						answer.add(item);
					}
					return answer;
				}
			});
		}

		private boolean shouldAccept(FileTransferRequest request) throws NumberFormatException, SqlJetException {
			String jid = request.getRequestor();
			if (jid.contains("/")) {
				jid = jid.substring(0, jid.indexOf("/"));
			}
			if (!smack.roster.contains(jid)) {
				return false;
			}
			if (db.checkBundleMappingRemote(jid, Long.valueOf(request.getDescription()))) {
				return false;
			}
			return true;
		}

		FileTransferListener ftL = null;

		void RecievingAFileFromAnotherUser() {
			ftL = new FileTransferListener() {
				public void fileTransferRequest(FileTransferRequest request) {
					felix.log(LogService.LOG_DEBUG, "RecievingAFileFromAnotherUser");
					try {
						if (shouldAccept(request)) {
							IncomingFileTransfer transfer = request.accept();
							String cache = db.getProperty("storage");
							String file = request.getRequestor() + "_" + request.getDescription() + ".jar";
							file = file.replaceAll("@", "_");
							file = file.replaceAll("/", "_");
							file = cache + "/" + file;
							File f = new File(file);
							transfer.recieveFile(f);
							new Thread(new MonitoringTheProgressOfAFileTransfer(transfer, request, f)).start();
						} else {
							request.reject();
							felix.log(LogService.LOG_DEBUG, "RecievingAFileFromAnotherUser reject");
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			fileTransfer.addFileTransferListener(ftL);
		}

		Map<String, ItemEventListener<PayloadItem<SimplePayload>>> ieLs = new HashMap<String, ItemEventListener<PayloadItem<SimplePayload>>>();

		void ReceivingPubsubMessages(final String remote_jid) throws XMPPException {
			LeafNode node = (LeafNode) pubSub.getNode(remote_jid);
			ItemEventListener<PayloadItem<SimplePayload>> ieL = new ItemEventListener<PayloadItem<SimplePayload>>() {
				public void handlePublishedItems(ItemPublishEvent<PayloadItem<SimplePayload>> items) {
					new Thread(new handlePublishedItems(items, remote_jid)).start();
				}
			};
			node.addItemEventListener(ieL);
			node.subscribe(smack.connection.getUser());
			ieLs.put(remote_jid, ieL);
		}
	}

	Smackxy smackxy = null;

	void setSmack(Smackxy smackxy) {
		this.smackxy = smackxy;
	}

	/*********
	 * function
	 */

	void connect() throws InvalidSyntaxException, SqlJetException {
		serviceDiscovery = ServiceDiscoveryManager.getInstanceFor(smack.connection);
		pubSub = new PubSubManager(smack.connection);
		fileTransfer = new FileTransferManager(smack.connection);

		passive.ManageXmppEntityFeatures();
		passive.ProvideNodeInformation();

		passive.RecievingAFileFromAnotherUser();
	}

	void disconnect() throws XMPPException, SqlJetException {
		fileTransfer.removeFileTransferListener(passive.ftL);

		for (Iterator<Entry<String, ItemEventListener<PayloadItem<SimplePayload>>>> it = passive.ieLs.entrySet()
				.iterator(); it.hasNext();) {
			Entry<String, ItemEventListener<PayloadItem<SimplePayload>>> e = it.next();
			pubSub.getNode(e.getKey()).removeItemEventListener(e.getValue());
		}

		for (Iterator<String> it = db.getAllLocalDiscoveryService().iterator(); it.hasNext();) {
			serviceDiscovery.removeNodeInformationProvider(it.next());
		}
		serviceDiscovery.removeNodeInformationProvider(MC_OSGI_INDEX_NODE);
	}

	/*********
	 * thread
	 */
	class MonitoringTheProgressOfAFileTransfer implements Runnable {
		FileTransfer transfer;
		FileTransferRequest request;
		File f;

		public MonitoringTheProgressOfAFileTransfer(FileTransfer transfer, FileTransferRequest request, File f) {
			super();
			this.transfer = transfer;
			this.request = request;
			this.f = f;
		}

		public void run() {
			while (!transfer.isDone()) {
				if (transfer.getStatus().equals(Status.error)) {
					felix.log(LogService.LOG_DEBUG, "ERROR!!! " + transfer.getError());
				} else {
					felix.log(LogService.LOG_DEBUG, transfer.getStatus() + " - " + transfer.getProgress());
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			try {
				long local_bundle_id = felix.active.installBundle(f, request.getRequestor(),
						Long.valueOf(request.getDescription()));
				mcosgi.ProxyGenerate();
				//
				String remote_jid = request.getRequestor();
				felix.log(LogService.LOG_DEBUG, "file transfer finish, check service migration status");
				if (db.checkMigrateService(remote_jid, local_bundle_id, "push")) {
					felix.context.getBundle(local_bundle_id).stop();
					db.endMigrateService(remote_jid, local_bundle_id, "push");
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			f.delete();
		}

	}

	class handlePublishedItems implements Runnable {
		ItemPublishEvent<PayloadItem<SimplePayload>> items;
		String remote_jid;

		public handlePublishedItems(ItemPublishEvent<PayloadItem<SimplePayload>> items, String remote_jid) {
			super();
			this.items = items;
			this.remote_jid = remote_jid;
		}

		public void run() {
			felix.log(LogService.LOG_DEBUG, "pubsub from " + remote_jid);
			try {
				mcosgi.Discover(remote_jid);
				mcosgi.ProxyUnreg();
				mcosgi.ProxyRecover();
				mcosgi.Prepare(remote_jid);
				MyProviderBundleUpdate buP = new MyProviderBundleUpdate();
				MyBundleUpdate bu = buP.fromXML(items.getItems().iterator().next().getPayload().toXML());
				if (bu.getOnOff() == BundleEvent.STOPPED) {
					Long local_bundle_id = db.getBundleMappingLocalBundle(remote_jid, bu.getId());
					if (local_bundle_id != null) {
						db.endMigrateService(remote_jid, local_bundle_id, "pull");
					}
				} else if (bu.getOnOff() == BundleEvent.STARTED) {
					Long local_bundle_id = db.getBundleMappingLocalBundle(remote_jid, bu.getId());
					if (local_bundle_id != null) {
						felix.context.getBundle(local_bundle_id).stop();
						db.endMigrateService(remote_jid, local_bundle_id, "push");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/*********
	 * constant
	 */
	public static final String MC_OSGI_JID_FEATURE = "MC_OSGI_JID_FEATURE";
	public static final String MC_OSGI_INDEX_NODE = "MC_OSGI_INDEX_NODE";
	public static final String MC_OSGI_SERVICE_NODE_IDENTITY_CATEGORY = "MC_OSGI_SERVICE_NODE_IDENTITY_CATEGORY";
	public static final String MC_OSGI_INDEX_NODE_IDENTITY_CATEGORY = "MC_OSGI_INDEX_NODE_IDENTITY_CATEGORY";
	public static final String MC_OSGI_INDEX_NODE_IDENTITY_TYPE = "MC_OSGI_INDEX_NODE_IDENTITY_TYPE";
	public static final String MC_OSGI_PUBSUB_EMELENT_NAME = "MC_OSGI_PUBSUB_EMELENT_NAME";
	public static final String MC_OSGI_PUBSUB_NAME_SPACE = "MC_OSGI_PUBSUB_NAME_SPACE";

}

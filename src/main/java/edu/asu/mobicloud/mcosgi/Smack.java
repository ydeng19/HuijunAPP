package edu.asu.mobicloud.mcosgi;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.ProviderManager;
import org.osgi.service.log.LogService;
import org.tmatesoft.sqljet.core.SqlJetException;

import edu.asu.mobicloud.mcosgi.IQ.IQPullFile;
import edu.asu.mobicloud.mcosgi.IQ.IQServiceMigration;
import edu.asu.mobicloud.mcosgi.IQ.IQServiceRequest;
import edu.asu.mobicloud.mcosgi.IQ.IQServiceResponse;
import edu.asu.mobicloud.mcosgi.IQProvider.ProviderPullFile;
import edu.asu.mobicloud.mcosgi.IQProvider.ProviderServiceMigration;
import edu.asu.mobicloud.mcosgi.IQProvider.ProviderServiceRequest;
import edu.asu.mobicloud.mcosgi.IQProvider.ProviderServiceResponse;

public class Smack {
	Connection connection = null;
	Roster roster = null;

	SQLJet db = null;

	public Smack(SQLJet db) {
		this.db = db;
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

		Set<Presence> getAllPresences() {
			Set<Presence> r = new HashSet<Presence>();
			Collection<RosterEntry> c_re = roster.getEntries();
			for (Iterator<RosterEntry> it = c_re.iterator(); it.hasNext();) {
				RosterEntry re = it.next();
				String remote_jid = re.getUser();
				Presence p = roster.getPresence(remote_jid);
				r.add(p);
			}
			return r;
		}

		String rmiRequest(String server_jid, String service_name, String json) throws InterruptedException {
			IQServiceRequest iq = new IQServiceRequest();
			iq.setHead(service_name);
			iq.setBody(json);
			iq.setTo(server_jid);
			//felix.log(LogService.LOG_DEBUG, "iq id " + iq.getPacketID());
			PacketFilter filter = new AndFilter(new PacketIDFilter(iq.getPacketID()), new PacketTypeFilter(
					IQServiceResponse.class));
			PacketCollector myCollector = connection.createPacketCollector(filter);
			connection.sendPacket(iq);
			Packet p = myCollector.nextResult();
			IQServiceResponse iq_ = (IQServiceResponse) p;
			String response = iq_.getBody();
			return response;
		}

		void rmiResponse(String from, String packetID, String service, String json) {
			IQServiceResponse iq = new IQServiceResponse();
			iq.setHead(service);
			iq.setBody(json);
			iq.setTo(from);
			iq.setPacketID(packetID);
			iq.setType(IQ.Type.RESULT);
			connection.sendPacket(iq);
		}
	}

	class Passive {
		PacketListener rmiRequestL = null;
		PacketListener pullFileL = null;
		PacketListener smL = null;

		void ProcessingIncomingPackets() {
			PacketFilter filter = new PacketTypeFilter(IQServiceRequest.class);
			rmiRequestL = new PacketListener() {

				public void processPacket(Packet p) {
					//felix.log(LogService.LOG_DEBUG, "processPacket " + IQServiceRequest.class.getName());
					mcosgi.handleRmiRequest(p);
				}

			};
			connection.addPacketListener(rmiRequestL, filter);
			//
			PacketFilter filter_file = new PacketTypeFilter(IQPullFile.class);
			pullFileL = new PacketListener() {

				public void processPacket(Packet p) {
					felix.log(LogService.LOG_DEBUG, "processPacket " + IQPullFile.class.getName());
					IQPullFile iqFile = (IQPullFile) p;
					Long bundle_id = iqFile.getRemote_bunlde_id();
					//
					try {
						String bundleJarFilePath = felix.active.getJarPath(bundle_id);
						smackx.active.SendAFileToAnotherUser(iqFile.getFrom(), bundleJarFilePath, bundle_id.toString());
						felix.log(LogService.LOG_DEBUG, "processPacket SendAFileToAnotherUser");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			};
			connection.addPacketListener(pullFileL, filter_file);
			//
			PacketFilter filter_servicemigration = new PacketTypeFilter(IQServiceMigration.class);
			smL = new PacketListener() {

				public void processPacket(Packet p) {
					felix.log(LogService.LOG_DEBUG, "processPacket " + IQServiceMigration.class.getName());
					IQServiceMigration iq_sm = (IQServiceMigration) p;
					Long bundle_id = iq_sm.getBundle_id();
					Integer on_off = iq_sm.getOn_off();
					//
					try {
						if (on_off == McOsgiService.SERVICE_MIGRATION_PULL) {
							felix.context.getBundle(bundle_id).stop();
						} else if (on_off == McOsgiService.SERVICE_MIGRATION_PUSH) {
							Long local_bundle_id = db.getBundleMappingLocalBundle(p.getFrom(), bundle_id);
							felix.context.getBundle(local_bundle_id).start();
						}
						felix.log(LogService.LOG_DEBUG, "processPacket SERVICE_MIGRATION");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			};
			connection.addPacketListener(smL, filter_servicemigration);
		}

		ConnectionListener cL = null;

		void addConnectionListener() {
			cL = new ConnectionListener() {
				void connectionLost() {
					try {
						db.deleteFromRemoteDiscovery();
					} catch (SqlJetException e) {
						e.printStackTrace();
					}
					try {
						mcosgi.ProxyUnreg();
					} catch (SqlJetException e) {
						e.printStackTrace();
					}
				}

				public void connectionClosed() {
					connectionLost();
				}

				public void connectionClosedOnError(Exception arg0) {
					connectionLost();
				}

				public void reconnectingIn(int arg0) {
				}

				public void reconnectionFailed(Exception arg0) {
					connectionLost();
				}

				public void reconnectionSuccessful() {
				}
			};
			connection.addConnectionListener(cL);
		}

		RosterListener rL = null;

		void ListeningForRosterAndPresenceChanges() {
			rL = new RosterListener() {
				public void entriesAdded(Collection<String> arg0) {
				}

				public void entriesDeleted(Collection<String> arg0) {
				}

				public void entriesUpdated(Collection<String> arg0) {
				}

				public void presenceChanged(Presence arg0) {
					new Thread(new presenceChanged(arg0)).start();
				}
			};
			roster.addRosterListener(rL);
		}
	}

	Smackx smackx = null;
	Smackxy smackxy = null;

	void setSmack(Smackx smackx, Smackxy smackxy) {
		this.smackx = smackx;
		this.smackxy = smackxy;
	}

	/************
	 * function
	 */

	// smack 3.2.2
	private void addProviderSmack() {
		ProviderManager pm = ProviderManager.getInstance();
		// Private Data Storage
		pm.addIQProvider("query", "jabber:iq:private",
				new org.jivesoftware.smackx.PrivateDataManager.PrivateDataIQProvider());
		// Time
		try {
			pm.addIQProvider("query", "jabber:iq:time", Class.forName("org.jivesoftware.smackx.packet.Time"));
		} catch (ClassNotFoundException e) {
		}
		// Roster Exchange
		pm.addExtensionProvider("x", "jabber:x:roster", new org.jivesoftware.smackx.provider.RosterExchangeProvider());
		// Message Events
		pm.addExtensionProvider("x", "jabber:x:event", new org.jivesoftware.smackx.provider.MessageEventProvider());
		// Chat State
		pm.addExtensionProvider("active", "http://jabber.org/protocol/chatstates",
				new org.jivesoftware.smackx.packet.ChatStateExtension.Provider());
		pm.addExtensionProvider("composing", "http://jabber.org/protocol/chatstates",
				new org.jivesoftware.smackx.packet.ChatStateExtension.Provider());
		pm.addExtensionProvider("paused", "http://jabber.org/protocol/chatstates",
				new org.jivesoftware.smackx.packet.ChatStateExtension.Provider());
		pm.addExtensionProvider("inactive", "http://jabber.org/protocol/chatstates",
				new org.jivesoftware.smackx.packet.ChatStateExtension.Provider());
		pm.addExtensionProvider("gone", "http://jabber.org/protocol/chatstates",
				new org.jivesoftware.smackx.packet.ChatStateExtension.Provider());
		// XHTML
		pm.addExtensionProvider("html", "http://jabber.org/protocol/xhtml-im",
				new org.jivesoftware.smackx.provider.XHTMLExtensionProvider());
		// Group Chat Invitations
		pm.addExtensionProvider("x", "jabber:x:conference", new org.jivesoftware.smackx.GroupChatInvitation.Provider());
		// Service Discovery # Items
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#items",
				new org.jivesoftware.smackx.provider.DiscoverItemsProvider());
		// Service Discovery # Info
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#info",
				new org.jivesoftware.smackx.provider.DiscoverInfoProvider());
		// Data Forms
		pm.addExtensionProvider("x", "jabber:x:data", new org.jivesoftware.smackx.provider.DataFormProvider());
		// MUC User
		pm.addExtensionProvider("x", "http://jabber.org/protocol/muc#user",
				new org.jivesoftware.smackx.provider.MUCUserProvider());
		// MUC Admin
		pm.addIQProvider("query", "http://jabber.org/protocol/muc#admin",
				new org.jivesoftware.smackx.provider.MUCAdminProvider());
		// MUC Owner
		pm.addIQProvider("query", "http://jabber.org/protocol/muc#owner",
				new org.jivesoftware.smackx.provider.MUCOwnerProvider());
		// Delayed Delivery
		pm.addExtensionProvider("delay", "urn:xmpp:delay", new org.jivesoftware.smackx.provider.DelayInfoProvider());
		// Version
		try {
			pm.addIQProvider("query", "jabber:iq:version", Class.forName("org.jivesoftware.smackx.packet.Version"));
		} catch (ClassNotFoundException e) {
		}
		// VCard
		pm.addIQProvider("vCard", "vcard-temp", new org.jivesoftware.smackx.provider.VCardProvider());
		// Offline Message Requests
		pm.addIQProvider("offline", "http://jabber.org/protocol/offline",
				new org.jivesoftware.smackx.packet.OfflineMessageRequest.Provider());
		// Offline Message Indicator
		pm.addExtensionProvider("offline", "http://jabber.org/protocol/offline",
				new org.jivesoftware.smackx.packet.OfflineMessageInfo.Provider());
		// Last Activity
		pm.addIQProvider("query", "jabber:iq:last", new org.jivesoftware.smackx.packet.LastActivity.Provider());
		// User Search
		pm.addIQProvider("query", "jabber:iq:search", new org.jivesoftware.smackx.search.UserSearch.Provider());
		// SharedGroupsInfo
		pm.addIQProvider("sharedgroup", "http://www.jivesoftware.org/protocol/sharedgroup",
				new org.jivesoftware.smackx.packet.SharedGroupsInfo.Provider());
		// JEP-33: Extended Stanza Addressing
		pm.addExtensionProvider("addresses", "http://jabber.org/protocol/address",
				new org.jivesoftware.smackx.provider.MultipleAddressesProvider());
		// FileTransfer
		pm.addIQProvider("si", "http://jabber.org/protocol/si",
				new org.jivesoftware.smackx.provider.StreamInitiationProvider());
		pm.addIQProvider("query", "http://jabber.org/protocol/bytestreams",
				new org.jivesoftware.smackx.bytestreams.socks5.provider.BytestreamsProvider());
		pm.addIQProvider("open", "http://jabber.org/protocol/ibb",
				new org.jivesoftware.smackx.bytestreams.ibb.provider.OpenIQProvider());
		pm.addIQProvider("data", "http://jabber.org/protocol/ibb",
				new org.jivesoftware.smackx.bytestreams.ibb.provider.DataPacketProvider());
		pm.addIQProvider("close", "http://jabber.org/protocol/ibb",
				new org.jivesoftware.smackx.bytestreams.ibb.provider.CloseIQProvider());
		pm.addExtensionProvider("data", "http://jabber.org/protocol/ibb",
				new org.jivesoftware.smackx.bytestreams.ibb.provider.DataPacketProvider());
		// Privacy
		pm.addIQProvider("query", "jabber:iq:privacy", new org.jivesoftware.smack.provider.PrivacyProvider());
		// Ad-Hoc Command
		pm.addIQProvider("command", "http://jabber.org/protocol/commands",
				new org.jivesoftware.smackx.provider.AdHocCommandDataProvider());
		pm.addExtensionProvider("bad-action", "http://jabber.org/protocol/commands",
				new org.jivesoftware.smackx.provider.AdHocCommandDataProvider.BadActionError());
		pm.addExtensionProvider("malformed-action", "http://jabber.org/protocol/commands",
				new org.jivesoftware.smackx.provider.AdHocCommandDataProvider.MalformedActionError());
		pm.addExtensionProvider("bad-locale", "http://jabber.org/protocol/commands",
				new org.jivesoftware.smackx.provider.AdHocCommandDataProvider.BadLocaleError());
		pm.addExtensionProvider("bad-payload", "http://jabber.org/protocol/commands",
				new org.jivesoftware.smackx.provider.AdHocCommandDataProvider.BadPayloadError());
		pm.addExtensionProvider("bad-sessionid", "http://jabber.org/protocol/commands",
				new org.jivesoftware.smackx.provider.AdHocCommandDataProvider.BadSessionIDError());
		pm.addExtensionProvider("session-expired", "http://jabber.org/protocol/commands",
				new org.jivesoftware.smackx.provider.AdHocCommandDataProvider.SessionExpiredError());
		// Fastpath providers
		pm.addIQProvider("offer", "http://jabber.org/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.packet.OfferRequestProvider());
		pm.addIQProvider("offer-revoke", "http://jabber.org/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.packet.OfferRevokeProvider());
		pm.addIQProvider("agent-status-request", "http://jabber.org/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.packet.AgentStatusRequest.Provider());
		pm.addIQProvider("transcripts", "http://jivesoftware.com/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.packet.TranscriptsProvider());
		pm.addIQProvider("transcript", "http://jivesoftware.com/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.packet.TranscriptProvider());
		pm.addIQProvider("workgroups", "http://jabber.org/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.packet.AgentWorkgroups.Provider());
		pm.addIQProvider("agent-info", "http://jivesoftware.com/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.packet.AgentInfo.Provider());
		pm.addIQProvider("transcript-search", "http://jivesoftware.com/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.packet.TranscriptSearch.Provider());
		pm.addIQProvider("occupants-info", "http://jivesoftware.com/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.packet.OccupantsInfo.Provider());
		pm.addIQProvider("chat-settings", "http://jivesoftware.com/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.settings.ChatSettings.InternalProvider());
		pm.addIQProvider("chat-notes", "http://jivesoftware.com/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.ext.notes.ChatNotes.Provider());
		pm.addIQProvider("chat-sessions", "http://jivesoftware.com/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.ext.history.AgentChatHistory.InternalProvider());
		pm.addIQProvider("offline-settings", "http://jivesoftware.com/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.settings.OfflineSettings.InternalProvider());
		pm.addIQProvider("sound-settings", "http://jivesoftware.com/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.settings.SoundSettings.InternalProvider());
		pm.addIQProvider("workgroup-properties", "http://jivesoftware.com/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.settings.WorkgroupProperties.InternalProvider());
		pm.addIQProvider("search-settings", "http://jivesoftware.com/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.settings.SearchSettings.InternalProvider());
		pm.addIQProvider("workgroup-form", "http://jivesoftware.com/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.ext.forms.WorkgroupForm.InternalProvider());
		pm.addIQProvider("macros", "http://jivesoftware.com/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.ext.macros.Macros.InternalProvider());
		pm.addIQProvider("chat-metadata", "http://jivesoftware.com/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.ext.history.ChatMetadata.Provider());
		pm.addIQProvider("generic-metadata", "http://jivesoftware.com/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.settings.GenericSettings.InternalProvider());
		pm.addIQProvider("monitor", "http://jivesoftware.com/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.packet.MonitorPacket.InternalProvider());
		// Packet Extension Providers
		pm.addExtensionProvider("queue-status", "http://jabber.org/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.packet.QueueUpdate.Provider());
		pm.addExtensionProvider("workgroup", "http://jabber.org/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.packet.WorkgroupInformation.Provider());
		pm.addExtensionProvider("metadata", "http://jivesoftware.com/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.packet.MetaDataProvider());
		pm.addExtensionProvider("session", "http://jivesoftware.com/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.packet.SessionID.Provider());
		pm.addExtensionProvider("user", "http://jivesoftware.com/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.packet.UserID.Provider());
		pm.addExtensionProvider("agent-status", "http://jabber.org/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.packet.AgentStatus.Provider());
		pm.addExtensionProvider("notify-queue-details", "http://jabber.org/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.packet.QueueDetails.Provider());
		pm.addExtensionProvider("notify-queue", "http://jabber.org/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.packet.QueueOverview.Provider());
		pm.addExtensionProvider("invite", "http://jabber.org/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.packet.RoomInvitation.Provider());
		pm.addExtensionProvider("transfer", "http://jabber.org/protocol/workgroup",
				new org.jivesoftware.smackx.workgroup.packet.RoomTransfer.Provider());
		// SHIM
		pm.addExtensionProvider("headers", "http://jabber.org/protocol/shim",
				new org.jivesoftware.smackx.provider.HeadersProvider());
		pm.addExtensionProvider("header", "http://jabber.org/protocol/shim",
				new org.jivesoftware.smackx.provider.HeaderProvider());
		// XEP-0060 pubsub
		pm.addIQProvider("pubsub", "http://jabber.org/protocol/pubsub",
				new org.jivesoftware.smackx.pubsub.provider.PubSubProvider());
		pm.addExtensionProvider("create", "http://jabber.org/protocol/pubsub",
				new org.jivesoftware.smackx.pubsub.provider.SimpleNodeProvider());
		pm.addExtensionProvider("items", "http://jabber.org/protocol/pubsub",
				new org.jivesoftware.smackx.pubsub.provider.ItemsProvider());
		pm.addExtensionProvider("item", "http://jabber.org/protocol/pubsub",
				new org.jivesoftware.smackx.pubsub.provider.ItemProvider());
		pm.addExtensionProvider("subscriptions", "http://jabber.org/protocol/pubsub",
				new org.jivesoftware.smackx.pubsub.provider.SubscriptionsProvider());
		pm.addExtensionProvider("subscription", "http://jabber.org/protocol/pubsub",
				new org.jivesoftware.smackx.pubsub.provider.SubscriptionProvider());
		pm.addExtensionProvider("affiliations", "http://jabber.org/protocol/pubsub",
				new org.jivesoftware.smackx.pubsub.provider.AffiliationsProvider());
		pm.addExtensionProvider("affiliation", "http://jabber.org/protocol/pubsub",
				new org.jivesoftware.smackx.pubsub.provider.AffiliationProvider());
		pm.addExtensionProvider("options", "http://jabber.org/protocol/pubsub",
				new org.jivesoftware.smackx.pubsub.provider.FormNodeProvider());
		// XEP-0060 pubsub#owner
		pm.addIQProvider("pubsub", "http://jabber.org/protocol/pubsub#owner",
				new org.jivesoftware.smackx.pubsub.provider.PubSubProvider());
		pm.addExtensionProvider("create", "http://jabber.org/protocol/pubsub",
				new org.jivesoftware.smackx.pubsub.provider.SimpleNodeProvider());
		pm.addExtensionProvider("configure", "http://jabber.org/protocol/pubsub",
				new org.jivesoftware.smackx.pubsub.provider.FormNodeProvider());
		pm.addExtensionProvider("default", "http://jabber.org/protocol/pubsub#owner",
				new org.jivesoftware.smackx.pubsub.provider.FormNodeProvider());
		// XEP-0060 pubsub#event
		pm.addExtensionProvider("event", "http://jabber.org/protocol/pubsub#event",
				new org.jivesoftware.smackx.pubsub.provider.EventProvider());
		pm.addExtensionProvider("configuration", "http://jabber.org/protocol/pubsub#event",
				new org.jivesoftware.smackx.pubsub.provider.ConfigEventProvider());
		pm.addExtensionProvider("delete", "http://jabber.org/protocol/pubsub#event",
				new org.jivesoftware.smackx.pubsub.provider.SimpleNodeProvider());
		pm.addExtensionProvider("options", "http://jabber.org/protocol/pubsub#event",
				new org.jivesoftware.smackx.pubsub.provider.FormNodeProvider());
		pm.addExtensionProvider("items", "http://jabber.org/protocol/pubsub#event",
				new org.jivesoftware.smackx.pubsub.provider.ItemsProvider());
		pm.addExtensionProvider("item", "http://jabber.org/protocol/pubsub#event",
				new org.jivesoftware.smackx.pubsub.provider.ItemProvider());
		pm.addExtensionProvider("retract", "http://jabber.org/protocol/pubsub#event",
				new org.jivesoftware.smackx.pubsub.provider.RetractEventProvider());
		pm.addExtensionProvider("purge", "http://jabber.org/protocol/pubsub#event",
				new org.jivesoftware.smackx.pubsub.provider.SimpleNodeProvider());
		// Nick Exchange
		pm.addExtensionProvider("nick", "http://jabber.org/protocol/nick",
				new org.jivesoftware.smackx.packet.Nick.Provider());
		// Attention
		pm.addExtensionProvider("attention", "urn:xmpp:attention:0",
				new org.jivesoftware.smackx.packet.AttentionExtension.Provider());
	}

	private void addProviderMcosgi() {
		ProviderManager pm = ProviderManager.getInstance();
		// MC OSGi
		pm.addIQProvider(IQPullFile.ELEMENT, IQPullFile.NAMESPACE, new ProviderPullFile());
		pm.addIQProvider(IQServiceRequest.ELEMENT, IQServiceRequest.NAMESPACE, new ProviderServiceRequest());
		pm.addIQProvider(IQServiceResponse.ELEMENT, IQServiceResponse.NAMESPACE, new ProviderServiceResponse());
		pm.addIQProvider(IQServiceMigration.ELEMENT, IQServiceMigration.NAMESPACE, new ProviderServiceMigration());
	}

	void connect() throws Exception {

		ConnectionConfiguration config = new ConnectionConfiguration(db.getProperty("openfire"), 5222);
		config.setSASLAuthenticationEnabled(false);
//		config.setDebuggerEnabled(true); // debug
		config.setSendPresence(false);

		addProviderSmack();
		addProviderMcosgi();

		connection = new XMPPConnection(config);
		roster = connection.getRoster();

		passive.ListeningForRosterAndPresenceChanges();
		connection.connect();
		connection.login(db.getProperty("username"), db.getProperty("password"), db.getProperty("resource"));
		passive.ProcessingIncomingPackets();
	}

	void disconnect() {
		connection.removePacketListener(passive.pullFileL);
		connection.removePacketListener(passive.smL);
		connection.removePacketListener(passive.rmiRequestL);
		roster.removeRosterListener(passive.rL);
		connection.removeConnectionListener(passive.cL);
		connection.disconnect();
	}

	/**********
	 * thread
	 */
	class presenceChanged implements Runnable {
		Presence presence = null;

		public presenceChanged(Presence arg0) {
			super();
			this.presence = arg0;
		}

		public void run() {
			try {
				String jid = presence.getFrom();
				if (presence.isAvailable()) {
					felix.log(LogService.LOG_DEBUG, "presenceChanged - " + jid + " online");
					mcosgi.Discover(jid);
					felix.log(LogService.LOG_DEBUG, "presenceChanged - Discover finish");
					smackx.passive.ReceivingPubsubMessages(jid);
					felix.log(LogService.LOG_DEBUG, "presenceChanged - ReceivingPubsubMessages finish");
					mcosgi.ProxyRecover();
					felix.log(LogService.LOG_DEBUG, "presenceChanged - ProxyRecover finish");
					mcosgi.Prepare(jid);
					felix.log(LogService.LOG_DEBUG, "presenceChanged - Prepare finish");
				} else {
					db.deleteRemoteDiscoveryJid(jid);
					mcosgi.ProxyUnreg();
					smackx.pubSub.getNode(jid).removeItemEventListener(smackx.passive.ieLs.get(jid));
					smackx.passive.ieLs.remove(jid);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
}

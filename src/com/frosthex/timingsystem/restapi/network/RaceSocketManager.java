package com.frosthex.timingsystem.restapi.network;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import com.frosthex.timingsystem.restapi.TimingSystemRESTApiPlugin;
import com.frosthex.timingsystem.restapi.utils.HeatJson;
import com.frosthex.timingsystem.restapi.utils.Messager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.heat.Heat;

/**
 * WebSocket endpoint for live race telemetry.
 * Endpoint: ws://host:port/ws/v1/live?api_key=YOUR_KEY
 */
@WebSocket
public class RaceSocketManager {

	private static final Set<Session> sessions = Collections.newSetFromMap(new ConcurrentHashMap<>());

	@OnWebSocketConnect
	public void onConnect(Session session) {
		if (!isAuthenticated(session)) {
			session.close(4401, "Invalid or missing api_key.");
			return;
		}
		sessions.add(session);
		String snapshot = buildRunningHeatsMessage();
		if (snapshot != null) {
			sendTo(session, snapshot);
		}
	}

	@OnWebSocketClose
	public void onClose(Session session, int statusCode, String reason) {
		sessions.remove(session);
	}

	@OnWebSocketError
	public void onError(Session session, Throwable error) {
		sessions.remove(session);
	}

	@OnWebSocketMessage
	public void onMessage(Session session, String message) {
		if (message != null && message.toLowerCase().contains("snapshot")) {
			String snapshot = buildRunningHeatsMessage();
			if (snapshot != null) {
				sendTo(session, snapshot);
			}
		}
	}

	/**
	 * Fan a message out to every connected session.
	 */
	public static void broadcast(String json) {
		if (json == null) {
			return;
		}
		for (Session session : sessions) {
			sendTo(session, json);
		}
	}

	public static int getSessionCount() {
		return sessions.size();
	}

	public static void closeAll() {
		for (Session session : sessions) {
			try {
				session.close();
			} catch (Exception ignored) {
			}
		}
		sessions.clear();
	}

	private static void sendTo(Session session, String json) {
		try {
			if (session.isOpen()) {
				session.getRemote().sendStringByFuture(json);
			} else {
				sessions.remove(session);
			}
		} catch (Exception e) {
			sessions.remove(session);
		}
	}

	/**
	 * Build a {@code running_heats} message containing a full snapshot of every running heat.
	 * Returns null if live state couldn't be read cleanly.
	 */
	private static String buildRunningHeatsMessage() {
		try {
			JsonArray heatsArray = new JsonArray();
			List<Heat> heats = TimingSystemAPI.getRunningHeats();
			if (heats != null) {
				for (Heat heat : heats) {
					heatsArray.add(HeatJson.heatToJson(heat));
				}
			}
			JsonObject message = new JsonObject();
			message.addProperty("type", "running_heats");
			message.add("heats", heatsArray);
			return message.toString();
		} catch (Exception e) {
			return null;
		}
	}

	private static boolean isAuthenticated(Session session) {
		List<String> readOnlyKeys = TimingSystemRESTApiPlugin.getInstance().getConfig().getStringList("api_keys.read_only");
		// No keys configured -> open access (matches an unconfigured, out-of-the-box setup).
		if (readOnlyKeys == null || readOnlyKeys.isEmpty()) {
			return true;
		}
		try {
			List<String> provided = session.getUpgradeRequest().getParameterMap().get("api_key");
			if (provided != null) {
				for (String candidate : provided) {
					for (String key : readOnlyKeys) {
						if (key.equalsIgnoreCase(candidate)) {
							return true;
						}
					}
				}
			}
		} catch (Exception e) {
			Messager.msgConsole("&c[WARN] Failed to read api_key from websocket handshake: " + e);
		}
		return false;
	}
}

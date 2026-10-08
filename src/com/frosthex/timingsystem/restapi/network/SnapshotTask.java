package com.frosthex.timingsystem.restapi.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.scheduler.BukkitRunnable;

import com.frosthex.timingsystem.restapi.utils.HeatJson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import me.makkuusen.timing.system.api.DriverDetails;
import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.heat.Heat;

public class SnapshotTask extends BukkitRunnable {

	private final Map<Integer, List<String>> lastOrder = new HashMap<>();

	@Override
	public void run() {
		if (RaceSocketManager.getSessionCount() == 0) {
			lastOrder.clear();
			return;
		}

		List<Heat> heats;
		try {
			heats = TimingSystemAPI.getRunningHeats();
		} catch (Exception e) {
			return;
		}
		if (heats == null) {
			heats = new ArrayList<>();
		}

		Set<Integer> currentHeatIds = new HashSet<>();

		for (Heat heat : heats) {
			int heatId = heat.getId();
			currentHeatIds.add(heatId);
			try {
				List<DriverDetails> details = TimingSystemAPI.getAllDriverDetailsFromHeat(heat);

				if (!lastOrder.containsKey(heatId)) {
					JsonObject startMsg = new JsonObject();
					startMsg.addProperty("type", "heat_start");
					startMsg.addProperty("timestamp", System.currentTimeMillis());
					startMsg.addProperty("heat_id", heatId);
					startMsg.addProperty("heat_laps", heat.getTotalLaps());
					startMsg.addProperty("heat_pits", heat.getTotalPits());
					startMsg.addProperty("heat_name", heat.getName());
					if (heat.getEvent() != null) {
						startMsg.addProperty("event_name", heat.getEvent().getDisplayName());
					}
					RaceSocketManager.broadcast(startMsg.toString());
				}

				// Full snapshot
				JsonObject snapshot = new JsonObject();
				snapshot.addProperty("type", "snapshot");
				snapshot.addProperty("timestamp", System.currentTimeMillis());
				snapshot.add("heat", HeatJson.heatToJson(heat));
				RaceSocketManager.broadcast(snapshot.toString());

				// Position change detection
				List<String> order = new ArrayList<>(details.size());
				for (DriverDetails dd : details) {
					order.add(dd.getUuid());
				}
				List<String> previous = lastOrder.get(heatId);
				if (previous != null && !previous.equals(order)) {
					JsonArray orderArray = new JsonArray();
					for (String uuid : order) {
						orderArray.add(uuid);
					}
					JsonObject posMsg = new JsonObject();
					posMsg.addProperty("type", "position_change");
					posMsg.addProperty("timestamp", System.currentTimeMillis());
					posMsg.addProperty("heat_id", heatId);
					posMsg.add("order", orderArray);
					RaceSocketManager.broadcast(posMsg.toString());
				}
				lastOrder.put(heatId, order);
			} catch (Exception e) {
			}
		}

		lastOrder.keySet().removeIf(heatId -> {
			if (!currentHeatIds.contains(heatId)) {
				JsonObject finishMsg = new JsonObject();
				finishMsg.addProperty("type", "heat_finish");
				finishMsg.addProperty("timestamp", System.currentTimeMillis());
				finishMsg.addProperty("heat_id", heatId);
				RaceSocketManager.broadcast(finishMsg.toString());
				return true;
			}
			return false;
		});
	}
}

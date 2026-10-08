package com.frosthex.timingsystem.restapi.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.frosthex.timingsystem.restapi.network.RaceSocketManager;
import com.google.gson.JsonObject;

import me.makkuusen.timing.system.api.events.HeatFinishEvent;
import me.makkuusen.timing.system.api.events.TimeTrialFinishEvent;
import me.makkuusen.timing.system.api.events.TimeTrialStartEvent;
import me.makkuusen.timing.system.api.events.driver.DriverDisqualifyEvent;
import me.makkuusen.timing.system.api.events.driver.DriverFinishHeatEvent;
import me.makkuusen.timing.system.api.events.driver.DriverFinishLapEvent;
import me.makkuusen.timing.system.api.events.driver.DriverNewLapEvent;
import me.makkuusen.timing.system.api.events.driver.DriverPassCheckpointEvent;
import me.makkuusen.timing.system.api.events.driver.DriverPassPitEvent;
import me.makkuusen.timing.system.api.events.driver.DriverStartEvent;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.Lap;
import me.makkuusen.timing.system.participant.Driver;

/**
 * Listens to TimingSystem's Bukkit events and pushes typed JSON deltas over the websocket.
 * All handlers run on the main server thread; the actual socket send is non-blocking.
 */
public class RaceEventListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR)
	public void onDriverStart(DriverStartEvent event) {
		JsonObject msg = base("driver_start", event.getDriver());
		RaceSocketManager.broadcast(msg.toString());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onDriverNewLap(DriverNewLapEvent event) {
		JsonObject msg = base("driver_new_lap", event.getDriver());
		msg.addProperty("lap_number", event.getDriver().getLaps().size());
		RaceSocketManager.broadcast(msg.toString());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onDriverFinishLap(DriverFinishLapEvent event) {
		JsonObject msg = base("lap_finish", event.getDriver());
		Lap lap = event.getLap();
		if (lap != null) {
			msg.addProperty("lap_time", lap.getLapTime());
			msg.addProperty("precise_lap_time", lap.getPreciseLapTime());
			msg.addProperty("pitted", lap.isPitted());
		}
		msg.addProperty("is_new_fastest_lap", event.getIsNewFastestLap());
		RaceSocketManager.broadcast(msg.toString());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onDriverPassCheckpoint(DriverPassCheckpointEvent event) {
		JsonObject msg = base("checkpoint", event.getDriver());
		Lap lap = event.getLap();
		if (lap != null) {
			msg.addProperty("checkpoint", lap.getLatestCheckpoint());
		}
		if (event.getTime() != null) {
			msg.addProperty("time", event.getTime().toEpochMilli());
		}
		RaceSocketManager.broadcast(msg.toString());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onDriverPassPit(DriverPassPitEvent event) {
		JsonObject msg = base("pit", event.getDriver());
		msg.addProperty("pits", event.getNewPits());
		RaceSocketManager.broadcast(msg.toString());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onDriverFinishHeat(DriverFinishHeatEvent event) {
		JsonObject msg = base("driver_finish", event.getDriver());
		RaceSocketManager.broadcast(msg.toString());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onDriverDisqualify(DriverDisqualifyEvent event) {
		JsonObject msg = base("driver_disqualify", event.getDriver());
		RaceSocketManager.broadcast(msg.toString());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onHeatFinish(HeatFinishEvent event) {
		Heat heat = event.getHeat();
		JsonObject msg = envelope("heat_finish");
		if (heat != null) {
			msg.addProperty("heat_id", heat.getId());
			msg.addProperty("heat_name", heat.getName());
			if (heat.getEvent() != null) {
				msg.addProperty("event_name", heat.getEvent().getDisplayName());
			}
		}
		RaceSocketManager.broadcast(msg.toString());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onTimeTrialStart(TimeTrialStartEvent event) {
		JsonObject msg = envelope("tt_start");
		addPlayer(msg, event.getPlayer());
		if (event.getTimeTrial() != null && event.getTimeTrial().getTrack() != null) {
			msg.addProperty("track", event.getTimeTrial().getTrack().getDisplayName());
		}
		RaceSocketManager.broadcast(msg.toString());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onTimeTrialFinish(TimeTrialFinishEvent event) {
		JsonObject msg = envelope("tt_finish");
		addPlayer(msg, event.getPlayer());
		if (event.getTimeTrial() != null && event.getTimeTrial().getTrack() != null) {
			msg.addProperty("track", event.getTimeTrial().getTrack().getDisplayName());
		}
		if (event.getTimeTrialFinish() != null) {
			msg.addProperty("time", event.getTimeTrialFinish().getTime());
		}
		msg.addProperty("old_best_time", event.getOldBestTime());
		msg.addProperty("is_new_best_time", event.isNewBestTime());
		RaceSocketManager.broadcast(msg.toString());
	}

	private static JsonObject envelope(String type) {
		JsonObject msg = new JsonObject();
		msg.addProperty("type", type);
		msg.addProperty("timestamp", System.currentTimeMillis());
		return msg;
	}

	private static JsonObject base(String type, Driver driver) {
		JsonObject msg = envelope(type);
		if (driver != null) {
			if (driver.getHeat() != null) {
				msg.addProperty("heat_id", driver.getHeat().getId());
                msg.addProperty("max_laps", driver.getHeat().getTotalLaps());
                msg.addProperty("max_pits", driver.getHeat().getTotalPits());
			}
			JsonObject driverObj = new JsonObject();
			if (driver.getTPlayer() != null) {
				driverObj.addProperty("name", driver.getTPlayer().getName());
				driverObj.addProperty("uuid", driver.getTPlayer().getUniqueId().toString());
			}
			driverObj.addProperty("position", driver.getPosition());
			driverObj.addProperty("start_position", driver.getStartPosition());
			driverObj.addProperty("laps", driver.getLaps().size());
			driverObj.addProperty("pits", driver.getPits());
			driverObj.addProperty("is_finished", driver.isFinished());
			driverObj.addProperty("is_disqualified", driver.isDisqualified());
			msg.add("driver", driverObj);
		}
		return msg;
	}

	private static void addPlayer(JsonObject msg, Player player) {
		if (player != null) {
			msg.addProperty("player_name", player.getName());
			msg.addProperty("player_uuid", player.getUniqueId().toString());
		}
	}
}

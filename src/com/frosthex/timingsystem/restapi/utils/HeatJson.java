package com.frosthex.timingsystem.restapi.utils;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import me.makkuusen.timing.system.api.DriverDetails;
import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.round.RoundType;

/**
 * Shared serialization of live heat/driver state into Gson objects, reused by the
 * REST running-heats route, the websocket snapshot loop and the event listener.
 */
public class HeatJson {

	private HeatJson() {
	}

	public static JsonObject heatToJson(Heat heat) {
		JsonObject heatObj = new JsonObject();
		heatObj.addProperty("name", heat.getName());
		heatObj.addProperty("event_name", heat.getEvent().getDisplayName());
		heatObj.addProperty("id", heat.getId());
		heatObj.addProperty("laps", heat.getTotalLaps());
		heatObj.addProperty("pits", heat.getTotalPits());
		heatObj.addProperty("qualifying", (heat.getRound().getType() == RoundType.QUALIFICATION));

		JsonArray driverPositionsArray = new JsonArray();
		List<DriverDetails> driverDetailsList = TimingSystemAPI.getAllDriverDetailsFromHeat(heat);
		for (DriverDetails dd : driverDetailsList) {
			driverPositionsArray.add(driverDetailsToJson(dd));
		}
		heatObj.add("driver_details", driverPositionsArray);
		return heatObj;
	}

	public static JsonObject driverDetailsToJson(DriverDetails dd) {
		JsonObject driverObj = new JsonObject();
		driverObj.addProperty("name", dd.getName());
		driverObj.addProperty("team_color", dd.getTeamColor());
		driverObj.addProperty("uuid", dd.getUuid());
		driverObj.addProperty("gap", dd.getGap());
		driverObj.addProperty("gap_to_leader", dd.getGapFromLeader());
		driverObj.addProperty("laps", dd.getLaps());
		driverObj.addProperty("pits", dd.getPits());
		driverObj.addProperty("position", dd.getPosition());
		driverObj.addProperty("start_position", dd.getStartPosition());
		driverObj.addProperty("is_in_pit", dd.isInpit());
		driverObj.addProperty("is_offline", dd.isOffline());
		driverObj.addProperty("best_lap", dd.getBestLap());
		return driverObj;
	}
}

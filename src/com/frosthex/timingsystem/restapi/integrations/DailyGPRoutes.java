package com.frosthex.timingsystem.restapi.integrations;

import static spark.Spark.get;
import static spark.Spark.halt;

import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import com.frosthex.timingsystem.restapi.utils.Messager;
import com.frosthex.timingsystem.restapi.utils.PlayerResolver;
import com.github.M2B5.dailyGP.DailyGP;
import com.github.M2B5.dailyGP.grandprix.GrandPrixManager;
import com.github.M2B5.dailyGP.results.GrandPrixResult;
import com.github.M2B5.dailyGP.stats.PlayerStats;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.track.Track;

/**
 * TimingSystemRESTApi - Provides a basic JSON REST API for the TimingSystem plugin.
 * Copyright (C) 2025 Justin "JustBru00" Brubaker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */
public class DailyGPRoutes {

	private DailyGPRoutes() {
	}

	public static boolean register() {
		Plugin plugin = Bukkit.getPluginManager().getPlugin("DailyGP");

		if (!(plugin instanceof DailyGP dailyGP)) {
			Messager.msgConsole("&c[WARN] A plugin named DailyGP is installed but is not the expected plugin. "
					+ "DailyGP routes will not be registered.");
			return false;
		}

		get("/api/v4/readonly/dailygp", (request, response) -> {
			GrandPrixManager manager = dailyGP.getGrandPrixManager();

			JsonObject responseObject = new JsonObject();

			try {
				responseObject.addProperty("state", manager.getState().name());
				responseObject.addProperty("running", manager.isRunning());

				Optional<Track> track = manager.getTrack();
				if (track.isPresent()) {
					responseObject.addProperty("track_command_name", track.get().getCommandName());
					responseObject.addProperty("track_display_name", track.get().getDisplayName());
					responseObject.addProperty("track_id", track.get().getId());
				}

				responseObject.addProperty("race_laps", manager.getRaceLaps());
				responseObject.addProperty("race_pits", manager.getRacePits());
				responseObject.addProperty("max_drivers", manager.getMaxDrivers());
				responseObject.addProperty("qualifying_time_left_ms", manager.getQualifyingTimeLeft().toMillis());

				JsonArray driversArray = new JsonArray();
				for (Driver driver : manager.getDrivers()) {
					JsonObject driverObject = new JsonObject();
					driverObject.addProperty("uuid", driver.getTPlayer().getUniqueId().toString());
					driverObject.addProperty("name", driver.getTPlayer().getName());
					driverObject.addProperty("position", driver.getPosition());
					driverObject.addProperty("laps", driver.getLaps().size());
					driversArray.add(driverObject);
				}
				responseObject.add("drivers", driversArray);

			} catch (ConcurrentModificationException e) {
				halt(503, "{\"error\":true,\"error_message\":\"The Grand Prix changed while it was being read. "
						+ "Please try again.\"}");
			}

			response.status(200);
			return responseObject.toString();
		});

		get("/api/v4/readonly/dailygp/results/latest", (request, response) -> {
			Optional<GrandPrixResult> result = dailyGP.getResultsStore().getLatestResult();

			if (result.isEmpty()) {
				halt(404, "{\"error\":true,\"error_message\":\"No Grand Prix has been run yet.\"}");
			}

			response.status(200);
			return serializeResult(result.get()).toString();
		});

		get("/api/v4/readonly/dailygp/stats", (request, response) -> {
			List<PlayerStats> allStats = dailyGP.getStatisticsManager().getStats().stream()
					.sorted(Comparator.comparingInt(PlayerStats::getTotalWins).reversed())
					.toList();

			JsonObject responseObject = new JsonObject();
			responseObject.addProperty("number", allStats.size());

			JsonArray statsArray = new JsonArray();
			for (PlayerStats stats : allStats) {
				statsArray.add(serializeStats(stats));
			}
			responseObject.add("players", statsArray);

			response.status(200);
			return responseObject.toString();
		});

		get("/api/v4/readonly/dailygp/stats/:uuidorusername", (request, response) -> {
			UUID uuid = PlayerResolver.resolve(request.params("uuidorusername"));

			if (uuid == null) {
				halt(400, "{\"error\":true,\"error_message\":\"Something went wrong. UUID or username couldn't be "
						+ "parsed from input.\"}");
			}

			JsonObject responseObject = new JsonObject();
			responseObject.addProperty("uuid", uuid.toString());
			responseObject.addProperty("name", PlayerResolver.nameOf(uuid));
			responseObject.addProperty("total_wins", dailyGP.getStatisticsManager().getTotalWins(uuid));
			responseObject.addProperty("current_streak", dailyGP.getStatisticsManager().getCurrentStreak(uuid));
			responseObject.addProperty("highest_streak", dailyGP.getStatisticsManager().getHighestStreak(uuid));

			response.status(200);
			return responseObject.toString();
		});

		get("/api/v4/readonly/dailygp/excluded-tracks", (request, response) -> {
			JsonObject responseObject = new JsonObject();
			responseObject.addProperty("capacity", dailyGP.getExcludedTracks().getCapacity());

			JsonArray trackIdsArray = new JsonArray();
			for (Integer trackId : dailyGP.getExcludedTracks().getTrackIds()) {
				trackIdsArray.add(trackId);
			}
			responseObject.add("track_ids", trackIdsArray);

			response.status(200);
			return responseObject.toString();
		});

		return true;
	}

	private static JsonObject serializeStats(PlayerStats stats) {
		JsonObject statsObject = new JsonObject();
		statsObject.addProperty("uuid", stats.getPlayerId().toString());
		statsObject.addProperty("name", PlayerResolver.nameOf(stats.getPlayerId()));
		statsObject.addProperty("total_wins", stats.getTotalWins());
		statsObject.addProperty("current_streak", stats.getCurrentStreak());
		statsObject.addProperty("highest_streak", stats.getHighestStreak());
		return statsObject;
	}

	private static JsonObject serializeResult(GrandPrixResult result) {
		JsonObject resultObject = new JsonObject();
		resultObject.addProperty("track_name", result.trackName());
		resultObject.addProperty("total_laps", result.totalLaps());
		resultObject.addProperty("finished_at", result.finishedAt());

		Optional<GrandPrixResult.Entry> winner = result.getWinner();
		resultObject.addProperty("winner_uuid", winner.map(entry -> entry.playerId().toString()).orElse(null));
		resultObject.addProperty("winner_name", winner.map(GrandPrixResult.Entry::playerName).orElse(null));

		JsonArray entriesArray = new JsonArray();
		for (GrandPrixResult.Entry entry : result.entries()) {
			JsonObject entryObject = new JsonObject();
			entryObject.addProperty("position", entry.position());
			entryObject.addProperty("uuid", entry.playerId().toString());
			entryObject.addProperty("name", entry.playerName());
			entryObject.addProperty("laps", entry.laps());
			entryObject.addProperty("race_time", entry.raceTime());
			entryObject.addProperty("completed", entry.completed());
			entriesArray.add(entryObject);
		}
		resultObject.add("entries", entriesArray);
		return resultObject;
	}
}

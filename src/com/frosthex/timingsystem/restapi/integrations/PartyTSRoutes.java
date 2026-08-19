package com.frosthex.timingsystem.restapi.integrations;

import static spark.Spark.get;
import static spark.Spark.halt;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import com.frosthex.timingsystem.restapi.utils.Messager;
import com.frosthex.timingsystem.restapi.utils.PlayerResolver;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import me._2818.partyTS.PartyTS;
import me._2818.partyTS.database.DatabaseManager;
import me._2818.partyTS.database.DatabaseManager.LeaderboardEntry;
import me._2818.partyTS.database.DatabaseManager.LeaderboardPage;
import me._2818.partyTS.database.DatabaseManager.PlayerStats;
import me._2818.partyTS.database.MatchRecord;

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
public class PartyTSRoutes {

	private static final int DEFAULT_PAGE_SIZE = 10;
	private static final int MAX_PAGE_SIZE = 100;
	private static final int DEFAULT_MATCH_LIMIT = 10;
	private static final int MAX_MATCH_LIMIT = 100;

	private static final int QUERY_TIMEOUT_SECONDS = 10;

	private PartyTSRoutes() {
	}

	public static boolean register() {
		Plugin plugin = Bukkit.getPluginManager().getPlugin("PartyTS");

		if (!(plugin instanceof PartyTS partyTS)) {
			Messager.msgConsole("&c[WARN] A plugin named PartyTS is installed but is not the expected plugin. "
					+ "Duel routes will not be registered.");
			return false;
		}

		DatabaseManager database = partyTS.getDatabaseManager();
		if (database == null) {
			Messager.msgConsole("&c[WARN] PartyTS has no duel database, so duel routes will not be registered.");
			return false;
		}

		get("/api/v4/readonly/duels/leaderboard", (request, response) -> {
			int page = readPositiveInt(request.queryParams("page"), 1);
			int pageSize = Math.min(MAX_PAGE_SIZE, readPositiveInt(request.queryParams("page_size"),
					DEFAULT_PAGE_SIZE));

			LeaderboardPage leaderboard;
			try {
				leaderboard = database.getLeaderboardPageAsync(page, pageSize)
						.get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			} catch (Exception e) {
				halt(500, "{\"error\":true,\"error_message\":\"Something went wrong. PartyTS could not read the duel "
						+ "leaderboard.\"}");
				return null;
			}

			JsonObject responseObject = new JsonObject();
			responseObject.addProperty("page", leaderboard.page());
			responseObject.addProperty("page_size", leaderboard.pageSize());
			responseObject.addProperty("total_pages", leaderboard.totalPages());
			responseObject.addProperty("total_players", leaderboard.totalPlayers());

			JsonArray entriesArray = new JsonArray();
			for (LeaderboardEntry entry : leaderboard.entries()) {
				JsonObject entryObject = new JsonObject();
				entryObject.addProperty("rank", entry.rank());
				entryObject.addProperty("uuid", entry.uuid().toString());
				entryObject.addProperty("name", entry.playerName());
				entryObject.addProperty("elo", entry.elo());
				entryObject.addProperty("wins", entry.wins());
				entryObject.addProperty("losses", entry.losses());
				entriesArray.add(entryObject);
			}
			responseObject.add("entries", entriesArray);

			response.status(200);
			return responseObject.toString();
		});

		get("/api/v4/readonly/duels/players/:uuidorusername", (request, response) -> {
			UUID uuid = PlayerResolver.resolve(request.params("uuidorusername"));

			if (uuid == null) {
				halt(400, "{\"error\":true,\"error_message\":\"Something went wrong. UUID or username couldn't be "
						+ "parsed from input.\"}");
			}

			PlayerStats stats;
			try {
				stats = database.getPlayerStatsAsync(uuid).get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			} catch (Exception e) {
				halt(500, "{\"error\":true,\"error_message\":\"Something went wrong. PartyTS could not read that "
						+ "player's duel stats.\"}");
				return null;
			}

			JsonObject responseObject = new JsonObject();
			responseObject.addProperty("uuid", uuid.toString());
			responseObject.addProperty("name", PlayerResolver.nameOf(uuid));
			responseObject.addProperty("elo", stats.elo());
			responseObject.addProperty("wins", stats.wins());
			responseObject.addProperty("losses", stats.losses());
			responseObject.addProperty("duels", stats.wins() + stats.losses());

			response.status(200);
			return responseObject.toString();
		});

		get("/api/v4/readonly/duels/players/:uuidorusername/matches", (request, response) -> {
			UUID uuid = PlayerResolver.resolve(request.params("uuidorusername"));

			if (uuid == null) {
				halt(400, "{\"error\":true,\"error_message\":\"Something went wrong. UUID or username couldn't be "
						+ "parsed from input.\"}");
			}

			int limit = Math.min(MAX_MATCH_LIMIT, readPositiveInt(request.queryParams("limit"), DEFAULT_MATCH_LIMIT));

			List<MatchRecord> matches;
			try {
				matches = database.getPlayerMatchHistoryAsync(uuid, limit).get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			} catch (Exception e) {
				halt(500, "{\"error\":true,\"error_message\":\"Something went wrong. PartyTS could not read that "
						+ "player's match history.\"}");
				return null;
			}

			JsonObject responseObject = new JsonObject();
			responseObject.addProperty("uuid", uuid.toString());
			responseObject.addProperty("name", PlayerResolver.nameOf(uuid));
			responseObject.addProperty("number", matches.size());

			JsonArray matchesArray = new JsonArray();
			for (MatchRecord match : matches) {
				JsonObject matchObject = new JsonObject();
				matchObject.addProperty("id", match.id());
				matchObject.addProperty("winner_uuid", match.winnerId().toString());
				matchObject.addProperty("winner_name", match.winnerName());
				matchObject.addProperty("loser_uuid", match.loserId().toString());
				matchObject.addProperty("loser_name", match.loserName());
				matchObject.addProperty("winner_elo_before", match.winnerEloBefore());
				matchObject.addProperty("winner_elo_after", match.winnerEloAfter());
				matchObject.addProperty("loser_elo_before", match.loserEloBefore());
				matchObject.addProperty("loser_elo_after", match.loserEloAfter());
				matchObject.addProperty("elo_change", match.getEloChange());
				matchObject.addProperty("won", match.isWinner(uuid));
				matchObject.addProperty("match_date", match.matchDate().getTime());
				matchesArray.add(matchObject);
			}
			responseObject.add("matches", matchesArray);

			response.status(200);
			return responseObject.toString();
		});

		return true;
	}

	private static int readPositiveInt(String queryParam, int fallback) {
		if (queryParam == null) {
			return fallback;
		}
		try {
			int value = Integer.parseInt(queryParam);
			return value > 0 ? value : fallback;
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}

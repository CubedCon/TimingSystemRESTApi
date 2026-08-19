package com.frosthex.timingsystem.restapi.utils;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

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
public class PlayerResolver {

	private PlayerResolver() {
	}

	public static UUID resolve(String uuidOrUsername) {
		if (uuidOrUsername == null) {
			return null;
		}

		try {
			return UUID.fromString(uuidOrUsername);
		} catch (IllegalArgumentException notAUuid) {
			OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(uuidOrUsername);
			return offline == null ? null : offline.getUniqueId();
		}
	}

	public static String nameOf(UUID uuid) {
		String name = Bukkit.getOfflinePlayer(uuid).getName();
		return name == null ? "null" : name;
	}
}

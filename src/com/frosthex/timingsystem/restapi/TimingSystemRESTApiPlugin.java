package com.frosthex.timingsystem.restapi;

import java.io.File;
import java.util.logging.Logger;

import com.tekad.TimingLeague.TImingLeague;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.scheduler.BukkitTask;

import com.frosthex.timingsystem.restapi.bstats.BStats;
import com.frosthex.timingsystem.restapi.commands.TimingSystemRestApiCommand;
import com.frosthex.timingsystem.restapi.listeners.RaceEventListener;
import com.frosthex.timingsystem.restapi.network.RaceSocketManager;
import com.frosthex.timingsystem.restapi.network.SnapshotTask;
import com.frosthex.timingsystem.restapi.network.SparkManager;
import com.frosthex.timingsystem.restapi.utils.Messager;

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
 * @author Justin Brubaker
 *
 */
public class TimingSystemRESTApiPlugin extends JavaPlugin {
	
	private static TimingSystemRESTApiPlugin instance;
	private static final int BSTATS_PLUGIN_ID = 18069;
	private static final String[] TIMING_SYSTEM_SUPPORTED_VERSIONS = {"3.3.3", "3.3.4", "3.3.5", "3.4", "3.5", "3.6"};
	
	public static ConsoleCommandSender clogger = Bukkit.getServer().getConsoleSender();
	public static Logger log = Bukkit.getLogger();
	
	public static String prefix = Messager.color("&8[&bTimingSystem&fRESTApi&8] &7");

	public static TImingLeague timingleague;

	private BukkitTask snapshotTask;


	@Override
	public void onDisable() {
		Messager.msgConsole("&6Disabled plugin.");
		if (snapshotTask != null) {
			snapshotTask.cancel();
			snapshotTask = null;
		}
		RaceSocketManager.closeAll();
		SparkManager.stopSpark();
		instance = null;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onEnable() {
		instance = this;
		Messager.msgConsole("&6Plugin is loading...");
		
		Plugin timingSystem = Bukkit.getPluginManager().getPlugin("TimingSystem");
		if (timingSystem == null) {
			// TimingSystem isn't installed.
			Bukkit.getPluginManager().disablePlugin(instance);
			return;
		} else {
			// TimingSystem version check
			String timingSystemVersion = timingSystem.getDescription().getVersion();
			boolean supportedVersion = false;
			for (String version : TIMING_SYSTEM_SUPPORTED_VERSIONS) {
				if (timingSystemVersion.contains(version)) {
					supportedVersion = true;
					break;
				}
			}		
			
			if (!supportedVersion) {
				Messager.msgConsole("&cTimingSystemRESTApi version " + getDescription().getVersion() + " doesn't support TimingSystem version "
			+ timingSystemVersion + ". The REST api will attempt to run as normal, but you may encounter issues.");
			}

			Plugin Tl = Bukkit.getPluginManager().getPlugin("TImingLeague");

			if (Tl instanceof TImingLeague league){
				timingleague = league;
				Messager.msgConsole("hooked into timing league");
			} else{
				Messager.msgConsole("Timing league not found");
			}
		}
		
		// Config
		saveDefaultConfig();
		int portFromConfig = getConfig().getInt("port");
		
		if (portFromConfig == 0) {
			// No port in config file.
			Messager.msgConsole("&cCouldn't read 'port' from config.yml. Defaulting to port 4567.");
			SparkManager.setPort(4567);
		} else {
			SparkManager.setPort(portFromConfig);
		}
		
		// Live race websocket
		boolean websocketEnabled = getConfig().getBoolean("websocket_enabled", true);
		SparkManager.setWebsocketEnabled(websocketEnabled);

		// Create public_html folder if it doesn't exist.
		SparkManager.setPathToPublicHtmlFolder(TimingSystemRESTApiPlugin.getInstance().getDataFolder().getPath() + File.separator + "public_html");
		
		File publicHtmlFolder = new File(SparkManager.getPathToPublicHtmlFolder());
		if (!publicHtmlFolder.exists()) {
			publicHtmlFolder.mkdir();
		}
		
		// bStats
		BStats metrics = new BStats(this, BSTATS_PLUGIN_ID);	
		String timingSystemVersion = timingSystem.getDescription().getVersion();
		metrics.addCustomChart(new BStats.SimplePie("timingsystem_version", () -> timingSystemVersion));
		
		// Commands
		getCommand("timingsystemrestapi").setExecutor(new TimingSystemRestApiCommand());
		
		// Live race websocket: register the TimingSystem event listener and start the snapshot loop.
		if (getConfig().getBoolean("rest_api_enabled") && websocketEnabled) {
			getServer().getPluginManager().registerEvents(new RaceEventListener(), this);
			long snapshotInterval = getConfig().getLong("snapshot_interval_ticks", 20L);
			snapshotTask = new SnapshotTask().runTaskTimer(this, snapshotInterval, snapshotInterval);
			Messager.msgConsole("&6Live race websocket wiring registered (snapshot every " + snapshotInterval + " ticks).");
		}

		// Strike the flint, ignite the spark in 20 seconds
		if (getConfig().getBoolean("rest_api_enabled")) {
			Bukkit.getScheduler().runTaskLaterAsynchronously(instance, new Runnable() {
				
				@Override
				public void run() {
					Messager.msgConsole("&6Striking the flint, igniting the spark.");
					SparkManager.initSpark();				
				}
			}, 20*20);
		} else {
			Messager.msgConsole("&cThe REST api is disabled in the config. Please enable it by setting rest_api_enabled to true.");
		}
		
		Messager.msgConsole("&aPlugin enabled.");
	}

	public static TimingSystemRESTApiPlugin getInstance() {
		return instance;
	}
}

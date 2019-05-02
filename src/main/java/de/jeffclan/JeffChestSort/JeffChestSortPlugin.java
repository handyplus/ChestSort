package de.jeffclan.JeffChestSort;

/*

	ChestSort - maintained by mfnalex / JEFF Media GbR ( www.jeff-media.de )
	
	THANK YOU for your interest in ChestSort :)
	
	ChestSort has been an open-source project from the day it started.
	Without the support of the community, many awesome features
	would be missing. A big THANK YOU to everyone who contributed to
	this project!
	
	If you have bug reports, feature requests etc. please message me at SpigotMC.org:
	https://www.spigotmc.org/members/mfnalex.175238/
	
	Please DO NOT post bug reports or feature requests in the review section at SpigotMC.org. Thank you.
	
	NOTE: The project has been converted to maven by Azzurite (thanks again). You will need to
	`mvn install` to create a working .jar.
	
	=============================================================================================
	
	TECHNICAL INFORMATION:
	
	If you want to know how the sorting works, have a look at the JeffChestSortOrganizer class.
	
	If you want to contribute, please note that messages sent to player must be made configurable in the config.yml.
	Please have a look at the JeffChestSortMessages class if you want to add a message.
	
*/

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import de.jeffclan.utils.Utils;

public class JeffChestSortPlugin extends JavaPlugin {

	// We need a map to store each player's settings
	Map<String, JeffChestSortPlayerSetting> PerPlayerSettings = new HashMap<String, JeffChestSortPlayerSetting>();
	JeffChestSortMessages messages;
	JeffChestSortOrganizer organizer;
	JeffChestSortUpdateChecker updateChecker;
	JeffChestSortListener listener;
	String sortingMethod;
	ArrayList<String> disabledWorlds;
	int currentConfigVersion = 8;
	boolean usingMatchingConfig = true;
	boolean debug = false;
	boolean verbose = true;
	private long updateCheckInterval = 86400; // in seconds. We check on startup and every 24 hours (if you never
												// restart your server)

	// Public API method to sort any given inventory
	public void sortInventory(Inventory inv) {
		this.organizer.sortInventory(inv);
	}

	// Public API method to sort any given inventory inbetween startSlot and endSlot
	public void sortInventory(Inventory inv, int startSlot, int endSlot) {
		this.organizer.sortInventory(inv, startSlot, endSlot);
	}

	// Creates the default configuration file
	// Also checks the config-version of an already existing file. If the existing
	// config is too
	// old (generated prior to ChestSort 2.0.0), we rename it to config.old.yml so
	// that users
	// can start off with a new config file that includes all new options. However,
	// on most
	// updates, the file will not be touched, even if new config options were added.
	// You will instead
	// get a warning in the console that you should consider adding the options
	// manually. If you do
	// not add them, the default values will be used for any unset values.
	void createConfig() {

		// This saves the config.yml included in the .jar file, but it will not
		// overwrite an existing config.yml
		this.saveDefaultConfig();

		// Config version prior to 5? Then it must have been generated by ChestSort 1.x
		if (getConfig().getInt("config-version", 0) < 5) {
			renameConfigIfTooOld();

			// Using old config version, but it's no problem. We just print a warning and
			// use the default values later on
		} else if (getConfig().getInt("config-version", 0) != currentConfigVersion) {
			showOldConfigWarning();
			usingMatchingConfig = false;
		}

		createDirectories();

		setDefaultConfigValues();

		// Load disabled-worlds. If it does not exist in the config, it returns null.
		// That's no problem
		disabledWorlds = (ArrayList<String>) getConfig().getStringList("disabled-worlds");
	}

	private void setDefaultConfigValues() {
		// If you use an old config file with missing options, the following default
		// values will be used instead
		// for every missing option.
		// By default, sorting is disabled. Every player has to run /chestsort once
		getConfig().addDefault("sorting-enabled-by-default", false);
		getConfig().addDefault("show-message-when-using-chest", true);
		getConfig().addDefault("show-message-when-using-chest-and-sorting-is-enabled", false);
		getConfig().addDefault("show-message-again-after-logout", true);
		getConfig().addDefault("sorting-method", "{category},{itemsFirst},{name},{color}");
		getConfig().addDefault("allow-player-inventory-sorting", false);
		getConfig().addDefault("check-for-updates", "true");
		getConfig().addDefault("auto-generate-category-files", true);
		getConfig().addDefault("sort-time", "close");
		getConfig().addDefault("verbose", true); // Prints some information in onEnable()
	}

	private void createDirectories() {
		// Create a playerdata folder that contains all the perPlayerSettings as .yml
		File playerDataFolder = new File(getDataFolder().getPath() + File.separator + "playerdata");
		if (!playerDataFolder.getAbsoluteFile().exists()) {
			playerDataFolder.mkdir();
		}

		// Create a categories folder that contains text files. ChestSort includes
		// default category files,
		// but you can also create your own
		File categoriesFolder = new File(getDataFolder().getPath() + File.separator + "categories");
		if (!categoriesFolder.getAbsoluteFile().exists()) {
			categoriesFolder.mkdir();
		}
	}

	private void showOldConfigWarning() {
		getLogger().warning("========================================================");
		getLogger().warning("YOU ARE USING AN OLD CONFIG FILE!");
		getLogger().warning("This is not a problem, as ChestSort will just use the");
		getLogger().warning("default settings for unset values. However, if you want");
		getLogger().warning("to configure the new options, please go to");
		getLogger().warning("https://www.chestsort.de and replace your config.yml");
		getLogger().warning("with the new one. You can then insert your old changes");
		getLogger().warning("into the new file.");
		getLogger().warning("========================================================");
	}

	private void renameConfigIfTooOld() {
		getLogger().warning("========================================================");
		getLogger().warning("You are using a config file that has been generated");
		getLogger().warning("prior to ChestSort version 2.0.0.");
		getLogger().warning("To allow everyone to use the new features, your config");
		getLogger().warning("has been renamed to config.old.yml and a new one has");
		getLogger().warning("been generated. Please examine the new config file to");
		getLogger().warning("see the new possibilities and adjust your settings.");
		getLogger().warning("========================================================");

		File configFile = new File(getDataFolder().getAbsolutePath() + File.separator + "config.yml");
		File oldConfigFile = new File(getDataFolder().getAbsolutePath() + File.separator + "config.old.yml");
		if (oldConfigFile.getAbsoluteFile().exists()) {
			oldConfigFile.getAbsoluteFile().delete();
		}
		configFile.getAbsoluteFile().renameTo(oldConfigFile.getAbsoluteFile());
		saveDefaultConfig();
		try {
			getConfig().load(configFile.getAbsoluteFile());
		} catch (IOException | InvalidConfigurationException e) {
			getLogger().warning("Could not load freshly generated config file!");
			e.printStackTrace();
		}
	}

	@Override
	public void onDisable() {
		// We have to unregister every player to save their perPlayerSettings
		for (Player p : getServer().getOnlinePlayers()) {
			unregisterPlayer(p);
		}
	}

	@Override
	public void onEnable() {
		// Create the config file, including checks for old config versions, and load
		// the default values for unset options
		createConfig();

		// Save default sorting category files when enabled in the config (default=true)
		saveDefaultCategories();

		verbose = getConfig().getBoolean("verbose");

		// Create all needed instances of our classes

		// Messages class will load messages from config, fallback to hardcoded default
		// messages
		messages = new JeffChestSortMessages(this);

		// Organizer will load all category files and will be ready to sort stuff
		organizer = new JeffChestSortOrganizer(this);

		// UpdateChecker will check on startup and every 24 hours for new updates (when
		// enabled)
		updateChecker = new JeffChestSortUpdateChecker(this);

		// The listener will register joining (and unregister leaving) players, and call
		// the Organizer to sort inventories when a player closes a chest, shulkerbox or
		// barrel inventory
		listener = new JeffChestSortListener(this);

		// The sorting method will determine how stuff is sorted
		sortingMethod = getConfig().getString("sorting-method");

		// Register the events for our Listener
		getServer().getPluginManager().registerEvents(listener, this);

		// Register the /chestsort command and associate it to a new CommandExecutor
		JeffChestSortCommandExecutor commandExecutor = new JeffChestSortCommandExecutor(this);
		this.getCommand("chestsort").setExecutor(commandExecutor);

		// Register the /invsort command
		this.getCommand("invsort").setExecutor(commandExecutor);

		// Does anyone actually need this?
		if (verbose) {
			getLogger().info("Current sorting method: " + sortingMethod);
			getLogger().info("Sorting enabled by default: " + getConfig().getBoolean("sorting-enabled-by-default"));
			getLogger().info("Auto generate category files: " + getConfig().getBoolean("auto-generate-category-files"));
			getLogger().info("Sort time: " + getConfig().getString("sort-time"));
			getLogger().info("Check for updates: " + getConfig().getString("check-for-updates"));
		}

		// Check for updates (async, of course)

		// When set to true, we check for updates right now, and every 24 hours (see
		// updateCheckInterval)
		if (getConfig().getString("check-for-updates", "true").equalsIgnoreCase("true")) {
			Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
				@Override
				public void run() {
					updateChecker.checkForUpdate();
				}
			}, 0L, updateCheckInterval * 20);

		} // When set to on-startup, we check right now (delay 0)
		else if (getConfig().getString("check-for-updates", "true").equalsIgnoreCase("on-startup")) {
			updateChecker.checkForUpdate();
		}

		// Metrics will need json-simple with 1.14 API. 
		Metrics metrics = new Metrics(this);
		metrics.addCustomChart(new Metrics.SimplePie("sorting_method", () -> sortingMethod));
		metrics.addCustomChart(new Metrics.SimplePie("config_version",
				() -> Integer.toString(getConfig().getInt("config-version", 0))));
		metrics.addCustomChart(
				new Metrics.SimplePie("check_for_updates", () -> getConfig().getString("check-for-updates", "true")));
		metrics.addCustomChart(new Metrics.SimplePie("show_message_when_using_chest",
				() -> Boolean.toString(getConfig().getBoolean("show-message-when-using-chest"))));
		metrics.addCustomChart(new Metrics.SimplePie("show_message_when_using_chest_and_sorting_is_enabl", () -> Boolean
				.toString(getConfig().getBoolean("show-message-when-using-chest-and-sorting-is-enabled"))));
		metrics.addCustomChart(new Metrics.SimplePie("show_message_again_after_logout",
				() -> Boolean.toString(getConfig().getBoolean("show-message-again-after-logout"))));
		metrics.addCustomChart(new Metrics.SimplePie("sorting_enabled_by_default",
				() -> Boolean.toString(getConfig().getBoolean("sorting-enabled-by-default"))));
		metrics.addCustomChart(
				new Metrics.SimplePie("using_matching_config_version", () -> Boolean.toString(usingMatchingConfig)));
		

	}

	// Saves default category files, when enabled in the config
	private void saveDefaultCategories() {

		// Abort when auto-generate-category-files is set to false in config.yml
		if (getConfig().getBoolean("auto-generate-category-files", true) != true) {
			return;
		}

		// Isn't there a smarter way to find all the 9** files in the .jar?
		String[] defaultCategories = { "900-tools", "910-valuables", "920-combat", "930-brewing", "940-food",
				"950-redstone", "960-wood", "970-stone", "980-plants", "981-corals" };

		for (String category : defaultCategories) {

			FileOutputStream fopDefault = null;
			File fileDefault;

			try {
				InputStream in = getClass().getResourceAsStream("/categories/" + category + ".default.txt");

				fileDefault = new File(getDataFolder().getAbsolutePath() + File.separator + "categories"
						+ File.separator + category + ".txt");
				fopDefault = new FileOutputStream(fileDefault);

				// overwrites existing files, on purpose.
				fileDefault.createNewFile();

				// get the content in bytes
				byte[] contentInBytes = Utils.getBytes(in);

				fopDefault.write(contentInBytes);
				fopDefault.flush();
				fopDefault.close();

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (fopDefault != null) {
						fopDefault.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// Check whether sorting is enabled for a player. Public because it can be
	// accessed as part of the API
	public boolean sortingEnabled(Player p) {

		// The following is for all the lazy server admins who use /reload instead of
		// properly restarting their
		// server ;) I am sometimes getting stacktraces although it is clearly stated
		// that /reload is NOT
		// supported. So, here is a quick fix
		if (PerPlayerSettings == null) {
			PerPlayerSettings = new HashMap<String, JeffChestSortPlayerSetting>();
		}
		listener.registerPlayerIfNeeded(p);
		// End of quick fix

		return PerPlayerSettings.get(p.getUniqueId().toString()).sortingEnabled;
	}

	// Unregister a player and save their settings in the playerdata folder
	void unregisterPlayer(Player p) {
		// File will be named by the player's uuid. This will prevent problems on player
		// name changes.
		UUID uniqueId = p.getUniqueId();

		// When using /reload or some other obscure features, it can happen that players
		// are online
		// but not registered. So, we only continue when the player has been registered
		if (PerPlayerSettings.containsKey(uniqueId.toString())) {
			JeffChestSortPlayerSetting setting = PerPlayerSettings.get(p.getUniqueId().toString());
			File playerFile = new File(getDataFolder() + File.separator + "playerdata",
					p.getUniqueId().toString() + ".yml");
			YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
			playerConfig.set("sortingEnabled", setting.sortingEnabled);
			playerConfig.set("hasSeenMessage", setting.hasSeenMessage);
			try {
				playerConfig.save(playerFile);
			} catch (IOException e) {
				e.printStackTrace();
			}

			PerPlayerSettings.remove(uniqueId.toString());
		}
	}

}

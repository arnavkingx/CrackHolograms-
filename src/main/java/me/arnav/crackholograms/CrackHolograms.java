package me.arnav.crackholograms;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class CrackHolograms extends JavaPlugin implements CommandExecutor {

    private final LegacyComponentSerializer legacy =
            LegacyComponentSerializer.legacyAmpersand();

    private File hologramsFile;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        hologramsFile = new File(getDataFolder(), "holograms.yml");

        if (!hologramsFile.exists()) {
            saveResource("holograms.yml", false);
        }

        if (getCommand("holo") != null) {
            getCommand("holo").setExecutor(this);
        }

        loadHolograms();

        getLogger().info("=================================");
        getLogger().info("      CrackHolograms v1.0.0");
        getLogger().info("      Plugin enabled successfully");
        getLogger().info("=================================");
    }

    @Override
    public void onDisable() {
        saveHolograms();
        removeAllHologramDisplays();

        getLogger().info("CrackHolograms disabled.");
    }

    // =========================================================
    // COMMANDS
    // =========================================================

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!command.getName().equalsIgnoreCase("holo")) {
            return false;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {

            case "help":
                sendHelp(sender);
                return true;

            case "create":
                createHologram(sender, args);
                return true;

            case "delete":
            case "remove":
                deleteHologram(sender, args);
                return true;

            case "list":
                listHolograms(sender);
                return true;

            case "info":
                hologramInfo(sender, args);
                return true;

            case "addline":
                addLine(sender, args);
                return true;

            case "setline":
                setLine(sender, args);
                return true;

            case "removeline":
                removeLine(sender, args);
                return true;

            case "movehere":
                moveHere(sender, args);
                return true;

            case "reload":
                reloadPlugin(sender);
                return true;

            default:
                sendMessage(sender, "&cUnknown subcommand.");
                sendHelp(sender);
                return true;
        }
    }

    // =========================================================
    // CREATE
    // =========================================================

    private void createHologram(CommandSender sender, String[] args) {

        if (!checkPermission(sender, "crackholograms.create")) {
            return;
        }

        if (args.length < 3) {
            sendMessage(sender,
                    "&cUsage: /holo create <name> <text>");
            sendMessage(sender,
                    "&7Console: /holo create <name> <world> <x> <y> <z> <text>");
            return;
        }

        String name = args[1];

        if (getHologramSection(name) != null) {
            sendMessage(sender,
                    "&cA hologram named &f" + name + " &calready exists.");
            return;
        }

        Location location;

        /*
         * PLAYER:
         * /holo create welcome &aWelcome!
         */
        if (sender instanceof Player player) {

            location = player.getLocation().clone();

            String text = join(args, 2);

            createStoredHologram(
                    name,
                    location,
                    List.of(text)
            );

            sendMessage(sender,
                    "&aHologram &f" + name + " &acreated!");

            return;
        }

        /*
         * CONSOLE:
         * /holo create welcome world 0 65 0 &aWelcome!
         */
        if (args.length < 7) {
            sendMessage(sender,
                    "&cConsole usage: /holo create <name> <world> <x> <y> <z> <text>");
            return;
        }

        World world = Bukkit.getWorld(args[2]);

        if (world == null) {
            sendMessage(sender,
                    "&cWorld not found: &f" + args[2]);
            return;
        }

        try {

            double x = Double.parseDouble(args[3]);
            double y = Double.parseDouble(args[4]);
            double z = Double.parseDouble(args[5]);

            String text = join(args, 6);

            location = new Location(world, x, y, z);

            createStoredHologram(
                    name,
                    location,
                    List.of(text)
            );

            sendMessage(sender,
                    "&aHologram &f" + name + " &acreated!");

        } catch (NumberFormatException e) {

            sendMessage(sender,
                    "&cInvalid coordinates.");
        }
    }

    // =========================================================
    // DELETE
    // =========================================================

    private void deleteHologram(CommandSender sender, String[] args) {

        if (!checkPermission(sender, "crackholograms.delete")) {
            return;
        }

        if (args.length < 2) {
            sendMessage(sender,
                    "&cUsage: /holo delete <name>");
            return;
        }

        String name = args[1];

        if (getHologramSection(name) == null) {
            sendMessage(sender,
                    "&cHologram not found: &f" + name);
            return;
        }

        removeHologramDisplays(name);

        getConfigHolograms().set(name, null);
        saveHolograms();

        sendMessage(sender,
                "&aHologram &f" + name + " &adeleted.");
    }

    // =========================================================
    // LIST
    // =========================================================

    private void listHolograms(CommandSender sender) {

        if (!checkPermission(sender, "crackholograms.admin")) {
            return;
        }

        var section = getConfigHolograms();

        if (section.getKeys(false).isEmpty()) {

            sendMessage(sender,
                    "&eNo holograms have been created.");

            return;
        }

        sendMessage(sender,
                "&6&lCrackHolograms &7- &fHolograms:");

        for (String name : section.getKeys(false)) {

            sendMessage(sender,
                    "&8- &e" + name);
        }
    }

    // =========================================================
    // INFO
    // =========================================================

    private void hologramInfo(CommandSender sender, String[] args) {

        if (!checkPermission(sender, "crackholograms.admin")) {
            return;
        }

        if (args.length < 2) {
            sendMessage(sender,
                    "&cUsage: /holo info <name>");
            return;
        }

        String name = args[1];

        var section = getHologramSection(name);

        if (section == null) {
            sendMessage(sender,
                    "&cHologram not found.");
            return;
        }

        sendMessage(sender, "&6&lHologram Information");
        sendMessage(sender, "&7Name: &f" + name);
        sendMessage(sender, "&7World: &f" + section.getString("world"));
        sendMessage(sender, "&7X: &f" + section.getDouble("x"));
        sendMessage(sender, "&7Y: &f" + section.getDouble("y"));
        sendMessage(sender, "&7Z: &f" + section.getDouble("z"));

        List<String> lines =
                section.getStringList("lines");

        sendMessage(sender,
                "&7Lines: &f" + lines.size());
    }

    // =========================================================
    // ADD LINE
    // =========================================================

    private void addLine(CommandSender sender, String[] args) {

        if (!checkPermission(sender, "crackholograms.edit")) {
            return;
        }

        if (args.length < 3) {
            sendMessage(sender,
                    "&cUsage: /holo addline <name> <text>");
            return;
        }

        String name = args[1];

        var section = getHologramSection(name);

        if (section == null) {
            sendMessage(sender,
                    "&cHologram not found.");
            return;
        }

        List<String> lines =
                new ArrayList<>(section.getStringList("lines"));

        lines.add(join(args, 2));

        section.set("lines", lines);

        saveHolograms();

        refreshHologram(name);

        sendMessage(sender,
                "&aLine added to &f" + name);
    }

    // =========================================================
    // SET LINE
    // =========================================================

    private void setLine(CommandSender sender, String[] args) {

        if (!checkPermission(sender, "crackholograms.edit")) {
            return;
        }

        if (args.length < 4) {
            sendMessage(sender,
                    "&cUsage: /holo setline <name> <line> <text>");
            return;
        }

        String name = args[1];

        var section = getHologramSection(name);

        if (section == null) {
            sendMessage(sender,
                    "&cHologram not found.");
            return;
        }

        try {

            int line = Integer.parseInt(args[2]);

            List<String> lines =
                    new ArrayList<>(section.getStringList("lines"));

            if (line < 1 || line > lines.size()) {

                sendMessage(sender,
                        "&cInvalid line number.");

                return;
            }

            lines.set(line - 1, join(args, 3));

            section.set("lines", lines);

            saveHolograms();

            refreshHologram(name);

            sendMessage(sender,
                    "&aLine &f" + line + " &aupdated.");

        } catch (NumberFormatException e) {

            sendMessage(sender,
                    "&cLine must be a number.");
        }
    }

    // =========================================================
    // REMOVE LINE
    // =========================================================

    private void removeLine(CommandSender sender, String[] args) {

        if (!checkPermission(sender, "crackholograms.edit")) {
            return;
        }

        if (args.length < 3) {
            sendMessage(sender,
                    "&cUsage: /holo removeline <name> <line>");
            return;
        }

        String name = args[1];

        var section = getHologramSection(name);

        if (section == null) {
            sendMessage(sender,
                    "&cHologram not found.");
            return;
        }

        try {

            int line = Integer.parseInt(args[2]);

            List<String> lines =
                    new ArrayList<>(section.getStringList("lines"));

            if (line < 1 || line > lines.size()) {

                sendMessage(sender,
                        "&cInvalid line number.");

                return;
            }

            lines.remove(line - 1);

            section.set("lines", lines);

            saveHolograms();

            refreshHologram(name);

            sendMessage(sender,
                    "&aLine removed.");

        } catch (NumberFormatException e) {

            sendMessage(sender,
                    "&cLine must be a number.");
        }
    }

    // =========================================================
    // MOVE HERE
    // =========================================================

    private void moveHere(CommandSender sender, String[] args) {

        if (!checkPermission(sender, "crackholograms.edit")) {
            return;
        }

        if (!(sender instanceof Player player)) {

            sendMessage(sender,
                    "&cThis command can only be used by a player.");

            return;
        }

        if (args.length < 2) {

            sendMessage(sender,
                    "&cUsage: /holo movehere <name>");

            return;
        }

        String name = args[1];

        var section = getHologramSection(name);

        if (section == null) {

            sendMessage(sender,
                    "&cHologram not found.");

            return;
        }

        Location loc = player.getLocation();

        section.set("world", loc.getWorld().getName());
        section.set("x", loc.getX());
        section.set("y", loc.getY());
        section.set("z", loc.getZ());

        saveHolograms();

        refreshHologram(name);

        sendMessage(sender,
                "&aHologram moved to your location.");
    }

    // =========================================================
    // RELOAD
    // =========================================================

    private void reloadPlugin(CommandSender sender) {

        if (!checkPermission(sender, "crackholograms.reload")) {
            return;
        }

        reloadConfig();

        removeAllHologramDisplays();

        loadHolograms();

        sendMessage(sender,
                "&aCrackHolograms reloaded successfully.");
    }

    // =========================================================
    // HOLOGRAM CREATION
    // =========================================================

    private void createStoredHologram(
            String name,
            Location location,
            List<String> lines
    ) {

        var section =
                getConfigHolograms().createSection(name);

        section.set(
                "world",
                location.getWorld().getName()
        );

        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());

        section.set("lines", lines);

        saveHolograms();

        spawnHologram(name, location, lines);
    }

    // =========================================================
    // SPAWN HOLOGRAM
    // =========================================================

    private void spawnHologram(
            String name,
            Location location,
            List<String> lines
    ) {

        removeHologramDisplays(name);

        double spacing =
                getConfig().getDouble(
                        "holograms.line-spacing",
                        0.25
                );

        for (int i = 0; i < lines.size(); i++) {

            String text = lines.get(i);

            Location lineLocation =
                    location.clone();

            lineLocation.add(
                    0,
                    -((double) i * spacing),
                    0
            );

            TextDisplay display =
                    (TextDisplay) location.getWorld()
                            .spawnEntity(
                                    lineLocation,
                                    EntityType.TEXT_DISPLAY
                            );

            display.setText(
                    legacy.deserialize(
                            colorize(text)
                    )
            );

            display.setBillboard(
                    Display.Billboard.CENTER
            );

            display.setAlignment(
                    TextDisplay.TextAlignment.CENTER
            );

            display.setSeeThrough(false);
            display.setShadowed(true);
            display.setLineWidth(400);

            display.addScoreboardTag(
                    "crackhologram"
            );

            display.addScoreboardTag(
                    "crackhologram_" + name
            );
        }
    }

    // =========================================================
    // LOAD ALL
    // =========================================================

    private void loadHolograms() {

        var section = getConfigHolograms();

        for (String name : section.getKeys(false)) {

            var holo = section.getConfigurationSection(name);

            if (holo == null) {
                continue;
            }

            String worldName =
                    holo.getString("world");

            World world =
                    Bukkit.getWorld(worldName);

            if (world == null) {

                getLogger().warning(
                        "World not found for hologram: "
                                + name
                );

                continue;
            }

            Location location =
                    new Location(
                            world,
                            holo.getDouble("x"),
                            holo.getDouble("y"),
                            holo.getDouble("z")
                    );

            List<String> lines =
                    holo.getStringList("lines");

            spawnHologram(
                    name,
                    location,
                    lines
            );
        }

        getLogger().info(
                "Loaded "
                        + section.getKeys(false).size()
                        + " hologram(s)."
        );
    }

    // =========================================================
    // REFRESH
    // =========================================================

    private void refreshHologram(String name) {

        var section =
                getHologramSection(name);

        if (section == null) {
            return;
        }

        World world =
                Bukkit.getWorld(
                        section.getString("world")
                );

        if (world == null) {
            return;
        }

        Location location =
                new Location(
                        world,
                        section.getDouble("x"),
                        section.getDouble("y"),
                        section.getDouble("z")
                );

        List<String> lines =
                section.getStringList("lines");

        spawnHologram(
                name,
                location,
                lines
        );
    }

    // =========================================================
    // REMOVE DISPLAYS
    // =========================================================

    private void removeHologramDisplays(String name) {

        String tag =
                "crackhologram_" + name;

        for (World world : Bukkit.getWorlds()) {

            for (Entity entity :
                    world.getEntitiesByClasses(
                            TextDisplay.class
                    )) {

                if (entity.getScoreboardTags()
                        .contains(tag)) {

                    entity.remove();
                }
            }
        }
    }

    private void removeAllHolog

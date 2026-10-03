mkdir -p src/main/java/me/arnav/crackholograms
cat > src/main/java/me/arnav/crackholograms/CrackHolograms.java <<'EOF'
package me.arnav.crackholograms;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CrackHolograms extends JavaPlugin implements TabExecutor {

    private static final String TAG_PREFIX = "crackhologram_";

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        if (!getDataFolder().toPath().resolve("holograms.yml").toFile().exists()) {
            saveResource("holograms.yml", false);
        }

        getCommand("hologram").setExecutor(this);
        getCommand("hologram").setTabCompleter(this);

        loadHolograms();

        getLogger().info("CrackHolograms enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("CrackHolograms disabled!");
    }

    // =========================================================
    // COMMAND
    // =========================================================

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!command.getName().equalsIgnoreCase("hologram")) {
            return false;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "create":
                createHologram(sender, args);
                return true;

            case "delete":
            case "remove":
                deleteHologram(sender, args);
                return true;

            case "set":
                setHologram(sender, args);
                return true;

            case "addline":
                addLine(sender, args);
                return true;

            case "removeline":
                removeLine(sender, args);
                return true;

            case "list":
                listHolograms(sender);
                return true;

            case "reload":
                reloadHolograms(sender);
                return true;

            case "tp":
                teleportToHologram(sender, args);
                return true;

            default:
                sendHelp(sender);
                return true;
        }
    }

    // =========================================================
    // CREATE
    // =========================================================

    private void createHologram(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can use this command."));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /hologram create <name> [text]"));
            return;
        }

        String name = args[1];

        if (getConfig().contains("holograms." + name)) {
            sender.sendMessage(color("&cA hologram with that name already exists."));
            return;
        }

        List<String> lines = new ArrayList<>();

        if (args.length >= 3) {
            StringBuilder text = new StringBuilder();

            for (int i = 2; i < args.length; i++) {
                if (i > 2) {
                    text.append(" ");
                }
                text.append(args[i]);
            }

            lines.add(text.toString());
        } else {
            lines.add("&fNew Hologram");
        }

        saveHologram(
                name,
                player.getLocation(),
                lines
        );

        spawnHologram(
                name,
                player.getLocation(),
                lines
        );

        sender.sendMessage(
                color("&aHologram &f" + name + " &acreated!")
        );
    }

    // =========================================================
    // DELETE
    // =========================================================

    private void deleteHologram(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /hologram delete <name>"));
            return;
        }

        String name = args[1];

        if (!getConfig().contains("holograms." + name)) {
            sender.sendMessage(color("&cHologram not found."));
            return;
        }

        removeHologramDisplays(name);

        getConfig().set("holograms." + name, null);
        saveConfig();

        sender.sendMessage(
                color("&aHologram &f" + name + " &adeleted.")
        );
    }

    // =========================================================
    // SET
    // =========================================================

    private void setHologram(CommandSender sender, String[] args) {

        if (args.length < 4) {
            sender.sendMessage(
                    color("&cUsage: /hologram set <name> <line> <text>")
            );
            return;
        }

        String name = args[1];

        if (!getConfig().contains("holograms." + name)) {
            sender.sendMessage(color("&cHologram not found."));
            return;
        }

        int line;

        try {
            line = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(color("&cLine must be a number."));
            return;
        }

        if (line < 1) {
            sender.sendMessage(color("&cLine must be 1 or higher."));
            return;
        }

        String text = join(args, 3);

        List<String> lines =
                getConfig().getStringList("holograms." + name + ".lines");

        while (lines.size() < line) {
            lines.add("");
        }

        lines.set(line - 1, text);

        getConfig().set(
                "holograms." + name + ".lines",
                lines
        );

        saveConfig();

        reloadSingleHologram(name);

        sender.sendMessage(
                color("&aHologram line updated.")
        );
    }

    // =========================================================
    // ADD LINE
    // =========================================================

    private void addLine(CommandSender sender, String[] args) {

        if (args.length < 3) {
            sender.sendMessage(
                    color("&cUsage: /hologram addline <name> <text>")
            );
            return;
        }

        String name = args[1];

        if (!getConfig().contains("holograms." + name)) {
            sender.sendMessage(color("&cHologram not found."));
            return;
        }

        String text = join(args, 2);

        List<String> lines =
                getConfig().getStringList("holograms." + name + ".lines");

        lines.add(text);

        getConfig().set(
                "holograms." + name + ".lines",
                lines
        );

        saveConfig();

        reloadSingleHologram(name);

        sender.sendMessage(color("&aLine added."));
    }

    // =========================================================
    // REMOVE LINE
    // =========================================================

    private void removeLine(CommandSender sender, String[] args) {

        if (args.length < 3) {
            sender.sendMessage(
                    color("&cUsage: /hologram removeline <name> <line>")
            );
            return;
        }

        String name = args[1];

        int line;

        try {
            line = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(color("&cLine must be a number."));
            return;
        }

        List<String> lines =
                getConfig().getStringList("holograms." + name + ".lines");

        if (line < 1 || line > lines.size()) {
            sender.sendMessage(color("&cThat line does not exist."));
            return;
        }

        lines.remove(line - 1);

        getConfig().set(
                "holograms." + name + ".lines",
                lines
        );

        saveConfig();

        reloadSingleHologram(name);

        sender.sendMessage(color("&aLine removed."));
    }

    // =========================================================
    // LIST
    // =========================================================

    private void listHolograms(CommandSender sender) {

        ConfigurationSection section =
                getConfig().getConfigurationSection("holograms");

        if (section == null || section.getKeys(false).isEmpty()) {
            sender.sendMessage(color("&eNo holograms found."));
            return;
        }

        sender.sendMessage(color("&6&lCrackHolograms"));
        sender.sendMessage(color("&7Holograms:"));

        for (String name : section.getKeys(false)) {
            sender.sendMessage(
                    color("&8- &f" + name)
            );
        }
    }

    // =========================================================
    // RELOAD
    // =========================================================

    private void reloadHolograms(CommandSender sender) {

        removeAllHolograms();
        reloadConfig();
        loadHolograms();

        sender.sendMessage(
                color("&aCrackHolograms reloaded.")
        );
    }

    // =========================================================
    // TELEPORT
    // =========================================================

    private void teleportToHologram(
            CommandSender sender,
            String[] args
    ) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cOnly players can use this command."));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /hologram tp <name>"));
            return;
        }

        String name = args[1];

        Location location =
                getHologramLocation(name);

        if (location == null) {
            sender.sendMessage(color("&cHologram not found."));
            return;
        }

        player.teleport(location);

        sender.sendMessage(
                color("&aTeleported to &f" + name)
        );
    }

    // =========================================================
    // SAVE
    // =========================================================

    private void saveHologram(
            String name,
            Location location,
            List<String> lines
    ) {

        String path = "holograms." + name;

        getConfig().set(
                path + ".world",
                location.getWorld().getName()
        );

        getConfig().set(
                path + ".x",
                location.getX()
        );

        getConfig().set(
                path + ".y",
                location.getY()
        );

        getConfig().set(
                path + ".z",
                location.getZ()
        );

        getConfig().set(
                path + ".yaw",
                location.getYaw()
        );

        getConfig().set(
                path + ".pitch",
                location.getPitch()
        );

        getConfig().set(
                path + ".lines",
                lines
        );

        saveConfig();
    }

    // =========================================================
    // LOAD
    // =========================================================

    private void loadHolograms() {

        ConfigurationSection section =
                getConfig().getConfigurationSection("holograms");

        if (section == null) {
            return;
        }

        for (String name : section.getKeys(false)) {

            Location location =
                    getHologramLocation(name);

            if (location == null) {
                getLogger().warning(
                        "Could not load hologram: " + name
                );
                continue;
            }

            List<String> lines =
                    getConfig().getStringList(
                            "holograms." + name + ".lines"
                    );

            spawnHologram(
                    name,
                    location,
                    lines
            );
        }
    }

    // =========================================================
    // LOCATION
    // =========================================================

    private Location getHologramLocation(String name) {

        String path = "holograms." + name;

        String worldName =
                getConfig().getString(path + ".world");

        if (worldName == null) {
            return null;
        }

        World world =
                Bukkit.getWorld(worldName);

        if (world == null) {
            return null;
        }

        double x =
                getConfig().getDouble(path + ".x");

        double y =
                getConfig().getDouble(path + ".y");

        double z =
                getConfig().getDouble(path + ".z");

        float yaw =
                (float) getConfig().getDouble(path + ".yaw");

        float pitch =
                (float) getConfig().getDouble(path + ".pitch");

        return new Location(
                world,
                x,
                y,
                z,
                yaw,
                pitch
        );
    }

    // =========================================================
    // SPAWN
    // =========================================================

    private void spawnHologram(
            String name,
            Location location,
            List<String> lines
    ) {

        removeHologramDisplays(name);

        if (location.getWorld() == null) {
            return;
        }

        double offset = 0.0;

        for (String line : lines) {

            Location lineLocation =
                    location.clone().add(
                            0,
                            -offset,
                            0
                    );

            TextDisplay display =
                    (TextDisplay) location.getWorld().spawnEntity(
                            lineLocation,
                            EntityType.TEXT_DISPLAY
                    );

            display.setText(
                    ChatColor.translateAlternateColorCodes(
                            '&',
                            line
                    )
            );

            display.setBillboard(
                    TextDisplay.Billboard.CENTER
            );

            display.setSeeThrough(false);
            display.setShadowed(true);
            display.setLineWidth(300);

            display.addScoreboardTag(
                    TAG_PREFIX + name
            );

            offset += 0.27;
        }
    }

    // =========================================================
    // REMOVE
    // =========================================================

    private void removeHologramDisplays(String name) {

        String tag =
                TAG_PREFIX + name;

        for (World world : Bukkit.getWorlds()) {

            for (Entity entity :
                    world.getEntitiesByClass(TextDisplay.class)) {

                if (entity.getScoreboardTags().contains(tag)) {
                    entity.remove();
                }
            }
        }
    }

    private void removeAllHolograms() {

        for (World world : Bukkit.getWorlds()) {

            for (Entity entity :
                    world.getEntitiesByClass(TextDisplay.class)) {

                for (String tag :
                        entity.getScoreboardTags()) {

                    if (tag.startsWith(TAG_PREFIX)) {
                        entity.remove();
                        break;
                    }
                }
            }
        }
    }

    // =========================================================
    // RELOAD SINGLE
    // =========================================================

    private void reloadSingleHologram(String name) {

        Location location =
                getHologramLocation(name);

        if (location == null) {
            return;
        }

        List<String> lines =
                getConfig().getStringList(
                        "holograms." + name + ".lines"
                );

        spawnHologram(
                name,
                location,
                lines
        );
    }

    // =========================================================
    // HELP
    // =========================================================

    private void sendHelp(CommandSender sender) {

        sender.sendMessage(color("&6&lCrackHolograms"));
        sender.sendMessage(color("&7"));
        sender.sendMessage(
                color("&e/hologram create <name> [text]")
        );
        sender.sendMessage(
                color("&e/hologram delete <name>")
        );
        sender.sendMessage(
                color("&e/hologram set <name> <line> <text>")
        );
        sender.sendMessage(
                color("&e/hologram addline <name> <text>")
        );
        sender.sendMessage(
                color("&e/hologram removeline <name> <line>")
        );
        sender.sendMessage(
                color("&e/hologram list")
        );
        sender.sendMessage(
                color("&e/hologram tp <name>")
        );
        sender.sendMessage(
                color("&e/hologram reload")
        );
    }

    // =========================================================
    // TAB COMPLETE
    // =========================================================

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {

        if (args.length == 1) {

            List<String> commands =
                    List.of(
                            "create",
                            "delete",
                            "set",
                            "addline",
                            "removeline",
                            "list",
                            "tp",
                            "reload"
                    );

            return filter(commands, args[0]);
        }

        if (args.length >= 2 &&
                (
                        args[0].equalsIgnoreCase("delete") ||
                        args[0].equalsIgnoreCase("set") ||
                        args[0].equalsIgnoreCase("addline") ||
                        args[0].equalsIgnoreCase("removeline") ||
                        args[0].equalsIgnoreCase("tp")
                )) {

            ConfigurationSection section =
                    getConfig().getConfigurationSection(
                            "holograms"
                    );

            if (section == null) {
                return Collections.emptyList();
            }

            return filter(
                    new ArrayList<>(section.getKeys(false)),
                    args[args.length - 1]
            );
        }

        return Collections.emptyList();
    }

    private List<String> filter(
            List<String> values,
            String input
    ) {

        List<String> result =
                new ArrayList<>();

        for (String value : values) {

            if (value.toLowerCase()
                    .startsWith(input.toLowerCase())) {

                result.add(valu

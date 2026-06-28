package dev.talkingpanda.twitchierchat;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class Config {
    private static final Path configPath = Path.of(TwitchierChat.configDir.toString(), "config.json");
    private static final File configFile = configPath.toFile();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final ConfigData configData = new ConfigData();

    public static String getServerUrl() {
        if (configData.serverAddress == null || configData.serverPort == null) {
            return null;
        }
        return configData.serverAddress + ":" + configData.serverPort;
    }

    public static void setServer(String address, Integer serverPort) {
        configData.serverAddress = address;
        configData.serverPort = serverPort;
        writeConfig();
    }

    public static boolean addManager(UUID manager) {
        User user = getUser(manager);
        if (user.isManager) {
            return false;
        }
        user.isManager = true;
        writeConfig();
        return true;
    }

    public static boolean removeManager(UUID manager) {
        User user = getUser(manager);
        if (!user.isManager) {
            return false;
        }

        user.isManager = false;
        writeConfig();
        return true;
    }

    public static void setPing(UUID player, boolean reply, boolean shouldPing) {
        User user = getUser(player);
        if (reply) {
            user.shouldReplyPing = shouldPing;
        } else {
            user.shouldPing = shouldPing;
        }
        writeConfig();
    }

    public static boolean shouldPing(UUID player, UUID sender, boolean reply) {
        User user = getUser(player);

        if (user.blockedPlayers.contains(sender)) {
            return false;
        }

        return reply ? user.shouldReplyPing : user.shouldPing;
    }


    public static boolean isManager(UUID player) {
        return getUser(player).isManager;
    }

    public static void setColor(UUID player, Integer color) {
        getUser(player).color = color;
        writeConfig();
    }

    public static void removeColor(UUID player) {
        getUser(player).color = null;
        writeConfig();
    }

    public static @Nullable Integer getColor(UUID player) {
        return getUser(player).color;
    }

    public static @NonNull User getUser(UUID player) {
        return configData.users.computeIfAbsent(player, _ -> new User());
    }

    private static void migrate(OldConfigData data) {
        configData.serverAddress = data.serverAddress;
        configData.serverPort = data.serverPort;
        configData.maxHistory = data.maxHistory;

        for (UUID uuid : data.managers) {
            addManager(uuid);
        }

        for (UUID uuid : data.dontPing) {
            setPing(uuid, false, false);
            setPing(uuid, true, false);
        }

        for (var entry : data.colorMap.entrySet()) {
            setColor(entry.getKey(), entry.getValue());
        }

    }


    public static void readConfig() {

        if(!configFile.exists()) {
            TwitchierChat.LOGGER.info("Creating default config file");
            writeConfig();
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            if (json == null) {
                return;
            }

            int version = json.has("version") ? json.get("version").getAsInt() : 1;

            if (version == 1) {
                OldConfigData oldData = gson.fromJson(json, OldConfigData.class);
                TwitchierChat.LOGGER.info("Version 1 config detected, migrating");
                migrate(oldData);
                writeConfig();
                return;
            }

            if (version != 2) {
                TwitchierChat.LOGGER.warn("Unknown config version overwriting");
                writeConfig();
                return;
            }

            ConfigData data = gson.fromJson(json, ConfigData.class);


            if (data.serverAddress != null) {

                configData.serverAddress = data.serverAddress;
            }

            if (data.serverPort != null) {
                configData.serverPort = data.serverPort;
            }

            if (data.maxHistory != null) {
                configData.maxHistory = data.maxHistory;
            }

            configData.users.clear();
            if (data.users != null) {
                configData.users = data.users;
            }


        } catch (IOException e) {
            TwitchierChat.LOGGER.error("Failed to read config file: ", e);
        }
    }

    public static Integer getMaxHistory() {
        return configData.maxHistory;
    }

    public static void setMaxHistory(Integer maxHistory) {
        configData.maxHistory = maxHistory;
        writeConfig();
    }

    private static void writeConfig() {

        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(configData, writer);
        } catch (IOException e) {
            TwitchierChat.LOGGER.error("Failed to write config file: ", e);

        }
    }

    public static boolean removeAlias(UUID player, String name) {
        User user = getUser(player);
        boolean result = user.aliases.remove(name.toLowerCase());
        writeConfig();
        return result;
    }

    public static boolean addAlias(UUID player, String name) {
        User user = getUser(player);
        boolean result = user.aliases.add(name.toLowerCase());
        writeConfig();
        return result;
    }

    public static String[] getAliases(UUID player) {
        User user = getUser(player);
        return user.aliases.toArray(new String[0]);
    }

    public static void setPingSound(UUID player, Identifier sound) {
        getUser(player).pingSound = sound;
        writeConfig();
    }

    public static Holder<SoundEvent> getPingSound(UUID player) {
        User user = getUser(player);
        if (user.pingSound == null) {
            return SoundEvents.NOTE_BLOCK_PLING;
        }
        return Holder.direct(SoundEvent.createVariableRangeEvent(user.pingSound));
    }

    public static boolean blockPlayer(UUID player, UUID blocked) {
        User user = getUser(player);
        boolean result = user.blockedPlayers.add(blocked);
        writeConfig();
        return result;
    }

    public static boolean unblockPlayer(UUID player, UUID blocked) {
        User user = getUser(player);
        boolean result = user.blockedPlayers.remove(blocked);
        writeConfig();
        return result;
    }

    public static class User {
        public boolean isManager = false;
        public boolean shouldPing = true;
        public boolean shouldReplyPing = true;
        public Integer color = null;
        public HashSet<String> aliases = new HashSet<>();
        public HashSet<UUID> blockedPlayers = new HashSet<>();
        public Identifier pingSound = null;
    }

    private static class ConfigData {
        public String serverAddress = null;
        public Integer serverPort = null;
        public Integer maxHistory = 100;
        public HashMap<UUID, User> users = new HashMap<>();
    }

    private static class OldConfigData {
        public final HashSet<UUID> managers = new HashSet<>();
        public final HashMap<UUID, Integer> colorMap = new HashMap<>();
        public String serverAddress = null;
        public Integer serverPort = null;
        public HashSet<UUID> dontPing = new HashSet<>();
        public Integer maxHistory = 100;
    }
}
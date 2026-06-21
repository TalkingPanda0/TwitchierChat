package dev.talkingpanda.twitchierchat;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.Nullable;

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
        boolean result = configData.managers.add(manager);
        writeConfig();

        return result;
    }

    public static boolean removeManager(UUID manager) {
        boolean result = configData.managers.remove(manager);
        writeConfig();
        return result;
    }

    public static void setPing(UUID player, boolean shouldPing) {
        if (shouldPing) {
            configData.dontPing.remove(player);
        } else {
            configData.dontPing.add(player);
        }
    }

    public static boolean shouldPing(UUID player) {
        return !configData.dontPing.contains(player);
    }


    public static boolean isManager(UUID player) {
        return configData.managers.contains(player);
    }

    public static void setColor(UUID player, Integer color) {
        configData.colorMap.put(player, color);
        writeConfig();
    }

    public static void removeColor(UUID player) {
        configData.colorMap.remove(player);
        writeConfig();
    }

    public static @Nullable Integer getColor(UUID player) {
        return configData.colorMap.get(player);
    }


    public static void readConfig() {

        try (FileReader reader = new FileReader(configFile)) {
            ConfigData data = gson.fromJson(reader, ConfigData.class);

            if (data != null) {
                if (data.serverAddress != null) {

                    configData.serverAddress = data.serverAddress;
                }

                if(data.serverPort != null) {
                    configData.serverPort = data.serverPort;
                }

                configData.managers.clear();
                if (data.managers != null) {
                    configData.managers.addAll(data.managers);
                }

                configData.colorMap.clear();
                if (data.colorMap != null) {
                    configData.colorMap.putAll(data.colorMap);
                }

                if(data.maxHistory != null) {
                    configData.maxHistory = data.maxHistory;
                }

                configData.dontPing.clear();
                if(data.dontPing != null) {
                    configData.dontPing = data.dontPing;
                }
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

    private static class ConfigData {
        public String serverAddress = null;
        public Integer serverPort = null;
        public HashSet<UUID> managers = new HashSet<>();
        public HashSet<UUID> dontPing = new HashSet<>();
        public Integer maxHistory = 100;
        public HashMap<UUID, Integer> colorMap = new HashMap<>();
    }
}

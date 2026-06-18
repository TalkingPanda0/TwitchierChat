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
    private static final HashSet<UUID> managers = new HashSet<>();
    private static final HashMap<UUID, Integer> colorMap = new HashMap<>();
    private static final Path configPath = Path.of(TwitchierChat.configDir.toString(), "config.json");
    private static final File configFile = configPath.toFile();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static String serverAddress = null;
    private static Integer serverPort = null;

    public static String getServerUrl() {
        if(serverAddress == null || serverPort == null ) {
            return null;
        }
        return serverAddress + ":" + serverPort;
    }

    public static String getServerAddress() {
        return serverAddress;
    }

    public static void setServerAddress(String serverAddress) {
        Config.serverAddress = serverAddress;
        writeConfig();
    }

    public static Integer getServerPort() {
        return serverPort;
    }

    public static void setServer(String address, Integer serverPort) {
        Config.serverAddress = address;
        Config.serverPort = serverPort;
        writeConfig();
    }

    public static boolean addManager(UUID manager) {
        boolean result = Config.managers.add(manager);
        writeConfig();

        return result;
    }

    public static boolean removeManager(UUID manager) {
        boolean result = Config.managers.remove(manager);
        writeConfig();
        return result;
    }


    public static boolean isManager(UUID player) {
        return Config.managers.contains(player);
    }

    public static void setColor(UUID player, Integer color) {
        Config.colorMap.put(player, color);
        writeConfig();
    }

    public static void removeColor(UUID player) {
        Config.colorMap.remove(player);
        writeConfig();
    }

    public static @Nullable Integer getColor(UUID player) {
        return Config.colorMap.get(player);
    }


    public static void readConfig() {
        if (!configFile.exists()) {
            Config.serverPort = TwitchierChat.minecraftServer.getServerPort();
            writeConfig();
            return;
        }
        try (FileReader reader = new FileReader(configFile)) {
            ConfigData data = gson.fromJson(reader, ConfigData.class);

            if (data != null) {
                Config.serverAddress = data.serverAddress;

                Config.serverPort = data.serverPort;

                Config.managers.clear();
                if (data.managers != null) {
                    Config.managers.addAll(data.managers);
                }

                Config.colorMap.clear();
                if (data.colorMap != null) {
                    Config.colorMap.putAll(data.colorMap);
                }
            }
        } catch (IOException e) {
            TwitchierChat.LOGGER.error("Failed to read config file: ", e);
        }


    }

    private static void writeConfig() {
        ConfigData data = new ConfigData(Config.serverAddress, Config.serverPort, Config.managers, Config.colorMap);

        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            TwitchierChat.LOGGER.error("Failed to write config file: ", e);

        }
    }

    private static class ConfigData {
        private final String serverAddress;
        private final Integer serverPort;
        private final HashSet<UUID> managers;
        private final HashMap<UUID, Integer> colorMap;

        public ConfigData(String serverAddress, Integer serverPort, HashSet<UUID> managers, HashMap<UUID, Integer> colorMap) {
            this.serverAddress = serverAddress;
            this.serverPort = serverPort;
            this.managers = managers;
            this.colorMap = colorMap;
        }
    }
}

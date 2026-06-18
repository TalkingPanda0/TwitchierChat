package dev.talkingpanda.twitchierchat;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class Emotes {
    public final static Map<String, ServerPlayer> emotes = new HashMap<>();

    final static String PACK_META = "{ \"pack\": { \"description\": \"Resource pack for chat emotes.\", \"pack_format\": 34, \"min_format\": 34, \"max_format\": 69, \"supported_formats\": {\"min_inclusive\": 34, \"max_inclusive\": 69} } }\n";
    final static String emotesPath = "assets/emotes/textures/gui/sprites/emotes/";

    static final Path emotePackPath = Path.of(TwitchierChat.configDir.toString(), "emotes.zip");
    final static File emotePackFile = emotePackPath.toFile();

    private static final int TILE_SIZE = 128;
    private static final int FPS = 20;


    private static boolean commandRequirement(CommandSourceStack source) {
        if (source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR)) {
            return true;
        }
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return false;
        }

        return Config.isManager(player.getUUID());
    }

    private static int executeAddCommand(CommandContext<CommandSourceStack> context, boolean showLogs, int tileSize, int fps) {
        final String emote = StringArgumentType.getString(context, "emote");
        final String sourceUrl = StringArgumentType.getString(context, "sourceUrl");
        if (emote.length() > 14) {
            context.getSource().sendSuccess(() -> Component.literal("Emote name is too long.").withColor(CommonColors.RED), false);
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Adding emote " + emote + " from " + sourceUrl + "..."), true);
        try {
            if (!addEmote(emote, sourceUrl, context.getSource(), showLogs, tileSize, fps)) {
                context.getSource().sendSuccess(() -> Component.literal("Emote " + emote + " already exists.").withColor(CommonColors.RED), true);
                return 0;
            }
        } catch (Exception e) {
            context.getSource().sendSuccess(() -> Component.literal("Error while adding emote " + emote + ": " + e.getMessage()).withColor(CommonColors.RED), true);
        }
        return 1;
    }

    public static ServerPlayer createFakePlayer(String emote) {
        return FakePlayer.get(TwitchierChat.minecraftServer.overworld(), new GameProfile(UUID.randomUUID(), ":" + emote + ":"));
    }


    private static void loadEmotes() throws IOException {
        var fs = FileSystems.newFileSystem(emotePackFile.toPath());
        var emotePath = fs.getPath(emotesPath);
        var emoteList = Files.list(emotePath);


        emoteList.map(path -> path.getFileName().toString()).filter(s -> !s.endsWith(".mcmeta")).forEach(file -> {
            String emote = file.replace(".png", "");
            emotes.put(emote, createFakePlayer(emote));

        });
        TwitchierChat.LOGGER.info("Loaded emotes: {}.", emotes.keySet());
        fs.close();
        emoteList.close();
    }

    public static boolean removeEmote(String emote) throws Exception {
        if (!emotes.containsKey(emote)) return false;

        var fs = FileSystems.newFileSystem(emotePackFile.toPath());
        var emotePath = fs.getPath(emotesPath + emote + ".png");
        var metaPath = fs.getPath(emotesPath + emote + ".png.mcmeta");

        Files.delete(emotePath);
        if (Files.exists(metaPath)) Files.delete(metaPath);

        var result = emotes.remove(emote);
        TwitchierChat.minecraftServer.getPlayerList().broadcastAll(new ClientboundPlayerInfoRemovePacket(List.of(result.getUUID())));
        fs.close();
        return true;
    }

    public static boolean addEmote(String emote, String url, CommandSourceStack source, boolean pro, int tileSize, int fps) {
        if (emotes.containsKey(emote)) return false;

        try {
            URL parsedUrl = new URI(url).toURL();
            downloadImageAsync(pro ? source : null, parsedUrl, pro ? tileSize : TILE_SIZE, pro ? fps : FPS).thenAccept(
                    result -> {
                        try {
                            addEmoteFile(emote, result.getFirst(), result.getSecond());
                            final ServerPlayer player = createFakePlayer(emote);
                            emotes.put(emote, player);
                            TwitchierChat.minecraftServer.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER), List.of(player)));
                            source.sendSuccess(() -> Component.literal("Added emote " + emote + ".").withColor(CommonColors.GREEN), true);
                            TwitchierChat.minecraftServer.getPlayerList().broadcastSystemMessage(Component.literal("New emote has been added: " + emote + " ").append(Component.literal("(Click here to update)").setStyle(Style.EMPTY.withColor(CommonColors.GRAY).withItalic(true).withClickEvent((new ClickEvent.RunCommand("/emotes update"))))), false);
                        } catch (Exception e) {
                            TwitchierChat.LOGGER.error(e.getMessage());
                            source.sendSuccess(() -> Component.literal(e.getMessage()).withColor(CommonColors.RED), true);
                        }
                    }
            ).exceptionally(e -> {
                source.sendSuccess(() -> Component.literal(e.getMessage()).withColor(CommonColors.RED), true);
                return null;
            });

        } catch (Exception e) {
            TwitchierChat.LOGGER.error(e.getMessage());
            source.sendSuccess(() -> Component.literal(e.getMessage()).withColor(CommonColors.RED), true);
        }


        return true;
    }

    public static CompletableFuture<Pair<InputStream, @Nullable String>> downloadImageAsync(@Nullable CommandSourceStack source, URL url, int tileSize, int ifps) throws Exception {


        return
                CompletableFuture.supplyAsync(() -> {
                    try {
                        Path framesDir = Files.createTempDirectory("frames_");
                        Path framesPattern = framesDir.resolve("frame_%05d.png");
                        int fps = Math.max(FPS, ifps);

                        runFfmpeg(
                                source,
                                "-i", url.toString(),
                                "-vf", "fps=" + fps + ",scale=" + tileSize + ":" + tileSize + ":force_original_aspect_ratio=increase,crop=" + tileSize + ":" + tileSize,
                                "-y",
                                framesPattern.toString()
                        );

                        // Count frames
                        long frameCount = Files.list(framesDir).count();

                        if (frameCount == 0) {
                            throw new RuntimeException("Failed to extract frames from the input.");
                        }
                        if (frameCount == 1) {
                            Path single = Files.list(framesDir).findFirst().get();
                            InputStream is = Files.newInputStream(single);
                            return new Pair<>(is, null);
                        }

                        Path output = Files.createTempFile("tiled_", ".png");
                        runFfmpeg(
                                source,
                                "-i", framesPattern.toString(),
                                "-filter_complex", "tile=1x" + frameCount,
                                "-y",
                                "-compression_level", "9",
                                output.toString()
                        );

                        String meta = getAnimationMeta(tileSize, tileSize, FPS / fps);

                        byte[] data = Files.readAllBytes(output);
                        Files.delete(output);
                        deleteDirRecursive(framesDir);
                        return new Pair<>(new ByteArrayInputStream(data), meta);


                    } catch (Exception e) {

                        throw new CompletionException(e);
                    }
                });

    }

    private static void deleteDirRecursive(Path dir) {
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    private static void runFfmpeg(@Nullable CommandSourceStack source, String... args) throws Exception {
        String[] cmd = new String[args.length + 1];
        cmd[0] = "ffmpeg";
        System.arraycopy(args, 0, cmd, 1, args.length);
        TwitchierChat.LOGGER.info("Running " + String.join(" ", cmd) + ".");
        Process p = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start();

        CompletableFuture<Void> out = CompletableFuture.runAsync(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (source == null) {
                        TwitchierChat.LOGGER.info(line);
                    } else {
                        String finalLine = line;
                        source.sendSuccess(() -> Component.literal(finalLine), false);
                    }
                }
            } catch (IOException ignored) {
            }
        });

        int exit = p.waitFor();
        out.join();

        if (exit != 0)
            throw new RuntimeException("ffmpeg failed with code " + exit);
    }

    private static String getAnimationMeta(int width, int height, int ticks) {
        return String.format("{ \"animation\": { \"width\": %d, \"height\": %d, \"frametime\": %d } }\n", width, height, ticks);

    }

    private static void addEmoteFile(String emote, InputStream asset, @Nullable String meta) throws IOException {
        var fs = FileSystems.newFileSystem(emotePackFile.toPath());
        var emotePath = fs.getPath(emotesPath + emote + ".png");

        TwitchierChat.LOGGER.info("Writing {}.", emotePath);
        var output = Files.newOutputStream(emotePath, StandardOpenOption.CREATE);
        output.write(asset.readAllBytes());
        output.close();
        if (meta != null) {
            var metaPath = fs.getPath(emotesPath + emote + ".png.mcmeta");
            TwitchierChat.LOGGER.info("Writing {}.", metaPath);

            var metaOutput = Files.newOutputStream(metaPath, StandardOpenOption.CREATE);
            metaOutput.write(meta.getBytes());
            metaOutput.close();
        }
        fs.close();
        TwitchierChat.LOGGER.info("Done writing emote files to the resource pack.");
    }

    private static void createEmptyResourcePack() throws IOException {
        TwitchierChat.LOGGER.info("Creating empty resource pack...");
        FileOutputStream fos = new FileOutputStream(emotePackFile);
        ZipOutputStream zipOut = new ZipOutputStream(fos);

        zipOut.putNextEntry(new ZipEntry("pack.mcmeta"));
        zipOut.write(PACK_META.getBytes(StandardCharsets.UTF_8));
        zipOut.closeEntry();

        zipOut.putNextEntry(new ZipEntry(emotesPath));
        zipOut.closeEntry();

        zipOut.close();
        fos.close();
    }

    public static String getPackHash() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");

        try (FileInputStream in = new FileInputStream(emotePackFile)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                digest.update(buf, 0, n);
            }
        }

        byte[] hash = digest.digest();
        return toHex(hash);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >>> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    public static @Nullable MinecraftServer.ServerResourcePackInfo getPackProperties() throws Exception {
        String url = Config.getServerUrl();
        if (url == null) {
            return null;
        }
        return new MinecraftServer.ServerResourcePackInfo(UUID.randomUUID(), "http://" + url + "/emotes.zip", getPackHash(),
                false, Component.literal("This is the resource pack for emotes.\n If you reject it, you will see ").append(Component.object(new AtlasSprite(AtlasIds.GUI, Identifier.fromNamespaceAndPath("minecraft", "error")))).append(" instead of emotes."));
    }

    public static void readConfig() {
        try {
            if (emotePackFile.createNewFile())
                createEmptyResourcePack();
            else
                loadEmotes();

        } catch (IOException e) {
            TwitchierChat.LOGGER.error(e.getMessage());
        }
    }

    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("emotes").then(
                Commands.literal("add")
                        .requires(Emotes::commandRequirement)
                        .then(
                                Commands.argument("emote", StringArgumentType.word())
                                        .then(
                                                Commands.argument("sourceUrl", StringArgumentType.string())
                                                        .executes(ctx ->
                                                                Emotes.executeAddCommand(ctx, false, 0, 0)
                                                        )
                                                        .then(
                                                                Commands.argument("showLogs", BoolArgumentType.bool())
                                                                        .then(
                                                                                Commands.argument("tileSize", IntegerArgumentType.integer(0))
                                                                                        .then(
                                                                                                Commands.argument("fps", IntegerArgumentType.integer(0, 20))
                                                                                                        .executes(ctx ->
                                                                                                                Emotes.executeAddCommand(
                                                                                                                        ctx,
                                                                                                                        BoolArgumentType.getBool(ctx, "showLogs"),
                                                                                                                        IntegerArgumentType.getInteger(ctx, "tileSize"),
                                                                                                                        IntegerArgumentType.getInteger(ctx, "fps")
                                                                                                                )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        ))


        );

        dispatcher.register(Commands.literal("emotes").then(
                Commands.literal("remove").requires(Emotes::commandRequirement).then(Commands.argument("emote", StringArgumentType.string()).suggests(new EmotesSuggestionProvider()).executes(context -> {
                    final String emote = StringArgumentType.getString(context, "emote");
                    try {
                        final boolean removedEmote = removeEmote(emote);
                        context.getSource().sendSuccess(() -> removedEmote ? Component.literal("Removed emote " + emote + ".").withColor(CommonColors.GREEN) : Component.literal("Emote " + emote + " does not exist.").withColor(CommonColors.RED), true);
                    } catch (Exception e) {
                        context.getSource().sendSuccess(() -> Component.literal("Failed to remove emote " + emote + ": " + e.getMessage()).withColor(CommonColors.RED), true);
                    }
                    return 1;
                }))));

        dispatcher.register(Commands.literal("emotes").then(
                Commands.literal("list").executes(context -> {
                    if (emotes.isEmpty()) {
                        context.getSource().sendSuccess(() -> Component.literal("There are currently no emotes.").withColor(CommonColors.RED), false);
                        return 1;
                    }
                    context.getSource().sendSuccess(() -> TwitchierChat.formatString("Emotes: " + Emotes.emotes.keySet().stream().map(e -> e + ": :" + e + ": ").collect(Collectors.joining(", "))), false);
                    return 1;
                })));

        dispatcher.register(Commands.literal("emotes").then(Commands.literal("update").requires(CommandSourceStack::isPlayer).executes(context -> {
            try {
                var pack = getPackProperties();
                if (pack == null) {
                    context.getSource().sendFailure(Component.literal("Failed to get resource pack properties.").withColor(CommonColors.RED));
                    return 1;
                }
                var player = context.getSource().getPlayer();
                if (player == null) {
                    context.getSource().sendFailure(Component.literal("Failed to send update packet: player is null.").withColor(CommonColors.RED));
                    return 1;
                }

                player.connection.send(new ClientboundResourcePackPushPacket(
                        pack.id(),
                        pack.url(),
                        pack.hash(),
                        pack.isRequired(),
                        Optional.ofNullable(pack.prompt())
                ));
            } catch (Exception e) {
                TwitchierChat.LOGGER.error(e.getMessage());
                context.getSource().sendFailure(Component.literal("Failed sending update packet: " + e.getMessage()).withColor(CommonColors.RED));
            }
            return 1;
        })));

        dispatcher.register(Commands.literal("emotes").then(
                Commands.literal("reload").requires(Emotes::commandRequirement).executes(context -> {
                    try {
                        loadEmotes();
                        context.getSource().sendSuccess(() -> Component.literal("Reloaded Emotes").withColor(CommonColors.GREEN), true);
                        TwitchierChat.minecraftServer.getPlayerList().broadcastSystemMessage(Component.literal("Emotes have been reloaded. ").append(Component.literal("(Click here to update)").setStyle(Style.EMPTY.withColor(CommonColors.GRAY).withItalic(true).withClickEvent((new ClickEvent.RunCommand("/emotes update"))))), false);

                    } catch (IOException e) {
                        context.getSource().sendFailure(Component.literal("Failed Reloading Emotes: " + e).withColor(CommonColors.RED));
                    }
                    return 1;
                })));
    }

}

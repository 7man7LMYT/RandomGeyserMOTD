package io.github.randomotd;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.event.connection.GeyserBedrockPingEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RandomGeyserMOTD implements Extension {

    private final List<String> motdList = new ArrayList<>();

    @Subscribe
    public void onGeyserPostInit(GeyserPostInitializeEvent event) {
        Path motdPath = dataFolder().resolve("motds.txt");

        // Create extension folder if needed
        if (Files.notExists(dataFolder())) {
            try {
                Files.createDirectory(dataFolder());
            } catch (IOException e) {
                logger().warning("Failed to create extension folder");
                return;
            }
        }

        // If MOTD file doesn't exist, try creating it
        if (Files.notExists(motdPath)) {
            try {
                URL configUrl = getClass().getResource("/motds.txt");

                if (configUrl == null) {
                    logger().warning("Failed to find default configuration");
                    return;
                }

                try (FileSystem system = FileSystems.newFileSystem(configUrl.toURI(), Collections.emptyMap(), null)) {
                    Files.copy(system.getPath("motds.txt"), motdPath);
                }
            } catch (IOException | URISyntaxException e) {
                logger().error("Failed to create default configuration", e);
                return;
            }
        }

        reloadMOTDsFromDisk();
    }

    @Subscribe
    public void onGeyserCommandDefine(GeyserDefineCommandsEvent event) {
        event.register(Command.builder(this)
                .source(CommandSource.class)
                .name("rndmotd")
                .description("Reloads MOTDs from disk")
                .permission("rndmotd.reload")
                .bedrockOnly(false)
                .suggestedOpOnly(true)
                .executableOnConsole(true)
                .executor(((source, command, args) -> {
                    if (source.isConsole() || source.hasPermission("rndmotd.reload")) {
                        source.sendMessage("§bReloading MOTDs!");

                        reloadMOTDsFromDisk();
                    }

                    source.sendMessage("§cYou do not have permission to run this command!");
                }))
                .build()
        );
    }

    @Subscribe
    public void onPing(GeyserBedrockPingEvent event) {
        event.primaryMotd(motdList.get(ThreadLocalRandom.current().nextInt(0, motdList.size())));
    }

    private void reloadMOTDsFromDisk() {
        motdList.clear();
        try (BufferedReader configReader = Files.newBufferedReader(dataFolder().resolve("motds.txt"))) {
            String currLine;
            while ((currLine = configReader.readLine()) != null) {
                if (currLine.isEmpty() || currLine.isBlank()) continue;

                motdList.add(currLine);
            }

            logger().info("Loaded " + motdList.size() + " MOTDs from disk");
        } catch (IOException e) {
            logger().error("Failed to read MOTDs", e);
        }

        // Add generic entries if the list is still empty
        if (motdList.isEmpty()) {
            motdList.add("RandomGeyserMOTD ");
        }
    }
}

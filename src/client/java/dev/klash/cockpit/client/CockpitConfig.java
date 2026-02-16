package dev.klash.cockpit.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class CockpitConfig implements ModMenuApi {
    public static boolean enableInfo = true;
    public static boolean enableSpeedGraph = true;
    public static boolean enableSnapPitch = true;
    public static boolean enableFireworkTimer = true;
    public static boolean enableWarnings = true;
    public static boolean enablePitchGuides = true;
    public static boolean enablePlayerRender = true;

    private static final String FILE_NAME = "cockpit-config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path file;

    public static void init() {
        file = MinecraftClient.getInstance().runDirectory.toPath().resolve(FILE_NAME);

        if (Files.exists(file)) {
            load();
        } else {
            save(); // create default config
        }
    }

    private static void load() {
        try (Reader reader = Files.newBufferedReader(file)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data != null) {
                enableInfo = data.enableInfo;
                enableSpeedGraph = data.enableSpeedGraph;
                enableSnapPitch = data.enableSnapPitch;
                enableFireworkTimer = data.enableFireworkTimer;
                enableWarnings = data.enableWarnings;
                enablePitchGuides = data.enablePitchGuides;
                enablePlayerRender = data.enablePlayerRender;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(file)) {
            ConfigData data = new ConfigData();
            data.enableInfo = enableInfo;
            data.enableSpeedGraph = enableSpeedGraph;
            data.enableSnapPitch = enableSnapPitch;
            data.enableFireworkTimer = enableFireworkTimer;
            data.enableWarnings = enableWarnings;
            data.enablePitchGuides = enablePitchGuides;
            data.enablePlayerRender = enablePlayerRender;

            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ConfigData {
        boolean enableInfo;
        boolean enableSpeedGraph;
        boolean enableSnapPitch;
        boolean enableFireworkTimer;
        boolean enableWarnings;
        boolean enablePitchGuides;
        boolean enablePlayerRender;
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {

            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.literal("Cockpit Config"));

            builder.setSavingRunnable(CockpitConfig::save);

            ConfigCategory visual = builder.getOrCreateCategory(Text.literal("Visual"));
            ConfigCategory advanced = builder.getOrCreateCategory(Text.literal("Advanced"));

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            visual.addEntry(entryBuilder.startBooleanToggle(
                            Text.literal("Enable Info"),
                            CockpitConfig.enableInfo)
                    .setDefaultValue(true)
                    .setSaveConsumer(value -> CockpitConfig.enableInfo = value)
                    .build());

            visual.addEntry(entryBuilder.startBooleanToggle(
                            Text.literal("Enable Speed Graph"),
                            CockpitConfig.enableSpeedGraph)
                    .setDefaultValue(true)
                    .setSaveConsumer(value -> CockpitConfig.enableSpeedGraph = value)
                    .build());

            visual.addEntry(entryBuilder.startBooleanToggle(
                            Text.literal("Enable Firework Timer"),
                            CockpitConfig.enableFireworkTimer)
                    .setDefaultValue(true)
                    .setSaveConsumer(value -> CockpitConfig.enableFireworkTimer = value)
                    .build());

            visual.addEntry(entryBuilder.startBooleanToggle(
                            Text.literal("Enable Warnings"),
                            CockpitConfig.enableWarnings)
                    .setDefaultValue(true)
                    .setSaveConsumer(value -> CockpitConfig.enableWarnings = value)
                    .build());

            visual.addEntry(entryBuilder.startBooleanToggle(
                            Text.literal("Enable Pitch Guides"),
                            CockpitConfig.enablePitchGuides)
                    .setDefaultValue(true)
                    .setSaveConsumer(value -> CockpitConfig.enablePitchGuides = value)
                    .build());

            visual.addEntry(entryBuilder.startBooleanToggle(
                            Text.literal("Enable Player Render"),
                            CockpitConfig.enablePlayerRender)
                    .setDefaultValue(true)
                    .setSaveConsumer(value -> CockpitConfig.enablePlayerRender = value)
                    .build());

            advanced.addEntry(entryBuilder.startBooleanToggle(
                            Text.literal("Enable Magnetic Pitch"),
                            CockpitConfig.enableSnapPitch)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("Magnifies your cursor toward -36 and 36 to help infinite flight."))
                    .setSaveConsumer(value -> CockpitConfig.enableSnapPitch = value)
                    .build());

            return builder.build();
        };
    }

}
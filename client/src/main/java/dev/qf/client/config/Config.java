package dev.qf.client.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import common.util.KioskLoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class Config {
    private static final Logger LOGGER = KioskLoggerFactory.getLogger();
    private static Config instance;
    private static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("host_addr").forGetter(Config::getHost),
            Codec.INT.fieldOf("port").forGetter(Config::getPort),
            Codec.STRING.fieldOf("kiosk_id").forGetter(Config::getKioskId)
    ).apply(instance, Config::new));

    private String host;
    private int port;
    private String kioskId;

    public Config(String host, int port, String kioskId) {
        this.host = host;
        this.port = port;
        this.kioskId = kioskId;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getKioskId() {
        return kioskId;
    }

    public static Config load() {
        File configFile = new File("config.json");
        Config config;
        if (!configFile.exists()) {
            LOGGER.info("Config file not found. Creating new one.");
            config = getDefault();
            try {
                Files.writeString(configFile.toPath(), CODEC.encodeStart(JsonOps.INSTANCE, config).resultOrPartial().get().toString());
            } catch (IOException e) {
                LOGGER.error("Failed to write config file", e);
            }
        } else {
            try {
                String s = Files.readString(configFile.toPath());
                config = CODEC.decode(JsonOps.INSTANCE, new Gson().fromJson(s, JsonObject.class)).resultOrPartial().get().getFirst();
            } catch (IOException e) {
                LOGGER.error("Failed to read config file", e);
                config = getDefault();
            }
        }

        instance = config;
        return config;
    }

    public static Config getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Config not loaded yet");
        }
        return instance;
    }

    private static Config getDefault() {
        return new Config("localhost", 8192, "kiosk1");
    }
}

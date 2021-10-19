package dev.tr7zw.discordbot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.server.Server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.tr7zw.discordbot.util.Config;
import dev.tr7zw.discordbot.util.Handler;
import lombok.Getter;
import lombok.extern.java.Log;

@Getter
@Log
public class LogisticsCraftBot {

    private final DiscordApi api;
    private final Config config;
    private final List<Handler> activeHandlers = new ArrayList<>();
    private final Server server;
    private final File configFile = new File("config.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    public LogisticsCraftBot(DiscordApi api) throws Exception {
        this.api = api;
        if(configFile.exists()) {
            config = gson.fromJson(new String(Files.readAllBytes(configFile.toPath())), Config.class);
        }else {
            config = new Config();
        }
        updateConfig();
        server = api.getServerById(config.getServerid()).orElseThrow(() -> new RuntimeException("Bot not connected to the target server!"));
    }
    
    public boolean addHandler(Handler handler) {
        try {
            handler.init(this);
            activeHandlers.add(handler);
            return true;
        }catch(Exception ex) {
            log.log(Level.SEVERE, "Error while enableing handler " + handler.getClass().getName(), ex);
            if(handler.failDeadly()) {
                System.exit(1);
            }
            return false;
        }
    }
    
    public void updateConfig() {
        try {
            Files.write(configFile.toPath(), gson.toJson(config).getBytes());
        } catch (IOException e) {
            log.log(Level.WARNING, "Error saving the config!", e);
        }
    }
    
    
}

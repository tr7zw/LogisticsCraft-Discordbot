package dev.tr7zw.discordbot.util;

import dev.tr7zw.discordbot.LogisticsCraftBot;

public interface Handler {

    public void init(LogisticsCraftBot bot) throws Exception;
    
    /**
     * @return true if the bot shouldn't start when init fails
     */
    public boolean failDeadly();
    
}

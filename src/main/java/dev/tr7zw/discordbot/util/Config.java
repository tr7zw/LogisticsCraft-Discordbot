package dev.tr7zw.discordbot.util;

import java.util.Arrays;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Config {

    private long serverid = 342814924310970398l;
    private List<RoleAssigner> roleAssigner = Arrays.asList(new RoleAssigner(900089706409242684l, 123l, "👍", 725707689606512721l));
    
    @Getter
    @AllArgsConstructor
    public static class RoleAssigner{
        private long channel;
        @Setter
        private long messageId;
        private String emote;
        private long role;
    }
    
}

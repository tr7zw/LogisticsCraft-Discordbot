package dev.tr7zw.discordbot.handler;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;
import org.javacord.api.exception.BadRequestException;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveListener;

import dev.tr7zw.discordbot.LogisticsCraftBot;
import dev.tr7zw.discordbot.util.Handler;
import dev.tr7zw.discordbot.util.Config.RoleAssigner;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.java.Log;

@Log
public class RoleProvider implements Handler, ReactionAddListener, ReactionRemoveListener {

    private Set<RoleData> watchingMessages = new HashSet<>();
    private LogisticsCraftBot bot;

    @Override
    public void init(LogisticsCraftBot bot) throws InterruptedException, ExecutionException {
        this.bot = bot;
        for (RoleAssigner roleAssigner : bot.getConfig().getRoleAssigner()) {
            TextChannel textChannel = bot.getServer().getChannelById(roleAssigner.getChannel()).get().asTextChannel()
                    .get();
            Message message = null;
            try {
                message = textChannel.getMessageById(roleAssigner.getMessageId()).get();
            } catch (Exception ex) {
            }
            Role role = bot.getServer().getRoleById(roleAssigner.getRole()).get();
            if (message == null) {
                message = textChannel.sendMessage("React to this message to get the role " + role.getMentionTag() + ".")
                        .get();
                roleAssigner.setMessageId(message.getId());
                bot.updateConfig();
            }
            message.addReaction(roleAssigner.getEmote()).get();
            watchingMessages.add(new RoleData(message.getId(), message, roleAssigner.getEmote(), role));
        }
        bot.getApi().addReactionAddListener(this);
        bot.getApi().addReactionRemoveListener(this);
    }

    @Override
    public boolean failDeadly() {
        return true;
    }

    @Override
    public void onReactionAdd(ReactionAddEvent event) {
        for (RoleData data : watchingMessages) {
            if (data.getMessageId() == event.getMessageId()) {
                try {
                    User user = event.requestUser().get();
                    log.info("Giving role " + data.getRole().getName() + " to " + user.getName());
                    user.addRole(data.getRole());
                } catch (InterruptedException | ExecutionException e) {
                    log.log(Level.WARNING, "Error in reaction handleing!", e);
                }
                return;
            }
        }
    }

    @Override
    public void onReactionRemove(ReactionRemoveEvent event) {
        for (RoleData data : watchingMessages) {
            if (data.getMessageId() == event.getMessageId()) {
                try {
                    User user = event.requestUser().get();
                    log.info("Taking role " + data.getRole().getName() + " from " + user.getName());
                    user.removeRole(data.getRole());
                } catch (InterruptedException | ExecutionException e) {
                    log.log(Level.WARNING, "Error in reaction handleing!", e);
                }
                return;
            }
        }
    }

    @AllArgsConstructor
    @Getter
    private class RoleData {
        private final long messageId;
        private final Message message;
        private final String emote;
        private final Role role;

    }

}

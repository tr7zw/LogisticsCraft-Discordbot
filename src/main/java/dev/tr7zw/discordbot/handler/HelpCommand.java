package dev.tr7zw.discordbot.handler;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.SelectMenu;
import org.javacord.api.entity.message.component.SelectMenuOption;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionChoice;
import org.javacord.api.interaction.SlashCommandOptionType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import dev.tr7zw.discordbot.LogisticsCraftBot;
import dev.tr7zw.discordbot.util.Handler;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

public class HelpCommand implements Handler {

    private Map<String, HelpEntry> helpMap = new HashMap<>();
    private Map<Topic, List<HelpEntry>> helpList = new HashMap<>();
    private Map<String, List<SelectMenuOption>> menuList = new HashMap<>();
    
    @Override
    public void init(LogisticsCraftBot bot) throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        File topicsFolder = new File("helptopics");
        for(File topicFolder : topicsFolder.listFiles()) {
            if(!topicFolder.isDirectory())continue;
            String topicKey = topicFolder.getName();
            String topicName = new String(Files.readAllBytes(new File(topicFolder, "name.txt").toPath()));
            Topic topic = new Topic(topicKey, topicName);
            List<HelpEntry> entries = new ArrayList<>();
            List<SelectMenuOption> menuEntries = new ArrayList<>();
            for(File file : topicFolder.listFiles()) {
                if(!file.getName().endsWith(".yml")) {
                    continue;
                }
                String entryKey = topicKey + "-" + file.getName().replace(".yml", "");
                HelpEntry entry = mapper.readValue(file, HelpEntry.class);
                helpMap.put(entryKey, entry);
                entries.add(entry);
                menuEntries.add(SelectMenuOption.create(entry.getLabel(), entryKey, entry.description));
                System.out.println(entry);
            }
            helpList.put(topic, entries);
            menuList.put(topicKey, menuEntries);
        }
        
        SlashCommand command =
                SlashCommand.with("faq", "Sends infos about a given general question into the current channel.",
                    Arrays.asList(
                            SlashCommandOption.createWithChoices(SlashCommandOptionType.STRING, "topic", "FAQ topic", true, 
                                    helpList.keySet().stream().map(t -> SlashCommandOptionChoice.create(t.getName(), t.getKey())).collect(Collectors.toList()))))
                .createForServer(bot.getServer())
                .join();
        
        bot.getApi().addSlashCommandCreateListener(event -> {
            SlashCommandInteraction slashCommandInteraction = event.getSlashCommandInteraction(); 
            if(command.getName().equals(slashCommandInteraction.getCommandName())) {
                List<SelectMenuOption> list;
                String topic = slashCommandInteraction.getOptionStringValueByName("topic").get();
                if(menuList.containsKey(topic)) {
                    list = menuList.get(topic);
                } else {
                    throw new RuntimeException("Unknown topic: " + topic);
                }
                slashCommandInteraction.createImmediateResponder().setContent("").addComponents(                    
                        ActionRow.of(SelectMenu.create("faqselection", "Click here to show the options", 1, 1, list)))
                .setFlags(MessageFlag.EPHEMERAL).respond().join();
            }
        });
        bot.getApi().addSelectMenuChooseListener(event -> {
           if("faqselection".equals(event.getSelectMenuInteraction().getCustomId())) {
               SelectMenuOption option = event.getSelectMenuInteraction().getChosenOptions().get(0);
               HelpEntry entry = helpMap.get(option.getValue());
               event.getInteraction().getChannel().get().sendMessage(new EmbedBuilder().setTitle(entry.getLabel()).setUrl(entry.getUrl()).setDescription(entry.text).setAuthor(event.getInteraction().getUser())).join();
               event.getSelectMenuInteraction().createOriginalMessageUpdater().removeAllComponents().setContent("Message sent!").update().join();
           }
        });
    }

    @Override
    public boolean failDeadly() {
        return false;
    }
    
    @Getter
    @ToString
    private static class HelpEntry {
        private String label;
        private String description;
        private String url;
        private String text;
        
    }
    
    @Getter
    @AllArgsConstructor
    private static class Topic {
        private String key;
        private String name;
    }
    
}

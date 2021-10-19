package dev.tr7zw.discordbot;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;

import com.google.gson.Gson;

import dev.tr7zw.discordbot.handler.RoleProvider;
import dev.tr7zw.discordbot.util.Config;

public class App {

	public static Gson gson = new Gson();
	public static final long serverid = 342814924310970398l; // Logisticsapi serverid
	public static Server mainServer;
	public static TextChannel nbtStatusChannel;
	public static TextChannel firstpersonStatusChannel;
	public static Message nbtStatus;
	public static Message firstpersonStatus;
	public static Emoji forkEmoji;
	public static Emoji githubEmoji;
	public static Emoji backendEmoji;
	public static Emoji issueEmoji;
	public static Emoji curseEmoji;
	public static Emoji spigotEmoji;
	public static Emoji codemcEmoji;
	private static final String discordApiEmptyString = "⠀";
	
	private static LogisticsCraftBot bot;

	public static void main(String[] args) throws Exception {
		String token = new String(Files.readAllBytes(new File("token.txt").toPath()), StandardCharsets.UTF_8).trim();

		DiscordApi api = new DiscordApiBuilder().setToken(token).login().join();
		
		bot = new LogisticsCraftBot(api);
		
		bot.addHandler(new RoleProvider());
		
		
		System.out.println("You can invite the bot by using the following url: " + api.createBotInvite());
		System.out.println(
				"On servers: " + api.getServers().stream().map(server -> server.getId()).collect(Collectors.toList()));
		mainServer = api.getServerById(serverid).get();
		setupNBTAPIChannel();
		setupFirstpersonChannel();

		setupMessageListener(api);
	}

	private static void setupNBTAPIChannel() throws InterruptedException, ExecutionException {
		nbtStatusChannel = mainServer.getChannelsByName("nbt-api-info").get(0).asTextChannel().get();
		MessageSet messages = nbtStatusChannel.getMessages(100).get();
		if (messages.size() == 1) {
			nbtStatus = messages.iterator().next();
		} else {
			for (Message message : messages) {
				message.delete().get();
			}
		}

		if (nbtStatus == null)
			nbtStatus = nbtStatusChannel.sendMessage("Loading...").get();

		// Just delete all new messages here
		nbtStatusChannel.addMessageCreateListener(event -> {
			event.getMessage().delete();
		});

		new Thread(() -> {
			while (true) {
				try {
					System.out.println("Update nbt post");
					try {
						updateNBTStatusMessage();
					} catch (Exception e) {
						e.printStackTrace();
					}
					Thread.sleep(2 * 60 * 1000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, "NBTAPI Channel Thread").start();
	}

	private static void setupFirstpersonChannel() throws InterruptedException, ExecutionException {
		firstpersonStatusChannel = mainServer.getChannelsByName("firstperson-info").get(0).asTextChannel().get();
		forkEmoji = mainServer.getCustomEmojisByName("fork").iterator().next();
		githubEmoji = mainServer.getCustomEmojisByName("github").iterator().next();
		backendEmoji = mainServer.getCustomEmojisByName("package").iterator().next();
		issueEmoji = mainServer.getCustomEmojisByName("issue").iterator().next();
		curseEmoji = mainServer.getCustomEmojisByName("curse").iterator().next();
		spigotEmoji = mainServer.getCustomEmojisByName("spigot").iterator().next();
		codemcEmoji = mainServer.getCustomEmojisByName("git").iterator().next();
		MessageSet messages = firstpersonStatusChannel.getMessages(100).get();
		if (messages.size() == 1) {
			firstpersonStatus = messages.iterator().next();
		} else {
			for (Message message : messages) {
				message.delete().get();
			}
		}

		if (firstpersonStatus == null)
			firstpersonStatus = firstpersonStatusChannel.sendMessage("Loading...").get();

		// Just delete all new messages here
		firstpersonStatusChannel.addMessageCreateListener(event -> {
			event.getMessage().delete();
		});

		new Thread(() -> {
			while (true) {
				try {
					System.out.println("Update firstperson post");
					try {
						updateFirstpersonStatusMessage();
					} catch (Exception e) {
						e.printStackTrace();
					}
					Thread.sleep(1 * 60 * 1000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, "Firstperson Channel Thread").start();
	}

	private static void setupMessageListener(DiscordApi api) {
		Emoji emojiGit = mainServer.getCustomEmojisByName("git").iterator().next();

		api.addMessageCreateListener(event -> {
			if (event.getMessageContent().toLowerCase().contains("git")) {
				event.getMessage().addReaction(emojiGit);
			}
		});
	}

	private static void updateNBTStatusMessage() throws InterruptedException, ExecutionException, IOException {
		SpigetData data = gson.fromJson(readFromUrl("https://api.spiget.org/v2/resources/7939"), SpigetData.class);
		GithubData github = gson.fromJson(readFromUrl("https://api.github.com/repos/tr7zw/Item-NBT-API"),
				GithubData.class);
		StringBuilder message = new StringBuilder();
		message.append("Links:\n");
		message.append("[" + githubEmoji.getMentionTag() + " Github](https://github.com/tr7zw/Item-NBT-API)\n");
		message.append("[" + curseEmoji.getMentionTag() + " Curseforge](https://www.curseforge.com/minecraft/bukkit-plugins/nbt-api)\n");
		message.append("[" + spigotEmoji.getMentionTag() + " SpigotMC](https://www.spigotmc.org/resources/nbt-api.7939/)\n");
		message.append("[" + codemcEmoji.getMentionTag() + " Latest Dev-Build](https://ci.codemc.io/job/Tr7zw/job/Item-NBT-API/)\n");
		message.append("[" + githubEmoji.getMentionTag() + " Wiki](https://github.com/tr7zw/Item-NBT-API/wiki)\n");

		nbtStatus.edit(null
				, new EmbedBuilder().setTitle("NBT-API Info").setTimestampToNow().setFooter("Last updated:")
				.setDescription(message.toString())
				.setThumbnail("https://tr7zw.dev/test/img/nbtapi-logo.png")
				.setImage("https://tr7zw.dev/nbtstats.php?time=" + System.currentTimeMillis())
					.addField(discordApiEmptyString, spigotEmoji.getMentionTag() + " **Spigot**", false)
					.addField("Stars", round(data.rating.average, 2) + "/5:star:", true)
					.addField("Votes", data.rating.count + " :ballot_box:", true)
					.addField("Downloads", data.downloads + " :arrow_down:", true)
					.addField("Likes", data.likes + " :thumbsup:", true)
						.addField(discordApiEmptyString, githubEmoji.getMentionTag() + " **Github**", false)
						.addField("Stars", github.stargazers_count + " :star:", true)
						.addField("Forks", github.forks_count + " " +forkEmoji.getMentionTag(), true)
						.addField("Watchers", github.watchers_count + " :eyes:", true)
						.addField("Open Issues", github.open_issues + " " + issueEmoji.getMentionTag(), true)
						.addField(discordApiEmptyString, ":regional_indicator_b: **Bstats**", false)
				).get();
	}

	private static void updateFirstpersonStatusMessage() throws InterruptedException, ExecutionException, IOException {
		FirstpersonData data = gson.fromJson(readFromUrl("https://firstperson.tr7zw.dev/firstperson/stats"),
				FirstpersonData.class);
		GithubData github = gson.fromJson(readFromUrl("https://api.github.com/repos/tr7zw/FirstPersonModel-Fabric"),
				GithubData.class);
		CurseForgeData curse = gson.fromJson(readFromUrl("https://api.cfwidget.com/minecraft/mc-mods/first-person-model"),
				CurseForgeData.class);
		StringBuilder message = new StringBuilder();
		message.append("Links:\n");
		message.append("[" + githubEmoji.getMentionTag() + " Github](https://github.com/tr7zw/FirstPersonModel-Fabric)\n");
		message.append("[" + curseEmoji.getMentionTag() + " Curseforge](https://www.curseforge.com/minecraft/mc-mods/first-person-model)\n");
		message.append("[" + curseEmoji.getMentionTag() + " Latest Release](" + curse.download.url + ")\n");
		message.append("[" + githubEmoji.getMentionTag() + " Latest Dev-Build](https://github.com/tr7zw/FirstPersonModel-Fabric/actions)\n");
		firstpersonStatus.edit(null
				, new EmbedBuilder().setTitle("FirstPerson Mod Info").setTimestampToNow().setFooter("Last updated:")
				.setDescription(message.toString())
				.setThumbnail("https://media.forgecdn.net/avatars/thumbnails/216/511/64/64/637002697221754383.png")
					.addField(discordApiEmptyString, curseEmoji.getMentionTag() + " **Curseforge**", false)
					.addField("Downloads", curse.downloads.total + ":arrow_down:", true)
					.addField("Latest Version", curse.download.display, true)
					.addField("Minecraft Version", curse.download.version + "+", true)
						.addField(discordApiEmptyString, githubEmoji.getMentionTag() + " **Github**", false)
						.addField("Stars", github.stargazers_count + " :star:", true)
						.addField("Forks", github.forks_count + " " +forkEmoji.getMentionTag(), true)
						.addField("Watchers", github.watchers_count + " :eyes:", true)
						.addField("Open Issues", github.open_issues + " " + issueEmoji.getMentionTag(), true)
						.addField(discordApiEmptyString, backendEmoji.getMentionTag() + " **Backend Stats**", false)
						.addField("Requests", data.requestCounter60s + " in the last 60 seconds", true)
						.addField("UUIDs Processed", data.uuidCounter60s + " in the last 60 seconds", true)
						.addField("Settings-Cache", data.cacheSize + " entries " + (int) (data.cacheHitRatio60s * 100)
								+ "% hitrate in the last 60 seconds", true))
				.get();
	}

	// thanks stackoverflow, I'm lazy :D
	public static double round(double value, int places) {
		if (places < 0)
			throw new IllegalArgumentException();

		BigDecimal bd = BigDecimal.valueOf(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	private static class FirstpersonData {
		public long uuidCounter60s = 0;
		public long requestCounter60s = 0;
		public long cacheSize = 0;
		public double cacheHitRatio60s = 0;
	}

	private static class GithubData {
		public int forks_count = 0;
		public int open_issues = 0;
		public int watchers_count = 0;
		public int stargazers_count = 0;
	}
	
	private static class CurseForgeData {
		public Downloads downloads;
		public Latest download;
		public static class Downloads{
			public int total = 0;
		}
		public static class Latest{
			public String display;
			public String version;
			public String url;
		}
	}

	private static class SpigetData {
		public String name;
		public String tag;
		public Rating rating;
		public int downloads;
		public int likes;

		public static class Rating {
			public int count;
			public double average;
		}
	}

	public static class BStatsData {
		public GraphData servers;
		public GraphData players;

		public static class GraphData {
			public int uid;
			public String title;
			public Data data;

			public static class Data {
				public String lineName;
			}
		}
	}

	private static String readFromUrl(String url) throws IOException {
		URLConnection con = new URL(url).openConnection();
		con.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3833.99 Safari/537.36");
		InputStream is = con.getInputStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			return jsonText;
		} finally {
			is.close();
		}
	}

	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

}

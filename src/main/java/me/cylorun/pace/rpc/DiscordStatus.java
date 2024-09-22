package me.cylorun.pace.rpc;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.cylorun.pace.PaceStatus;
import me.cylorun.pace.PaceStatusOptions;
import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;


public class DiscordStatus {
    private String cliendId;
    private long start;

    public DiscordStatus(String clientId) {
        this.cliendId = clientId;
    }

    public void init() {
        this.start = System.currentTimeMillis();
        DiscordEventHandlers handlers = new DiscordEventHandlers.Builder().setReadyEventHandler((user) -> System.out.println(user.username)).build();
        DiscordRPC.discordInitialize(this.cliendId, handlers, true);
        try {
            this.update();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void update() throws IOException {
        DiscordRichPresence p = this.getNewPresence();
        if (p == null) {
            DiscordRPC.discordClearPresence();
            return;
        }

        DiscordRPC.discordUpdatePresence(p);
    }

    private PaceMan.PaceManStats getStats() throws IOException {
        PaceStatusOptions options = PaceStatusOptions.getInstance();
        return PaceMan.getEnterStats(options.username).orElseThrow(() -> new IOException("Failed to fetch stats"));
    }

    // returns reset stats in a specific format
    private String getStatsString() throws IOException {
        PaceStatusOptions options = PaceStatusOptions.getInstance();
        if (!options.show_enter_avg && !options.show_enter_count) {
            return "";
        }

        PaceMan.PaceManStats stats = PaceMan.getEnterStats(options.username).orElseThrow(() -> new IOException("Failed to fetch stats"));
        String enterString = options.show_enter_count ? String.format("Enters: %s", stats.count) : "";
        String enterAvgString = options.show_enter_avg ? String.format("Enter Avg: %s", stats.avg) : "";
        return String.format("%s %s %s", enterString, enterString.isEmpty() || enterAvgString.isEmpty() ? "" : " | ", enterAvgString);
    }

    private String getCurrentSplit() throws IOException {
        PaceStatusOptions options = PaceStatusOptions.getInstance();
        JsonObject run = PaceMan.getRun(options.username.toLowerCase()).orElseThrow(() -> new IOException("Failed to fetch run data"));

        JsonArray eventList = run.getAsJsonArray("eventList");
        JsonObject latestEvent = eventList.get(eventList.size() - 1).getAsJsonObject();

        return latestEvent.get("eventId").getAsString();
    }

    private String getCurrentTime() throws IOException {
        PaceStatusOptions options = PaceStatusOptions.getInstance();
        JsonObject run = PaceMan.getRun(options.username.toLowerCase()).orElseThrow(() -> new IOException("Failed to fetch run data"));

        JsonArray eventList = run.getAsJsonArray("eventList");
        JsonObject latestEvent = eventList.get(eventList.size() - 1).getAsJsonObject();

        return PaceMan.formatTime(latestEvent.get("igt").getAsInt());
    }

    private Pair<String, String> getDiscordText(String currentSplit) throws IOException {
        String statsString = this.getStatsString();
        System.out.println(statsString);
        if (currentSplit == null) {
            return Pair.of("Idle", statsString);
        }
        return Pair.of(PaceMan.getRunDesc(currentSplit), statsString);
    }

    private DiscordRichPresence getNewPresence() throws IOException {
        PaceStatusOptions options = PaceStatusOptions.getInstance();
        JsonObject run = PaceMan.getRun(options.username.toLowerCase()).orElseThrow(() -> new IOException("Failed to fetch run data"));
        if (run != null) {
            String currentSplit = this.getCurrentSplit();
            String currentTime = this.getCurrentTime();
            Pair<String, String> text = this.getDiscordText(currentSplit);

            return new DiscordRichPresence.Builder("Current Time: " + currentTime)
                    .setStartTimestamps(this.start)
                    .setDetails(text.getRight())
                    .setBigImage(PaceMan.getIcon(currentSplit), PaceMan.getRunDesc(currentSplit))
                    .setSmallImage("app_icon", "paceman.gg")
                    .build();
        }


        PaceMan.PaceManStats stats = this.getStats();
        if (stats == null) {
            return null;
        }

        String enters = !options.show_enter_avg ? "" : String.format("Enters: %s", stats.count);
        String avg = stats.avg == null || !options.show_enter_avg ? "" : String.format("Enter Avg: %s", stats.avg);

        if (PaceStatus.isAfk()) {
            return new DiscordRichPresence.Builder("Currently AFK")
                    .setStartTimestamps(this.start)
                    .setDetails(getStatsString())
                    .setBigImage("idle", "Not on pace")
                    .setSmallImage("app_icon", "paceman.gg")
                    .build();
        }

        return new DiscordRichPresence.Builder(avg)
                .setStartTimestamps(this.start)
                .setDetails(enters)
                .setBigImage("idle", "Not on pace")
                .setSmallImage("app_icon", "paceman.gg")
                .build();
    }
}


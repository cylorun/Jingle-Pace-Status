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
            this.updatePresence();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void updatePresence() throws IOException {
        DiscordRichPresence p = this.getNewPresence();
        if (p == null) {
            DiscordRPC.discordClearPresence();
            return;
        }
        DiscordRPC.discordUpdatePresence(p);
    }

    public String getStatsString() throws IOException {
        PaceStatusOptions options = PaceStatusOptions.getInstance();
        if (!options.show_enter_avg && !options.show_enter_count) {
            return "";
        }
        Pair<Integer, String> stats = PaceMan.getEnterStats(options.username);
        if (stats == null) {
            return "";
        }

        String enterString = options.show_enter_count ? String.format("Enters: %s", stats.getLeft()) : "";
        String enterAvgString = options.show_enter_avg ? String.format("Enter Avg: %s", stats.getRight()) : "";
        return String.format("%s %s %s", enterString, enterString.isEmpty() || enterAvgString.isEmpty() ? "" : " | ", enterAvgString);
    }

    public Pair<Integer, String> getStats() throws IOException {
        return PaceMan.getEnterStats(PaceStatusOptions.getInstance().username);
    }

    private Pair<String, String> getDiscordText(String currentSplit) throws IOException {
        String stats = getStatsString();
        if (currentSplit == null) {
            return Pair.of("Idle", stats);
        }
        return Pair.of(PaceMan.getRunDesc(currentSplit), stats);
    }

    private DiscordRichPresence getNewPresence() throws IOException {
        PaceStatusOptions options = PaceStatusOptions.getInstance();
        JsonObject run = PaceMan.getRun(options.username.toLowerCase());

        if (run != null) {
            JsonArray eventList = run.getAsJsonArray("eventList");
            JsonObject latestEvent = eventList.get(eventList.size() - 1).getAsJsonObject();
            String currentSplit = latestEvent.get("eventId").getAsString();
            String currentTime = PaceMan.formatTime(Integer.parseInt(latestEvent.get("igt").getAsString()));
            Pair<String, String> text = this.getDiscordText(currentSplit);

            return new DiscordRichPresence.Builder("Current Time: " + currentTime)
                    .setStartTimestamps(this.start)
                    .setDetails(text.getRight())
                    .setBigImage(PaceMan.getIcon(currentSplit), PaceMan.getRunDesc(currentSplit))
                    .setSmallImage("app_icon", "paceman.gg")
                    .build();
        }


        Pair<Integer, String> stats = this.getStats();
        if (stats == null) {
            return null;
        }

        String enters = stats.getLeft() == null || !options.show_enter_avg ? "" : String.format("Enters: %s", stats.getLeft());
        String avg = stats.getRight() == null || !options.show_enter_avg ? "" : String.format("Enter Avg: %s", stats.getRight());

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


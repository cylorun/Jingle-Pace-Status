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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DiscordStatus {
    private final String clientId;
    private long startTime;

    public DiscordStatus(String clientId) {
        this.clientId = clientId;
    }

    public void init() {
        this.startTime = System.currentTimeMillis();
        setupDiscordHandlers();
        try {
            update();
        } catch (Exception e) {
            throw new RuntimeException("Error initializing Discord status", e);
        }
    }

    private void setupDiscordHandlers() {
        DiscordEventHandlers handlers = new DiscordEventHandlers.Builder()
                .setReadyEventHandler(user -> System.out.println("Connected as " + user.username))
                .build();
        DiscordRPC.discordInitialize(clientId, handlers, true);
    }

    public void update() throws Exception {
        DiscordRichPresence presence = createNewPresence();
        if (presence == null) {
            DiscordRPC.discordClearPresence();
        } else {
            DiscordRPC.discordUpdatePresence(presence);
        }
    }

    private DiscordRichPresence createNewPresence() throws IOException {
        Optional<JsonObject> runData = fetchRunData();
        if (runData.isPresent()) {
            return buildRunPresence(runData.get());
        }
        return buildAfkOrStatsPresence();
    }

    private Optional<JsonObject> fetchRunData() {
        try {
            String username = PaceStatusOptions.getInstance().username.toLowerCase();
            return PaceManUtil.getRun(username);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private DiscordRichPresence buildRunPresence(JsonObject run) throws IOException {
        String currentSplit = getCurrentSplit(run);
        String currentTime = getCurrentTime(run);
        Pair<String, String> discordText = buildDiscordText(currentSplit);

        return new DiscordRichPresence.Builder("Current Time: " + currentTime)
                .setStartTimestamps(startTime)
                .setDetails(discordText.getRight())
                .setBigImage(PaceManUtil.getIcon(currentSplit), discordText.getLeft())
                .setSmallImage("app_icon", "paceman.gg")
                .build();
    }

    private DiscordRichPresence buildAfkOrStatsPresence() throws IOException {
        if (PaceStatus.isAfk()) {
            return new DiscordRichPresence.Builder("Currently AFK")
                    .setStartTimestamps(startTime)
                    .setDetails(buildStatsString())
                    .setBigImage("idle", "Not on pace")
                    .setSmallImage("app_icon", "paceman.gg")
                    .build();
        }

        PaceManUtil.PaceManStats stats = fetchStats();
        String details = String.format("Enters: %s | Enter Avg: %s", stats.count, stats.avg);

        return new DiscordRichPresence.Builder(details)
                .setStartTimestamps(startTime)
                .setBigImage("idle", "Not on pace")
                .setSmallImage("app_icon", "paceman.gg")
                .build();
    }

    private String getCurrentSplit(JsonObject run) {
        JsonArray eventList = run.getAsJsonArray("eventList");
        JsonObject latestEvent = eventList.get(eventList.size() - 1).getAsJsonObject();
        return latestEvent.get("eventId").getAsString();
    }

    private String getCurrentTime(JsonObject run) {
        JsonArray eventList = run.getAsJsonArray("eventList");
        JsonObject latestEvent = eventList.get(eventList.size() - 1).getAsJsonObject();
        return PaceManUtil.formatTime(latestEvent.get("igt").getAsInt());
    }

    private Pair<String, String> buildDiscordText(String currentSplit) throws IOException {
        String statsString = buildStatsString();
        if (currentSplit == null) {
            return Pair.of("Idle", statsString);
        }
        String runDesc = PaceManUtil.getRunDesc(currentSplit);
        return Pair.of(runDesc, statsString);
    }

    private PaceManUtil.PaceManStats fetchStats() throws IOException {
        String username = PaceStatusOptions.getInstance().username;
        return PaceManUtil.getEnterStats(username)
                .orElseThrow(() -> new IOException("Failed to fetch stats"));
    }

    private String buildStatsString() throws IOException {
        PaceStatusOptions options = PaceStatusOptions.getInstance();
        if (!options.show_enter_avg && !options.show_enter_count) return "";

        PaceManUtil.PaceManStats stats = fetchStats();
        String enterCount = options.show_enter_count ? "Enters: " + stats.count : "";
        String enterAvg = options.show_enter_avg ? "Enter Avg: " + stats.avg : "";

        return Stream.of(enterCount, enterAvg)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" | "));
    }
}

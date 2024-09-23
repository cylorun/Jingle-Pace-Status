package me.cylorun.pace;

import com.google.common.io.Resources;
import me.cylorun.pace.gui.PaceStatusGUI;
import me.cylorun.pace.rpc.DiscordStatus;
import net.arikia.dev.drpc.DiscordRPC;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.JingleAppLaunch;
import xyz.duncanruns.jingle.gui.JingleGUI;
import xyz.duncanruns.jingle.plugin.PluginEvents;
import xyz.duncanruns.jingle.plugin.PluginManager;
import xyz.duncanruns.jingle.util.ExceptionUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PaceStatus {
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final String CLIENT_ID = "1188623641513050224";
    private static long lastResetTime = System.currentTimeMillis();

    public static void main(String[] args) throws IOException {
        JingleAppLaunch.launchWithDevPlugin(args, PluginManager.JinglePluginData.fromString(
                Resources.toString(Resources.getResource(PaceStatus.class, "/jingle.plugin.json"), Charset.defaultCharset())
        ), PaceStatus::initialize);
    }

    public static void initialize() {
        Jingle.log(Level.INFO, "Pace-Status plugin initialized");
        JingleGUI.addPluginTab("Pace Status", new PaceStatusGUI());

        PaceStatusOptions options = PaceStatusOptions.getInstance();
        DiscordStatus ds = new DiscordStatus(PaceStatus.CLIENT_ID);

        AtomicInteger errorCounter = new AtomicInteger();
        AtomicBoolean hasInitialized = new AtomicBoolean(false);

        EXECUTOR.scheduleWithFixedDelay(() -> {
            if (!hasInitialized.get()) {
                ds.init();
                hasInitialized.set(true);
            }
            try {
                if (options.enabled) ds.update();
                else DiscordRPC.discordClearPresence();

                errorCounter.set(0);
            } catch (Throwable t) {
                t.printStackTrace();
                Jingle.log(Level.ERROR, ExceptionUtil.toDetailedString(t));
                if (errorCounter.incrementAndGet() > 5) {
                    DiscordRPC.discordClearPresence();
                    Jingle.log(Level.ERROR, "Pace Status Error: " + ExceptionUtil.toDetailedString(t));
                }
            }
        }, 1, 10, TimeUnit.SECONDS);

        PluginEvents.EXIT_WORLD.register(() -> PaceStatus.lastResetTime = System.currentTimeMillis());
    }

    public static boolean isAfk() {
        return (System.currentTimeMillis() - PaceStatus.lastResetTime) > (1000 * 300); // 5 minutes
    }
}

package me.cylorun.pace;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import xyz.duncanruns.jingle.Jingle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PaceStatusOptions {
    public String username = "";
    public boolean enabled = true;
    public boolean show_enter_count = true;
    public boolean show_enter_avg = true;
    public int time_period = 24;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SAVE_PATH = Jingle.FOLDER.resolve("pace-status-options.json");
    private static PaceStatusOptions instance;
    public static void save() throws IOException {
        FileWriter writer = new FileWriter(SAVE_PATH.toFile());
        GSON.toJson(instance, writer);
        writer.close();
    }

    public static PaceStatusOptions getInstance() {
        System.out.println("Grabbing optionss");
        if (instance == null) {
            if (Files.exists(SAVE_PATH)) {
                try {
                    instance = GSON.fromJson(new String(Files.readAllBytes(SAVE_PATH)), PaceStatusOptions.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                instance = new PaceStatusOptions();
            }
        }

        return instance;
    }

}


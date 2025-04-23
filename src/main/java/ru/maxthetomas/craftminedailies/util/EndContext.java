package ru.maxthetomas.craftminedailies.util;

import com.google.gson.JsonObject;
import ru.maxthetomas.craftminedailies.CraftmineDailies;

public abstract class EndContext {
    private final int experience;
    private final int passedTime;

    protected EndContext(int experience, int passedTime) {
        this.experience = experience;
        this.passedTime = passedTime;
    }

    public abstract boolean isWin();

    public abstract String getStringName();

    public int getExperience() {
        return experience;
    }

    public int getPassedTime() {
        return passedTime;
    }

    public JsonObject getAsJson() {
        var json = new JsonObject();
        json.addProperty("type", getStringName());
        json.addProperty("is_win", isWin());
        json.addProperty("experience", experience);
        // For backward compat
        json.addProperty("remaining_time", CraftmineDailies.DEFAULT_MAX_GAME_TIME - passedTime);
        json.addProperty("passed_time", passedTime);
        return json;
    }
}

package ru.maxthetomas.craftminedailies.util;

import com.google.gson.JsonObject;

public abstract class EndContext {
    private final int experience;
    private final int remainingTime;

    protected EndContext(int experience, int remainingTime) {
        this.experience = experience;
        this.remainingTime = remainingTime;
    }

    public abstract boolean isWin();

    public abstract String getStringName();

    public JsonObject getAsJson() {
        var json = new JsonObject();
        json.addProperty("type", getStringName());
        json.addProperty("is_win", isWin());
        json.addProperty("experience", experience);
        json.addProperty("remaining_time", remainingTime);
        return json;
    }
}

package ru.maxthetomas.craftminedailies.util.ends;

import ru.maxthetomas.craftminedailies.util.EndContext;

public class WinEndContext extends EndContext {
    public WinEndContext(int experience, int remainingTime) {
        super(experience, remainingTime);
    }

    @Override
    public boolean isWin() {
        return true;
    }

    @Override
    public String getStringName() {
        return "win";
    }
}

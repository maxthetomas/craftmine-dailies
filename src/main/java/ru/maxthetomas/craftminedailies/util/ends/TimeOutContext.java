package ru.maxthetomas.craftminedailies.util.ends;

import ru.maxthetomas.craftminedailies.util.EndContext;

public class TimeOutContext extends EndContext {
    public TimeOutContext(int experience) {
        super(experience, 0);
    }

    @Override
    public boolean isWin() {
        return false;
    }

    @Override
    public String getStringName() {
        return "time_out";
    }
}

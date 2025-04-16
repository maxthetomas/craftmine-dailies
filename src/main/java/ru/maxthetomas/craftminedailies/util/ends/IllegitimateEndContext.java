package ru.maxthetomas.craftminedailies.util.ends;

import ru.maxthetomas.craftminedailies.util.EndContext;

// Any illegitimate reason
public class IllegitimateEndContext extends EndContext {
    public IllegitimateEndContext() {
        super(0, 0);
    }

    public IllegitimateEndContext(int remainingTime) {
        super(0, remainingTime);
    }

    @Override
    public boolean isWin() {
        return false;
    }

    @Override
    public String getStringName() {
        return "illegitimate";
    }
}

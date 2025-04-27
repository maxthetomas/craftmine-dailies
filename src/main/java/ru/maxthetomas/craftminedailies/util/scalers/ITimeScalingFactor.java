package ru.maxthetomas.craftminedailies.util.scalers;

public interface ITimeScalingFactor {
    float getScale();

    default float getShakiness() {
        return 0;
    }
}

package ru.maxthetomas.craftminedailies.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class NetworkingManager {
    public static void updateCurrentRunDetails() {
    }

    public static void sendCompletedRun() {
    }


    public record DailyRunInfo(String date, long seed) {
        public static MapCodec<DailyRunInfo> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("date").forGetter(DailyRunInfo::date),
                Codec.LONG.fieldOf("seed").forGetter(DailyRunInfo::seed)
        ).apply(instance, DailyRunInfo::new));
    }
}

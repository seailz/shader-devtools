package com.seailz.csdt.client.service;

import net.minecraft.client.renderer.fog.FogData;

import java.util.ArrayDeque;
import java.util.List;

/** Debug-only CPU mirror of the Fog UBO writes used to verify partial ticks. */
public final class FogFrameInspectionService {

    private static final int MAX_SAMPLES = 512;
    private static final ArrayDeque<Sample> SAMPLES = new ArrayDeque<>();
    private static long sequence;

    private FogFrameInspectionService() {
    }

    public static synchronized void record(FogData fogData) {
        SAMPLES.addLast(new Sample(sequence++, System.nanoTime(),
                fogData.environmentalStart, fogData.environmentalEnd,
                fogData.renderDistanceStart, fogData.renderDistanceEnd));
        while (SAMPLES.size() > MAX_SAMPLES) {
            SAMPLES.removeFirst();
        }
    }

    public static synchronized List<Sample> snapshot() {
        return List.copyOf(SAMPLES);
    }

    public record Sample(long sequence, long nanoTime,
                         float environmentalStart, float environmentalEnd,
                         float renderDistanceStart, float renderDistanceEnd) {
    }
}

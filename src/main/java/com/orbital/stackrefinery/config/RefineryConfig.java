package com.orbital.stackrefinery.config;

public class RefineryConfig {

    private static int consolidationRadius = 25;

    public static int getRadius() {
        return consolidationRadius;
    }

    public static void setRadius(int radius) {
        consolidationRadius = Math.max(1, Math.min(64, radius));
    }
}
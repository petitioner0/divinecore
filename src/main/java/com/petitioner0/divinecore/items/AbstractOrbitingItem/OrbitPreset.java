package com.petitioner0.divinecore.items.AbstractOrbitingItem;

import java.util.concurrent.ThreadLocalRandom;

public enum OrbitPreset {
    //some presets
    ETHEREAL(1.7f, 2.5f, 1.8f, 2.0f, 0.018f, 0.030f),
    STABLE(1.1f, 1.5f, 1.1f, 1.3f, 0.028f, 0.038f),
    FIERY(1.2f, 1.6f, 1.0f, 1.2f, 0.045f, 0.065f),
    HEAVY(0.9f, 1.3f, 1.2f, 1.4f, 0.022f, 0.032f),
    ENERGETIC(1.0f, 1.4f, 1.7f, 1.9f, 0.055f, 0.075f),
    VOID(1.6f, 2.2f, 1.3f, 1.6f, 0.018f, 0.026f);

    private final float minRadius, maxRadius;
    private final float minHeight, maxHeight;
    private final float minSpeed, maxSpeed;

    OrbitPreset(float minR, float maxR, float minH, float maxH, float minS, float maxS) {
        this.minRadius = minR;
        this.maxRadius = maxR;
        this.minHeight = minH;
        this.maxHeight = maxH;
        this.minSpeed = minS;
        this.maxSpeed = maxS;
    }

    public float getRadius() {
        return ThreadLocalRandom.current().nextFloat(minRadius, maxRadius);
    }

    public float getHeight() {
        return ThreadLocalRandom.current().nextFloat(minHeight, maxHeight);
    }

    public float getSpeed() {
        return ThreadLocalRandom.current().nextFloat(minSpeed, maxSpeed);
    }
}
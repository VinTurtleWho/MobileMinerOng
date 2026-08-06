package com.mobileminerong.util;

import java.util.Random;

public class OrnsteinUhlenbeckDrift {
    private double x = 0.0;
    private double y = 0.0;
    private final double theta = 0.1; // Focus strength
    private final double sigma = 0.05; // Volatility
    private final Random random = new Random();

    public void update() {
        // dx_t = -theta * (x_t - mu) * dt + sigma * dWt
        // mu is 0 for drift velocity around center
        x += -theta * x * 0.05 + sigma * random.nextGaussian() * Math.sqrt(0.05);
        y += -theta * y * 0.05 + sigma * random.nextGaussian() * Math.sqrt(0.05);
    }

    public double getX() { return x; }
    public double getY() { return y; }
}

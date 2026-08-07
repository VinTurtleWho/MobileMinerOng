package com.mobileminerong.util;

import java.util.Random;

/**
 * Ex-Gaussian distribution generator for human-like cognitive latency.
 * X = N(mu, sigma^2) + E(1/tau)
 */
public class ExGaussianGenerator {
    private static final Random random = new Random();

    public static long nextDelay(double mu, double sigma, double tau) {
        double gaussian = mu + sigma * random.nextGaussian();
        double exponential = -tau * Math.log(1.0 - random.nextDouble());
        return (long) Math.max(1.0, gaussian + exponential);
    }
}

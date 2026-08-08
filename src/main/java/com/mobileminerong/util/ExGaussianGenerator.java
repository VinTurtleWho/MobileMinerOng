package com.mobileminerong.util;

import java.security.SecureRandom;

/**
 * Ex-Gaussian distribution generator for human-like cognitive latency.
 * X = N(mu, sigma^2) + E(1/tau)
 */
public class ExGaussianGenerator {
    private static final SecureRandom random = new SecureRandom();

    public static long nextDelay(double mu, double sigma, double tau) {
        double gaussian = mu + sigma * random.nextGaussian();
        double exponential = -tau * Math.log(1.0 - random.nextDouble());
        return (long) Math.max(1.0, gaussian + exponential);
    }
}

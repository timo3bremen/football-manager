package com.example.manager.model;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for Player market value / age multiplier behaviour.
 */
public class PlayerTest {

    // helper that mirrors the calculation in Player.calculateMarketValue()
    private long computeExpectedMarketValue(Player p, long nowMs) {
        int rating = p.getRating();
        long baseValue;
        if (rating > 80) {
            baseValue = rating * 300_000L;
        } else if (rating > 85) {
            baseValue = rating * 500_000L;
        } else if (rating > 90) {
            baseValue = rating * 1_000_000L;
        } else {
            baseValue = rating * 100_000L;
        }

        // age multiplier: piecewise mapping
        double ageMultiplier;
        int age = p.getAge();
        if (age <= 18) {
            ageMultiplier = 4.0;
        } else if (age <= 24) {
            ageMultiplier = 4.0 - ((age - 18) * (1.0 / 6.0));
        } else if (age <= 28) {
            ageMultiplier = 3.0 - ((age - 24) * (1.0 / 4.0));
        } else if (age <= 32) {
            ageMultiplier = 2.0 - ((age - 28) * (1.0 / 4.0));
        } else {
            ageMultiplier = 1.0;
        }

        long remainingMs = p.getContractEndDate() - nowMs;
        double remainingYears = Math.max(0, remainingMs / (365.25 * 24 * 60 * 60 * 1000));
        double contractMultiplier = Math.min(1.5, 0.5 + (remainingYears / 10.0));

        return Math.round(baseValue * ageMultiplier * contractMultiplier);
    }

    @Test
    public void testAgeAnchorsProduceExpectedMultipliers() {
        long now = System.currentTimeMillis();
        // set contract 10 years in future
        long contractEnd = now + (long) (10.0 * 365.25 * 24 * 60 * 60 * 1000);

        Player p18 = new Player("P18", 60, 60, 0);
        p18.setAge(18);
        p18.setContractEndDate(contractEnd);

        Player p24 = new Player("P24", 60, 60, 0);
        p24.setAge(24);
        p24.setContractEndDate(contractEnd);

        Player p28 = new Player("P28", 60, 60, 0);
        p28.setAge(28);
        p28.setContractEndDate(contractEnd);

        Player p32 = new Player("P32", 60, 60, 0);
        p32.setAge(32);
        p32.setContractEndDate(contractEnd);

        // compute actual values by invoking the production method
        p18.calculateMarketValue();
        p24.calculateMarketValue();
        p28.calculateMarketValue();
        p32.calculateMarketValue();

        long actual18 = p18.getMarketValue();
        long actual24 = p24.getMarketValue();
        long actual28 = p28.getMarketValue();
        long actual32 = p32.getMarketValue();

        // compute expected using the same now timestamp
        long expected18 = computeExpectedMarketValue(p18, now);
        long expected24 = computeExpectedMarketValue(p24, now);
        long expected28 = computeExpectedMarketValue(p28, now);
        long expected32 = computeExpectedMarketValue(p32, now);

        // Allow small differences due to timing; assert roughly equal
        assertTrue(Math.abs(expected18 - actual18) <= 2, "age18 expected~actual");
        assertTrue(Math.abs(expected24 - actual24) <= 2, "age24 expected~actual");
        assertTrue(Math.abs(expected28 - actual28) <= 2, "age28 expected~actual");
        assertTrue(Math.abs(expected32 - actual32) <= 2, "age32 expected~actual");

        // additionally assert the multipliers ordering (18>24>28>32)
        assertTrue(actual18 > actual24, "marketValue(18) should be greater than marketValue(24)");
        assertTrue(actual24 > actual28, "marketValue(24) should be greater than marketValue(28)");
        assertTrue(actual28 > actual32, "marketValue(28) should be greater than marketValue(32)");
    }
}

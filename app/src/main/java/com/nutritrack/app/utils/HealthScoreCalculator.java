package com.nutritrack.app.utils;

import com.nutritrack.app.models.DailyTotals;

/**
 * Implements the rule-based Health Score from SRS §4.3 (REQ-7, REQ-8).
 *
 *   Health Score = Protein Balance   × 0.30
 *                + Calorie Discipline × 0.30
 *                + Fat Moderation     × 0.20
 *                + Consistency Score  × 0.20
 *
 * No AI/ML used (per SRS §2.5 constraint). All factors normalised to 0-100.
 */
public class HealthScoreCalculator {

    public static class Result {
        public int total;          // 0..100
        public int proteinBalance; // 0..100
        public int calorieDiscipline;
        public int fatModeration;
        public int consistency;
    }

    /**
     * @param totals             today's intake
     * @param targetKcal         user's daily calorie target
     * @param logStreakDays      consecutive logged days (drives consistency)
     */
    public static Result compute(DailyTotals totals, int targetKcal, int logStreakDays) {
        Result r = new Result();
        r.proteinBalance    = scoreProtein(totals, targetKcal);
        r.calorieDiscipline = scoreCalories(totals.calories, targetKcal);
        r.fatModeration     = scoreFat(totals, targetKcal);
        r.consistency       = scoreConsistency(logStreakDays);

        double weighted =
                  r.proteinBalance    * 0.30
                + r.calorieDiscipline * 0.30
                + r.fatModeration     * 0.20
                + r.consistency       * 0.20;
        r.total = clamp((int) Math.round(weighted));
        return r;
    }

    /* ---------- Component scores ---------- */

    /** Protein target ≈ 0.8 g per kcal/10 (rough heuristic ~ 50–80 g for 2000 kcal). */
    private static int scoreProtein(DailyTotals t, int targetKcal) {
        double targetProtein = targetKcal * 0.075;        // ~150g for 2000kcal at upper end
        if (targetProtein <= 0) return 0;
        double ratio = t.proteinG / targetProtein;
        // peak score at ratio = 1.0; falls off either side
        if (ratio >= 1.0) return clamp((int) (100 - (ratio - 1.0) * 30));
        return clamp((int) (ratio * 100));
    }

    /** Best score when calories are within ±10% of target. */
    private static int scoreCalories(double caloriesEaten, int targetKcal) {
        if (targetKcal <= 0) return 0;
        double diffPct = Math.abs(caloriesEaten - targetKcal) / (double) targetKcal;
        if (diffPct <= 0.10) return 100;
        if (diffPct >= 0.50) return 0;
        return clamp((int) (100 - ((diffPct - 0.10) / 0.40) * 100));
    }

    /** Fat should be ≤ 30% of total energy (1g fat = 9 kcal). */
    private static int scoreFat(DailyTotals t, int targetKcal) {
        if (targetKcal <= 0) return 0;
        double fatKcal = t.fatG * 9.0;
        double pct = fatKcal / (double) targetKcal;
        if (pct <= 0.30) return 100;
        if (pct >= 0.55) return 0;
        return clamp((int) (100 - ((pct - 0.30) / 0.25) * 100));
    }

    /** 7-day streak = full marks; linear up to that. */
    private static int scoreConsistency(int streakDays) {
        if (streakDays >= 7) return 100;
        if (streakDays <= 0) return 0;
        return clamp((streakDays * 100) / 7);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(100, v)); }

    public static String describe(int score) {
        if (score >= 85) return "Excellent";
        if (score >= 70) return "Good";
        if (score >= 50) return "Fair";
        return "Needs work";
    }
}

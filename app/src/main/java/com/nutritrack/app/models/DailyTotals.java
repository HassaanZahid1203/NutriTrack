package com.nutritrack.app.models;

import java.util.List;

/**
 * Aggregated daily nutrition totals computed from a list of MealEntry.
 */
public class DailyTotals {
    public double calories;
    public double proteinG;
    public double carbsG;
    public double fatG;

    public double breakfastKcal;
    public double lunchKcal;
    public double dinnerKcal;
    public double snackKcal;

    public boolean breakfastLogged;
    public boolean lunchLogged;
    public boolean dinnerLogged;
    public boolean snackLogged;

    public static DailyTotals from(List<MealEntry> entries) {
        DailyTotals t = new DailyTotals();
        if (entries == null) return t;
        for (MealEntry e : entries) {
            t.calories += e.getCalories();
            t.proteinG += e.getProteinG();
            t.carbsG   += e.getCarbsG();
            t.fatG     += e.getFatG();
            switch (e.getMealType()) {
                case MealEntry.TYPE_BREAKFAST:
                    t.breakfastKcal += e.getCalories();
                    t.breakfastLogged = true;
                    break;
                case MealEntry.TYPE_LUNCH:
                    t.lunchKcal += e.getCalories();
                    t.lunchLogged = true;
                    break;
                case MealEntry.TYPE_DINNER:
                    t.dinnerKcal += e.getCalories();
                    t.dinnerLogged = true;
                    break;
                case MealEntry.TYPE_SNACK:
                    t.snackKcal += e.getCalories();
                    t.snackLogged = true;
                    break;
            }
        }
        return t;
    }
}

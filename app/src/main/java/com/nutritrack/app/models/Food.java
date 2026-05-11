package com.nutritrack.app.models;

/**
 * Single food item. Persisted in Firestore at the top-level `foods` collection
 * so the admin user class (SRS §2.3.3) can maintain a shared catalogue. The id
 * is the Firestore document id.
 *
 * Values are per unit indicated by the `unit` field — "100g", "100ml",
 * "piece", "cup", or "serving".
 */
public class Food {
    private String id;
    private String name;
    private String emoji;
    private double caloriesPerUnit;
    private double proteinG;
    private double carbsG;
    private double fatG;
    private String unit;

    /** Required no-arg ctor for Firestore deserialization. */
    public Food() {}

    public Food(String id, String name, String emoji, double caloriesPerUnit,
                double proteinG, double carbsG, double fatG, String unit) {
        this.id = id;
        this.name = name;
        this.emoji = emoji;
        this.caloriesPerUnit = caloriesPerUnit;
        this.proteinG = proteinG;
        this.carbsG = carbsG;
        this.fatG = fatG;
        this.unit = unit;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
    public double getCaloriesPerUnit() { return caloriesPerUnit; }
    public void setCaloriesPerUnit(double caloriesPerUnit) { this.caloriesPerUnit = caloriesPerUnit; }
    public double getProteinG() { return proteinG; }
    public void setProteinG(double proteinG) { this.proteinG = proteinG; }
    public double getCarbsG() { return carbsG; }
    public void setCarbsG(double carbsG) { this.carbsG = carbsG; }
    public double getFatG() { return fatG; }
    public void setFatG(double fatG) { this.fatG = fatG; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getMacroLine() {
        return String.format("P: %.1fg · C: %.1fg · F: %.1fg per %s",
                proteinG, carbsG, fatG, unit);
    }
}

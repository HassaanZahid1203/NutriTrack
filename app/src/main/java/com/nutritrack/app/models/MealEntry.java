package com.nutritrack.app.models;

/**
 * One logged food entry in a meal slot. Stored in Firestore under
 * users/{uid}/meals/{mealId} for cloud sync (SRS REQ-6).
 */
public class MealEntry {
    public static final String TYPE_BREAKFAST = "breakfast";
    public static final String TYPE_LUNCH     = "lunch";
    public static final String TYPE_DINNER    = "dinner";
    public static final String TYPE_SNACK     = "snack";

    private String id;
    private String uid;
    private String mealType;
    private String foodId;
    private String foodName;
    private String foodEmoji;
    private double quantity;          // # of units consumed
    private double calories;          // computed at log time
    private double proteinG;
    private double carbsG;
    private double fatG;
    private long timestamp;

    public MealEntry() {}

    public MealEntry(String mealType, Food food, double quantity) {
        this.mealType = mealType;
        this.foodId = food.getId();
        this.foodName = food.getName();
        this.foodEmoji = food.getEmoji();
        this.quantity = quantity;
        this.calories = food.getCaloriesPerUnit() * quantity;
        this.proteinG = food.getProteinG() * quantity;
        this.carbsG = food.getCarbsG() * quantity;
        this.fatG = food.getFatG() * quantity;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public String getFoodId() { return foodId; }
    public void setFoodId(String foodId) { this.foodId = foodId; }
    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }
    public String getFoodEmoji() { return foodEmoji; }
    public void setFoodEmoji(String foodEmoji) { this.foodEmoji = foodEmoji; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public double getCalories() { return calories; }
    public void setCalories(double calories) { this.calories = calories; }
    public double getProteinG() { return proteinG; }
    public void setProteinG(double proteinG) { this.proteinG = proteinG; }
    public double getCarbsG() { return carbsG; }
    public void setCarbsG(double carbsG) { this.carbsG = carbsG; }
    public double getFatG() { return fatG; }
    public void setFatG(double fatG) { this.fatG = fatG; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getDisplayName() {
        if (quantity > 1 && quantity == Math.floor(quantity)) {
            return foodName + " × " + (int) quantity;
        }
        return foodName;
    }
}

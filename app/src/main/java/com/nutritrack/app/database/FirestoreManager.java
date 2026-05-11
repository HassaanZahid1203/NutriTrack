package com.nutritrack.app.database;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.nutritrack.app.models.Food;
import com.nutritrack.app.models.MealEntry;
import com.nutritrack.app.models.User;
import com.nutritrack.app.utils.DateUtils;

/**
 * Wrapper around Firestore for user profiles, meal-entry cloud sync, and the
 * shared food catalogue maintained by admins (SRS §2.3.3, §3.3, §3.4).
 */
public class FirestoreManager {

    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_MEALS = "meals";
    private static final String COLLECTION_FOODS = "foods";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public FirestoreManager() {
        this.db   = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    /** Currently signed-in user's UID, or null. */
    public String getUid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    /* ---------- User profile ---------- */

    public Task<Void> saveUser(@NonNull User user) {
        return db.collection(COLLECTION_USERS)
                .document(user.getUid())
                .set(user);
    }

    public DocumentReference userDoc() {
        return db.collection(COLLECTION_USERS).document(getUid());
    }

    /** Fetch the current user's profile document. */
    public com.google.android.gms.tasks.Task<com.google.firebase.firestore.DocumentSnapshot> getUser() {
        return userDoc().get();
    }

    /* ---------- Meal entries ---------- */

    public CollectionReference mealsRef() {
        return db.collection(COLLECTION_USERS)
                .document(getUid())
                .collection(COLLECTION_MEALS);
    }

    public Task<Void> addMealEntry(@NonNull MealEntry entry) {
        entry.setUid(getUid());
        DocumentReference doc = mealsRef().document();
        entry.setId(doc.getId());
        return doc.set(entry);
    }

    /** Today's meals (00:00 to 24:00 of current day). */
    public Task<QuerySnapshot> getTodaysMeals() {
        return mealsRef()
                .whereGreaterThanOrEqualTo("timestamp", DateUtils.startOfDay())
                .whereLessThan          ("timestamp", DateUtils.endOfDay())
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get();
    }

    /** Last 7 days for the weekly trend chart (SRS dashboard §3.1.2). */
    public Task<QuerySnapshot> getLastSevenDaysMeals() {
        long sevenDaysAgo = DateUtils.startOfDay() - 6L * 24 * 60 * 60 * 1000;
        return mealsRef()
                .whereGreaterThanOrEqualTo("timestamp", sevenDaysAgo)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get();
    }

    /* ---------- Foods (shared catalogue, admin-maintained) ---------- */

    public CollectionReference foodsRef() {
        return db.collection(COLLECTION_FOODS);
    }

    public Task<QuerySnapshot> getAllFoods() {
        return foodsRef().orderBy("name", Query.Direction.ASCENDING).get();
    }

    /**
     * Persist a food. If the Food has no id, a new document is created and its
     * id is assigned to the Food before the write.
     */
    public Task<Void> saveFood(@NonNull Food food) {
        DocumentReference doc = (food.getId() == null || food.getId().isEmpty())
                ? foodsRef().document()
                : foodsRef().document(food.getId());
        food.setId(doc.getId());
        return doc.set(food);
    }

    public Task<Void> deleteFood(@NonNull String foodId) {
        return foodsRef().document(foodId).delete();
    }
}

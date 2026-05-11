package com.nutritrack.app.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.nutritrack.app.models.Food;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Authoritative source of food data, backed by the top-level `foods`
 * collection in Firestore. Maintained by admin users (SRS §2.3.3).
 *
 * Holds an in-memory cache of all foods so {@link com.nutritrack.app.ui.log.LogFoodFragment}
 * can run client-side substring search without a query per keystroke.
 *
 * On first launch (when the collection is empty), seeds the catalogue with
 * the same 20 items the legacy SQLite database used, including the Pakistani
 * meals required by SRS §1.4.
 */
public class FoodRepository {

    public interface Callback {
        void onResult(@NonNull List<Food> foods);
    }

    private static final FoodRepository INSTANCE = new FoodRepository();
    public static FoodRepository get() { return INSTANCE; }

    private final FirestoreManager firestore = new FirestoreManager();
    private final List<Food> cache = new ArrayList<>();
    private boolean loaded = false;
    private boolean loading = false;
    private final List<Callback> pending = new ArrayList<>();

    /** Snapshot of the cache. May be empty if not yet loaded. */
    @NonNull
    public List<Food> getCached() {
        return new ArrayList<>(cache);
    }

    public void load(@NonNull Callback cb) {
        if (loaded) {
            cb.onResult(getCached());
            return;
        }
        pending.add(cb);
        if (loading) return;

        loading = true;
        firestore.getAllFoods()
                .addOnSuccessListener(snap -> {
                    cache.clear();
                    for (QueryDocumentSnapshot d : snap) {
                        Food f = d.toObject(Food.class);
                        f.setId(d.getId());
                        cache.add(f);
                    }
                    if (cache.isEmpty()) {
                        // First-ever launch — seed Firestore with the canonical list.
                        seedIfEmpty();
                    } else {
                        markLoaded();
                    }
                })
                .addOnFailureListener(e -> markLoaded());
    }

    public void search(@Nullable String query, @NonNull Callback cb) {
        load(all -> cb.onResult(filter(all, query)));
    }

    public Task<Void> addFood(@NonNull Food food) {
        return firestore.saveFood(food);
    }

    public Task<Void> updateFood(@NonNull Food food) {
        return firestore.saveFood(food);
    }

    public Task<Void> deleteFood(@NonNull String foodId) {
        return firestore.deleteFood(foodId);
    }

    /** Force a re-fetch — called after admin edits/deletes. */
    public void invalidate() {
        loaded = false;
        cache.clear();
    }

    /** Local apply after an admin write so the UI updates immediately. */
    public void applyLocalChange(@NonNull Food updated) {
        for (int i = 0; i < cache.size(); i++) {
            if (cache.get(i).getId() != null && cache.get(i).getId().equals(updated.getId())) {
                cache.set(i, updated);
                return;
            }
        }
        cache.add(updated);
    }

    public void applyLocalDelete(@NonNull String foodId) {
        for (int i = 0; i < cache.size(); i++) {
            if (foodId.equals(cache.get(i).getId())) {
                cache.remove(i);
                return;
            }
        }
    }

    /* ---------- Internals ---------- */

    private List<Food> filter(List<Food> all, @Nullable String query) {
        if (query == null || query.trim().isEmpty()) return new ArrayList<>(all);
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<Food> out = new ArrayList<>();
        for (Food f : all) {
            if (f.getName() != null && f.getName().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(f);
            }
        }
        return out;
    }

    private void markLoaded() {
        loaded = true;
        loading = false;
        List<Callback> toFire = new ArrayList<>(pending);
        pending.clear();
        for (Callback cb : toFire) cb.onResult(getCached());
    }

    private void seedIfEmpty() {
        Food[] seed = defaultSeed();
        final int[] remaining = { seed.length };
        for (Food f : seed) {
            firestore.saveFood(f)
                    .addOnCompleteListener(t -> {
                        if (t.isSuccessful()) cache.add(f);
                        remaining[0]--;
                        if (remaining[0] == 0) markLoaded();
                    });
        }
    }

    private Food[] defaultSeed() {
        return new Food[] {
            // Western basics
            new Food(null, "Boiled Eggs",      "🥚",  78.0,  6.0,  0.6,  5.3, "piece"),
            new Food(null, "Banana",           "🍌",  89.0,  1.1, 27.0,  0.3, "100g"),
            new Food(null, "Whole Milk",       "🥛",  61.0,  3.4,  4.8,  3.7, "100ml"),
            new Food(null, "Brown Bread",      "🍞", 246.0,  8.0, 44.0,  3.5, "100g"),
            new Food(null, "Chicken Rice Bowl","🍛", 520.0, 35.0, 58.0, 12.0, "serving"),
            new Food(null, "Greek Yogurt",     "🥣",  59.0, 10.0,  3.6,  0.4, "100g"),
            new Food(null, "Apple",            "🍎",  52.0,  0.3, 14.0,  0.2, "100g"),
            new Food(null, "Almonds",          "🌰", 579.0, 21.0, 22.0, 50.0, "100g"),

            // Pakistani / South Asian (SRS §1.4)
            new Food(null, "Chicken Biryani",  "🍚", 290.0, 13.0, 32.0, 12.0, "100g"),
            new Food(null, "Daal Chawal",      "🍲", 175.0,  7.0, 28.0,  4.0, "100g"),
            new Food(null, "Chapati / Roti",   "🫓", 297.0, 10.5, 56.0,  7.5, "piece"),
            new Food(null, "Aloo Paratha",     "🥞", 260.0,  5.0, 35.0, 11.0, "piece"),
            new Food(null, "Chicken Karahi",   "🍗", 220.0, 18.0,  5.0, 14.0, "100g"),
            new Food(null, "Beef Nihari",      "🥩", 312.0, 22.0,  6.0, 22.0, "100g"),
            new Food(null, "Seekh Kebab",      "🍢", 270.0, 18.0,  4.0, 20.0, "piece"),
            new Food(null, "Samosa",           "🥟", 308.0,  5.0, 32.0, 18.0, "piece"),
            new Food(null, "Lassi",            "🥤",  98.0,  3.0, 11.0,  4.0, "100ml"),
            new Food(null, "Mango",            "🥭",  60.0,  0.8, 15.0,  0.4, "100g"),
            new Food(null, "Chai (with milk)", "☕",  88.0,  2.5,  8.0,  5.0, "cup"),
            new Food(null, "Naan",             "🫓", 310.0,  9.0, 50.0,  7.0, "piece"),
        };
    }
}

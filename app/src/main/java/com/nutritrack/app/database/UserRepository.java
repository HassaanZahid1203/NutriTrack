package com.nutritrack.app.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nutritrack.app.models.User;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory cache for the signed-in user's profile.
 *
 * Loaded once after sign-in (or on app start if already signed in) so multiple
 * fragments don't each issue a Firestore read for the same data.
 *
 * Fragments register a one-shot callback via {@link #getUser(Callback)}; if the
 * user is already cached the callback fires synchronously, otherwise it
 * triggers a fetch and the callback fires when the fetch completes.
 */
public class UserRepository {

    public interface Callback {
        void onResult(@Nullable User user);
    }

    private static final UserRepository INSTANCE = new UserRepository();
    public static UserRepository get() { return INSTANCE; }

    private final FirestoreManager firestore = new FirestoreManager();

    @Nullable private User cached;
    private boolean fetching;
    private final List<Callback> pending = new ArrayList<>();

    /** Synchronous cached value, or null if not loaded yet. */
    @Nullable
    public User getCached() { return cached; }

    /** Daily calorie target from cache, or 2000 fallback if profile not loaded. */
    public int getDailyCalorieTarget() {
        return cached != null ? cached.getDailyCalorieTarget() : 2000;
    }

    /** Fetch (or return cached) user. Safe to call before sign-in: returns null. */
    public void getUser(@NonNull Callback cb) {
        if (cached != null) {
            cb.onResult(cached);
            return;
        }
        if (firestore.getUid() == null) {
            cb.onResult(null);
            return;
        }
        pending.add(cb);
        if (fetching) return;

        fetching = true;
        firestore.getUser()
                .addOnSuccessListener(snap -> {
                    cached = snap.exists() ? snap.toObject(User.class) : null;
                    flushPending();
                })
                .addOnFailureListener(e -> flushPending());
    }

    private void flushPending() {
        fetching = false;
        List<Callback> toFire = new ArrayList<>(pending);
        pending.clear();
        for (Callback cb : toFire) cb.onResult(cached);
    }

    /** Clear the cache (call on sign-out). */
    public void clear() {
        cached = null;
        pending.clear();
        fetching = false;
    }
}

package com.nutritrack.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.nutritrack.app.auth.LoginActivity;
import com.nutritrack.app.database.FirestoreManager;
import com.nutritrack.app.database.UserRepository;
import com.nutritrack.app.databinding.FragmentProfileBinding;
import com.nutritrack.app.models.DailyTotals;
import com.nutritrack.app.models.MealEntry;
import com.nutritrack.app.models.User;
import com.nutritrack.app.utils.HealthScoreCalculator;

import java.util.ArrayList;
import java.util.List;

/**
 * Profile / settings screen (mockup page 7, SRS §3.1.6).
 *
 * Pulls live data from the user profile and today's meal log:
 *  - Daily calorie target from User.dailyCalorieTarget
 *  - Today's progress against target from logged meals
 *  - Health score recomputed via HealthScoreCalculator
 *  - Weight goal % from User.weightCurrentKg / weightGoalKg
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding b;
    private final FirestoreManager firestore = new FirestoreManager();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentProfileBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        wireSettingsRows();
        wireSignOut();

        UserRepository.get().getUser(this::onUserLoaded);
    }

    private void onUserLoaded(@Nullable User user) {
        if (b == null) return;

        if (user == null) {
            b.userName.setText("Guest");
            b.userEmail.setText("");
            b.avatarInitials.setText("G");
            return;
        }

        // Identity
        String name = user.getDisplayName();
        if (name == null || name.isEmpty()) name = "User";
        b.userName.setText(name);
        b.userEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        b.avatarInitials.setText(user.getInitials());

        // Admin row visibility (SRS §2.3.3)
        boolean isAdmin = user.isAdmin();
        b.rowAdminFoods   .setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        b.adminRowDivider .setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        b.rowAdminFoods.setOnClickListener(v ->
                startActivity(new Intent(requireContext(),
                        com.nutritrack.app.ui.admin.AdminFoodsActivity.class)));

        renderWeightGoal(user);

        // Set the static target labels even before today's meals load
        b.calorieTargetLabel.setText(String.format("Target: %d kcal/day",
                user.getDailyCalorieTarget()));
        b.calorieTargetProgressLabel.setText(String.format("0 / %d kcal today",
                user.getDailyCalorieTarget()));
        b.calorieTargetBar.setProgress(0);
        b.calorieTargetPct.setText("0%");

        // Load today's meals to compute live progress + health score
        if (firestore.getUid() != null) {
            firestore.getTodaysMeals()
                    .addOnSuccessListener(snap -> {
                        if (b == null) return;
                        List<MealEntry> entries = new ArrayList<>();
                        for (QueryDocumentSnapshot d : snap)
                            entries.add(d.toObject(MealEntry.class));
                        renderLiveStats(user, DailyTotals.from(entries));
                    })
                    .addOnFailureListener(e -> {
                        if (b == null) return;
                        renderLiveStats(user, new DailyTotals());
                    });
        } else {
            renderLiveStats(user, new DailyTotals());
        }
    }

    private void renderLiveStats(@NonNull User user, @NonNull DailyTotals today) {
        int target = user.getDailyCalorieTarget();
        int eaten  = (int) today.calories;
        int pct    = target > 0 ? Math.min(100, Math.max(0, (eaten * 100) / target)) : 0;

        b.calorieTargetPct.setText(pct + "%");
        b.calorieTargetBar.setProgress(pct);
        b.calorieTargetProgressLabel.setText(
                String.format("%d / %d kcal today", eaten, target));

        // Same Health Score formula as the dashboard
        HealthScoreCalculator.Result score =
                HealthScoreCalculator.compute(today, target, /* streak placeholder */ 5);
        b.statHealthScore.setText(String.valueOf(score.total));

        // Day streak placeholder until streak tracking is implemented
        b.statDayStreak.setText("—");
    }

    private void renderWeightGoal(@NonNull User user) {
        double current = user.getWeightCurrentKg();
        double goal    = user.getWeightGoalKg();

        b.weightGoalLabel.setText(String.format("Target: %.1f kg", goal));
        b.weightGoalProgressLabel.setText(String.format("Current: %.1f kg", current));

        if (current > goal) {
            // Weight loss scenario — assume start was ~5kg above current
            double startWeight = current + 5;
            double progress = (startWeight - current) / Math.max(0.001, startWeight - goal);
            int pct = (int) Math.max(0, Math.min(100, progress * 100));
            b.weightGoalPct.setText(pct + "%");
            b.weightGoalBar.setProgress(pct);
            b.statKgLost.setText(String.format("%.1f", startWeight - current));
        } else if (current < goal) {
            // Weight gain scenario
            double startWeight = current - 5;
            double progress = (current - startWeight) / Math.max(0.001, goal - startWeight);
            int pct = (int) Math.max(0, Math.min(100, progress * 100));
            b.weightGoalPct.setText(pct + "%");
            b.weightGoalBar.setProgress(pct);
            b.statKgLost.setText(String.format("+%.1f", current - startWeight));
        } else {
            b.weightGoalPct.setText("100%");
            b.weightGoalBar.setProgress(100);
            b.statKgLost.setText("0.0");
        }
    }

    private void wireSettingsRows() {
        b.rowEditProfile  .setOnClickListener(v -> toast("Edit profile (coming soon)"));
        b.rowNotifications.setOnClickListener(v -> toast("Notifications (coming soon)"));
        b.rowPrivacy      .setOnClickListener(v -> toast("Privacy & Security (coming soon)"));
    }

    private void wireSignOut() {
        b.btnSignOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            UserRepository.get().clear();
            Intent i = new Intent(requireActivity(), LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            requireActivity().finishAffinity();
        });
    }

    private void toast(String s) {
        Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}

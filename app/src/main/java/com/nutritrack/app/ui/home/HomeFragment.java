package com.nutritrack.app.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.nutritrack.app.databinding.FragmentHomeBinding;
import com.nutritrack.app.database.FirestoreManager;
import com.nutritrack.app.database.UserRepository;
import com.nutritrack.app.models.DailyTotals;
import com.nutritrack.app.models.MealEntry;
import com.nutritrack.app.models.User;
import com.nutritrack.app.utils.DateUtils;
import com.nutritrack.app.utils.HealthScoreCalculator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding b;
    private FirestoreManager firestore;

    /** Fallback target when the user profile hasn't loaded yet. */
    private static final int DEFAULT_TARGET_KCAL = 2000;
    private int dailyTargetKcal = DEFAULT_TARGET_KCAL;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentHomeBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestore = new FirestoreManager();

        b.greeting.setText(DateUtils.greeting());

        // Resolve the user profile first, then load the dashboard, so calorie
        // targets and the user's name reflect their actual profile.
        UserRepository.get().getUser(this::onUserResolved);
        loadWeeklyTrend();
    }

    private void onUserResolved(@Nullable User user) {
        if (b == null) return; // fragment torn down before fetch returned
        if (user != null) {
            dailyTargetKcal = user.getDailyCalorieTarget();
            b.userName.setText(user.getDisplayName() != null ? user.getDisplayName() : "User");
            b.avatarInitials.setText(user.getInitials());
        } else {
            b.userName.setText("Welcome");
            b.avatarInitials.setText("U");
        }
        loadTodayDashboard();
    }

    private void loadTodayDashboard() {
        if (firestore.getUid() == null) {
            renderTotals(new DailyTotals());
            return;
        }
        firestore.getTodaysMeals()
                .addOnSuccessListener(snap -> {
                    if (b == null) return;
                    List<MealEntry> entries = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        MealEntry m = doc.toObject(MealEntry.class);
                        entries.add(m);
                    }
                    renderTotals(DailyTotals.from(entries));
                })
                .addOnFailureListener(e -> {
                    if (b == null) return;
                    renderTotals(new DailyTotals());
                });
    }

    private void renderTotals(DailyTotals t) {
        b.progressRing.setValue(String.valueOf((int) t.calories));
        b.dailyTarget.setText(dailyTargetKcal + " kcal");
        b.chipProtein.setText(String.format("P: %dg", (int) t.proteinG));
        b.chipCarbs  .setText(String.format("C: %dg", (int) t.carbsG));
        b.chipFat    .setText(String.format("F: %dg", (int) t.fatG));

        int progressPct = (int) ((t.calories / dailyTargetKcal) * 100);
        b.progressRing.setProgress(Math.min(100, Math.max(0, progressPct)));

        // Health Score (SRS §4.3)
        HealthScoreCalculator.Result score =
                HealthScoreCalculator.compute(t, dailyTargetKcal, 5);
        b.scoreNumber.setText(String.valueOf(score.total));
        b.scoreCaption.setText(HealthScoreCalculator.describe(score.total) + " • Updated today");

        b.proteinScore.setText(String.valueOf(score.proteinBalance));
        b.disciplineScore.setText(String.valueOf(score.calorieDiscipline));
        b.proteinBar   .setProgress(score.proteinBalance);
        b.disciplineBar.setProgress(score.calorieDiscipline);

        // Surplus alert (mockup)
        boolean surplus = t.calories > dailyTargetKcal;
        b.alertCard.setVisibility(surplus ? View.VISIBLE : View.GONE);
    }

    private void loadWeeklyTrend() {
        // Default placeholder bars in case Firestore is empty / not configured.
        renderBars(new float[]{ 1280, 1980, 1320, 2100, 1820, 1120, 980 });

        if (firestore.getUid() == null) return;

        firestore.getLastSevenDaysMeals().addOnSuccessListener(snap -> {
            float[] perDay = new float[7];
            long firstDayStart = DateUtils.startOfDay() - 6L * 24 * 60 * 60 * 1000;
            for (QueryDocumentSnapshot doc : snap) {
                Long ts = doc.getLong("timestamp");
                Double cal = doc.getDouble("calories");
                if (ts == null || cal == null) continue;
                int idx = (int) ((ts - firstDayStart) / (24L * 60 * 60 * 1000));
                if (idx >= 0 && idx < 7) perDay[idx] += cal;
            }
            // Only override placeholder when at least one day has data.
            for (float v : perDay) {
                if (v > 0) { renderBars(perDay); break; }
            }
        });
    }

    private void renderBars(float[] perDay) {
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < perDay.length; i++) entries.add(new BarEntry(i, perDay[i]));

        BarDataSet set = new BarDataSet(entries, "");
        set.setColors(new int[]{
                0xFFCDE8D2, 0xFF7FC68F, 0xFFCDE8D2, 0xFF7FC68F,
                0xFF2E8B45, 0xFFCDE8D2, 0xFFCDE8D2 });
        set.setDrawValues(false);

        BarData data = new BarData(set);
        data.setBarWidth(0.55f);
        b.weeklyChart.setData(data);

        // Day labels under x-axis (M T W T F S S, like the mockup)
        String[] labels = dayInitialsForLastSeven();
        XAxis x = b.weeklyChart.getXAxis();
        x.setValueFormatter(new IndexAxisValueFormatter(labels));
        x.setDrawGridLines(false);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setGranularity(1f);
        x.setTextColor(0xFF6B6B6B);

        b.weeklyChart.getAxisLeft().setEnabled(false);
        b.weeklyChart.getAxisRight().setEnabled(false);
        b.weeklyChart.getDescription().setEnabled(false);
        b.weeklyChart.getLegend().setEnabled(false);
        b.weeklyChart.setFitBars(true);
        b.weeklyChart.setTouchEnabled(false);
        b.weeklyChart.invalidate();
    }

    private String[] dayInitialsForLastSeven() {
        String[] base = { "S", "M", "T", "W", "T", "F", "S" };
        String[] result = new String[7];
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -6);
        for (int i = 0; i < 7; i++) {
            int dow = c.get(Calendar.DAY_OF_WEEK);   // 1=Sun .. 7=Sat
            result[i] = base[dow - 1];
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        return result;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}

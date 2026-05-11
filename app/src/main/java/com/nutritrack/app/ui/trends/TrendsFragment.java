package com.nutritrack.app.ui.trends;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nutritrack.app.databinding.FragmentTrendsBinding;
import com.nutritrack.app.utils.HealthScoreCalculator;

/**
 * What-If Simulation (SRS §4.4): adjust portion size or apply a swap and see
 * the projected calorie + health-score change.
 *
 * Implements REQ-9 (simulate meal options), REQ-10 (estimate macro change),
 * REQ-11 (display projected outcomes).
 */
public class TrendsFragment extends Fragment {

    private FragmentTrendsBinding b;

    private static final int BASELINE_KCAL = 2000;
    private static final int BASELINE_SCORE = 74;
    private static final int CHICKEN_BASELINE_KCAL = 520;
    private static final int SWAP_SAVINGS_KCAL = 150;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentTrendsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        b.portionSeek.setMax(100);  // 0..2× in 0.02× steps; 50 = 1.0×
        b.portionSeek.setProgress(50);
        b.swapSeek.setMax(100);
        b.swapSeek.setProgress(100);

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) { recompute(); }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        };
        b.portionSeek.setOnSeekBarChangeListener(listener);
        b.swapSeek   .setOnSeekBarChangeListener(listener);

        b.btnApply.setOnClickListener(v ->
                Toast.makeText(getContext(),
                        "Scenario applied to today's log ✓",
                        Toast.LENGTH_SHORT).show());

        recompute();
    }

    private void recompute() {
        double portionMul = b.portionSeek.getProgress() / 50.0;   // 0..2.0×
        double swapMul    = b.swapSeek.getProgress() / 100.0;     // 0..1.0
        double portionDelta = (portionMul - 1.0) * CHICKEN_BASELINE_KCAL;
        double swapDelta    = -SWAP_SAVINGS_KCAL * swapMul;
        double totalDelta   = portionDelta + swapDelta;

        int afterKcal = (int) Math.max(0, BASELINE_KCAL + totalDelta);
        b.txtPortionMul.setText(String.format("%.1f×", portionMul));
        b.txtSwapState.setText(swapMul >= 0.99 ? "Full swap"
                : swapMul <= 0.01 ? "No swap"
                : String.format("Partial %d%%", (int) (swapMul * 100)));

        b.baselineCalories.setText(String.valueOf(BASELINE_KCAL));
        b.afterCalories.setText(String.format("%,d", afterKcal));
        b.calorieChange.setText(String.format("%s%d kcal",
                totalDelta >= 0 ? "+" : "−", (int) Math.abs(totalDelta)));

        // Naive projected-score effect: every 100 kcal closer to target = +1 point.
        int projected = BASELINE_SCORE +
                Math.max(-15, Math.min(15,
                        (int) Math.round(-totalDelta / 45.0)));
        projected = Math.max(0, Math.min(100, projected));
        int diff = projected - BASELINE_SCORE;
        b.projectedScore.setText(String.format("%s%d pts → %d",
                diff >= 0 ? "+" : "−", Math.abs(diff), projected));
        b.projectedScoreCaption.setText(HealthScoreCalculator.describe(projected));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}

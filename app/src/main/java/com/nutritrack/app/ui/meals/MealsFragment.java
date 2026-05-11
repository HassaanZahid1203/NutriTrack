package com.nutritrack.app.ui.meals;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.nutritrack.app.MainActivity;
import com.nutritrack.app.R;
import com.nutritrack.app.adapters.MealEntryAdapter;
import com.nutritrack.app.database.FirestoreManager;
import com.nutritrack.app.database.UserRepository;
import com.nutritrack.app.databinding.FragmentMealsBinding;
import com.nutritrack.app.models.DailyTotals;
import com.nutritrack.app.models.MealEntry;
import com.nutritrack.app.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Today's meals overview (SRS §3.1.5, mockup page 4).
 */
public class MealsFragment extends Fragment {

    private FragmentMealsBinding b;
    private FirestoreManager firestore;
    private MealEntryAdapter breakfastAdapter, lunchAdapter, dinnerAdapter, snackAdapter;

    /** Falls back to 2000 until the user profile loads. */
    private int dailyTargetKcal = 2000;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentMealsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        firestore = new FirestoreManager();

        b.headerDate.setText(DateUtils.dayHeader());

        breakfastAdapter = setupList(b.breakfastList);
        lunchAdapter     = setupList(b.lunchList);
        dinnerAdapter    = setupList(b.dinnerList);
        snackAdapter     = setupList(b.snackList);

        b.dinnerEmpty.setOnClickListener(v ->
                ((MainActivity) requireActivity()).selectTab(R.id.nav_log));
        b.breakfastEmpty.setOnClickListener(v ->
                ((MainActivity) requireActivity()).selectTab(R.id.nav_log));
        b.lunchEmpty.setOnClickListener(v ->
                ((MainActivity) requireActivity()).selectTab(R.id.nav_log));
        b.snackEmpty.setOnClickListener(v ->
                ((MainActivity) requireActivity()).selectTab(R.id.nav_log));

        loadMeals();
    }

    private MealEntryAdapter setupList(androidx.recyclerview.widget.RecyclerView rv) {
        MealEntryAdapter a = new MealEntryAdapter();
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(a);
        rv.setNestedScrollingEnabled(false);
        return a;
    }

    private void loadMeals() {
        // Pick up the user's actual target before rendering anything.
        UserRepository.get().getUser(user -> {
            if (b == null) return;
            dailyTargetKcal = UserRepository.get().getDailyCalorieTarget();
            doLoadMeals();
        });
    }

    private void doLoadMeals() {
        if (firestore.getUid() == null) {
            renderEmpty();
            return;
        }
        firestore.getTodaysMeals().addOnSuccessListener(snap -> {
            if (b == null) return;
            List<MealEntry> all = new ArrayList<>();
            for (QueryDocumentSnapshot d : snap) all.add(d.toObject(MealEntry.class));
            renderEntries(all);
        }).addOnFailureListener(e -> {
            if (b == null) return;
            renderEmpty();
        });
    }

    private void renderEntries(List<MealEntry> all) {
        DailyTotals t = DailyTotals.from(all);

        b.kcalEaten.setText(String.valueOf((int) t.calories));
        b.kcalRemaining.setText(String.format("%d kcal",
                Math.max(0, dailyTargetKcal - (int) t.calories)));
        b.chipProtein.setText(String.format("P: %dg", (int) t.proteinG));
        b.chipCarbs  .setText(String.format("C: %dg", (int) t.carbsG));
        b.chipFat    .setText(String.format("F: %dg", (int) t.fatG));
        int pct = Math.min(100, (int) ((t.calories / dailyTargetKcal) * 100));
        b.progressRing.setProgress(pct);
        b.progressRing.setValue(String.valueOf((int) t.calories));

        // Distribute by meal type
        List<MealEntry> bf = filter(all, MealEntry.TYPE_BREAKFAST);
        List<MealEntry> ln = filter(all, MealEntry.TYPE_LUNCH);
        List<MealEntry> dn = filter(all, MealEntry.TYPE_DINNER);
        List<MealEntry> sn = filter(all, MealEntry.TYPE_SNACK);

        bindSection(b.breakfastHeader, b.breakfastList, b.breakfastEmpty,
                "BREAKFAST", t.breakfastKcal, bf, breakfastAdapter);
        bindSection(b.lunchHeader, b.lunchList, b.lunchEmpty,
                "LUNCH", t.lunchKcal, ln, lunchAdapter);
        bindSection(b.dinnerHeader, b.dinnerList, b.dinnerEmpty,
                "DINNER", t.dinnerKcal, dn, dinnerAdapter);
        bindSection(b.snackHeader, b.snackList, b.snackEmpty,
                "SNACK", t.snackKcal, sn, snackAdapter);
    }

    private void renderEmpty() {
        renderEntries(new ArrayList<>());
    }

    private List<MealEntry> filter(List<MealEntry> all, String type) {
        List<MealEntry> r = new ArrayList<>();
        for (MealEntry e : all) if (type.equals(e.getMealType())) r.add(e);
        return r;
    }

    private void bindSection(android.widget.TextView header,
                             androidx.recyclerview.widget.RecyclerView list,
                             View emptyView,
                             String typeLabel, double kcal,
                             List<MealEntry> entries, MealEntryAdapter adapter) {
        if (entries.isEmpty()) {
            header.setText(String.format("%s · NOT LOGGED YET", typeLabel));
            list.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            header.setText(String.format("%s · %d KCAL", typeLabel, (int) kcal));
            list.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            adapter.setItems(entries);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}

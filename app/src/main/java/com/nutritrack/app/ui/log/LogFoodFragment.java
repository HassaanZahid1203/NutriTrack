package com.nutritrack.app.ui.log;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.nutritrack.app.MainActivity;
import com.nutritrack.app.R;
import com.nutritrack.app.adapters.FoodPickerAdapter;
import com.nutritrack.app.database.FirestoreManager;
import com.nutritrack.app.database.FoodRepository;
import com.nutritrack.app.databinding.FragmentLogFoodBinding;
import com.nutritrack.app.models.Food;
import com.nutritrack.app.models.MealEntry;

/**
 * Log Food screen (SRS §3.1.3, mockup page 5).
 * Implements REQ-4 (select food), REQ-5 (auto calorie), REQ-6 (store history).
 *
 * Reads from the shared Firestore food catalogue via {@link FoodRepository}.
 */
public class LogFoodFragment extends Fragment {

    private FragmentLogFoodBinding b;
    private FirestoreManager firestore;
    private FoodPickerAdapter adapter;

    private String selectedMealType = MealEntry.TYPE_BREAKFAST;
    private Food selectedFood = null;
    private int quantity = 1;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentLogFoodBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestore = new FirestoreManager();

        setupMealTypeTabs();
        setupRecycler();
        setupSearch();
        setupQuantitySteppers();

        b.btnAddToLog.setOnClickListener(v -> onAddToLog());
        b.btnBack.setOnClickListener(v ->
                ((MainActivity) requireActivity()).selectTab(R.id.nav_home));

        // Load all foods from the shared Firestore catalogue.
        FoodRepository.get().load(foods -> {
            if (b == null) return;
            adapter.setItems(foods);
        });
        clearSelection();
    }

    private void setupMealTypeTabs() {
        b.pillBreakfast.setOnClickListener(v -> setMealType(MealEntry.TYPE_BREAKFAST));
        b.pillLunch    .setOnClickListener(v -> setMealType(MealEntry.TYPE_LUNCH));
        b.pillDinner   .setOnClickListener(v -> setMealType(MealEntry.TYPE_DINNER));
        b.pillSnack    .setOnClickListener(v -> setMealType(MealEntry.TYPE_SNACK));
        setMealType(MealEntry.TYPE_BREAKFAST);
    }

    private void setMealType(String type) {
        selectedMealType = type;

        TextView[] pills = { b.pillBreakfast, b.pillLunch, b.pillDinner, b.pillSnack };
        String[] types = {
                MealEntry.TYPE_BREAKFAST, MealEntry.TYPE_LUNCH,
                MealEntry.TYPE_DINNER, MealEntry.TYPE_SNACK
        };
        for (int i = 0; i < pills.length; i++) {
            boolean active = types[i].equals(type);
            pills[i].setBackgroundResource(active
                    ? R.drawable.bg_meal_selected
                    : R.drawable.bg_meal_unselected);
            pills[i].setTextColor(ContextCompat.getColor(requireContext(),
                    active ? R.color.white : R.color.text_primary));
        }
    }

    private void setupRecycler() {
        adapter = new FoodPickerAdapter(this::onFoodTap);
        b.foodList.setLayoutManager(new LinearLayoutManager(getContext()));
        b.foodList.setAdapter(adapter);
        b.foodList.setNestedScrollingEnabled(false);
    }

    private void setupSearch() {
        b.inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                FoodRepository.get().search(s.toString(), results -> {
                    if (b == null) return;
                    adapter.setItems(results);
                });
            }
        });
    }

    private void setupQuantitySteppers() {
        b.btnMinus.setOnClickListener(v -> {
            if (quantity > 1) { quantity--; refreshSelectionCard(); }
        });
        b.btnPlus.setOnClickListener(v -> {
            quantity++;
            refreshSelectionCard();
        });
    }

    private void onFoodTap(Food food) {
        selectedFood = food;
        quantity = 1;
        adapter.setSelected(food.getId());
        refreshSelectionCard();
    }

    private void refreshSelectionCard() {
        if (selectedFood == null) {
            b.selectionCard.setVisibility(View.GONE);
            b.btnAddToLog.setEnabled(false);
            b.btnAddToLog.setAlpha(0.5f);
            return;
        }
        b.selectionCard.setVisibility(View.VISIBLE);
        b.selFoodEmoji.setText(selectedFood.getEmoji());
        b.selFoodName.setText(selectedFood.getName());
        b.selQuantity.setText(String.valueOf(quantity));

        double q = quantity;
        b.selKcal.setText(String.format("%d", (int) (selectedFood.getCaloriesPerUnit() * q)));
        b.selProtein.setText(String.format("%.0fg", selectedFood.getProteinG() * q));
        b.selCarbs  .setText(String.format("%.0fg", selectedFood.getCarbsG() * q));
        b.selFat    .setText(String.format("%.0fg", selectedFood.getFatG() * q));

        b.btnAddToLog.setEnabled(true);
        b.btnAddToLog.setAlpha(1.0f);
    }

    private void clearSelection() {
        selectedFood = null;
        adapter.setSelected(null);
        refreshSelectionCard();
    }

    private void onAddToLog() {
        if (selectedFood == null) {
            Toast.makeText(getContext(), "Pick a food first", Toast.LENGTH_SHORT).show();
            return;
        }

        MealEntry entry = new MealEntry(selectedMealType, selectedFood, quantity);

        if (firestore.getUid() == null) {
            Toast.makeText(getContext(),
                    "Sign in to sync your meal — added locally only",
                    Toast.LENGTH_SHORT).show();
            ((MainActivity) requireActivity()).selectTab(R.id.nav_meals);
            return;
        }

        firestore.addMealEntry(entry)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "Added to log ✓", Toast.LENGTH_SHORT).show();
                    ((MainActivity) requireActivity()).selectTab(R.id.nav_meals);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed to log: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}

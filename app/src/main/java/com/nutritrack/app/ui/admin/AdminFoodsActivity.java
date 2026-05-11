package com.nutritrack.app.ui.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.nutritrack.app.database.FoodRepository;
import com.nutritrack.app.databinding.ActivityAdminFoodsBinding;
import com.nutritrack.app.models.Food;

/**
 * Admin panel for the shared food database (SRS §2.3.3, Appendix C).
 *
 * Lets admin users add, edit, and delete entries in the Firestore `foods`
 * collection. Entry is gated by {@code User.isAdmin}; this activity is only
 * launched from a Profile row that's hidden for non-admins, and Firestore
 * security rules should additionally enforce admin writes server-side
 * (see README — Firestore Security Rules).
 */
public class AdminFoodsActivity extends AppCompatActivity {

    private ActivityAdminFoodsBinding b;
    private AdminFoodAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityAdminFoodsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        b.btnBack.setOnClickListener(v -> finish());

        adapter = new AdminFoodAdapter(new AdminFoodAdapter.OnAction() {
            @Override public void onEdit(Food f) { showEditDialog(f); }
            @Override public void onDelete(Food f) { confirmDelete(f); }
        });
        b.foodList.setLayoutManager(new LinearLayoutManager(this));
        b.foodList.setAdapter(adapter);

        b.fabAdd.setOnClickListener(v -> showEditDialog(null));

        b.inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { refresh(s.toString()); }
        });

        refresh("");
    }

    private void refresh(@NonNull String query) {
        b.emptyState.setVisibility(android.view.View.GONE);
        b.progress.setVisibility(android.view.View.VISIBLE);
        FoodRepository.get().search(query, foods -> {
            if (b == null) return;
            b.progress.setVisibility(android.view.View.GONE);
            adapter.setItems(foods);
            b.emptyState.setVisibility(foods.isEmpty()
                    ? android.view.View.VISIBLE
                    : android.view.View.GONE);
        });
    }

    private void showEditDialog(Food existing) {
        EditFoodDialog dialog = EditFoodDialog.newInstance(existing,
                food -> save(existing, food));
        dialog.show(getSupportFragmentManager(), "edit_food");
    }

    private void save(Food existing, Food edited) {
        if (existing == null) {
            FoodRepository.get().addFood(edited)
                    .addOnSuccessListener(v -> {
                        FoodRepository.get().applyLocalChange(edited);
                        toast("Added " + edited.getName());
                        refresh(b.inputSearch.getText().toString());
                    })
                    .addOnFailureListener(this::failure);
        } else {
            edited.setId(existing.getId());
            FoodRepository.get().updateFood(edited)
                    .addOnSuccessListener(v -> {
                        FoodRepository.get().applyLocalChange(edited);
                        toast("Updated " + edited.getName());
                        refresh(b.inputSearch.getText().toString());
                    })
                    .addOnFailureListener(this::failure);
        }
    }

    private void confirmDelete(Food food) {
        new AlertDialog.Builder(this)
                .setTitle("Delete " + food.getName() + "?")
                .setMessage("This removes it from the shared database for all users.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, w) -> doDelete(food))
                .show();
    }

    private void doDelete(Food food) {
        FoodRepository.get().deleteFood(food.getId())
                .addOnSuccessListener(v -> {
                    FoodRepository.get().applyLocalDelete(food.getId());
                    toast("Deleted " + food.getName());
                    refresh(b.inputSearch.getText().toString());
                })
                .addOnFailureListener(this::failure);
    }

    private void failure(@NonNull Exception e) {
        toast("Failed: " + e.getMessage());
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}

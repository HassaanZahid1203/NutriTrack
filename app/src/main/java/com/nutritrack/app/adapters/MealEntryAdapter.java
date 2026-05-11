package com.nutritrack.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nutritrack.app.R;
import com.nutritrack.app.models.MealEntry;

import java.util.ArrayList;
import java.util.List;

public class MealEntryAdapter extends RecyclerView.Adapter<MealEntryAdapter.VH> {

    private final List<MealEntry> items = new ArrayList<>();

    public void setItems(List<MealEntry> entries) {
        items.clear();
        if (entries != null) items.addAll(entries);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meal_entry, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        MealEntry e = items.get(i);
        h.emoji.setText(e.getFoodEmoji() != null ? e.getFoodEmoji() : "🍽️");
        h.name.setText(e.getDisplayName());
        h.macros.setText(String.format("P: %.1fg · C: %.1fg · F: %.1fg",
                e.getProteinG(), e.getCarbsG(), e.getFatG()));
        h.kcal.setText(String.valueOf((int) e.getCalories()));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView emoji, name, macros, kcal;
        VH(@NonNull View v) {
            super(v);
            emoji  = v.findViewById(R.id.foodEmoji);
            name   = v.findViewById(R.id.foodName);
            macros = v.findViewById(R.id.foodMacros);
            kcal   = v.findViewById(R.id.foodKcal);
        }
    }
}

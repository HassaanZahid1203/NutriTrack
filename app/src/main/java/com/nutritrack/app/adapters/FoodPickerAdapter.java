package com.nutritrack.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.nutritrack.app.R;
import com.nutritrack.app.models.Food;

import java.util.ArrayList;
import java.util.List;

public class FoodPickerAdapter extends RecyclerView.Adapter<FoodPickerAdapter.VH> {

    public interface OnFoodClick { void onClick(Food food); }

    private final List<Food> items = new ArrayList<>();
    private String selectedId = null;
    private final OnFoodClick listener;

    public FoodPickerAdapter(OnFoodClick listener) {
        this.listener = listener;
    }

    public void setItems(List<Food> foods) {
        items.clear();
        if (foods != null) items.addAll(foods);
        notifyDataSetChanged();
    }

    public void setSelected(String id) {
        selectedId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_food_picker, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        Food f = items.get(i);
        h.emoji.setText(f.getEmoji() != null ? f.getEmoji() : "🍽️");
        h.name.setText(f.getName());
        h.macros.setText(f.getMacroLine());
        h.kcal.setText(String.valueOf((int) f.getCaloriesPerUnit()));

        boolean selected = f.getId() != null && f.getId().equals(selectedId);
        h.itemView.setBackgroundResource(selected
                ? R.drawable.bg_food_selected
                : R.drawable.bg_white_card);
        int color = ContextCompat.getColor(h.itemView.getContext(),
                selected ? R.color.brand_green : R.color.brand_green);
        h.kcal.setTextColor(color);
        h.itemView.setOnClickListener(v -> listener.onClick(f));
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

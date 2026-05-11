package com.nutritrack.app.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nutritrack.app.R;
import com.nutritrack.app.models.Food;

import java.util.ArrayList;
import java.util.List;

class AdminFoodAdapter extends RecyclerView.Adapter<AdminFoodAdapter.VH> {

    interface OnAction {
        void onEdit(Food f);
        void onDelete(Food f);
    }

    private final List<Food> items = new ArrayList<>();
    private final OnAction listener;

    AdminFoodAdapter(@NonNull OnAction listener) { this.listener = listener; }

    void setItems(@NonNull List<Food> foods) {
        items.clear();
        items.addAll(foods);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_food, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        Food f = items.get(i);
        h.emoji.setText(f.getEmoji() != null ? f.getEmoji() : "🍽️");
        h.name.setText(f.getName());
        h.macros.setText(f.getMacroLine());
        h.kcal.setText(String.format("%d kcal", (int) f.getCaloriesPerUnit()));
        h.itemView.setOnClickListener(v -> listener.onEdit(f));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(f));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView emoji, name, macros, kcal;
        ImageView btnDelete;
        VH(@NonNull View v) {
            super(v);
            emoji     = v.findViewById(R.id.foodEmoji);
            name      = v.findViewById(R.id.foodName);
            macros    = v.findViewById(R.id.foodMacros);
            kcal      = v.findViewById(R.id.foodKcal);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}

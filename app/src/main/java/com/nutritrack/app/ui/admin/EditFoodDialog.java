package com.nutritrack.app.ui.admin;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.nutritrack.app.databinding.DialogEditFoodBinding;
import com.nutritrack.app.models.Food;

/**
 * Modal dialog used by the admin panel to create or edit a food entry.
 * Returns the resulting {@link Food} via the {@link OnSave} callback.
 */
public class EditFoodDialog extends DialogFragment {

    public interface OnSave { void onSave(@NonNull Food food); }

    private static final String ARG_ID    = "id";
    private static final String ARG_NAME  = "name";
    private static final String ARG_EMOJI = "emoji";
    private static final String ARG_KCAL  = "kcal";
    private static final String ARG_P     = "protein";
    private static final String ARG_C     = "carbs";
    private static final String ARG_F     = "fat";
    private static final String ARG_UNIT  = "unit";

    private DialogEditFoodBinding b;
    private OnSave saveListener;

    public static EditFoodDialog newInstance(@Nullable Food existing, @NonNull OnSave onSave) {
        EditFoodDialog d = new EditFoodDialog();
        d.saveListener = onSave;
        Bundle args = new Bundle();
        if (existing != null) {
            args.putString(ARG_ID,    existing.getId());
            args.putString(ARG_NAME,  existing.getName());
            args.putString(ARG_EMOJI, existing.getEmoji());
            args.putDouble(ARG_KCAL,  existing.getCaloriesPerUnit());
            args.putDouble(ARG_P,     existing.getProteinG());
            args.putDouble(ARG_C,     existing.getCarbsG());
            args.putDouble(ARG_F,     existing.getFatG());
            args.putString(ARG_UNIT,  existing.getUnit());
        }
        d.setArguments(args);
        return d;
    }

    @NonNull @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        b = DialogEditFoodBinding.inflate(LayoutInflater.from(getContext()));
        Bundle args = getArguments();
        boolean editing = args != null && args.getString(ARG_ID) != null;

        if (editing) {
            b.title.setText("Edit food");
            b.inputName .setText(args.getString(ARG_NAME, ""));
            b.inputEmoji.setText(args.getString(ARG_EMOJI, ""));
            b.inputKcal .setText(numberText(args.getDouble(ARG_KCAL, 0)));
            b.inputProtein.setText(numberText(args.getDouble(ARG_P, 0)));
            b.inputCarbs.setText(numberText(args.getDouble(ARG_C, 0)));
            b.inputFat  .setText(numberText(args.getDouble(ARG_F, 0)));
            b.inputUnit .setText(args.getString(ARG_UNIT, "100g"));
        } else {
            b.title.setText("Add food");
            b.inputUnit.setText("100g");
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(b.getRoot())
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null) // overridden below to skip auto-dismiss on error
                .create();

        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    Food result = readForm();
                    if (result == null) return; // validation failed; stay open
                    if (saveListener != null) saveListener.onSave(result);
                    dialog.dismiss();
                }));
        return dialog;
    }

    @Nullable
    private Food readForm() {
        String name  = b.inputName.getText().toString().trim();
        String emoji = b.inputEmoji.getText().toString().trim();
        String unit  = b.inputUnit.getText().toString().trim();
        if (name.isEmpty()) { fail("Name is required"); return null; }
        if (unit.isEmpty()) unit = "100g";
        if (emoji.isEmpty()) emoji = "🍽️";

        double kcal = parseDouble(b.inputKcal.getText().toString(), -1);
        double p    = parseDouble(b.inputProtein.getText().toString(), -1);
        double c    = parseDouble(b.inputCarbs.getText().toString(), -1);
        double f    = parseDouble(b.inputFat.getText().toString(), -1);

        if (kcal < 0) { fail("Enter a valid calorie value"); return null; }
        if (p < 0 || c < 0 || f < 0) {
            fail("Macros must be 0 or greater");
            return null;
        }

        Food out = new Food(null, name, emoji, kcal, p, c, f, unit);
        return out;
    }

    private void fail(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private static String numberText(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    private static double parseDouble(String s, double fallback) {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception e) { return fallback; }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}

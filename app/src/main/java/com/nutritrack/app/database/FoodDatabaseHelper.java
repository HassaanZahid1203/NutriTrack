package com.nutritrack.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.nutritrack.app.models.Food;

import java.util.ArrayList;
import java.util.List;

/**
 * Local SQLite database for the offline food catalogue.
 * Per SRS §2.5: "Food nutrition data stored locally"
 * Per SRS §1.4: includes localized Pakistani foods.
 */
public class FoodDatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "nutritrack.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_FOODS  = "foods";
    public static final String COL_ID       = "_id";
    public static final String COL_NAME     = "name";
    public static final String COL_EMOJI    = "emoji";
    public static final String COL_KCAL     = "calories";
    public static final String COL_PROTEIN  = "protein";
    public static final String COL_CARBS    = "carbs";
    public static final String COL_FAT      = "fat";
    public static final String COL_UNIT     = "unit";

    public FoodDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_FOODS + " ("
                + COL_ID      + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_NAME    + " TEXT NOT NULL, "
                + COL_EMOJI   + " TEXT, "
                + COL_KCAL    + " REAL, "
                + COL_PROTEIN + " REAL, "
                + COL_CARBS   + " REAL, "
                + COL_FAT     + " REAL, "
                + COL_UNIT    + " TEXT)");
        seedFoods(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FOODS);
        onCreate(db);
    }

    private void seedFoods(SQLiteDatabase db) {
        // Common foods + Pakistani localized meals
        Object[][] seed = new Object[][] {
            // Western basics
            { "Boiled Eggs",      "🥚",  78.0,  6.0, 0.6, 5.3, "piece" },
            { "Banana",           "🍌",  89.0,  1.1, 27.0, 0.3, "100g" },
            { "Whole Milk",       "🥛",  61.0,  3.4, 4.8, 3.7, "100ml" },
            { "Brown Bread",      "🍞", 246.0,  8.0, 44.0, 3.5, "100g" },
            { "Chicken Rice Bowl","🍛", 520.0, 35.0, 58.0, 12.0, "serving" },
            { "Greek Yogurt",     "🥣",  59.0, 10.0, 3.6, 0.4, "100g" },
            { "Apple",            "🍎",  52.0, 0.3, 14.0, 0.2, "100g" },
            { "Almonds",          "🌰", 579.0, 21.0, 22.0, 50.0, "100g" },

            // Pakistani / South Asian (SRS §1.4)
            { "Chicken Biryani",  "🍚", 290.0, 13.0, 32.0, 12.0, "100g" },
            { "Daal Chawal",      "🍲", 175.0, 7.0, 28.0, 4.0,  "100g" },
            { "Chapati / Roti",   "🫓", 297.0, 10.5, 56.0, 7.5, "piece" },
            { "Aloo Paratha",     "🥞", 260.0, 5.0, 35.0, 11.0, "piece" },
            { "Chicken Karahi",   "🍗", 220.0, 18.0, 5.0, 14.0, "100g" },
            { "Beef Nihari",      "🥩", 312.0, 22.0, 6.0, 22.0, "100g" },
            { "Seekh Kebab",      "🍢", 270.0, 18.0, 4.0, 20.0, "piece" },
            { "Samosa",           "🥟", 308.0, 5.0, 32.0, 18.0, "piece" },
            { "Lassi",            "🥤",  98.0, 3.0, 11.0, 4.0,  "100ml" },
            { "Mango",            "🥭",  60.0, 0.8, 15.0, 0.4,  "100g" },
            { "Chai (with milk)", "☕",  88.0, 2.5, 8.0, 5.0,   "cup" },
            { "Naan",             "🫓", 310.0, 9.0, 50.0, 7.0,  "piece" },
        };

        for (Object[] row : seed) {
            ContentValues v = new ContentValues();
            v.put(COL_NAME,    (String) row[0]);
            v.put(COL_EMOJI,   (String) row[1]);
            v.put(COL_KCAL,    (Double) row[2]);
            v.put(COL_PROTEIN, (Double) row[3]);
            v.put(COL_CARBS,   (Double) row[4]);
            v.put(COL_FAT,     (Double) row[5]);
            v.put(COL_UNIT,    (String) row[6]);
            db.insert(TABLE_FOODS, null, v);
        }
    }

    /* ---------- Queries ---------- */

    public List<Food> searchFoods(String query) {
        List<Food> results = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String selection = (query != null && !query.trim().isEmpty())
                ? COL_NAME + " LIKE ?"
                : null;
        String[] args = (selection != null) ? new String[]{ "%" + query.trim() + "%" } : null;

        Cursor c = db.query(TABLE_FOODS, null, selection, args, null, null, COL_NAME);
        while (c.moveToNext()) {
            results.add(cursorToFood(c));
        }
        c.close();
        return results;
    }

    public List<Food> getRecentFoods(int limit) {
        // For now, simply return first N items. A real implementation would track usage.
        List<Food> results = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_FOODS, null, null, null, null, null,
                COL_ID + " ASC", String.valueOf(limit));
        while (c.moveToNext()) {
            results.add(cursorToFood(c));
        }
        c.close();
        return results;
    }

    public Food getFoodById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_FOODS, null, COL_ID + "=?",
                new String[]{ String.valueOf(id) }, null, null, null);
        Food f = c.moveToFirst() ? cursorToFood(c) : null;
        c.close();
        return f;
    }

    private Food cursorToFood(Cursor c) {
        return new Food(
                String.valueOf(c.getLong(c.getColumnIndexOrThrow(COL_ID))),
                c.getString(c.getColumnIndexOrThrow(COL_NAME)),
                c.getString(c.getColumnIndexOrThrow(COL_EMOJI)),
                c.getDouble(c.getColumnIndexOrThrow(COL_KCAL)),
                c.getDouble(c.getColumnIndexOrThrow(COL_PROTEIN)),
                c.getDouble(c.getColumnIndexOrThrow(COL_CARBS)),
                c.getDouble(c.getColumnIndexOrThrow(COL_FAT)),
                c.getString(c.getColumnIndexOrThrow(COL_UNIT))
        );
    }
}

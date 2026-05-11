package com.nutritrack.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.nutritrack.app.databinding.ActivityMainBinding;
import com.nutritrack.app.ui.home.HomeFragment;
import com.nutritrack.app.ui.log.LogFoodFragment;
import com.nutritrack.app.ui.meals.MealsFragment;
import com.nutritrack.app.ui.profile.ProfileFragment;
import com.nutritrack.app.ui.trends.TrendsFragment;

/**
 * Main entry point hosting the 5 bottom-nav destinations from the UI mockups:
 * Home · Meals · Log · Trends · Profile.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        if (savedInstanceState == null) {
            switchTo(new HomeFragment());
            b.bottomNav.setSelectedItemId(R.id.nav_home);
        }

        b.bottomNav.setOnItemSelectedListener(this::onTabSelected);
    }

    private boolean onTabSelected(android.view.MenuItem item) {
        Fragment next;
        int id = item.getItemId();
        if      (id == R.id.nav_home)    next = new HomeFragment();
        else if (id == R.id.nav_meals)   next = new MealsFragment();
        else if (id == R.id.nav_log)     next = new LogFoodFragment();
        else if (id == R.id.nav_trends)  next = new TrendsFragment();
        else if (id == R.id.nav_profile) next = new ProfileFragment();
        else return false;

        switchTo(next);
        return true;
    }

    private void switchTo(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    /** Allow fragments (e.g., HomeFragment quick action) to navigate the bottom nav. */
    public void selectTab(int menuItemId) {
        b.bottomNav.setSelectedItemId(menuItemId);
    }
}

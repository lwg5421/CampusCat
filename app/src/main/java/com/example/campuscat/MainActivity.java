package com.example.campuscat;

import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.campuscat.ui.CatFragment;
import com.example.campuscat.ui.HomeFragment;
import com.example.campuscat.ui.RecordsFragment;
import com.example.campuscat.ui.SettingsFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });
        bottomNav.setOnItemReselectedListener(item -> {
            // 같은 탭을 다시 눌러도 화면을 새로 만들지 않는다.
        });

        if (savedInstanceState == null) {
            showTab(R.id.nav_home);
        }
    }

    /** 다른 화면에서 탭을 전환할 때 사용한다. */
    public void selectTab(@IdRes int itemId) {
        bottomNav.setSelectedItemId(itemId);
    }

    private void showTab(@IdRes int itemId) {
        Fragment fragment;
        if (itemId == R.id.nav_records) {
            fragment = new RecordsFragment();
        } else if (itemId == R.id.nav_cat) {
            fragment = new CatFragment();
        } else if (itemId == R.id.nav_settings) {
            fragment = new SettingsFragment();
        } else {
            fragment = new HomeFragment();
        }
        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}

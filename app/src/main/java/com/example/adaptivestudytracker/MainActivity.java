package com.example.adaptivestudytracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import android.app.AlarmManager;
import android.content.Context;
import android.os.Build;

public class MainActivity extends AppCompatActivity {
    private int currentNavItemId = R.id.nav_dashboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ★ 创建通知渠道
        NotificationHelper.createChannels(this);

        // ★ Android 13+ 请求通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }

        // ★ Android 12+ 请求精确闹钟权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!am.canScheduleExactAlarms()) {
                // 弹出对话框引导用户去设置
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Exact Alarm Permission Needed")
                        .setMessage("To send task reminders on time, please allow exact alarms for this app.")
                        .setPositiveButton("Go to Settings", (dialog, which) -> {
                            Intent intent = new Intent(
                                    android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            intent.setData(android.net.Uri.parse(
                                    "package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("Later", null)
                        .show();
            }
        }

        setContentView(R.layout.activity_main);
        MaterialToolbar topAppBar = findViewById(R.id.top_app_bar);
        setSupportActionBar(topAppBar);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DashboardFragment())
                    .commit();
            setTitle(R.string.title_dashboard);
            currentNavItemId = R.id.nav_dashboard;
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment instanceof FocusFragment && id != R.id.nav_focus) {
                boolean sessionInterrupted = ((FocusFragment) currentFragment).onTabSwitchedAway();
                if (sessionInterrupted) {
                    Snackbar.make(findViewById(R.id.fragment_container), R.string.focus_session_interrupted, Snackbar.LENGTH_SHORT)
                            .setAnchorView(R.id.bottom_navigation)
                            .show();
                }
            }

            currentNavItemId = id;

            if (id == R.id.nav_dashboard) {
                selectedFragment = new DashboardFragment();
                setTitle(R.string.title_dashboard);
            } else if (id == R.id.nav_schedule) {
                selectedFragment = new ScheduleFragment();
                setTitle(R.string.title_schedule);
            } else if (id == R.id.nav_focus) {
                selectedFragment = new FocusFragment();
                setTitle(R.string.title_focus);
            } else if (id == R.id.nav_insights) {
                selectedFragment = new StatisticsFragment();
                setTitle(R.string.title_insights);
            } else if (id == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
                setTitle(R.string.title_settings);
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            invalidateOptionsMenu();
            return true;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem more = menu.findItem(R.id.action_more);
        if (more != null) {
            more.setVisible(currentNavItemId == R.id.nav_schedule);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_task_history) {
            startActivity(new Intent(this, TaskHistoryActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}


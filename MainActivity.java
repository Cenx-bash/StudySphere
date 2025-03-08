package com.example.studysphere;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String PREFERENCES_FILE = "com.example.studysphere.PREFERENCES";
    private static final String KEY_RECENT_ITEMS = "recent_items";
    private static final long EXPIRY_TIME_MS = 24 * 60 * 60 * 1000;
    private static final long MOTIVATION_INTERVAL = 90 * 1000;
    private static final long REMINDER_INTERVAL = 3 * 60 * 60 * 1000L;
    private final Handler handler = new Handler();
    private DrawerLayout drawerLayout;
    private TextView upcomingScheduleTextView;
    private LinearLayout upcomingLayout;
    private int resourceId;    private final Runnable updateMotivationRunnable = this::displayMotivation;
    private ImageView profilePic;
    private SharedPreferences sharedPreferences;
    private Map<String, Integer> weeklyUsage = new HashMap<>();
    private SharedPreferences preferences;
    private static final String PREF_NAME = "WeeklyUsage";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        initializeViews();

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }


        loadWeeklyUsage();
        setupFunctionality();
    }

    private void trackLinkClick(String appName) {
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        int currentCount = preferences.getInt(appName, 0);
        editor.putInt(appName, currentCount + 1);
        editor.apply();
    }

    private void loadWeeklyUsage() {
        SharedPreferences prefs = getSharedPreferences("WeeklyUsage", MODE_PRIVATE);
        weeklyUsage = new HashMap<>();

        for (String key : prefs.getAll().keySet()) {
            weeklyUsage.put(key, prefs.getInt(key, 0));
        }
    }
    private void saveWeeklyUsage() {
        SharedPreferences prefs = getSharedPreferences("WeeklyUsage", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        for (Map.Entry<String, Integer> entry : weeklyUsage.entrySet()) {
            editor.putInt(entry.getKey(), entry.getValue());
        }

        editor.apply();
    }
    private String readRawFile(int gmsFontsCertsProd) {
        InputStream inputStream = getResources().openRawResource(resourceId);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;

        try {
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stringBuilder.toString().trim();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        upcomingScheduleTextView = findViewById(R.id.upcomingScheduleTextView);
        upcomingLayout = findViewById(R.id.upcomingLayout);
        Button reportButton = findViewById(R.id.reportButton);
        reportButton.setOnClickListener(view -> showReportPopup());


        if (drawerLayout == null || upcomingScheduleTextView == null || upcomingLayout == null) {
            Log.e("MainActivity", "One or more views not found");
            finish();
        }
    }

    private void applyButtonAnimation(Button button) {
        button.setOnClickListener(v -> {
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100));
        });
    }

    private void setupFunctionality() {
        createDynamicButtons();
        displayRecentOpenedItems();
        scheduleReminder();
        displayMotivation();
        scheduleMotivationNotifications();
        loadScheduledAlarm();
        handler.postDelayed(updateMotivationRunnable, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadScheduledAlarm();
        handler.postDelayed(updateMotivationRunnable, 0);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateMotivationRunnable);
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.aboutButton) {
            Intent intent = new Intent(this, About.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.contactButton) {
            Intent intent = new Intent(this, Contact.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.drawer_menu, menu);
        return true;
    }

    private void loadScheduledAlarm() {
        SharedPreferences prefs = getSharedPreferences("AlarmPrefs", MODE_PRIVATE);
        Set<String> scheduledAlarms = prefs.getStringSet("scheduledAlarms", new HashSet<>());

        if (upcomingLayout == null) {
            Log.e(TAG, "upcomingLayout is null. Cannot update UI.");
            return;
        }

        upcomingLayout.removeAllViews();
        long currentTime = System.currentTimeMillis();
        List<String> sortedAlarms = new ArrayList<>();

        if (scheduledAlarms == null || scheduledAlarms.isEmpty()) {
            showNoAlarmsMessage();
            return;
        }

        for (String alarmEntry : scheduledAlarms) {
            String[] parts = alarmEntry.split("\\|");
            if (parts.length == 3) {
                try {
                    long alarmTime = Long.parseLong(parts[1]);
                    if (alarmTime > currentTime) {
                        sortedAlarms.add(alarmEntry);
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid alarm time format: " + parts[1], e);
                }
            } else {
                Log.e(TAG, "Invalid alarm entry format: " + alarmEntry);
            }
        }

        Collections.sort(sortedAlarms, (a, b) -> {
            long timeA = Long.parseLong(a.split("\\|")[1]);
            long timeB = Long.parseLong(b.split("\\|")[1]);
            return Long.compare(timeA, timeB);
        });

        if (sortedAlarms.isEmpty()) {
            showNoAlarmsMessage();
        } else {
            for (String alarmEntry : sortedAlarms) {
                String[] parts = alarmEntry.split("\\|");
                addAlarmView(parts[0], parts[1], parts[2]);
            }
        }
    }

    private void addAlarmView(String label, String time, String date) {
        runOnUiThread(() -> {
            TextView textView = new TextView(this);
            textView.setText(label + " - " + formatTime(time) + " - " + date);
            textView.setTextSize(16);
            textView.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            textView.setPadding(30, 20, 30, 20);
            upcomingLayout.addView(textView);
            Log.d(TAG, "Added alarm to UI: " + label + " - " + formatTime(time) + " - " + date);
        });
    }


    private void showNoAlarmsMessage() {
        runOnUiThread(() -> {
            TextView noAlarmsText = new TextView(this);
            noAlarmsText.setText("No upcoming schedule");
            noAlarmsText.setTextSize(16);
            noAlarmsText.setTypeface(Typeface.DEFAULT_BOLD);
            noAlarmsText.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            noAlarmsText.setGravity(Gravity.CENTER);
            noAlarmsText.setPadding(50, 20, 50, 20);

            // Center the TextView within its parent
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.gravity = Gravity.CENTER;
            noAlarmsText.setLayoutParams(layoutParams);

            if (upcomingLayout instanceof LinearLayout) {
                ((LinearLayout) upcomingLayout).setGravity(Gravity.CENTER);
            }

            upcomingLayout.removeAllViews();
            upcomingLayout.addView(noAlarmsText);

            Log.d("AlarmLoader", "Displayed 'No upcoming schedule' message.");
        });
    }


    private String formatTime(String timestamp) {
        try {
            long timeMillis = Long.parseLong(timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            return sdf.format(new Date(timeMillis));
        } catch (NumberFormatException e) {
            Log.e("AlarmLoader", "Invalid timestamp: " + timestamp, e);
            return "Invalid Time";
        }
    }
    private void showReportPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📊 Weekly App Usage");

        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        Map<String, Integer> usageData = getTrackedUsage();

        // Create a layout for better styling
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30);

        if (usageData.isEmpty()) {
            TextView noDataText = new TextView(this);
            noDataText.setText("🚫 No usage data available.");
            noDataText.setTextSize(16);
            noDataText.setTypeface(Typeface.DEFAULT_BOLD);
            noDataText.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            layout.addView(noDataText);
        } else {
            for (Map.Entry<String, Integer> entry : usageData.entrySet()) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 10, 0, 10);
                row.setGravity(Gravity.CENTER_VERTICAL);

                TextView icon = new TextView(this);
                icon.setText("🔹 ");
                icon.setTextSize(18);
                row.addView(icon);

                TextView appName = new TextView(this);
                appName.setText(entry.getKey() + ": ");
                appName.setTextSize(16);
                appName.setTypeface(Typeface.DEFAULT_BOLD);
                appName.setTextColor(ContextCompat.getColor(this, android.R.color.black));
                row.addView(appName);

                TextView usageCount = new TextView(this);
                usageCount.setText(String.valueOf(entry.getValue()));
                usageCount.setTextSize(16);
                usageCount.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
                usageCount.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
                row.addView(usageCount);

                layout.addView(row);
            }
        }

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(layout);
        builder.setView(scrollView);

        builder.setPositiveButton("✅ Close", (dialog, which) -> dialog.dismiss());

        runOnUiThread(() -> {
            AlertDialog dialog = builder.create();
            dialog.show();
        });
    }

    // Retrieve data from SharedPreferences
    private Map<String, Integer> getTrackedUsage() {
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        Map<String, Integer> data = new HashMap<>();

        Map<String, ?> allEntries = preferences.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getValue() instanceof Integer) {
                data.put(entry.getKey(), (Integer) entry.getValue());
            }
        }
        return data;
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(new Date(timestamp));
    }

    private void scheduleMotivationNotifications() {
        Intent intent = new Intent(this, MotivationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1001, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + MOTIVATION_INTERVAL,
                    MOTIVATION_INTERVAL,
                    pendingIntent
            );
        }
    }

    private void displayMotivation() {
        String[] motivationalQuotes = {
                "Believe in yourself and all that you are.",
                "The secret of getting ahead is getting started.",
                "Your only limit is your mind.",
                "Do something today that your future self will thank you for.",
                "Success is not final, failure is not fatal: it is the courage to continue that counts.",
                "Dream big and dare to fail.",
                "Great things never come from comfort zones.",
                "Push yourself, because no one else is going to do it for you.",
                "Don’t stop when you’re tired. Stop when you’re done.",
                "Little by little, one travels far.",
                "Hard work beats talent when talent doesn’t work hard.",
                "Your future is created by what you do today, not tomorrow.",
                "You don’t have to be great to start, but you have to start to be great.",
                "Discipline is the bridge between goals and accomplishment.",
                "Doubt kills more dreams than failure ever will.",
                "You are capable of amazing things.",
                "The way to get started is to quit talking and begin doing.",
                "Everything you’ve ever wanted is on the other side of fear.",
                "You are stronger than you think.",
                "No matter how slow you go, you’re still lapping everyone on the couch.",
                "Difficult roads often lead to beautiful destinations.",
                "Believe in your infinite potential. Your only limitations are those you set upon yourself.",
                "Don’t wait for opportunity. Create it.",
                "It’s going to be hard, but hard does not mean impossible.",
                "The harder you work for something, the greater you'll feel when you achieve it.",
                "Failure is the opportunity to begin again, only this time more wisely.",
                "You don't have to be perfect to be amazing.",
                "Never let small minds convince you that your dreams are too big.",
                "You are never too old to set another goal or to dream a new dream.",
                "Everything is hard before it is easy.",
                "It’s not about how fast you run but how far you go.",
                "Don’t be pushed around by the fears in your mind. Be led by the dreams in your heart.",
                "Act as if what you do makes a difference. It does.",
                "Happiness is not something ready-made. It comes from your own actions.",
                "Perseverance is not a long race; it is many short races one after another.",
                "You miss 100% of the shots you don’t take.",
                "A little progress each day adds up to big results.",
                "Be somebody nobody thought you could be.",
                "The best time to plant a tree was 20 years ago. The second best time is now.",
                "Start where you are. Use what you have. Do what you can.",
                "Courage doesn’t always roar. Sometimes it’s the quiet voice at the end of the day saying, ‘I will try again tomorrow.’",
                "Every accomplishment starts with the decision to try.",
                "One day or day one. You decide.",
                "Opportunities don't happen, you create them.",
                "Everything you can imagine is real.",
                "There are no limits to what you can accomplish, except the limits you place on your own thinking.",
                "Turn your wounds into wisdom.",
                "Believe that you will succeed, and you will.",
                "That's the beauty of love, knowing the risk of heartbreak but still fall in love anyway bc it will be worth it at the end of it all.",
                "Small steps in the right direction can turn out to be the biggest step of your life.",
                "Make your life a masterpiece; imagine no limitations on what you can be, have, or do.",
                // Additional Motivation Quotes
                "The only way to do great work is to love what you do.",
                "Don’t watch the clock; do what it does. Keep going.",
                "Success is the sum of small efforts, repeated day in and day out.",
                "The pain you feel today will be the strength you feel tomorrow.",
                "You were born to win, but to be a winner, you must plan to win, prepare to win, and expect to win.",
                "The only limit to our realization of tomorrow is our doubts of today.",
                "The best revenge is massive success.",
                "If you want to achieve greatness, stop asking for permission.",
                "The only place where success comes before work is in the dictionary.",
                "You don’t have to see the whole staircase, just take the first step.",
                // Love Quotes
                "Love is not about how much you say 'I love you,' but how much you prove that it’s true.",
                "The best thing to hold onto in life is each other.",
                "Love is when the other person’s happiness is more important than your own.",
                "You don’t love someone for their looks, or their clothes, or for their fancy car, but because they sing a song only you can hear.",
                "Love is composed of a single soul inhabiting two bodies.",
                "In all the world, there is no heart for me like yours. In all the world, there is no love for you like mine.",
                "Love isn’t something you find. Love is something that finds you.",
                "I have found the one whom my soul loves.",
                "Love is like the wind, you can’t see it, but you can feel it.",
                "To love and be loved is to feel the sun from both sides.",
                // Deep Quotes
                "The soul becomes dyed with the color of its thoughts.",
                "We are not human beings having a spiritual experience. We are spiritual beings having a human experience.",
                "The deeper the sorrow, the greater the joy.",
                "The purpose of life is not to be happy. It is to be useful, to be honorable, to be compassionate, to have it make some difference that you have lived and lived well.",
                "The unexamined life is not worth living.",
                "What lies behind us and what lies before us are tiny matters compared to what lies within us.",
                "The only true wisdom is in knowing you know nothing.",
                "The mind is everything. What you think, you become.",
                "The greatest glory in living lies not in never falling, but in rising every time we fall.",
                "The two most important days in your life are the day you are born and the day you find out why.",
                // Broken Quotes
                "Sometimes, you have to let go of the picture of what you thought life would be like and learn to find joy in the story you are actually living.",
                "The hardest part of loving someone is knowing when to let go.",
                "You can love someone with all your heart, but you can’t force them to stay.",
                "Sometimes, the person you’d take a bullet for ends up being the one behind the gun.",
                "It hurts to breathe because every breath proves I’m still alive without you.",
                "The saddest thing about love is that not only the love cannot last forever, but even the heartbreak is soon forgotten.",
                "I didn’t lose you. You lost me. You’ll see.",
                "The worst feeling is not being lonely. It’s being forgotten by someone you’ll never forget.",
                "You broke me, but I’m the one who’s picking up the pieces.",
                "I gave you my heart, and you gave me the art of letting go.",
                // Combination of All
                "Love is the most powerful motivator, but it’s also the deepest wound.",
                "Sometimes, the bravest thing you can do is to love again after being broken.",
                "The scars you can’t see are the hardest to heal.",
                "You can’t start the next chapter of your life if you keep re-reading the last one.",
                "The strongest hearts are the ones that have been broken but still beat with hope.",
                "Love is worth the risk, even if it ends in heartbreak, because it teaches you how to rise again.",
                "The deeper the pain, the more room there is for growth and love.",
                "You are not defined by your past. You are prepared by it.",
                "The most beautiful people are those who have known defeat, known suffering, known struggle, known loss, and have found their way out of the depths.",
                "Love is the light that guides you through the darkest nights, even when it feels like the stars have burned out."
        };

        TextView motivationTextView = findViewById(R.id.motivationText);
        if (motivationTextView != null) {
            motivationTextView.setText(motivationalQuotes[new Random().nextInt(motivationalQuotes.length)]);
        }
        handler.postDelayed(updateMotivationRunnable, MOTIVATION_INTERVAL);
    }

    private void scheduleReminder() {
        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + REMINDER_INTERVAL,
                    REMINDER_INTERVAL,
                    pendingIntent
            );
        }
    }

    public void openScheduleActivity(View view) {
        startActivity(new Intent(this, Schedule.class));
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }

    public void openMainNoteActivity(View view) {
        startActivity(new Intent(this, MainNote.class));
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }

    public void openProfile_User(View view) {
        startActivity(new Intent(this, Profile_User.class));
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }

    private void openNewItem(String itemTitle) {
        if (!itemTitle.equals("Others")) {
            saveRecentOpenedInfo(itemTitle);
        }
        displayRecentOpenedItems();
    }

    private void saveRecentOpenedInfo(String title) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        long currentTimeMillis = System.currentTimeMillis();
        String timeFormatted = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(currentTimeMillis));
        String dateFormatted = new SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(new Date(currentTimeMillis));

        String itemWithTimestamp = title + " - " + timeFormatted + " - " + dateFormatted;
        Map<String, Long> recentItems = getRecentItems(sharedPreferences);
        recentItems.put(itemWithTimestamp, currentTimeMillis);
        saveRecentItems(editor, recentItems);
    }

    private void displayRecentOpenedItems() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        Map<String, Long> recentItems = getRecentItems(sharedPreferences);

        long currentTimeMillis = System.currentTimeMillis();
        LinearLayout recentOpenedLayout = findViewById(R.id.recentOpenedLayout);
        if (recentOpenedLayout != null) {
            recentOpenedLayout.removeAllViews();

            for (Map.Entry<String, Long> entry : recentItems.entrySet()) {
                long savedTimeMillis = entry.getValue();
                if (currentTimeMillis - savedTimeMillis <= EXPIRY_TIME_MS) {
                    String[] itemParts = entry.getKey().split(" - ");
                    if (itemParts.length == 3) {
                        LinearLayout itemLayout = createRecentItemLayout(itemParts[0], itemParts[1], itemParts[2]);
                        recentOpenedLayout.addView(itemLayout);
                    }
                }
            }
        }
    }

    private LinearLayout createRecentItemLayout(String title, String time, String date) {
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        itemLayout.setPadding(8, 8, 8, 8);

        TextView titleTextView = new TextView(this);
        titleTextView.setText(title);
        titleTextView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        itemLayout.addView(titleTextView);

        TextView timeTextView = new TextView(this);
        timeTextView.setText(time);
        timeTextView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        timeTextView.setGravity(Gravity.CENTER);
        itemLayout.addView(timeTextView);

        TextView dateTextView = new TextView(this);
        dateTextView.setText(date);
        dateTextView.setGravity(Gravity.RIGHT);
        dateTextView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        itemLayout.addView(dateTextView);

        return itemLayout;
    }

    private Map<String, Long> getRecentItems(SharedPreferences sharedPreferences) {
        Map<String, Long> recentItems = new HashMap<>();
        Set<String> recentItemsSet = sharedPreferences.getStringSet(KEY_RECENT_ITEMS, null);

        if (recentItemsSet != null) {
            for (String item : recentItemsSet) {
                String[] itemParts = item.split(";");
                if (itemParts.length == 2) {
                    recentItems.put(itemParts[0], Long.parseLong(itemParts[1]));
                }
            }
        }
        return recentItems;
    }

    private void saveRecentItems(SharedPreferences.Editor editor, Map<String, Long> recentItems) {
        Set<String> recentItemsSet = new HashSet<>();
        for (Map.Entry<String, Long> entry : recentItems.entrySet()) {
            recentItemsSet.add(entry.getKey() + ";" + entry.getValue());
        }
        editor.putStringSet(KEY_RECENT_ITEMS, recentItemsSet);
        editor.apply();
    }

    private void createDynamicButtons() {
        LinearLayout contentLayout = findViewById(R.id.dynamicButtonsLayout);
        String[] buttonNames = {"Elms", "Google", "Outlook", "Others"};

        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);
        horizontalScrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        for (String buttonName : buttonNames) {
            Button button = createButton(buttonName);
            buttonRow.addView(button);
        }

        horizontalScrollView.addView(buttonRow);
        contentLayout.removeAllViews();
        contentLayout.addView(horizontalScrollView);
    }

    private Button createButton(String buttonName) {
        Button button = new Button(this);
        button.setText(buttonName);
        button.setTextSize(15);
        button.setAllCaps(true);
        button.setTextColor(Color.WHITE);
        button.setBackgroundResource(R.drawable.button_ripple);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(10, 5, 10, 5);
        button.setLayoutParams(layoutParams);
        button.setPadding(50, 20, 50, 20);

        button.setOnClickListener(v -> handleButtonClick(buttonName));
        return button;
    }

    private void handleButtonClick(String buttonName) {
        if (buttonName.equals("Others")) {
            showSeeAllPopup();
        } else {
            openNewItem(buttonName);
            openWebsite(buttonName);
        }
    }

    private void showSeeAllPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("See Others");
        String[] options = {"Google Drive", "Canva", "Grammarly", "Youtube", "One Sti", "ChatGpt", "QuillBot"};

        builder.setItems(options, (dialog, which) -> {
            String selectedOption = options[which];
            openNewItem(selectedOption);
            openWebsite(selectedOption);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void openWebsite(String buttonName) {
        String url = "";

        trackLinkClick(buttonName);

        switch (buttonName) {
            case "Elms":
                url = "https://elms.sti.edu";
                trackLinkClick("Elms");
                break;
            case "Google":
                url = "https://www.google.com";
                trackLinkClick("Google");
                break;
            case "Outlook":
                url = "https://www.outlook.com";
                trackLinkClick("Outlook");
                break;
            case "Google Drive":
                url = "https://drive.google.com";
                trackLinkClick("Google Drive");
                break;
            case "Canva":
                url = "https://www.canva.com";
                trackLinkClick("Canva");
                break;
            case "Grammarly":
                url = "https://www.grammarly.com";
                trackLinkClick("Grammarly");
                break;
            case "YouTube":
                url = "https://www.youtube.com";
                trackLinkClick("YouTube");
                break;
            case "One STI":
                url = "https://www.sti.edu";
                trackLinkClick("One STI");
                break;
            case "ChatGPT":
                url = "https://www.chat.openai.com";
                trackLinkClick("ChatGPT");
                break;
            case "QuillBot":
                url = "https://www.quillbot.com";
                trackLinkClick("QuillBot");
                break;
            default:
                url = "https://www.example.com";
                break;
        }

        if (!url.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
}
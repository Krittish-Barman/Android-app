package com.krittish.dailytracker.ui.settings;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricManager;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.krittish.dailytracker.DailyTrackerApp;
import com.krittish.dailytracker.R;
import com.krittish.dailytracker.data.prefs.SettingsRepository;
import com.krittish.dailytracker.databinding.FragmentSettingsBinding;
import com.krittish.dailytracker.notifications.NotificationHelper;
import com.krittish.dailytracker.notifications.NotificationScheduler;
import com.krittish.dailytracker.sms.SmsImporter;
import com.krittish.dailytracker.ui.common.Dialogs;
import com.krittish.dailytracker.util.Callback;
import com.krittish.dailytracker.util.Constants;
import com.krittish.dailytracker.util.DateUtils;
import com.krittish.dailytracker.util.MoneyFormat;

import java.util.Map;

/** Rows are built in code, so adding a new setting is only a few lines. */
public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding b;
    private SettingsRepository s;
    private MaterialSwitch smsSwitch;

    private final ActivityResultLauncher<String[]> smsPermission =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), (Map<String, Boolean> result) -> {
                boolean ok = Boolean.TRUE.equals(result.get(Manifest.permission.RECEIVE_SMS))
                        && Boolean.TRUE.equals(result.get(Manifest.permission.READ_SMS));
                s.setAutoDetectSms(ok);
                if (smsSwitch != null) smsSwitch.setChecked(ok);
                if (!ok && getContext() != null) {
                    Dialogs.info(requireContext(), "SMS permission not granted",
                            "If Android blocks it for a sideloaded app: Settings > Apps > Daily Tracker > "
                                    + "(three dots) > Allow restricted settings. Then try again.");
                }
            });

    private final ActivityResultLauncher<String> notifPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (b != null) build();
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentSettingsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        s = SettingsRepository.get(requireContext());
        b.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        build();
    }

    private void build() {
        b.container.removeAllViews();

        // ---------------- notifications ----------------
        header("Notifications");
        if (!NotificationHelper.canNotify(requireContext())) {
            value("Notifications are blocked", "Allow", v -> {
                if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            });
        }
        String[][] types = {
                {Constants.NT_NEXT_UP, "Next up (before habits and tasks)"},
                {Constants.NT_MORNING, "Morning plan"},
                {Constants.NT_EVENING, "Evening check"},
                {Constants.NT_STREAK, "Streak at risk"},
                {Constants.NT_BUDGET, "Budget alerts (80% and 100%)"},
                {Constants.NT_WEEKLY, "Weekly summary (Sunday)"},
                {Constants.NT_FOCUS, "Focus session done"},
                {Constants.NT_DETECTED, "Detected expenses"}
        };
        for (String[] t : types) toggle(t[1], s.notifOn(t[0]), on -> s.setNotifOn(t[0], on));

        TextView lead = value("Next up reminder", s.leadMinutes() + " min before", null);
        ((View) lead.getParent()).setOnClickListener(v -> {
            String[] labels = {"5 min", "10 min", "15 min", "30 min"};
            int[] mins = {5, 10, 15, 30};
            new MaterialAlertDialogBuilder(requireContext()).setTitle("Remind me before").setItems(labels, (d, which) -> {
                s.setLeadMinutes(mins[which]);
                lead.setText(mins[which] + " min before");
                NotificationScheduler.refreshAsync(requireContext());
            }).show();
        });
        timeRow("Morning plan time", s.morningTime(), m -> {
            s.setMorningTime(m);
            NotificationScheduler.scheduleWork(requireContext(), true);
        });
        timeRow("Evening check time", s.eveningTime(), m -> {
            s.setEveningTime(m);
            NotificationScheduler.scheduleWork(requireContext(), true);
        });
        toggle("Quiet hours", s.quietOn(), s::setQuietOn);
        timeRow("Quiet from", s.quietStart(), s::setQuietStart);
        timeRow("Quiet until", s.quietEnd(), s::setQuietEnd);
        toggle("Always-visible \"Up next\" notification", s.upNextCard(), on -> {
            s.setUpNextCard(on);
            NotificationScheduler.refreshAsync(requireContext());
        });
        value("Reminders not on time?", "Battery help", v -> batteryHelp());

        // ---------------- money ----------------
        header("Money");
        smsSwitch = toggle("Auto-detect expenses from SMS", s.autoDetectSms(), this::onAutoDetect);
        TextView senders = value("Allowed SMS senders", s.smsSenders().isEmpty() ? "none yet" : s.smsSenders(), null);
        ((View) senders.getParent()).setOnClickListener(v -> Dialogs.promptText(requireContext(),
                "Allowed senders", "e.g. HDFCBK, SBIUPI, AXISBK", s.smsSenders(), InputType.TYPE_CLASS_TEXT, text -> {
                    s.setSmsSenders(text);
                    senders.setText(text.isEmpty() ? "none yet" : text);
                }));
        TextView budget = value("Monthly budget", MoneyFormat.format(s.monthlyBudgetPaise()), null);
        ((View) budget.getParent()).setOnClickListener(v -> Dialogs.promptNumber(requireContext(),
                "Monthly budget (₹)", "e.g. 6000", s.monthlyBudgetPaise() / 100, rupees -> {
                    s.setMonthlyBudgetPaise(rupees * 100);
                    budget.setText(MoneyFormat.format(rupees * 100));
                }));
        value("Import old SMS", "Last 30-90 days", v -> importOldSms());

        // ---------------- focus ----------------
        header("Focus");
        TextView goal = value("Daily focus goal", s.focusGoal() + " min", null);
        ((View) goal.getParent()).setOnClickListener(v -> Dialogs.promptNumber(requireContext(),
                "Daily goal (minutes)", "e.g. 120", s.focusGoal(), mins -> {
                    s.setFocusGoal((int) Math.max(10, Math.min(1000, mins)));
                    goal.setText(s.focusGoal() + " min");
                }));
        toggle("Mark session \"interrupted\" if I leave the app for 10+ seconds", s.interruptedRule(), s::setInterruptedRule);

        // ---------------- privacy & data ----------------
        header("Privacy & data");
        MaterialSwitch lock = toggle("Fingerprint / screen lock", s.lockOn(), null);
        lock.setOnCheckedChangeListener((btn, on) -> {
            if (on) {
                int auth = BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
                if (BiometricManager.from(requireContext()).canAuthenticate(auth) != BiometricManager.BIOMETRIC_SUCCESS) {
                    Toast.makeText(requireContext(), "Set a fingerprint or screen lock on your phone first", Toast.LENGTH_LONG).show();
                    lock.setChecked(false);
                    return;
                }
                DailyTrackerApp.markUnlocked();
            }
            s.setLockOn(on);
        });
        TextView theme = value("Theme", s.theme(), null);
        ((View) theme.getParent()).setOnClickListener(v -> {
            String[] keys = {"system", "light", "dark"};
            String[] labels = {"Follow system", "Light", "Dark"};
            new MaterialAlertDialogBuilder(requireContext()).setTitle("Theme").setItems(labels, (d, which) -> {
                s.setTheme(keys[which]);
                theme.setText(keys[which]);
                AppCompatDelegate.setDefaultNightMode(DailyTrackerApp.nightMode(keys[which]));
            }).show();
        });
        value("Export / import data", "Phase 4", v -> Dialogs.info(requireContext(), "Coming in Phase 4",
                "Backup export and import will be added in Phase 4 of the build plan."));

        TextView version = new TextView(requireContext());
        version.setText("Daily Tracker · v1.1 (Java)");
        version.setPadding(0, 32, 0, 0);
        version.setTextAppearance(R.style.TextAppearance_DT_Caption);
        b.container.addView(version);
    }

    // ---------------------------------------------------------------- row helpers
    private void header(String text) {
        TextView t = (TextView) getLayoutInflater().inflate(R.layout.item_setting_header, b.container, false);
        t.setText(text);
        b.container.addView(t);
    }

    private MaterialSwitch toggle(String label, boolean checked, @Nullable Callback<Boolean> onChange) {
        MaterialSwitch sw = (MaterialSwitch) getLayoutInflater().inflate(R.layout.item_setting_switch, b.container, false);
        sw.setText(label);
        sw.setChecked(checked);
        if (onChange != null) sw.setOnCheckedChangeListener((btn, on) -> onChange.on(on));
        b.container.addView(sw);
        return sw;
    }

    private TextView value(String label, String value, @Nullable View.OnClickListener click) {
        View row = getLayoutInflater().inflate(R.layout.item_setting_value, b.container, false);
        ((TextView) row.findViewById(R.id.tvLabel)).setText(label);
        TextView v = row.findViewById(R.id.tvValue);
        v.setText(value);
        if (click != null) row.setOnClickListener(click);
        b.container.addView(row);
        return v;
    }

    private void timeRow(String label, int minute, Callback<Integer> onPicked) {
        TextView v = value(label, DateUtils.formatTime(minute), null);
        final int[] current = {minute};
        ((View) v.getParent()).setOnClickListener(x -> Dialogs.pickTime(requireContext(), current[0], m -> {
            current[0] = m;
            v.setText(DateUtils.formatTime(m));
            onPicked.on(m);
        }));
    }

    // ---------------------------------------------------------------- actions
    private void onAutoDetect(boolean on) {
        if (!on) {
            s.setAutoDetectSms(false);
            return;
        }
        boolean granted = hasSmsPermission();
        if (granted) {
            s.setAutoDetectSms(true);
            return;
        }
        smsSwitch.setChecked(false);    // stays off until the permission is granted
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Read bank SMS on this phone?")
                .setMessage("The app reads only SMS from the senders YOU allow (add them in \"Allowed SMS senders\"). "
                        + "Everything is processed on your phone. The app has no internet permission, so nothing can be uploaded. "
                        + "Raw SMS text is deleted after you confirm an entry.")
                .setPositiveButton("Allow", (d, w) -> smsPermission.launch(
                        new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS}))
                .setNegativeButton("Not now", null).show();
    }

    private boolean hasSmsPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void importOldSms() {
        if (!hasSmsPermission()) {
            Dialogs.info(requireContext(), "Turn on auto-detect first", "Allow the SMS permission by turning on \"Auto-detect expenses from SMS\".");
            return;
        }
        if (s.smsSenders().trim().isEmpty()) {
            Dialogs.info(requireContext(), "Add senders first", "Add your bank sender ids in \"Allowed SMS senders\" before importing.");
            return;
        }
        String[] labels = {"Last 30 days", "Last 60 days", "Last 90 days"};
        int[] days = {30, 60, 90};
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Import old SMS").setItems(labels, (d, which) -> {
            LinearProgressIndicator bar = new LinearProgressIndicator(requireContext());
            bar.setIndeterminate(false);
            FrameLayout box = new FrameLayout(requireContext());
            box.setPadding(48, 24, 48, 0);
            box.addView(bar);
            AlertDialog progress = new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Reading SMS...").setView(box).setCancelable(false).show();
            SmsImporter.importLastDays(requireContext(), days[which], new SmsImporter.Progress() {
                @Override public void onProgress(int done, int total) {
                    bar.setMax(Math.max(1, total));
                    bar.setProgress(done);
                }

                @Override public void onFinished(int created) {
                    progress.dismiss();
                    if (getContext() != null) {
                        Toast.makeText(requireContext(), created + " entries are waiting in Money > Review", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }).show();
    }

    private void batteryHelp() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Make reminders reliable")
                .setMessage("Some phones (Xiaomi, Oppo, Vivo, Realme...) stop background apps.\n\n"
                        + "1. Open App info > Battery > set to Unrestricted.\n"
                        + "2. Turn on Auto-start for Daily Tracker.\n"
                        + "3. Allow \"Alarms & reminders\" so times are exact.")
                .setPositiveButton("Open app settings", (d, w) -> startActivity(
                        new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + requireContext().getPackageName()))))
                .setNeutralButton("Alarm permission", (d, w) -> {
                    if (Build.VERSION.SDK_INT >= 31) {
                        startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:" + requireContext().getPackageName())));
                    }
                })
                .setNegativeButton("Close", null).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        smsSwitch = null;
        b = null;
    }
}

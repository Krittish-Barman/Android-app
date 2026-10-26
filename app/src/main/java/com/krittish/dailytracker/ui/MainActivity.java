package com.krittish.dailytracker.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.krittish.dailytracker.DailyTrackerApp;
import com.krittish.dailytracker.R;
import com.krittish.dailytracker.data.prefs.SettingsRepository;
import com.krittish.dailytracker.databinding.ActivityMainBinding;
import com.krittish.dailytracker.ui.money.AddEntryBottomSheet;
import com.krittish.dailytracker.util.Constants;

/** One Activity, many Fragments. Hosts the NavHostFragment and the 5 bottom tabs. */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController nav;
    private boolean prompting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, 0);   // the bottom bar handles the bottom inset itself
            return insets;
        });

        NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.navHost);
        nav = host.getNavController();
        NavigationUI.setupWithNavController(binding.bottomNav, nav);
        nav.addOnDestinationChangedListener((controller, destination, args) -> {
            int id = destination.getId();
            boolean tab = id == R.id.homeFragment || id == R.id.habitsFragment || id == R.id.moneyFragment
                    || id == R.id.focusFragment || id == R.id.statsFragment;
            binding.bottomNav.setVisibility(tab ? View.VISIBLE : View.GONE);
        });

        binding.btnUnlock.setOnClickListener(v -> promptBiometric());

        if (savedInstanceState == null) {
            if (!SettingsRepository.get(this).onboardingDone()) {
                nav.navigate(R.id.onboardingFragment, null,
                        new NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build());
            }
            handleIntent(getIntent());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (DailyTrackerApp.needsLock(this)) {
            binding.lockOverlay.setVisibility(View.VISIBLE);
            promptBiometric();
        } else {
            binding.lockOverlay.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    /** Jump to a tab / screen when a notification is tapped. */
    private void handleIntent(Intent intent) {
        if (intent == null) return;
        String open = intent.getStringExtra(Constants.EXTRA_OPEN);
        if (open == null) return;
        intent.removeExtra(Constants.EXTRA_OPEN);
        switch (open) {
            case Constants.OPEN_HABITS: openTab(R.id.habitsFragment); break;
            case Constants.OPEN_MONEY: openTab(R.id.moneyFragment); break;
            case Constants.OPEN_FOCUS: openTab(R.id.focusFragment); break;
            case Constants.OPEN_STATS: openTab(R.id.statsFragment); break;
            case Constants.OPEN_REVIEW:
                openTab(R.id.moneyFragment);
                nav.navigate(R.id.reviewFragment);
                break;
            case Constants.OPEN_ADD_EXPENSE:
                openTab(R.id.moneyFragment);
                new AddEntryBottomSheet().show(getSupportFragmentManager(), "add_entry");
                break;
            case Constants.OPEN_TASKS:
                openTab(R.id.homeFragment);
                nav.navigate(R.id.tasksFragment);
                break;
            default: openTab(R.id.homeFragment); break;
        }
    }

    public void openTab(int menuId) {
        binding.bottomNav.setSelectedItemId(menuId);
    }

    // ---------------------------------------------------------------- fingerprint lock
    private void promptBiometric() {
        if (prompting) return;
        final int auth = BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        if (BiometricManager.from(this).canAuthenticate(auth) != BiometricManager.BIOMETRIC_SUCCESS) {
            // phone has no fingerprint / screen lock: do not lock the user out
            DailyTrackerApp.markUnlocked();
            binding.lockOverlay.setVisibility(View.GONE);
            return;
        }
        prompting = true;
        BiometricPrompt prompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        prompting = false;
                        DailyTrackerApp.markUnlocked();
                        binding.lockOverlay.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        prompting = false;   // overlay stays; tap "Unlock" to try again
                    }
                });
        prompt.authenticate(new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Daily Tracker")
                .setAllowedAuthenticators(auth)
                .build());
    }
}

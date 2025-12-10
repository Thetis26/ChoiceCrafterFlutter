package com.choicecrafter.students.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.choicecrafter.students.R;
import com.choicecrafter.students.databinding.FragmentSettingsBinding;
import com.choicecrafter.students.models.NotificationType;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public class SettingsFragment extends Fragment {

    private static final String PREFERENCES_NAME = "settings";
    private static final String KEY_APP_LANG = "app_lang";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_FONT_SCALE = "font_scale";
    private static final float DEFAULT_FONT_SCALE = 1.0f;
    private static final float FONT_SCALE_TOLERANCE = 0.01f;
    private static final float[] FONT_SCALE_VALUES = {0.9f, DEFAULT_FONT_SCALE, 1.15f};
    private FragmentSettingsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        SettingsViewModel settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        final TextView textView = binding.textSlideshow;
        settingsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        requireActivity().setTitle("Settings");

        setupLanguageSpinner();
        setupFontSizeSpinner();
        setupNotificationSwitches();
        setupThemeToggle();
        return root;
    }

    private void setupFontSizeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.font_size_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.fontSizeSpinner.setAdapter(adapter);

        binding.fontSizeSpinner.setSelection(getFontScaleSelection());
        binding.fontSizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean isInitialSelection = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isInitialSelection) {
                    isInitialSelection = false;
                    return;
                }
                if (position >= 0 && position < FONT_SCALE_VALUES.length) {
                    updateFontScale(FONT_SCALE_VALUES[position]);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });
    }

    private void setupLanguageSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.language_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.languageSpinner.setAdapter(adapter);

        String savedLanguage = getSavedLanguage();
        int selection = "ro".equals(savedLanguage) ? 1 : 0;
        binding.languageSpinner.setSelection(selection);

        binding.languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean isInitialSelection = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isInitialSelection) {
                    isInitialSelection = false;
                    return;
                }

                String selectedLanguage = position == 1 ? "ro" : "en";
                if (!selectedLanguage.equals(getSavedLanguage())) {
                    setLocale(selectedLanguage);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });
    }

    private void setupNotificationSwitches() {
        final SharedPreferences preferences = requireContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        Map<NotificationType, SwitchMaterial> switches = new EnumMap<>(NotificationType.class);
        switches.put(NotificationType.ACTIVITY_STARTED, binding.switchNotificationActivityStarted);
        switches.put(NotificationType.COLLEAGUE_ACTIVITY_STARTED, binding.switchNotificationColleagueActivity);
        switches.put(NotificationType.POINTS_THRESHOLD_REACHED, binding.switchNotificationPointsMilestone);
        switches.put(NotificationType.COMMENT_ADDED, binding.switchNotificationCommentAdded);
        switches.put(NotificationType.CHAT_MESSAGE, binding.switchNotificationChatMessages);

        for (Map.Entry<NotificationType, SwitchMaterial> entry : switches.entrySet()) {
            NotificationType type = entry.getKey();
            SwitchMaterial toggle = entry.getValue();
            String preferenceKey = getNotificationPreferenceKey(type);
            boolean enabled = preferences.getBoolean(preferenceKey, true);
            toggle.setChecked(enabled);
            toggle.setOnCheckedChangeListener((buttonView, isChecked) ->
                    preferences.edit().putBoolean(preferenceKey, isChecked).apply()
            );
        }
    }

    private void setupThemeToggle() {
        final SharedPreferences preferences = requireContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        final SwitchMaterial themeSwitch = binding.switchThemeToggle;
        themeSwitch.setChecked(isNightModeActive());

        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int nextMode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            AppCompatDelegate.setDefaultNightMode(nextMode);
            preferences.edit().putInt(KEY_THEME_MODE, nextMode).apply();
        });
    }

    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        requireActivity().getResources().updateConfiguration(config, requireActivity().getResources().getDisplayMetrics());
        SharedPreferences.Editor editor = requireActivity().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_APP_LANG, lang);
        editor.apply();
        requireActivity().recreate();
    }

    private String getSavedLanguage() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_APP_LANG, "en");
    }

    private int getFontScaleSelection() {
        float savedScale = getSavedFontScale();
        for (int i = 0; i < FONT_SCALE_VALUES.length; i++) {
            if (Math.abs(FONT_SCALE_VALUES[i] - savedScale) < FONT_SCALE_TOLERANCE) {
                return i;
            }
        }
        return 1;
    }

    private float getSavedFontScale() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return prefs.getFloat(KEY_FONT_SCALE, DEFAULT_FONT_SCALE);
    }

    private void updateFontScale(float newScale) {
        float currentScale = getSavedFontScale();
        if (Math.abs(currentScale - newScale) < FONT_SCALE_TOLERANCE) {
            return;
        }
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        prefs.edit().putFloat(KEY_FONT_SCALE, newScale).apply();
        applyFontScale(newScale);
    }

    private void applyFontScale(float scale) {
        Configuration configuration = new Configuration(requireActivity().getResources().getConfiguration());
        if (Math.abs(configuration.fontScale - scale) < FONT_SCALE_TOLERANCE) {
            return;
        }
        configuration.fontScale = scale;
        requireActivity().getResources().updateConfiguration(configuration, requireActivity().getResources().getDisplayMetrics());
        requireActivity().getApplicationContext().getResources().updateConfiguration(configuration, requireActivity().getApplicationContext().getResources().getDisplayMetrics());
        requireActivity().recreate();
    }

    private String getNotificationPreferenceKey(NotificationType type) {
        return "notification_" + type.name();
    }

    private boolean isNightModeActive() {
        int nightModeFlags = requireContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
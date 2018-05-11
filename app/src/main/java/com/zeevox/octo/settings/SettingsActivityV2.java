package com.zeevox.octo.settings;

import android.app.ActionBar;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.view.MenuItem;
import android.widget.Toast;

import com.rarepebble.colorpicker.ColorPreference;
import com.zeevox.octo.FeedbackActivity;
import com.zeevox.octo.R;
import com.zeevox.octo.wallpaper.OcquariumWallpaperService;

import java.util.List;
import java.util.Objects;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivityV2 extends PreferenceActivity {

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || BackgroundPreferenceFragment.class.getName().equals(fragmentName)
                || WallpaperFragment.class.getName().equals(fragmentName)
                || ExperimentalFragment.class.getName().equals(fragmentName)
                || OctopusFragment.class.getName().equals(fragmentName)
                || FeedbackFragment.class.getName().equals(fragmentName);
    }

    public static class GeneralPreferenceFragment extends BasePreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("octo_fade_in_duration"));

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                ((SwitchPreference) findPreference("platlogo_v2")).setChecked(false);
                findPreference("platlogo_v2").setEnabled(false);
                ((PreferenceScreen) findPreference("category_general")).removePreference(findPreference("platlogo_v2"));
            } else {
                findPreference("platlogo_v2").setEnabled(((SwitchPreference) findPreference("show_platlogo")).isChecked());
                findPreference("show_platlogo").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        findPreference("platlogo_v2").setEnabled(((boolean) newValue));
                        return true;
                    }
                });
            }
        }
    }

    public static class BackgroundPreferenceFragment extends BasePreferenceFragment {

        private boolean wasGradientStartDefault = false;
        private boolean wasGradientEndDefault = false;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_background);

            ActionBar actionBar = getActivity().getActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }

            findPreference("gradient_start_color").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    wasGradientStartDefault = Objects.equals(newValue,
                            getResources().getColor(R.color.octo_bg_default_start_color));
                    if (wasGradientStartDefault && wasGradientEndDefault) {
                        findPreference("reset_background_colors").setEnabled(false);
                    } else {
                        findPreference("reset_background_colors").setEnabled(true);
                    }
                    return true;
                }
            });

            findPreference("gradient_end_color").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    wasGradientEndDefault = Objects.equals(newValue,
                            getResources().getColor(R.color.octo_bg_default_end_color));
                    if (wasGradientStartDefault && wasGradientEndDefault) {
                        findPreference("reset_background_colors").setEnabled(false);
                    } else {
                        findPreference("reset_background_colors").setEnabled(true);
                    }
                    return true;
                }
            });

            findPreference("reset_background_colors").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    ColorPreference startColor = (ColorPreference) findPreference("gradient_start_color");
                    startColor.setColor(getResources().getColor(R.color.octo_bg_default_start_color));
                    ColorPreference endColor = (ColorPreference) findPreference("gradient_end_color");
                    endColor.setColor(getResources().getColor(R.color.octo_bg_default_end_color));
                    preference.setEnabled(false);
                    return true;
                }
            });
        }
    }

    public static class OctopusFragment extends BasePreferenceFragment {
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_octopus);
        }
    }

    public static class WallpaperFragment extends BasePreferenceFragment {
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_wallpaper);

            findPreference("set_live_wallpaper").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(
                            WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                    intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            new ComponentName(getActivity(), OcquariumWallpaperService.class));
                    startActivity(intent);
                    return false;
                }
            });

            findPreference("restart_live_wallpaper").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Toast.makeText(getActivity(), "Restarting live wallpaper...", Toast.LENGTH_SHORT).show();
                    restartLiveWallpaper();
                    return false;
                }
            });

            findPreference("set_screensaver").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    try {
                        startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS));
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(getActivity(), "Your device doesn't support setting a screensaver, sorry.", Toast.LENGTH_SHORT).show();
                    }
                    return false;
                }
            });
        }

        @Override
        public void onPause() {
            super.onPause();
            restartLiveWallpaper();
        }

        @Override
        public void onResume() {
            super.onResume();
            Preference setLiveWallpaper = findPreference("set_live_wallpaper");
            Preference restartLiveWallpaper = findPreference("restart_live_wallpaper");
            if (isLiveWallpaperSet()) {
                setLiveWallpaper.setEnabled(false);
                setLiveWallpaper.setSummary("Ocquarium live wallpaper already set.");
                restartLiveWallpaper.setEnabled(true);
            }
        }

        private boolean isLiveWallpaperSet() {
            WallpaperInfo info = WallpaperManager.getInstance(getActivity()).getWallpaperInfo();
            return info != null && info.getPackageName().equals(getActivity().getPackageName());
        }

        private void restartLiveWallpaper() {
            if (isLiveWallpaperSet()) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                editor.putBoolean("restart_live_wallpaper", true);
                editor.apply();
            }
        }
    }

    public static class ExperimentalFragment extends BasePreferenceFragment {
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_experimental);
        }
    }

    public static class FeedbackFragment extends BasePreferenceFragment {
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_feedback);

            findPreference("send_feedback").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(getActivity(), FeedbackActivity.class));
                    return true;
                }
            });
        }
    }

    /**
     * This is the base class that enables the UI 'back' button in all child fragments.
     * Prevents unnecessary code duplication.
     */
    public static class BasePreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivityV2.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
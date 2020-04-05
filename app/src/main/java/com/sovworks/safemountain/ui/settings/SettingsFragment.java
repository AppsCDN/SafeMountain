package com.sovworks.safemountain.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.sovworks.safemountain.R;

public class SettingsFragment extends Fragment {

    private com.sovworks.safemountain.ui.settings.SettingsViewModel settingsViewModel;

    public SettingsFragment(){}

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        settingsViewModel =
                ViewModelProviders.of(this).get(com.sovworks.safemountain.ui.settings.SettingsViewModel.class);
        View root = inflater.inflate(R.layout.fragment_settings, container, false);
        return root;
    }
}
package com.sovworks.safemountain.ui.mountain;

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

import com.sovworks.safemountain.FileSystemObserverService;
import com.sovworks.safemountain.MainActivity;
import com.sovworks.safemountain.R;

public class MountainFragment extends Fragment {

    private com.sovworks.safemountain.ui.mountain.MountainViewModel mountainViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        mountainViewModel =
                ViewModelProviders.of(this).get(com.sovworks.safemountain.ui.mountain.MountainViewModel.class);
        View root = inflater.inflate(R.layout.fragment_mountain, container, false);
        TextView t = root.findViewById(R.id.text_home);
        t.setText(Integer.toString(FileSystemObserverService.Thread_Count));
        return root;
    }
}

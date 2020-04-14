package com.kigael.safemountain.ui.mountain;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.kigael.safemountain.MainActivity;
import com.kigael.safemountain.R;

public class MountainFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_mountain, container, false);
        TextView select = root.findViewById(R.id.text_mountain);
        String sql = "select * from Files_To_Transfer";
        Cursor cursor = MainActivity.database.rawQuery(sql,null);
        String selection_result="";
        while(cursor.moveToNext()){
            String ID = cursor.getString(0);
            String path = cursor.getString(1);
            selection_result+=ID+" "+path+"\n";
        }
        cursor.close();
        select.setText(selection_result);
        return root;
    }

}

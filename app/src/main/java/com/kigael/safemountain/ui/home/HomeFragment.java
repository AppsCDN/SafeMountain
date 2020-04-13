package com.kigael.safemountain.ui.home;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProviders;
import com.kigael.safemountain.MainActivity;
import com.kigael.safemountain.R;
import com.kigael.safemountain.service.FileSystemObserverService;
import com.kigael.safemountain.transfer.Restore;
import com.kigael.safemountain.ui.mountain.MountainFragment;
import com.kigael.safemountain.ui.settings.SettingsFragment;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class HomeFragment extends Fragment {

    private com.kigael.safemountain.ui.home.HomeViewModel homeViewModel;
    private Context context;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                ViewModelProviders.of(this).get(com.kigael.safemountain.ui.home.HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        final Button home_activate_deactivate_Button  = root.findViewById(R.id.buttonActivate);
        final Button mountain = root.findViewById(R.id.buttonMountain);
        final Button settings = root.findViewById(R.id.buttonSettings);
        final Button restore = root.findViewById(R.id.buttonRestore);
        final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        if(FileSystemObserverService.is_running){
                            Intent myIntent = new Intent(context, FileSystemObserverService.class);
                            context.stopService(myIntent);
                            Log.e("is_running",""+FileSystemObserverService.is_running);
                            home_activate_deactivate_Button.setBackgroundResource(R.drawable.activate);
                            changeActivateStatus(context);
                            new Restore(context,true);
                        }
                        else{
                            new Restore(context,false);
                        }
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        Toast.makeText(context,"Restoration cancelled",Toast.LENGTH_LONG).show();
                        break;
                }
            }
        };
        context = container.getContext();
        if(!checkActivationStatus(context)){home_activate_deactivate_Button.setBackgroundResource(R.drawable.activate);}
        else{home_activate_deactivate_Button.setBackgroundResource(R.drawable.deactivate);}
        home_activate_deactivate_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!checkLoginStatus(context)){
                    Toast.makeText(context,"Login is Required",Toast.LENGTH_LONG).show();
                }
                else{
                    if(!checkActivationStatus(context)){
                        changeActivateStatus(context);
                        if(FileSystemObserverService.is_running){
                            home_activate_deactivate_Button.setBackgroundResource(R.drawable.deactivate);
                            Toast.makeText(context,"Deactivate cancelled",Toast.LENGTH_LONG).show();
                        }
                        else{
                            Intent myIntent = new Intent(context, FileSystemObserverService.class);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(myIntent);
                            } else {
                                context.startService(myIntent);
                            }
                            home_activate_deactivate_Button.setBackgroundResource(R.drawable.deactivate);
                            Toast.makeText(context,"Safe Mountain activated",Toast.LENGTH_LONG).show();
                        }
                    }
                    else{
                        changeActivateStatus(context);
                        home_activate_deactivate_Button.setBackgroundResource(R.drawable.activate);
                        Toast.makeText(context,"Reboot the system to deactivate",Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        mountain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction fragmentTransaction = getActivity()
                        .getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.drawer_layout, new MountainFragment());
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        });
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction fragmentTransaction = getActivity()
                        .getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.drawer_layout, new SettingsFragment());
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        });
        restore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sql = "SELECT * FROM Files_To_Transfer";
                Cursor cursor = MainActivity.database.rawQuery(sql,null);
                if(cursor!=null&&cursor.getCount()!=0){
                    Toast.makeText(context,"Backup is still in progress", Toast.LENGTH_LONG).show();
                }
                else{
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Proceed Restoration?"+"\n"+"SafeMountain will be deactivated during restoration").setPositiveButton("YES", dialogClickListener)
                            .setNegativeButton("NO", dialogClickListener).show();
                }
                if(cursor!=null){
                    cursor.close();
                }
            }
        });
        return root;
    }

    private boolean checkActivationStatus(Context context){
        String activate_info_path = context.getFilesDir().toString()+"/activate_info.txt";
        File activate_info = new File(activate_info_path);
        if(!activate_info.exists()) return false;
        else if(activate_info.exists()){
            try{
                BufferedReader br = new BufferedReader(new FileReader(activate_info));
                boolean check = Boolean.parseBoolean(br.readLine());
                br.close();
                return check;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return false;
    }

    private boolean checkLoginStatus(Context context){
        String account_info_path = context.getFilesDir().toString()+"/account_info.txt";
        File account_info = new File(account_info_path);
        return account_info.exists();
    }

    private void changeActivateStatus(Context context){
        String activate_info_path = context.getFilesDir().toString()+"/activate_info.txt";
        File activate_info = new File(activate_info_path);
        boolean currentStatus;
        try{
            BufferedReader br = new BufferedReader(new FileReader(activate_info));
            currentStatus = Boolean.parseBoolean(br.readLine());
            br.close();
            BufferedWriter bw = new BufferedWriter(new FileWriter(activate_info,false));
            if(currentStatus){bw.write("false");}
            else{bw.write("true");}
            bw.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
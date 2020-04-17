package com.kigael.safemountain.ui.home;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
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

    private Context context;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
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
                        new Restore(context);
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
                    Toast.makeText(context,"Login is required",Toast.LENGTH_LONG).show();
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
                if(isNetworkConnected()){
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Proceed restoration?").setPositiveButton("YES", dialogClickListener)
                            .setNegativeButton("NO", dialogClickListener).show();
                }
                else{
                    Toast.makeText(context,"No network connection",Toast.LENGTH_LONG).show();
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

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork) {
            return true;
        }
        return false;
    }

    private boolean isWiFiConnected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork) {
            return activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
        }
        return false;
    }

    private boolean isMobileDataAllowed(){
        String activate_info_path = context.getFilesDir().toString()+"/mobile_info.txt";
        File activate_info = new File(activate_info_path);
        if(!activate_info.exists()) return false;
        else if(activate_info.exists()){
            try{
                BufferedReader br = new BufferedReader(new FileReader(activate_info_path));
                boolean check = Boolean.parseBoolean(br.readLine());
                br.close();
                return check;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return false;
    }

}
package com.sovworks.safemountain.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import com.sovworks.safemountain.FileSystemObserverService;
import com.sovworks.safemountain.R;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Calendar;

public class HomeFragment extends Fragment {

    private com.sovworks.safemountain.ui.home.HomeViewModel homeViewModel;
    private Context context;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                ViewModelProviders.of(this).get(com.sovworks.safemountain.ui.home.HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        final Button home_activate_deactivate_Button  = root.findViewById(R.id.buttonActivate);
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
                        home_activate_deactivate_Button.setBackgroundResource(R.drawable.deactivate);
                        //TODO ask to reboot
                        //Toast.makeText(context,"Please Reboot the System",Toast.LENGTH_LONG).show();
                        Intent myIntent = new Intent(context, FileSystemObserverService.class);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            initLog(context);
                            context.startForegroundService(myIntent);
                        } else {
                            initLog(context);
                            context.startService(myIntent);
                        }
                    }
                    else{
                        changeActivateStatus(context);
                        home_activate_deactivate_Button.setBackgroundResource(R.drawable.activate);
                        Intent myIntent = new Intent(context, FileSystemObserverService.class);
                        context.stopService(myIntent);
                        deleteLog(context);
                    }
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

    private void deleteLog(Context context){
        String log_path = context.getFilesDir().toString()+"/log.txt";
        File log_to_Delete = new File(log_path);
        log_to_Delete.delete();
    }

    private void initLog(Context context){
        String filename = context.getFilesDir().toString()+"/log.txt";
        String currentTime = "INIT_START_FROM "+ Calendar.getInstance().getTime().toString()+"\n";
        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true));
            bw.write(currentTime);
            bw.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
package com.sovworks.safemountain.ui.activate;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import com.sovworks.safemountain.R;
import com.sovworks.safemountain.service.FileSystemObserverService;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class ActivateFragment extends Fragment {

    private com.sovworks.safemountain.ui.activate.ActivateViewModel activateViewModel;
    private Context context;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        activateViewModel =
                ViewModelProviders.of(this).get(com.sovworks.safemountain.ui.activate.ActivateViewModel.class);
        View root = inflater.inflate(R.layout.fragment_activate, container, false);
        final Button activate_deactivate_Button  = root.findViewById(R.id.buttonActivateNDeactivate);
        final TextView ObserverCount = root.findViewById(R.id.ObserverCount);
        context = container.getContext();
        if(!checkActivationStatus(context)){activate_deactivate_Button.setBackgroundResource(R.drawable.activate);}
        else{activate_deactivate_Button.setBackgroundResource(R.drawable.deactivate);}
        activate_deactivate_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!checkLoginStatus(context)){
                    Toast.makeText(context,"Login is Required",Toast.LENGTH_LONG).show();
                }
                else{
                    if(!checkActivationStatus(context)){
                        changeActivateStatus(context);
                        if(FileSystemObserverService.is_running){
                            activate_deactivate_Button.setBackgroundResource(R.drawable.deactivate);
                            Toast.makeText(context,"Deactivate cancelled",Toast.LENGTH_LONG).show();
                        }
                        else{
                            Intent myIntent = new Intent(context, FileSystemObserverService.class);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(myIntent);
                            } else {
                                context.startService(myIntent);
                            }
                            activate_deactivate_Button.setBackgroundResource(R.drawable.deactivate);
                            Toast.makeText(context,"Safe Mountain activated",Toast.LENGTH_LONG).show();
                        }
                    }
                    else{
                        changeActivateStatus(context);
                        activate_deactivate_Button.setBackgroundResource(R.drawable.activate);
                        Toast.makeText(context,"Reboot the system to deactivate",Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        if(checkActivationStatus(context)){
            ObserverCount.setText(Integer.toString(FileSystemObserverService.Observer_Count)+" Files are being watched");
        }
        else{
            ObserverCount.setText("Safe Mountain deactivated");
        }
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
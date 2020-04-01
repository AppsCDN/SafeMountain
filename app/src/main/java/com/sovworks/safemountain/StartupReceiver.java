package com.sovworks.safemountain;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Calendar;

public class StartupReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean activateStatus = checkActivationStatus(context);
        boolean loginStatus = checkLoginStatus(context);
        if(activateStatus&&loginStatus){
            Intent myIntent = new Intent(context, FileSystemObserverService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                initLog(context);
                context.startForegroundService(myIntent);
            } else {
                initLog(context);
                context.startService(myIntent);
            }
        }
    }

    private void initLog(Context context){
        String filename = context.getFilesDir().toString()+"/log.txt";
        String currentTime = "INIT_START_FROM "+Calendar.getInstance().getTime().toString()+"\n";
        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename, false));
            bw.write(currentTime);
            bw.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private boolean checkActivationStatus(Context context){
        String activate_info_path = context.getFilesDir().toString()+"/activate_info.txt";
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

    private boolean checkLoginStatus(Context context){
        String account_info_path = context.getFilesDir().toString()+"/account_info.txt";
        File account_info = new File(account_info_path);
        return account_info.exists();
    }

}
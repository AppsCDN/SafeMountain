package com.kigael.safemountain.ui.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import com.google.android.material.navigation.NavigationView;
import com.kigael.safemountain.Login;
import com.kigael.safemountain.MainActivity;
import com.kigael.safemountain.R;
import com.kigael.safemountain.transfer.Restore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class SettingsFragment extends Fragment {
    private Context context;
    private static String ID="",PW="",HOST="";
    private static int PORT=0;
    private com.kigael.safemountain.ui.settings.SettingsViewModel settingsViewModel;

    public SettingsFragment(){}

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        settingsViewModel =
                ViewModelProviders.of(this).get(com.kigael.safemountain.ui.settings.SettingsViewModel.class);
        final View root = inflater.inflate(R.layout.fragment_settings, container, false);
        context = container.getContext();
        final Button serverConfigure = root.findViewById(R.id.ButtonConfigureServer);
        final Button allowMobileData = root.findViewById(R.id.ButtonMobileData);
        final Button restore = root.findViewById(R.id.buttonRestore);
        final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        Thread t = new Restore(context);
                        t.setPriority(Thread.MIN_PRIORITY);
                        t.run();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        Toast.makeText(context,"Restoration cancelled",Toast.LENGTH_LONG).show();
                        break;
                }
            }
        };
        if(checkMobileUsageStatus(context)){
            allowMobileData.setBackgroundResource(R.drawable.cell);
        }
        else{
            allowMobileData.setBackgroundResource(R.drawable.wifi);
        }
        serverConfigure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    showLoginDialog(root);
            }
        });
        allowMobileData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeMobileStatus(context);
                if(checkMobileUsageStatus(context)){
                    Toast.makeText(context,"Mobile Data Usage is Allowed",Toast.LENGTH_LONG).show();
                    allowMobileData.setBackgroundResource(R.drawable.cell);
                }
                else{
                    Toast.makeText(context,"Mobile Data Usage is Prevented",Toast.LENGTH_LONG).show();
                    allowMobileData.setBackgroundResource(R.drawable.wifi);
                }
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
                    builder.setMessage("Proceed Restoration?").setPositiveButton("YES", dialogClickListener)
                            .setNegativeButton("NO", dialogClickListener).show();
                }
                if(cursor!=null){
                    cursor.close();
                }
            }
        });
        return root;
    }

    private boolean checkMobileUsageStatus(Context context){
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

    private void changeMobileStatus(Context context){
        String activate_info_path = context.getFilesDir().toString()+"/mobile_info.txt";
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

    private void createLoginFile(Context context, String in_id, String in_pw, String in_Host, String in_Port){
        String account_info_path = context.getFilesDir().toString()+"/account_info.txt";
        String toWrite = in_id+"\n"+in_pw+"\n"+in_Host+"\n"+in_Port;
        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(account_info_path, false));
            bw.write(toWrite);
            bw.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void showLoginDialog(View root) {
        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout loginLayout =
                (LinearLayout) vi.inflate(R.layout.login_dialog, null);
        NavigationView navigationView = getActivity().findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        final TextView hostView = (TextView) headerView.findViewById(R.id.accountHOST);
        final TextView idView = (TextView) headerView.findViewById(R.id.accountID);
        final EditText host = (EditText) loginLayout.findViewById(R.id.hostEdit);
        final EditText port = (EditText) loginLayout.findViewById(R.id.portEdit);
        final EditText id = (EditText) loginLayout.findViewById(R.id.idEdit);
        final EditText pw = (EditText) loginLayout.findViewById(R.id.pwEdit);
        new AlertDialog.Builder(context).setTitle("LOGIN").setView(loginLayout).setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                HOST = host.getText().toString();
                PORT = Integer.parseInt(port.getText().toString());
                ID = id.getText().toString();
                PW = pw.getText().toString();
                if(!HOST.isEmpty()&&!(PORT==0)&&!ID.isEmpty()&&!PW.isEmpty()){
                    try {
                        new Login(ID,PW,HOST,PORT);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    boolean isLoginSuccess = Login.result;
                    if(isLoginSuccess){
                        idView.setText(ID);
                        hostView.setText(HOST);
                        createLoginFile(context,ID,PW,HOST,Integer.toString(PORT));
                        Toast.makeText(context,"Server Configuration Success",Toast.LENGTH_SHORT).show();
                    }
                    else {
                        HOST=""; PORT=0; ID=""; PW="";
                        Toast.makeText(context,"Server Configuration Failed",Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }).show();
    }

}

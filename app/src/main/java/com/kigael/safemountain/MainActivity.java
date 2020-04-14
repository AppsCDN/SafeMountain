package com.kigael.safemountain;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.navigation.NavigationView;
import com.kigael.safemountain.db.Log_DB;
import com.kigael.safemountain.transfer.Restore;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class MainActivity extends AppCompatActivity {
    private AppBarConfiguration mAppBarConfiguration;
    private static String ID="",PW="",HOST="";
    private static int PORT=0;
    public static SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        final TextView hostView = (TextView) headerView.findViewById(R.id.accountHOST);
        final TextView idView = (TextView) headerView.findViewById(R.id.accountID);
        boolean loginStatus = checkLoginStatus(MainActivity.this);
        if(loginStatus){
            idView.setText(getID(MainActivity.this));
            hostView.setText(getHOST(MainActivity.this));
        }
        hostView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!checkLoginStatus(MainActivity.this)){
                    showLoginDialog();
                }
            }
        });
        navigationView.setItemIconTintList(null);
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_mountain, R.id.nav_activate, R.id.nav_settings)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        boolean isWriteStorageGranted = isWriteStoragePermissionGranted(this);
        if(!isWriteStorageGranted){
            MainActivity.this.finish();
            System.exit(0);
        }
        database = openOrCreateDatabase("LOG_DB",MODE_PRIVATE,null);
        database.execSQL(Log_DB.SQL_CREATE_LOG_TABLE);
        database.execSQL(Log_DB.SQL_CREATE_DELETE_TABLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private  boolean isWriteStoragePermissionGranted(Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 3);
                return false;
            }
        }
        else {
            return true;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onActivityResult(int requestCode,int resultCode,Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode != RESULT_OK) {
            return;
        }
        Uri treeUri = resultData.getData();
        DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
        grantUriPermission(getPackageName(), treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        Restore.rootUri.push(pickedDir);
        if(Restore.asked.empty()){
            Thread t = new Restore(MainActivity.this);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
        }
    }

    private boolean checkLoginStatus(Context context){
        String account_info_path = context.getFilesDir().toString()+"/account_info.txt";
        File account_info = new File(account_info_path);
        return account_info.exists();
    }

    private void createLoginFile(Context context, String in_id, String in_pw,String in_Host, String in_Port){
        String account_info_path = context.getFilesDir().toString()+"/account_info.txt";
        String activate_info_path = context.getFilesDir().toString()+"/activate_info.txt";
        String mobile_info_path = context.getFilesDir().toString()+"/mobile_info.txt";
        String toWrite = in_id+"\n"+in_pw+"\n"+in_Host+"\n"+in_Port;
        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(account_info_path, false));
            bw.write(toWrite);
            bw.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(activate_info_path, false));
            bw.write("false");
            bw.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(mobile_info_path, false));
            bw.write("false");
            bw.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private String getID(Context context){
        String account_info_path = context.getFilesDir().toString()+"/account_info.txt";
        String retID = "";
        try{
            BufferedReader br = new BufferedReader(new FileReader(account_info_path));
            retID = br.readLine();
            br.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return retID;
    }

    private String getHOST(Context context){
        String account_info_path = context.getFilesDir().toString()+"/account_info.txt";
        String retID = "";
        try{
            BufferedReader br = new BufferedReader(new FileReader(account_info_path));
            br.readLine();
            br.readLine();
            retID = br.readLine();
            br.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return retID;
    }

    private void showLoginDialog() {
        LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout loginLayout =
                (LinearLayout) vi.inflate(R.layout.login_dialog, null);
        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        final TextView hostView = (TextView) headerView.findViewById(R.id.accountHOST);
        final TextView idView = (TextView) headerView.findViewById(R.id.accountID);
        final EditText host = (EditText) loginLayout.findViewById(R.id.hostEdit);
        final EditText port = (EditText) loginLayout.findViewById(R.id.portEdit);
        final EditText id = (EditText) loginLayout.findViewById(R.id.idEdit);
        final EditText pw = (EditText) loginLayout.findViewById(R.id.pwEdit);
        new AlertDialog.Builder(this).setTitle("Login").setView(loginLayout).setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        HOST = host.getText().toString();
                        PORT = Integer.parseInt(port.getText().toString());
                        ID = id.getText().toString();
                        PW = pw.getText().toString();
                        if(!HOST.isEmpty()&&!(PORT==0)&&!ID.isEmpty()&&!PW.isEmpty()&&!checkLoginStatus(MainActivity.this)){
                            try {
                                new Login(ID,PW,HOST,PORT);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            boolean isLoginSuccess = Login.result;
                            if(isLoginSuccess){
                                idView.setText(ID);
                                hostView.setText(HOST);
                                createLoginFile(MainActivity.this,ID,PW,HOST,Integer.toString(PORT));
                            }
                            else {
                                HOST=""; PORT=0; ID=""; PW="";
                                Toast.makeText(MainActivity.this,"Login failed",Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }).show();
    }

}
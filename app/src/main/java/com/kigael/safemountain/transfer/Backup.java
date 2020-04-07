package com.kigael.safemountain.transfer;

import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.kigael.safemountain.MainActivity;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Backup extends Thread implements Runnable {
    private String sql;
    private String path;
    private Cursor cursor;
    private long LastModified;
    private Session session;
    private Channel channel;
    private ChannelSftp channelSftp;
    private JSch jsch;
    private String Host;
    private String ID;
    private String PW;
    private int Port;
    private Context context;

    public Backup(Context context){
        this.context = context;
    }

    @Override
    public void run() {
        super.run();
        while(true){
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(isNetworkConnected()&&((isMobileDataAllowed() && isWiFiConnected())||(!isMobileDataAllowed() && isWiFiConnected())||(isMobileDataAllowed() && !isWiFiConnected()))){
                this.ID = getID(context);
                this.PW = getPW(context);
                this.Host = getHOST(context);
                this.Port = getPORT(context);
                sql = "SELECT * FROM Files_To_Transfer";
                cursor = MainActivity.database.rawQuery(sql,null);
                if(cursor!=null&&cursor.getCount()!=0){
                    cursor.moveToFirst();
                    path = cursor.getString(cursor.getColumnIndex("PATH"));
                    cursor.close();
                    LastModified = getLastModifiedDate(path);
                    boolean sent_success = true;
                    try {
                        sendFile(path);
                    } catch (SftpException e) {
                        sent_success = false;
                        e.printStackTrace();
                    }
                    if(!ifFileExist(path)){
                        Log.e("TransferFail-cancel",path);
                        try {
                            cancelTransfer(path);
                        } catch (SftpException e) {
                            //TODO: what if deletion fails?
                            e.printStackTrace();
                        }
                        send_done(path);
                    }
                    else if(ifFileExist(path) && getLastModifiedDate(path) == LastModified){
                        if(sent_success){
                            Log.e("TransferSuccess",path);
                            send_done(path);
                        }
                    }
                    else if(ifFileExist(path) && getLastModifiedDate(path) != LastModified){
                        Log.e("TransferFail-resend",path);
                    }
                }
                else{
                    cursor.close();
                }
                sql = "SELECT * FROM Files_To_Delete";
                cursor = MainActivity.database.rawQuery(sql,null);
                if(cursor!=null&&cursor.getCount()!=0){
                    cursor.moveToFirst();
                    path = cursor.getString(cursor.getColumnIndex("PATH"));
                    cursor.close();
                    try {
                        cancelTransfer(path);
                    } catch (SftpException e) {
                        e.printStackTrace();
                    }
                    Log.e("DeleteFile",path);
                    delete_done(path);
                }
                else{
                    cursor.close();
                }
            }
        }
    }

    public void sendFile(String path) throws SftpException {
        String dir = new File(path).getParent();
        init(Host,ID,PW,Port);
        mkdirDir("."+dir);
        uploadFile(new File(path));
        disconnect();
    }

    private void cancelTransfer(String path) throws SftpException {
        init(Host,ID,PW,Port);
        deleteFile("."+path);
        disconnect();
    }

    public void init(String host, String userName, String password, int port) {
        jsch = new JSch();
        try {
            session = jsch.getSession(userName, host, port);
            session.setPassword(password);
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;
        } catch (JSchException e) {
            e.printStackTrace();
        }
    }

    public void mkdirDir(String path) throws SftpException {
        String[] pathArray = path.split("/");
        String currentDirectory = channelSftp.pwd();
        String totPathArray = "";
        for(int i =0; i< pathArray.length; i++) {
            totPathArray += pathArray[i] + "/";
            String currentPath = currentDirectory+ "/" + totPathArray;
            try {
                channelSftp.mkdir(currentPath);
                channelSftp.cd(currentPath);
            } catch (Exception e) {
                channelSftp.cd(currentPath);
            }
        }
    }

    public void uploadFile(File file){
        FileInputStream in = null;
        try{
            in = new FileInputStream(file);
            channelSftp.put(in,file.getName());
        } catch(SftpException se){
            se.printStackTrace();
        }catch(FileNotFoundException fe){
            fe.printStackTrace();
        } finally{
            try{
                if(in!=null){
                    in.close();
                }
            } catch(IOException ioe){
                ioe.printStackTrace();
            }
        }
    }

    public void deleteFile(String path){
        try{
            channelSftp.rm(path);
        } catch(SftpException se){
            se.printStackTrace();
        }
    }

    public void disconnect(){
        channelSftp.quit();
        session.disconnect();
    }

   private long getLastModifiedDate(String path){
        File f = new File(path);
        return f.lastModified();
   }

   private boolean ifFileExist(String path){
       File f = new File(path);
       return f.exists();
   }

   private void send_done(String path){
        sql = "delete from Files_To_Transfer where PATH = " + "\"" + path + "\"";
        MainActivity.database.execSQL(sql);
    }

    private void delete_done(String path){
        sql = "delete from Files_To_Delete where PATH = " + "\"" + path + "\"";
        MainActivity.database.execSQL(sql);
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

    private String getPW(Context context){
        String account_info_path = context.getFilesDir().toString()+"/account_info.txt";
        String retPW = "";
        try{
            BufferedReader br = new BufferedReader(new FileReader(account_info_path));
            br.readLine();
            retPW = br.readLine();
            br.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return retPW;
    }

    private String getHOST(Context context){
        String account_info_path = context.getFilesDir().toString()+"/account_info.txt";
        String retPW = "";
        try{
            BufferedReader br = new BufferedReader(new FileReader(account_info_path));
            br.readLine();
            br.readLine();
            br.readLine();
            retPW = br.readLine();
            br.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return retPW;
    }

    private int getPORT(Context context){
        String account_info_path = context.getFilesDir().toString()+"/account_info.txt";
        int retPW=22;
        try{
            BufferedReader br = new BufferedReader(new FileReader(account_info_path));
            br.readLine();
            br.readLine();
            br.readLine();
            br.readLine();
            retPW = Integer.parseInt(br.readLine());
            br.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return retPW;
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    private boolean isWiFiConnected(){
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi.isConnected();
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
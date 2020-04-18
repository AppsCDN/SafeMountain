package com.kigael.safemountain.transfer;

import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.kigael.safemountain.MainActivity;
import com.kigael.safemountain.service.FileSystemObserverService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;

public class Backup extends Thread implements Runnable {
    private String sql;
    private String file_path;
    private String backup_path;
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
        while(FileSystemObserverService.is_running){
            if(isNetworkConnected()&&(isMobileDataAllowed() || isWiFiConnected())){
                this.ID = getID(context);
                this.PW = getPW(context);
                this.Host = getHOST(context);
                this.Port = getPORT(context);
                sql = "SELECT * FROM Files_To_Delete";
                cursor = MainActivity.database.rawQuery(sql,null);
                if(cursor!=null&&cursor.getCount()!=0){
                    cursor.moveToFirst();
                    file_path = cursor.getString(cursor.getColumnIndex("PATH"));
                    boolean validDeletePath = true;
                    if(file_path.startsWith(getExtenerStoragePath().toString())){
                        backup_path = file_path.replace(getExtenerStoragePath().toString(),"/SafeMountainBackup/Internal");
                    }
                    else if(new SDCard().getExternalSDCardPath()!=null&&file_path.startsWith(new SDCard().getExternalSDCardPath())){
                        backup_path = file_path.replace(new SDCard().getExternalSDCardPath(),"/SafeMountainBackup/External");
                    }
                    else{
                        validDeletePath = false;
                        delete_done(file_path);
                    }
                    cursor.close();
                    if(validDeletePath){
                        if(!new File(file_path).exists()){
                            cancelTransfer(backup_path);
                            try {
                                deleteEmptyDir("."+new File(backup_path).getParent());
                            } catch (SftpException e) {
                                e.printStackTrace();
                            }
                            delete_done(file_path);
                        }
                    }
                }
                else if(cursor!=null){
                    cursor.close();
                }
                sql = "SELECT * FROM Files_To_Transfer";
                cursor = MainActivity.database.rawQuery(sql,null);
                if(cursor!=null&&cursor.getCount()!=0){
                    cursor.moveToFirst();
                    file_path = cursor.getString(cursor.getColumnIndex("PATH"));
                    boolean validSendPath = true;
                    if(file_path.startsWith(getExtenerStoragePath().toString())){
                        backup_path = file_path.replace(getExtenerStoragePath().toString(),"/SafeMountainBackup/Internal");
                    }
                    else if(new SDCard().getExternalSDCardPath()!=null&&file_path.startsWith(new SDCard().getExternalSDCardPath())){
                        backup_path = file_path.replace(new SDCard().getExternalSDCardPath(),"/SafeMountainBackup/External");
                    }
                    else{
                        validSendPath = false;
                        send_done(file_path);
                    }
                    cursor.close();
                    if(validSendPath){
                        LastModified = getLastModifiedDate(file_path);
                        boolean sent_success = true;
                        try {
                            if(!Restore.restoringFiles.contains(file_path)){
                                sendFile(backup_path,file_path);
                            }
                            else{
                                send_done(file_path);
                            }
                        } catch (Exception e) {
                            sent_success = false;
                        }
                        if(!ifFileExist(file_path)){
                            cancelTransfer(backup_path);
                            send_done(file_path);
                        }
                        else if(ifFileExist(file_path) && getLastModifiedDate(file_path) == LastModified){
                            if(sent_success){
                                send_done(file_path);
                            }
                        }
                    }
                }
                else if(cursor!=null){
                    cursor.close();
                }
            }
        }
    }

    private void sendFile(String backup_path, String file_path) throws SftpException {
        String dir = new File(backup_path).getParent();
        init(Host,ID,PW,Port);
        mkdirDir("."+dir);
        uploadFile(new File(file_path));
        disconnect();
    }

    private void cancelTransfer(String backup_path) {
        init(Host,ID,PW,Port);
        deleteFile("."+backup_path);
        disconnect();
    }

    private void deleteEmptyDir(String backup_dir_path) throws SftpException {
        init(Host,ID,PW,Port);
        Vector<ChannelSftp.LsEntry> ls = channelSftp.ls(backup_dir_path);
        if(ls.size()==2){
            channelSftp.rmdir(backup_dir_path);
        }
        disconnect();
    }

    private void init(String host, String userName, String password, int port) {
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

    private void mkdirDir(String backup_path) throws SftpException {
        String[] pathArray = backup_path.split("/");
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

    private void uploadFile(File file){
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

    private void deleteFile(String backup_path){
        try{
            channelSftp.rm(backup_path);
        } catch(SftpException se){
            se.printStackTrace();
        }
    }

    private void disconnect(){
        if(channelSftp!=null){channelSftp.disconnect();}
        if(session!=null){session.disconnect();}
    }

    private long getLastModifiedDate(String file_path){
        File f = new File(file_path);
        return f.lastModified();
    }

    private boolean ifFileExist(String file_path){
        File f = new File(file_path);
        return f.exists();
    }

    private void send_done(String file_path){
        sql = "delete from Files_To_Transfer where PATH = " + "\"" + file_path + "\"";
        MainActivity.database.execSQL(sql);
    }

    private void delete_done(String file_path){
        sql = "delete from Files_To_Delete where PATH = " + "\"" + file_path + "\"";
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
            retPW = Integer.parseInt(br.readLine());
            br.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return retPW;
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

    private File getExtenerStoragePath() { return Environment.getExternalStorageDirectory(); }

    private static class SDCard {
        private String getExternalSDCardPath() {
            HashSet<String> hs = getExternalMounts();
            for(String extSDCardPath : hs) {
                return extSDCardPath;
            }
            return null;
        }
        private HashSet<String> getExternalMounts() {
            final HashSet<String> out = new HashSet<String>();
            String reg = "(?i).*media_rw.*(storage).*(sdcardfs).*rw.*";
            String s = "";
            try {
                final Process process = new ProcessBuilder().command("mount").redirectErrorStream(true).start();
                process.waitFor();
                final InputStream is = process.getInputStream();
                final byte[] buffer = new byte[1024];
                while (is.read(buffer) != -1) {
                    s = s + new String(buffer);
                }
                is.close();
            } catch (final Exception e) {
                e.printStackTrace();
            }
            final String[] lines = s.split("\n");
            for (String line : lines) {
                if (!line.toLowerCase(Locale.US).contains("asec")) {
                    if (line.matches(reg)) {
                        String[] parts = line.split(" ");
                        for (String part : parts) {
                            if (part.startsWith("/")) {
                                if (!part.toLowerCase(Locale.US).contains("vold") && !part.toLowerCase(Locale.US).contains("/mnt/")) {
                                    out.add(part);
                                }
                            }
                        }
                    }
                }
            }
            return out;
        }
    }

}
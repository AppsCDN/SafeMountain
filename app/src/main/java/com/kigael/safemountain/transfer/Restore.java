package com.kigael.safemountain.transfer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.StatFs;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.kigael.safemountain.MainActivity;
import com.kigael.safemountain.service.RestoreService;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Stack;
import java.util.Vector;

public class Restore extends Thread implements Runnable {
    private static Context context;
    private boolean serverConnection;
    private Session session;
    private Channel channel;
    private ChannelSftp channelSftp;
    private ChannelExec channelExec;
    private JSch jsch;
    private ByteArrayOutputStream baos;
    private static String Host;
    private static String ID;
    private static String PW;
    private static int Port;
    private boolean is_internal_backup;
    private boolean is_external_backup;
    private boolean is_SDcard_Mounted;
    private String internal_left_storage;
    private String internal_backup_size;
    private String SDcard_left_storage;
    private String external_backup_size;
    private String popUpMessage="";
    public static Stack<Character> asked;
    private static Stack<String> src = new Stack<String>();
    public static Stack<DocumentFile> rootUri = new Stack<DocumentFile>();
    public static String restoreStatus = "";
    public static String dialogContext = "";
    public static ArrayList<String> restoringFiles = new ArrayList<String>();

    public Restore(){}

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public Restore(final Context context) {
        this.context = context;
        this.Host = getHOST(context);
        this.ID = getID(context);
        this.PW = getPW(context);
        this.Port = getPORT(context);
        asked = new Stack<>();
        try {
            connectToServer(Host,ID,PW,Port);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(!serverConnection){
            Toast.makeText(context,"Server connection failed",Toast.LENGTH_LONG).show();
            return;
        }
        try {
            checkIfInternalBackedUp();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            checkIfExternalBackedUp();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        is_SDcard_Mounted = new SDCard().getExternalSDCardPath() != null;
        internal_left_storage = String.format("%.2f",(double)(getInternalLeftStorage())/1024/1024/1024)+"GB";
        if(is_SDcard_Mounted) {SDcard_left_storage = String.format("%.2f",(double)(getSDcardLeftStorage())/1024/1024/1024)+"GB";}
        if(is_internal_backup){
            asked.push('i');
            try {
                getInternalBackupSize();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(is_external_backup){
            asked.push('e');
            try {
                getExternalBackupSize();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(!is_internal_backup&&!is_external_backup){
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        Toast.makeText(context, "Please backup before restoration", Toast.LENGTH_LONG).show();
                    }
                }
            };
            AlertDialog.Builder cantRestore = new AlertDialog.Builder(context);
            cantRestore.setMessage("No backup exists").setPositiveButton("OK", dialogClickListener).show();
        }
        if(!is_internal_backup&&is_external_backup&&!is_SDcard_Mounted){
            popUpMessage = "Will you proceed restoration?\n" +
                    "Internal free space: "+internal_left_storage+"\n"+
                    "Internal backup size: "+"none"+"\n"+
                    "SDcard free space: "+"none"+"\n"+
                    "External backup size: "+external_backup_size;
        }
        else if(!is_internal_backup&&is_external_backup&&is_SDcard_Mounted){
            popUpMessage = "Will you proceed restoration?\n" +
                    "Internal free space: "+internal_left_storage+"\n"+
                    "Internal backup size: "+"none"+"\n"+
                    "SDcard free space: "+SDcard_left_storage+"\n"+
                    "External backup size: "+external_backup_size;
        }
        else if(is_internal_backup&&!is_external_backup&&!is_SDcard_Mounted){
            popUpMessage = "Will you proceed restoration?\n" +
                    "Internal free space: "+internal_left_storage+"\n"+
                    "Internal backup size: "+internal_backup_size+"\n"+
                    "SDcard free space: "+"none"+"\n"+
                    "External backup size: "+"none";
        }
        else if(is_internal_backup&&!is_external_backup&&is_SDcard_Mounted){
            popUpMessage= "Will you proceed restoration?\n" +
                    "Internal free space: "+internal_left_storage+"\n"+
                    "Internal backup size: "+internal_backup_size+"\n"+
                    "SDcard free space: "+SDcard_left_storage+"\n"+
                    "External backup size: "+"none";
        }
        else if(is_internal_backup&&is_external_backup&&!is_SDcard_Mounted){
            popUpMessage = "Will you proceed restoration?\n" +
                    "Internal free space: "+internal_left_storage+"\n"+
                    "Internal backup size: "+internal_backup_size+"\n"+
                    "SDcard free space: "+"none"+"\n"+
                    "External backup size: "+external_backup_size;
        }
        else if(is_internal_backup&&is_external_backup&&is_SDcard_Mounted){
            popUpMessage = "Will you proceed restoration?\n" +
                    "Internal free space: "+internal_left_storage+"\n"+
                    "Internal backup size: "+internal_backup_size+"\n"+
                    "SDcard free space: "+SDcard_left_storage+"\n"+
                    "External backup size: "+external_backup_size;
        }
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        showOption();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        Toast.makeText(context,"Restoration cancelled",Toast.LENGTH_LONG).show();
                        break;
                }
            }
        };
        AlertDialog.Builder restore = new AlertDialog.Builder(context);
        restore.setMessage(popUpMessage).
                setPositiveButton("YES", dialogClickListener).
                setNegativeButton("NO",dialogClickListener).
                show();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void run() {
        super.run();
        init_sftp(Host,ID,PW,Port);
        MainActivity.showLoadingScreen();
        try {
            while(!src.empty()&&!rootUri.empty()){
                recursiveFolderDownload(src.pop(),rootUri.pop());
            }
        } catch (SftpException e) {
            //TODO: handle restore failure.
        }
        MainActivity.hideLoadingScreen();
        disconnect_sftp();
        restoringFiles.clear();
        stopRestoreService();
    }

    private void stopRestoreService(){
        Intent myIntent = new Intent(context, RestoreService.class);
        context.stopService(myIntent);
    }

    private void download(final String in_src) {
        src.push(in_src);
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        ((Activity) context).startActivityForResult(intent, 42);
    }

    private void showOption(){
        if(is_external_backup){
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(which==DialogInterface.BUTTON_POSITIVE){
                        asked.pop();
                        download("./SafeMountainBackup/External");
                    }
                    else if(which==DialogInterface.BUTTON_NEGATIVE){
                        asked.pop();
                        Toast.makeText(context,"External backup restoration cancelled",Toast.LENGTH_LONG).show();
                        if(!src.empty()&&asked.empty()){
                            Intent myIntent = new Intent(context, RestoreService.class);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(myIntent);
                            } else {
                                context.startService(myIntent);
                            }
                        }
                    }
                }
            };
            AlertDialog.Builder restore = new AlertDialog.Builder(context);
            restore.setMessage("On which path would you restore external backup?").
                    setPositiveButton("Select path", dialogClickListener).
                    setNegativeButton("Do not restore",dialogClickListener).
                    show();
        }
        if(is_internal_backup){
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(which==DialogInterface.BUTTON_POSITIVE){
                        asked.pop();
                        download("./SafeMountainBackup/Internal");
                    }
                    else if(which==DialogInterface.BUTTON_NEGATIVE){
                        asked.pop();
                        Toast.makeText(context,"Internal backup restoration cancelled",Toast.LENGTH_LONG).show();
                        if(!src.empty()&&asked.empty()){
                            Intent myIntent = new Intent(context, RestoreService.class);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(myIntent);
                            } else {
                                context.startService(myIntent);
                            }
                        }
                    }
                }
            };
            AlertDialog.Builder restore = new AlertDialog.Builder(context);
            restore.setMessage("On which path would you restore internal backup?").
                    setPositiveButton("Select path", dialogClickListener).
                    setNegativeButton("Do not restore",dialogClickListener).
                    show();
        }
    }

    private void recursiveFolderDownload(String src, DocumentFile pickedDir) throws SftpException {
        Vector<ChannelSftp.LsEntry> fileAndFolderList = channelSftp.ls(src);
        for (ChannelSftp.LsEntry item : fileAndFolderList) {
            if (!item.getAttrs().isDir()) {
                DocumentFile newFile = pickedDir.findFile(item.getFilename());
                if(newFile==null){
                    MainActivity.changeLoadingMessage("Fetching "+item.getFilename());
                    restoreStatus = "Fetching "+item.getFilename();
                    newFile = pickedDir.createFile("",item.getFilename());
                    restoringFiles.add(getPathFromUri(newFile.getUri()));
                    write(src + "/" + item.getFilename(),newFile.getUri());
                }
                else{
                    MainActivity.changeLoadingMessage("Skipping "+item.getFilename());
                    restoreStatus = "Skipping "+item.getFilename();
                }
            } else if (!(".".equals(item.getFilename()) || "..".equals(item.getFilename()))) {
                DocumentFile newDir = pickedDir.findFile(item.getFilename());
                if(newDir==null){
                    newDir = pickedDir.createDirectory(item.getFilename());
                }
                recursiveFolderDownload(src + "/" + item.getFilename(), newDir);
            }
        }
    }

    private void write(String src, Uri uri){
        try {
            ParcelFileDescriptor descriptor=context.getContentResolver().openFileDescriptor(uri,"w");
            if(descriptor!=null) {
                FileOutputStream fos=new FileOutputStream(descriptor.getFileDescriptor());
                channelSftp.get(src,fos);
                fos.close();
            }
        } catch (IOException | SftpException e) {
            e.printStackTrace();
        }
    }

    private String getPathFromUri(Uri uri){
        String path = uri.getPath();
        String[] split = path.split(":");
        if(split[0].contains("/tree/primary")){
            path = Environment.getExternalStorageDirectory().toString()+"/";
            path += split[2];
            return path;
        }
        else{
            path = new SDCard().getExternalSDCardPath()+"/";
            path += split[2];
            return path;
        }
    }

    private void connectToServer(final String host, final String userName, final String password, final int port) throws InterruptedException {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                jsch = new JSch();
                try {
                    session = jsch.getSession(userName, host, port);
                    session.setPassword(password);
                    java.util.Properties config = new java.util.Properties();
                    config.put("StrictHostKeyChecking", "no");
                    session.setConfig(config);
                    session.setTimeout(3000);
                    session.connect();
                } catch (JSchException e) {
                    serverConnection = false;
                    if(session!=null){
                        session.disconnect();
                    }
                    return;
                }
                if(session.isConnected()){serverConnection = true;}
                else{serverConnection = false;}
                if(session!=null){
                    session.disconnect();
                }
                return;
            }
        });
        t.start();
        t.join();
    }

    private void checkIfInternalBackedUp() throws InterruptedException {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                init_sftp(Host,ID,PW,Port);
                try{
                    channelSftp.lstat("./SafeMountainBackup/Internal");
                }catch(SftpException e){
                    is_internal_backup = false;
                    disconnect_sftp();
                    return;
                }
                is_internal_backup = true;
                disconnect_sftp();
                return;
            }
        });
        t.start();
        t.join();
    }

    private void checkIfExternalBackedUp() throws InterruptedException {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                init_sftp(Host,ID,PW,Port);
                try{
                    channelSftp.lstat("./SafeMountainBackup/External");
                }catch(SftpException e){
                    is_external_backup = false;
                    disconnect_sftp();
                    return;
                }
                is_external_backup = true;
                disconnect_sftp();
                return;
            }
        });
        t.start();
        t.join();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private long getInternalLeftStorage(){
        return new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath()).getAvailableBlocksLong()*new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath()).getBlockSizeLong();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private long getSDcardLeftStorage(){
        String SDcardPath = new SDCard().getExternalSDCardPath();
        long SDcard_blockSize = new StatFs(SDcardPath).getBlockSizeLong();
        return new StatFs(SDcardPath).getAvailableBlocksLong()*SDcard_blockSize;
    }

    private void getInternalBackupSize() throws InterruptedException {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                init_exec(Host,ID,PW,Port,"du -s ./SafeMountainBackup/Internal | awk '{print $1}'");
                String result="";
                while(result.isEmpty()) {
                    result = new String(baos.toByteArray());
                }
                result = result.substring(0, result.length() - 1);
                disconnect_exec();
                internal_backup_size =String.format("%.2f",(double)Long.parseLong(result)/1024/1024)+"GB";
            }
        });
        t.start();
        t.join();
    }

    private void getExternalBackupSize() throws InterruptedException {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                init_exec(Host,ID,PW,Port,"du -s ./SafeMountainBackup/External | awk '{print $1}'");
                String result="";
                while(result.isEmpty()) {
                    result = new String(baos.toByteArray());
                }
                result = result.substring(0, result.length() - 1);
                disconnect_exec();
                external_backup_size =String.format("%.2f",(double)Long.parseLong(result)/1024/1024)+"GB";
            }
        });
        t.start();
        t.join();
    }

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

    private void init_sftp(final String host, final String userName, final String password, final int port) {
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

    private void init_exec(String host, String userName, String password, int port, String command) {
        jsch = new JSch();
        try {
            session = jsch.getSession(userName, host, port);
            session.setPassword(password);
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            channelExec = (ChannelExec) session.openChannel("exec");
            channelExec.setCommand(command);
            baos = new ByteArrayOutputStream();
            channelExec.setOutputStream(baos);
            channelExec.connect();
        } catch (JSchException e) {
            e.printStackTrace();
        }
    }

    private void disconnect_sftp(){
        if(channelSftp!=null){channelSftp.disconnect();}
        if(session!=null){session.disconnect();}
    }

    private void disconnect_exec(){
        if(channelExec!=null){channelExec.disconnect();}
        if(session!=null){session.disconnect();}
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

}
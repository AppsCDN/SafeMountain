package com.kigael.safemountain.transfer;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.StatFs;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import com.kigael.safemountain.R;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import static android.view.View.GONE;

public class Restore extends Thread implements Runnable {
    private Context context;
    private boolean serverConnection;
    private Session session;
    private Channel channel;
    private ChannelSftp channelSftp;
    private ChannelExec channelExec;
    private JSch jsch;
    private ByteArrayOutputStream baos;
    private String Host;
    private String ID;
    private String PW;
    private int Port;
    private boolean is_internal_backup;
    private boolean is_external_backup;
    private boolean is_SDcard_Mounted;
    private String internal_left_storage;
    private String internal_backup_size;
    private String SDcard_left_storage;
    private String external_backup_size;
    private String internalDest;
    private String externalDest;
    public static boolean go = false;

    public Restore(final Context context) {
        this.context = context;
        this.Host = getHOST(context);
        this.ID = getID(context);
        this.PW = getPW(context);
        this.Port = getPORT(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void run() {
        try {
            connectToServer(Host,ID,PW,Port);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(!serverConnection){
            Toast.makeText(context,"Server Connection Failed",Toast.LENGTH_LONG).show();
            return;
        }
        super.run();
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
            try {
                getInternalBackupSize();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(is_external_backup){
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
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            Toast.makeText(context,"Please Backup Before Restoration",Toast.LENGTH_LONG).show();
                            break;
                    }
                }
            };
            AlertDialog.Builder cantRestore = new AlertDialog.Builder(context);
            cantRestore.setMessage("No Backup Exists").setPositiveButton("OK", dialogClickListener).show();
        }
        else if(!is_internal_backup&&is_external_backup&&!is_SDcard_Mounted){
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            showOption();
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            Toast.makeText(context,"Restoration Cancelled",Toast.LENGTH_LONG).show();
                            break;
                    }
                }
            };
            AlertDialog.Builder restore = new AlertDialog.Builder(context);
            restore.setMessage("" +
                    "Will You Proceed Restoration?\n" +
                    "Internal Free Space: "+internal_left_storage+"\n"+
                    "Internal Backup Size: "+"NONE"+"\n"+
                    "SDcard Free Space: "+"NONE"+"\n"+
                    "External Backup Size: "+external_backup_size).
                    setPositiveButton("YES", dialogClickListener).
                    setNegativeButton("NO",dialogClickListener).
                    show();
        }
        else if(!is_internal_backup&&is_external_backup&&is_SDcard_Mounted){
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            showOption();
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            Toast.makeText(context,"Restoration Cancelled",Toast.LENGTH_LONG).show();
                            break;
                    }
                }
            };
            AlertDialog.Builder restore = new AlertDialog.Builder(context);
            restore.setMessage("" +
                    "Will You Proceed Restoration?\n" +
                    "Internal Free Space: "+internal_left_storage+"\n"+
                    "Internal Backup Size: "+"NONE"+"\n"+
                    "SDcard Free Space: "+SDcard_left_storage+"\n"+
                    "External Backup Size: "+external_backup_size).
                    setPositiveButton("YES", dialogClickListener).
                    setNegativeButton("NO",dialogClickListener).
                    show();
        }
        else if(is_internal_backup&&!is_external_backup&&!is_SDcard_Mounted){
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            showOption();
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            Toast.makeText(context,"Restoration Cancelled",Toast.LENGTH_LONG).show();
                            break;
                    }
                }
            };
            AlertDialog.Builder restore = new AlertDialog.Builder(context);
            restore.setMessage("" +
                    "Will You Proceed Restoration?\n" +
                    "Internal Free Space: "+internal_left_storage+"\n"+
                    "Internal Backup Size: "+internal_backup_size+"\n"+
                    "SDcard Free Space: "+"NONE"+"\n"+
                    "External Backup Size: "+"NONE").
                    setPositiveButton("YES", dialogClickListener).
                    setNegativeButton("NO",dialogClickListener).
                    show();
        }
        else if(is_internal_backup&&!is_external_backup&&is_SDcard_Mounted){
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            showOption();
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            Toast.makeText(context,"Restoration Cancelled",Toast.LENGTH_LONG).show();
                            break;
                    }
                }
            };
            AlertDialog.Builder restore = new AlertDialog.Builder(context);
            restore.setMessage("" +
                    "Will You Proceed Restoration?\n" +
                    "Internal Free Space: "+internal_left_storage+"\n"+
                    "Internal Backup Size: "+internal_backup_size+"\n"+
                    "SDcard Free Space: "+SDcard_left_storage+"\n"+
                    "External Backup Size: "+"NONE").
                    setPositiveButton("YES", dialogClickListener).
                    setNegativeButton("NO",dialogClickListener).
                    show();
        }
        else if(is_internal_backup&&is_external_backup&&!is_SDcard_Mounted){
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            showOption();
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            Toast.makeText(context,"Restoration Cancelled",Toast.LENGTH_LONG).show();
                            break;
                    }
                }
            };
            AlertDialog.Builder restore = new AlertDialog.Builder(context);
            restore.setMessage("" +
                    "Will You Proceed Restoration?\n" +
                    "Internal Free Space: "+internal_left_storage+"\n"+
                    "Internal Backup Size: "+internal_backup_size+"\n"+
                    "SDcard Free Space: "+"NONE"+"\n"+
                    "External Backup Size: "+external_backup_size).
                    setPositiveButton("YES", dialogClickListener).
                    setNegativeButton("NO",dialogClickListener).
                    show();
        }
        else if(is_internal_backup&&is_external_backup&&is_SDcard_Mounted){
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            showOption();
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            Toast.makeText(context,"Restoration Cancelled",Toast.LENGTH_LONG).show();
                            break;
                    }
                }
            };
            AlertDialog.Builder restore = new AlertDialog.Builder(context);
            restore.setMessage("" +
                    "Will You Proceed Restoration?\n" +
                    "Internal Free Space: "+internal_left_storage+"\n"+
                    "Internal Backup Size: "+internal_backup_size+"\n"+
                    "SDcard Free Space: "+SDcard_left_storage+"\n"+
                    "External Backup Size: "+external_backup_size).
                    setPositiveButton("YES", dialogClickListener).
                    setNegativeButton("NO",dialogClickListener).
                    show();
        }
    }

    public static String getPrintStackTrace(Exception e) {

        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));

        return errors.toString();

    }

    private void execute(){
        if(is_internal_backup){
            if(internalDest.equals("Internal Storage")){
                try {
                    download("Internal Backup","Internal Storage");
                } catch (InterruptedException e) {
                    String str = getPrintStackTrace(e);
                    Log.e("ERROR",str);
                }
            }
            else if(internalDest.equals("SD Card")){
                try {
                    download("Internal Backup","SD Card");
                } catch (InterruptedException e) {
                    String str = getPrintStackTrace(e);
                    Log.e("ERROR",str);
                }
            }
        }
        if(is_external_backup){
            if(externalDest.equals("Internal Storage")){
                try {
                    download("External Backup","Internal Storage");
                } catch (InterruptedException e) {
                    String str = getPrintStackTrace(e);
                    Log.e("ERROR",str);
                }
            }
            else if(externalDest.equals("SD Card")){
                try {
                    download("External Backup","SD Card");
                } catch (InterruptedException e) {
                    String str = getPrintStackTrace(e);
                    Log.e("ERROR",str);
                }
            }
        }
    }

    private void download(final String in_src, final String in_dst) throws InterruptedException {
        Thread t = new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void run() {
                String src="", dst="";
                if(in_src.equals("Internal Backup")){
                    src = "./SafeMountainBackup/Internal";
                }
                else if(in_src.equals("External Backup")){
                    src = "./SafeMountainBackup/External";
                }
                if(in_dst.equals("Internal Storage")){
                    dst = Environment.getExternalStorageDirectory().getAbsolutePath();
                }
                else if(in_dst.equals("SD Card")){
                    dst = new SDCard().getExternalSDCardPath();
                }
                init_sftp(Host,ID,PW,Port);
                try {
                    recursiveFolderDownload(src,dst);
                } catch (SftpException e) {
                    String str = getPrintStackTrace(e);
                    Log.e("ERROR",str);
                }
                disconnect_sftp();
            }
        });
        t.start();
        t.join();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void recursiveFolderDownload(String src, String dst) throws SftpException {
        channelSftp.lcd(dst);
        channelSftp.cd(src);
        Log.e("Lpwd",channelSftp.lpwd());
        Log.e("Rpwd",channelSftp.pwd());
        String Lpwd = channelSftp.lpwd();
        String Rpwd = channelSftp.pwd();
        Vector<ChannelSftp.LsEntry> fileAndFolderList = channelSftp.ls(".");
        for (ChannelSftp.LsEntry item : fileAndFolderList) {
            File f = new File(Lpwd+"/"+item.getFilename());
            if (!item.getAttrs().isDir()) {
                try{
                    Log.e("File",channelSftp.lpwd()+"/"+item.getFilename());
                    if(Lpwd.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath())){
                        channelSftp.get(Rpwd+"/"+item.getFilename(),Lpwd+"/"+item.getFilename());
                    }
                    else{
                        DocumentFile uri = DocumentFile.fromFile(new File(Lpwd));
                        writeFile(uri,Rpwd,item.getFilename());
                    }
                }catch(SftpException se){
                    se.printStackTrace();
                }
            } else if (!(".".equals(item.getFilename()) || "..".equals(item.getFilename()))) {
                f.mkdirs();
                //TODO: will mkdir need SAF too?
                recursiveFolderDownload("./" + item.getFilename(), "./" + item.getFilename());
            }
        }
        channelSftp.lcd("..");
        channelSftp.cd("..");
    }

    private void writeFile(DocumentFile pickedDir, String src, String fileName) {
        try {
            DocumentFile file = pickedDir.createFile("image/jpeg", fileName);
            OutputStream out = context.getContentResolver().openOutputStream(file.getUri());
            try {
                channelSftp.get(src,out);
            } finally {
                out.close();
            }
        } catch (IOException | SftpException e) {
            throw new RuntimeException("Something went wrong : " + e.getMessage(), e);
        }
    }

    private void showOption(){
        String[] options1 = {"Internal Storage", "SD Card", "Do Not Restore"};
        String[] options2 = {"Internal Storage", "Do Not Restore"};

        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout restoreLayout =
                (LinearLayout) vi.inflate(R.layout.restore_dialog, null);
        final LinearLayout internalRestore = restoreLayout.findViewById(R.id.InternalRestoreBlock);
        final LinearLayout externalRestore = restoreLayout.findViewById(R.id.ExternalRestoreBlock);
        if(!is_internal_backup){internalRestore.setVisibility(GONE);}
        if(!is_external_backup){externalRestore.setVisibility(GONE);}
        final Spinner internalSpinner = restoreLayout.findViewById(R.id.InternalSpinner);
        final Spinner externalSpinner = restoreLayout.findViewById(R.id.ExternalSpinner);
        ArrayAdapter<String> adapter;
        if(is_SDcard_Mounted){adapter = new ArrayAdapter<String>(context,android.R.layout.simple_spinner_item, options1);}
        else {adapter = new ArrayAdapter<String>(context,android.R.layout.simple_spinner_item, options2);}
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        internalSpinner.setAdapter(adapter);
        externalSpinner.setAdapter(adapter);
        new AlertDialog.Builder(context).setTitle("Restore Location Setting").setView(restoreLayout).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(context,"Restoration Cancelled",Toast.LENGTH_LONG).show();
            }
        }).setPositiveButton("RESTORE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                internalDest = internalSpinner.getSelectedItem().toString();
                externalDest = externalSpinner.getSelectedItem().toString();
                execute();
            }
        }).show();
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
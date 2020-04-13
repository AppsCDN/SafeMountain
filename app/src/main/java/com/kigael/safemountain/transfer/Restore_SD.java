package com.kigael.safemountain.transfer;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.documentfile.provider.DocumentFile;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

public class Restore_SD extends Thread implements Runnable {
    private Context context;
    private Session session;
    private Channel channel;
    private ChannelSftp channelSftp;
    private JSch jsch;
    private String Host;
    private String ID;
    private String PW;
    private int Port;
    public static String src;
    private DocumentFile doc;

    public Restore_SD(Context context, DocumentFile doc){
        this.context = context;
        this.doc = doc;
        this.Host = getHOST(context);
        this.ID = getID(context);
        this.PW = getPW(context);
        this.Port = getPORT(context);
    }

    @Override
    public void run() {
        super.run();
        init_sftp(Host,ID,PW,Port);
        try {
            recursiveFolderDownload(src,doc);
        } catch (SftpException e) {
            e.printStackTrace();
        }
        disconnect_sftp();
    }

    private void recursiveFolderDownload(String src, DocumentFile pickedDir) throws SftpException {
        Log.e("dst",pickedDir.getUri().toString());
        Vector<ChannelSftp.LsEntry> fileAndFolderList = channelSftp.ls(src);
        for (ChannelSftp.LsEntry item : fileAndFolderList) {
            if (!item.getAttrs().isDir()) {
                DocumentFile newFile = pickedDir.createFile("",item.getFilename());
                write(src + "/" + item.getFilename(),newFile.getUri());
            } else if (!(".".equals(item.getFilename()) || "..".equals(item.getFilename()))) {
                DocumentFile newDir = pickedDir.createDirectory(item.getFilename());
                recursiveFolderDownload(src + "/" + item.getFilename(), newDir);
            }
        }
    }

    private void write(String src, Uri uri){
        Log.e(src,uri.toString());
        try {
            ParcelFileDescriptor descriptor=context.getContentResolver().openFileDescriptor(uri,"w");
            if(descriptor!=null) {
                FileOutputStream fos=new FileOutputStream(descriptor.getFileDescriptor());
                channelSftp.get(src,fos);
                fos.close();
            }
        } catch (IOException | SftpException e) {
            Log.e("Error", "again "+e.getMessage());
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

    private void disconnect_sftp(){
        if(channelSftp!=null){channelSftp.disconnect();}
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

package com.kigael.safemountain.ui.mountain;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
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
import java.io.FileReader;
import java.util.ArrayList;

public class MountainFragment extends Fragment {
    private JSch jsch;
    private Channel channel;
    private Session session;
    private ChannelSftp channelSftp;
    private ChannelExec channelExec;
    private ByteArrayOutputStream baos;
    private boolean serverConnection;
    private boolean is_internal_backup;
    private boolean is_external_backup;
    private long internalBlocks=0;
    private long externalBlocks=0;
    private long totalFreeBlocks=0;
    private String Host;
    private String ID;
    private String PW;
    private int Port;
    private String result = "";
    private String[] split;
    private Context context;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_mountain, container, false);
        context = container.getContext();
        final PieChart mountainPieChart = root.findViewById(R.id.mountainPieChart);
        Host = getHOST(context);
        ID = getID(context);
        PW = getPW(context);
        Port = getPORT(context);
        try {
            connectToServer(Host,ID,PW,Port);
        } catch (InterruptedException e) {
            serverConnection = false;
        }
        if(serverConnection){
            try {
                checkIfInternalBackedUp();
            } catch (InterruptedException e) {
                is_internal_backup = false;
            }
            try {
                checkIfExternalBackedUp();
            } catch (InterruptedException e) {
                is_external_backup = false;
            }
            try {
                runCommand(Host,ID,PW,Port);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ArrayList storageData = new ArrayList();
            if(is_internal_backup){
                storageData.add(new PieEntry(internalBlocks,"Internal Backup: "+String.format("%.2f",(float)internalBlocks/1024/1024)+" GB"));
            }
            if(is_external_backup){
                storageData.add(new PieEntry(externalBlocks,"SDcard Backup: "+String.format("%.2f",(float)externalBlocks/1024/1024)+" GB"));
            }
               storageData.add(new PieEntry(totalFreeBlocks,"Free Space: "+String.format("%.2f",(float)totalFreeBlocks/1024/1024)+" GB"));
            PieDataSet pieDataSet = new PieDataSet(storageData,"");
            pieDataSet.setDrawValues(false);
            pieDataSet.setColors(ColorTemplate.LIBERTY_COLORS);
            PieData pieData = new PieData(pieDataSet);
            mountainPieChart.setRotationEnabled(false);
            mountainPieChart.setData(pieData);
            mountainPieChart.setHoleColor(0);
            mountainPieChart.setCenterText("Safe Mountain\nBackup Storage Status");
            mountainPieChart.setCenterTextColor(Color.WHITE);
            mountainPieChart.setCenterTextSize(16f);
            mountainPieChart.getDescription().setEnabled(false);
            mountainPieChart.getLegend().setOrientation(Legend.LegendOrientation.VERTICAL);;
            mountainPieChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
            mountainPieChart.getLegend().setTextColor(Color.WHITE);
            mountainPieChart.getLegend().setTextSize(16f);
            mountainPieChart.getLegend().setYOffset(10f);
            mountainPieChart.setDrawEntryLabels(false);
            mountainPieChart.animate();
        }
        else{
            Toast.makeText(context,"Server connection failed",Toast.LENGTH_LONG).show();
        }
        return root;
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

    private void runCommand(final String host, final String userName, final String password, final int port) throws InterruptedException {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                init_exec(host,userName,password,port,"df ./SafeMountainBackup | grep $HOME");
                while(result.isEmpty()) {
                    result = new String(baos.toByteArray());
                }
                result = result.substring(0, result.length() - 1);
                split = result.split("\\s+");
                totalFreeBlocks = Long.parseLong(split[3]);
                disconnect_exec();
                result = "";
                if(is_internal_backup) {
                    init_exec(host, userName, password, port, "du -s ./SafeMountainBackup/Internal");
                    while (result.isEmpty()) {
                        result = new String(baos.toByteArray());
                    }
                    result = result.substring(0, result.length() - 1);
                    split = result.split("\\s+");
                    internalBlocks = Long.parseLong(split[0]);
                    disconnect_exec();
                    result = "";
                }
                if(is_external_backup){
                    init_exec(host,userName,password,port,"du -s ./SafeMountainBackup/External");
                    while(result.isEmpty()) {
                        result = new String(baos.toByteArray());
                    }
                    result = result.substring(0, result.length() - 1);
                    split = result.split("\\s+");
                    externalBlocks = Long.parseLong(split[0]);
                    disconnect_exec();
                    result = "";
                }
            }
        });
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
        t.join();
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

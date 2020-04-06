package com.sovworks.safemountain;

import android.content.Context;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.util.Properties;

public class Login {
    private String id;
    private String pw;
    private final static String host = MainActivity.HOST;
    private final static int port = MainActivity.PORT;
    public static boolean result;

    public Login(String in_id, String in_pw) throws InterruptedException {
        id = in_id;
        pw = in_pw;
        tryLogin();
    }

    private void tryLogin() throws InterruptedException {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Session session;
                JSch jsch;
                try {
                    jsch = new JSch();
                    session = jsch.getSession(id, host, port);
                    session.setPassword(pw);
                    Properties prop = new Properties();
                    prop.put("StrictHostKeyChecking", "no");
                    session.setConfig(prop);
                    session.connect();
                    if(session.isConnected()){
                        result = true;
                        session.disconnect();
                    }else{
                        result = false;
                    }
                }catch (Exception e){
                        result = false;
                }
            }
        });
        t.start();
        t.join();
    }

}

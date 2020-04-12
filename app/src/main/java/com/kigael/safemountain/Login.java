package com.kigael.safemountain;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.util.Properties;

public class Login {
    private String id;
    private String pw;
    private static String host;
    private static int port;
    public static boolean result;

    public Login(String in_id, String in_pw, String in_host, int in_port) throws InterruptedException {
        id = in_id;
        pw = in_pw;
        host = in_host;
        port = in_port;
        result = false;
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
                    session.setTimeout(3000);
                    session.connect();
                    if(session.isConnected()){
                        result = true;
                        session.disconnect();
                        return;
                    }else{
                        result = false;
                        return;
                    }
                }catch (Exception e){
                        result = false;
                        return;
                }
            }
        });
        t.start();
        t.join();
    }

}

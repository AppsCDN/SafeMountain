package com.sovworks.safemountain;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.FileObserver;
import android.os.IBinder;
import android.os.Environment;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class FileSystemObserverService extends Service {
    private final ReentrantLock locker = new ReentrantLock();
    String externalPath = "";

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        observe();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final String strId = getString(R.string.noti_channel_id);
            final String strTitle = getString(R.string.app_name);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = notificationManager.getNotificationChannel(strId);
            if (channel == null) {
                channel = new NotificationChannel(strId, strTitle, NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(channel);
            }
            Notification notification = new NotificationCompat.Builder(this, strId).build();
            startForeground(1, notification);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        } else {
            stopSelf();
        }
    }

    public File getExtenerStoragePath() {
        return Environment.getExternalStorageDirectory();
    }

    public void observe() {
        Thread t;
        t = new Thread(new Runnable() {

            @Override
            public void run() {

                //File[]    listOfFiles = new File(path).listFiles();
                File str = getExtenerStoragePath();
                if (str != null) {
                    externalPath = str.getAbsolutePath();
                    new Obsever(externalPath).startWatching();
                }
            }
        });
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    class Obsever extends FileObserver {

        List<SingleFileObserver> mObservers;
        String mPath;
        int mMask;

        public Obsever(String path) {
            // TODO Auto-generated constructor stub
            this(path, ALL_EVENTS);
        }

        public Obsever(String path, int mask) {
            super(path, mask);
            mPath = path;
            mMask = mask;
            // TODO Auto-generated constructor stub

        }

        @Override
        public void startWatching() {
            // TODO Auto-generated method stub
            if (mObservers != null)
                return;
            mObservers = new ArrayList<SingleFileObserver>();
            Stack<String> stack = new Stack<String>();
            stack.push(mPath);
            while (!stack.empty()) {
                String parent = stack.pop();
                mObservers.add(new SingleFileObserver(parent, mMask));
                File path = new File(parent);
                File[] files = path.listFiles();
                if (files == null) continue;
                for (int i = 0; i < files.length; ++i) {
                    if (files[i].isDirectory() && !files[i].getName().equals(".") && !files[i].getName().equals("..")) {
                        stack.push(files[i].getPath());
                    }
                }
            }
            for (int i = 0; i < mObservers.size(); i++) {
                mObservers.get(i).startWatching();
            }
        }

        @Override
        public void stopWatching() {
            // TODO Auto-generated method stub
            if (mObservers == null)
                return;
            for (int i = 0; i < mObservers.size(); ++i) {
                mObservers.get(i).stopWatching();
            }
            mObservers.clear();
            mObservers = null;
        }

        @Override
        public void onEvent(int event, final String path) {
            if (event == FileObserver.CREATE && (!path.equals(".probe"))) {
                //writeLog("CREATE",path);
                Log.d("CREATE",path);
            } else if (event == FileObserver.DELETE_SELF || event == FileObserver.DELETE) {
                //writeLog("DELETE",path);
                Log.d("DELETE",path);
            } else if (event == FileObserver.MOVE_SELF || event == FileObserver.MOVED_FROM || event == FileObserver.MOVED_TO) {
                //writeLog("MOVE",path);
                Log.d("MOVE",path);
            }
        }

        private class SingleFileObserver extends FileObserver {
            private String mPath;

            public SingleFileObserver(String path, int mask) {
                super(path, mask);
                // TODO Auto-generated constructor stub
                mPath = path;
            }

            @Override
            public void onEvent(int event, String path) {
                // TODO Auto-generated method stub
                String newPath = mPath + "/" + path;
                Obsever.this.onEvent(event, newPath);
            }

        }

    }

    public void writeLog(final String event, final String path){
        String filename = getFilesDir().toString()+"/log.txt";
        String to_write = event+" "+path+"\n";
        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true));
            bw.write(to_write);
            bw.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
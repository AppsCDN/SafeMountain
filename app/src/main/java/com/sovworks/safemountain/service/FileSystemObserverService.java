package com.sovworks.safemountain.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.FileObserver;
import android.os.IBinder;
import android.os.Environment;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.sovworks.safemountain.MainActivity;
import com.sovworks.safemountain.R;

public class FileSystemObserverService extends Service {
    public static int Observer_Count;
    private static Thread Observers;
    private static final String[] Forbidden_List = {"mtptemp", ".tmp",".mtp",".thumbnails",".face",".crdownload","com.",".chromium"};
    private String sql;
    private Cursor cursor;
    String externalPath = "";
    String internalPath = "";

    @Override
    public IBinder onBind(Intent intent) {
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
        while(!Observers.isInterrupted()){
            Observers.interrupt();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        } else {
            stopSelf();
        }
    }

    public File getExtenerStoragePath() {
        return Environment.getExternalStorageDirectory();
    }

    public File getSDStoragePath() { return Environment.getExternalStorageDirectory().getParentFile().getParentFile(); }

    public class ObserverThread extends Thread{
        private Obsever in,ex;
        private boolean isInterrupted = false;

        @Override
        public void interrupt() {
            super.interrupt();
            isInterrupted = true;
            if(in!=null){in.stopWatching();}
            if(ex!=null){ex.stopWatching();}
        }

        @Override
        public boolean isInterrupted() {
            return isInterrupted;
        }

        @Override
        public synchronized void start() {
            super.start();
            File str = getExtenerStoragePath();
            if (str != null) {
                internalPath = str.getAbsolutePath();
                in = new Obsever(internalPath);
                in.startWatching();
            }
            str = getSDStoragePath();
            if (str != null) {
                externalPath = str.getAbsolutePath();
                ex = new Obsever(externalPath);
                ex.startWatching();
            }
        }
    }


    public void observe() {
        Observers = new ObserverThread();
        Observers.setPriority(Thread.MIN_PRIORITY);
        Observers.start();
    }

    private void RE(){
        while(!Observers.isInterrupted()){
            Observers.interrupt();
        }
        observe();
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
                Observer_Count++;
                mObservers.get(i).startWatching();
            }
        }

        @Override
        public void stopWatching() {
            // TODO Auto-generated method stub
            if (mObservers == null)
                return;
            for (int i = 0; i < mObservers.size(); ++i) {
                Observer_Count--;
                mObservers.get(i).stopWatching();
            }
            mObservers.clear();
            mObservers = null;
        }

        @Override
        public void onEvent(int in_event, String path) {
            if(!Thread.currentThread().isInterrupted()){
                int event = 0x00FFFFFF & in_event;
                if(isForbidden(path)){
                    if(event==FileObserver.MOVED_TO||event==FileObserver.CLOSE_WRITE||event==FileObserver.CREATE){
                        if(new File(path).isDirectory()){
                            RE();
                        }
                        else{
                            InsertPath(path);
                        }
                    }
                    else if(event==FileObserver.DELETE||event==FileObserver.DELETE_SELF||event==FileObserver.MOVED_FROM){
                        if(!new File(path).isDirectory()){
                            DeletePath(path);
                        }
                    }
                }
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

    private void InsertPath(String path){
        sql = "select ID from Files_To_Transfer where PATH = " + "\"" + path + "\"";
        cursor = MainActivity.database.rawQuery(sql,null);
        if(cursor.getCount()==0){
            cursor.close();
            sql = "insert into Files_To_Transfer (PATH) values ("+"\""+path+"\""+")";
            MainActivity.database.execSQL(sql);
            Log.e("DB insert",path);
        }
    }

    private void DeletePath(String path){
        sql = "delete from Files_To_Transfer where PATH = " + "\"" + path + "\"";
        MainActivity.database.execSQL(sql);
        Log.e("DB delete",path);
    }

    private boolean isForbidden(String path){
        if(path.endsWith("null")) return false;
        else if(path.endsWith("/")) return false;
        for(String str:Forbidden_List){
            if(path.contains(str)){return false;}
        }
        return true;
    }

}
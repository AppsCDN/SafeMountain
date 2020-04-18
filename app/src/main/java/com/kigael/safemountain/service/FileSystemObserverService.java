package com.kigael.safemountain.service;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
import androidx.core.app.NotificationCompat;
import com.kigael.safemountain.MainActivity;
import com.kigael.safemountain.R;
import com.kigael.safemountain.transfer.Backup;

public class FileSystemObserverService extends Service {
    public static int Observer_Count;
    private static Thread Observers;
    private static final String[] Forbidden_List = {"mtptemp", ".tmp",".mtp",".thumbnails",".face",".crdownload","com.",".chromium",".cache"};
    public static boolean is_running = false;
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
        backup();
        is_running = true;
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final String strId = getString(R.string.noti_channel_id_observer);
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
        is_running = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        } else {
            stopSelf();
        }
    }

    public File getExtenerStoragePath() {
        return Environment.getExternalStorageDirectory();
    }

    public File getSDStoragePath() {
        String SDcardPath = new SDCard().getExternalSDCardPath();
        if(SDcardPath==null) return null;
        else return new File(SDcardPath);
    }

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

    public void backup(){
        Thread t = new Backup(getApplicationContext());
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
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

        private Obsever(String path) {
            this(path, ALL_EVENTS);
        }

        private Obsever(String path, int mask) {
            super(path, mask);
            mPath = path;
            mMask = mask;
        }

        @Override
        public void startWatching() {
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
                    if (files[i].isDirectory() && !files[i].getName().equals(".") && !files[i].getName().equals("..")&& !files[i].getName().equals("Android")) {
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
                            DeletePath(path, event);
                        }
                    }
                }
            }
        }

        private class SingleFileObserver extends FileObserver {
            private String mPath;

            private SingleFileObserver(String path, int mask) {
                super(path, mask);
                mPath = path;
            }

            @Override
            public void onEvent(int event, String path) {
                String newPath = mPath + "/" + path;
                Obsever.this.onEvent(event, newPath);
            }

        }

    }

    private void InsertPath(String path){
        File f = new File(path);
        if(!f.isDirectory()){
            sql = "SELECT * FROM Files_To_Transfer WHERE PATH =" + "\"" + path + "\"";
            cursor = MainActivity.database.rawQuery(sql,null);
            if(cursor==null||cursor.getCount()==0){
                if(cursor!=null){
                    cursor.close();
                }
                sql = "INSERT INTO Files_To_Transfer (PATH) VALUES ("+"\""+path+"\""+")";
                MainActivity.database.execSQL(sql);
            }
        }
    }

    private void DeletePath(String path, int event){
        File f = new File(path);
        if(!f.isDirectory()){
            sql = "DELETE FROM Files_To_Transfer WHERE PATH = "+"\""+path+"\"";
            MainActivity.database.execSQL(sql);
            if(event==FileObserver.MOVED_FROM){
                sql = "INSERT INTO Files_To_Delete (PATH) VALUES ("+"\""+path+"\""+")";
                MainActivity.database.execSQL(sql);
            }
        }
    }

    private boolean isForbidden(String path){
        if(path.endsWith("null")) return false;
        else if(path.endsWith("/")) return false;
        for(String str:Forbidden_List){
            if(path.contains(str)){return false;}
        }
        return true;
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

}
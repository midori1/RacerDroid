package com.appcheck.event;

import android.os.AsyncTask;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.UiDevice;
import android.util.Log;


public class Instrumentor {
    private static final String TAG = "MessageSchedulerInAPP";
    public static final String INSTRUMENTED_EVENT = "doPressBack";
    public static final String TARGET_MESSAGE_HANDLER = "org.openintents.filemanager.lists.FileListFragment$FileListMessageHandler";
    public static final int TARGET_MESSAGE_WHAT = 500;

    private static final int MSG_WINDOW_FOCUS_CHANGED = 6;
    private static final int RESUME_ACTIVITY = 107;

	public static boolean INSTRUMENT = false;
    
    public static UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    public static String waitingMessageHandler;
    public static int waitingMessageWhat;


    private static void notifyMessageScheduler(String handler, int what){
        waitingMessageHandler = handler;
        waitingMessageWhat = what;
    }

    private static void setWaitingMessage(String handler, int what){
        notifyMessageScheduler(handler, what);
    }

    public static void doPressBack(){
            Log.d(TAG, "start do press back");
            Thread thread = new Thread(){
                @Override
                public void run(){
                    mDevice.pressBack();
                }
            };
            thread.start();
            notifyMessageScheduler("org.openintents.filemanager.lists.FileListFragment$FileListMessageHandler", TARGET_MESSAGE_WHAT);
    }

    public static void doRotateEvents(){
        try {
            Log.d(TAG, "start do rotate events");
            mDevice.setOrientationRight();
            Thread.sleep(500L);
            mDevice.setOrientationNatural();
            Log.d(TAG, "end do rotate events");
            notifyMessageScheduler("android.view.ViewRootImpl$ViewRootHandler", MSG_WINDOW_FOCUS_CHANGED);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static void doSleepWakeUpEvents(){
        try{
            mDevice.sleep();
            Log.d(TAG, "device sleep");
            Thread.sleep(500L);
            mDevice.wakeUp();
            Thread.sleep(500L);
            Log.d(TAG, "device wake up");
            mDevice.swipe(400, 1500, 500, 500, 2);
            Log.d(TAG, "swipe up");
            notifyMessageScheduler("android.app.ActivityThread$H", RESUME_ACTIVITY);
        }catch(RemoteException e){
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

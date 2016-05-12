package com.appcheck.event;

import android.os.AsyncTask;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.UiDevice;
import android.util.Log;


public class Instrumentor {
    private static final String TAG = Instrumentor.class.getSimpleName();
    public static final String INSTRUMENTED_EVENT = "doNotifyMessageScheduler";
    public static final String TARGET_MESSAGE_HANDLER = "android.app.ActivityThread$H";
    public static final int TARGET_MESSAGE_WHAT = 0x1;
    public static final String TARGET_MESSAGE_CALLBACK = "android.app.LoadedApk$ServiceDispatcher$RunConnection";
    
    public static final boolean targetUseWhat = false;

    private static final int MSG_WINDOW_FOCUS_CHANGED = 6;
    private static final int RESUME_ACTIVITY = 107;
    private static final int DESTROY_ACTIVITY = 109;
    private static final int STOP_SERVICE = 116;

	public static boolean INSTRUMENT = false;

    public static UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    
    public static String waitingMessageHandler;
    public static int waitingMessageWhat;
    public static String waitingMessageCallback;
    public static boolean waitingUseWhat = true;


    private static void notifyMessageScheduler(String handler, int what){
        waitingMessageHandler = handler;
        waitingMessageWhat = what;
    }

    private static void notifyMessageScheduler(String handler, String callback){
        waitingMessageHandler = handler;
        waitingMessageCallback = callback;
    }

    public static void doNotifyMessageScheduler(){
        Log.i(TAG, "start do notify message scheduler");
        notifyMessageScheduler("android.app.ActivityThread$H", STOP_SERVICE);
        Log.i(TAG, "end do notify message scheduler");

    }

    public static void doRotateEvents(){
        try {
            Log.i(TAG, "start do rotate events");
            mDevice.setOrientationRight();
            Thread.sleep(500L);
            mDevice.setOrientationNatural();
            Log.i(TAG, "end do rotate events");
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
            Log.i(TAG, "device sleep");
            Thread.sleep(500L);
            mDevice.wakeUp();
            Thread.sleep(500L);
            Log.i(TAG, "device wake up");
            mDevice.swipe(400, 1500, 500, 500, 2);
            Log.i(TAG, "swipe up");
            notifyMessageScheduler("android.app.ActivityThread$H", RESUME_ACTIVITY);
        }catch(RemoteException e){
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

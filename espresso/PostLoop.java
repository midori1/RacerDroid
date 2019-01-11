package android.support.test.espresso.extra;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by nikle on 17-4-3.
 */
public class PostLoop {

    /**
     * make sure the Runnable object is posted only once
     */
    static boolean IsMyLoopPosted = false;


    /**
     * created by JiahongZhou
     */
    public static void postMyLoopToMainThread() {


        if(IsMyLoopPosted)
            return;
        IsMyLoopPosted = true;

        /**
         * run in main thread
         */
        Runnable runInMainThread = new Runnable() {
            @Override
            public void run() {
                //get messageQueue by Looper
                MessageQueue messageQueue = Looper.getMainLooper().getQueue();

                //get the method of MessageQueue by reflect
                //note: we can't invoke the method directly because the next method can be access by the class in same package
                //when we get the method by reflect we can invoke the method by next.setAccessible(true)
                Method next = null;
                try {
                    next = Looper.getMainLooper().getQueue().getClass().getDeclaredMethod("next");
                    next.setAccessible(true);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }


                //loop dispatch messages
                for (; ; ) {
//          Log.i("MyLoop", "start");
                    //invoke method next of messageQueue without any parameter
                    Message message = null;
                    try {
                        message = (Message) next.invoke(messageQueue,null);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                    if(message == null) {
                        Log.i("MyLoop", "exit : message is null");
                        return;
                    }


                    Log.i("MyLoop", "scheduleMessage" + message);
//                    message.getTarget().dispatchMessage(message);
                    MessageScheduler.scheduleMessage(message);

                    //get the method of Message
//          Method recycleUnchecked = null;
//          try {
//            recycleUnchecked = message.getClass().getDeclaredMethod("recycleUnchecked");
//            recycleUnchecked.setAccessible(true);
//          } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//          }

                    message.recycle();

//          //invoke the method recycleUnchecked of message without any parameter
//          try {
//            recycleUnchecked.invoke(message, null);
//
//          } catch (IllegalAccessException e) {
//            e.printStackTrace();
//          } catch (InvocationTargetException e) {
//            e.printStackTrace();
//          }

//          recycler.recycle(message);
//          message.recycle();
//          Log.i("MyLoop", "end");
                }
            }
        };


        //post the Runnable object to main thread
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(runInMainThread);


    }
}

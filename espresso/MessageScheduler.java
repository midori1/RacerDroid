package android.support.test.espresso.extra;

import android.app.Activity;
import android.content.ComponentName;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.util.Log;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;



/**
 * Created by nikle on 16-3-17.
 *
 */
public class MessageScheduler {

    private static final String TAG = "MessageInfo";// MessageScheduler.class.getSimpleName();
    private static final String INSTRUMENTOR_CLASSNAME = "com.appcheck.event.Instrumentor";
    private static Method instrumentMethod;
    private static Class<?> instrumentorClass;
    private static LinkedList<Message> backupMessageQueue;

    private static String targetMessageHandler;
    private static int targetMessageWhat;
    private static String targetMessageCallback;

    private static boolean targetUseWhat;
    private static boolean waitingUseWhat;

    private static String waitingMessageHandler;
    private static int waitingMessageWhat;
    private static String waitingMessageCallback;

    private static boolean instrument = false;

    //sometimes we need to choose second message, sometimes is third message(those messages have no difference)
    private static int occurNumWaiting = 0;
    private static int occurNumTarget = 0;

    private static int targetNum;
    private static int waitingNum;

    //make sure the scheduleMessage is right the waitingMessage was dispatched before the targetMessage
    private static boolean waitingDispatched = false;

    private static boolean waitingMessageEqualsToTargetMessage = false;

    static{
        try{
            instrumentorClass = Class.forName(INSTRUMENTOR_CLASSNAME);

            instrument  = (boolean) instrumentorClass.getDeclaredField("INSTRUMENT").get(null);

            targetMessageHandler = (String) instrumentorClass.getDeclaredField("TARGET_MESSAGE_HANDLER").get(null);
            targetMessageWhat = (int) instrumentorClass.getDeclaredField("TARGET_MESSAGE_WHAT").get(null);
            targetMessageCallback = (String) instrumentorClass.getDeclaredField("TARGET_MESSAGE_CALLBACK").get(null);

            targetUseWhat = (boolean) instrumentorClass.getDeclaredField("targetUseWhat").get(null);

            instrumentMethod = instrumentorClass.getMethod((String) instrumentorClass.getDeclaredField("INSTRUMENTED_EVENT").get(null));

            targetNum = (int) instrumentorClass.getDeclaredField("targetNum").get(null);
            waitingNum = (int) instrumentorClass.getDeclaredField("waitingNum").get(null);

            waitingMessageEqualsToTargetMessage = (boolean) instrumentorClass.getDeclaredField("waitingMessageEqualsToTargetMessage").get(null);

            Log.i(TAG, "targetNum " + targetNum + "\nwaitingNum " + waitingNum);
            backupMessageQueue = new LinkedList<>();
            Log.i(TAG, "init successfully found class");
        }catch (ClassNotFoundException e){
            Log.e(TAG, "Can not found " + INSTRUMENTOR_CLASSNAME);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Can not found Method ");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, e.toString());
        }
    }



    //target: we make sure that waitingMessage be dispatched before targetMassage
    public static void scheduleMessage(Message msg){
        Log.i(TAG, "scheduleMessage");
        if(instrument && !waitingMessageEqualsToTargetMessage){
            try {
                Log.i(TAG, "received message :  " + msg.toString());

                if(!waitingDispatched && targetUseWhat && msg.getTarget().getClass().getName().equals(targetMessageHandler) && msg.what == targetMessageWhat){
                    occurNumTarget++;
                    if(occurNumTarget == targetNum) {
                        Log.i(TAG, "got target message by msg.what" + msg.toString());
                        backupMessageQueue.add(Message.obtain(msg));
                        instrumentMethod.invoke(null);
                        waitingMessageHandler = (String) instrumentorClass.getDeclaredField("waitingMessageHandler").get(null);
                        waitingUseWhat = (boolean) instrumentorClass.getDeclaredField("waitingUseWhat").get(null);
                        if (waitingUseWhat)
                            waitingMessageWhat = (int) instrumentorClass.getDeclaredField("waitingMessageWhat").get(null);
                        else
                            waitingMessageCallback = (String) instrumentorClass.getDeclaredField("waitingMessageCallback").get(null);
                        targetMessageHandler = "";
                    }else{
                        msg.getTarget().dispatchMessage(msg);
                    }
                }else if(!waitingDispatched && !targetUseWhat && msg.getTarget().getClass().getName().equals(targetMessageHandler)
                        && msg.getCallback() != null && msg.getCallback().getClass().getName().startsWith(targetMessageCallback)) {

                    occurNumTarget++;
                    if(occurNumTarget == targetNum) {
                        Log.i(TAG, "got target message by msg.callback" + msg.toString());
                        backupMessageQueue.add(Message.obtain(msg));
                        instrumentMethod.invoke(null);
                        waitingMessageHandler = (String) instrumentorClass.getDeclaredField("waitingMessageHandler").get(null);
                        waitingUseWhat = (boolean) instrumentorClass.getDeclaredField("waitingUseWhat").get(null);
                        if (waitingUseWhat)
                            waitingMessageWhat = (int) instrumentorClass.getDeclaredField("waitingMessageWhat").get(null);
                        else
                            waitingMessageCallback = (String) instrumentorClass.getDeclaredField("waitingMessageCallback").get(null);


                        targetMessageHandler = "";
                    }else{
                        msg.getTarget().dispatchMessage(msg);
                    }

                }else if(waitingUseWhat && msg.getTarget().getClass().getName().equals(waitingMessageHandler)
                        && msg.what == waitingMessageWhat){
                    occurNumWaiting++;
                    if(occurNumWaiting == waitingNum) {
                        Log.i(TAG, "got waiting message by msg.what, dispatch messages in queue" + msg.toString());

                        msg.getTarget().dispatchMessage(msg);
                        Message targetMessage = backupMessageQueue.poll();
                        if(targetMessage != null){
                            Log.i(TAG, "targetMessage : " + targetMessage.toString());
                            targetMessage.getTarget().dispatchMessage(targetMessage);
                        }
                        waitingDispatched = true;
                    }else msg.getTarget().dispatchMessage(msg);
                }else if(!waitingUseWhat && msg.getTarget().getClass().getName().equals(waitingMessageHandler)
                        && msg.getCallback() != null && msg.getCallback().getClass().getName().equals(waitingMessageCallback)){
                    occurNumWaiting++;
                    if(occurNumWaiting == waitingNum) {
                        Log.i(TAG, "got waiting message by msg.callback, dispatch messages in queue" + msg.toString());

                        msg.getTarget().dispatchMessage(msg);
                        Message targetMessage = backupMessageQueue.poll();
                        if (targetMessage != null) {
                            Log.i(TAG, "targetMessage : " + targetMessage.toString());
                            targetMessage.getTarget().dispatchMessage(targetMessage);

                        }
                        waitingDispatched = true;
                    }else msg.getTarget().dispatchMessage(msg);
                }
                else{
                    msg.getTarget().dispatchMessage(msg);
                }

            } catch (IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
                Log.e(TAG, e.toString());
            }
        }else if(instrument ){
            //we can get them by different num(targetNum and waitingNum)
            Log.i(TAG, "waiting message equals to target message");
            Log.i(TAG, "received message :  " + msg.toString());
//            waitingMessageEqualsToTargetMessage always be true now
            if(targetUseWhat && msg.getTarget().getClass().getName().equals(targetMessageHandler) && msg.what == targetMessageWhat){
                occurNumTarget++;
                occurNumWaiting++;
                if(occurNumTarget == targetNum) {
                    Log.i(TAG, "got target message by msg.what" + msg.toString());
                    backupMessageQueue.add(Message.obtain(msg));

//                    targetMessageHandler = "";
                }else if(occurNumWaiting == waitingNum){
                    Log.i(TAG, "got waiting message by msg.what" + msg.toString());

                    msg.getTarget().dispatchMessage(msg);
                    Message targetMessage = backupMessageQueue.poll();
                    if (targetMessage != null) {
                        Log.i(TAG, "targetMessage : " + targetMessage.toString());
                        targetMessage.getTarget().dispatchMessage(targetMessage);

                    }
                }
                else{
                    msg.getTarget().dispatchMessage(msg);
                }
            }else if(!targetUseWhat && msg.getTarget().getClass().getName().equals(targetMessageHandler)
                    && msg.getCallback() != null && msg.getCallback().getClass().getName().startsWith(targetMessageCallback)) {

                occurNumTarget++;
                occurNumWaiting++;
                if(occurNumTarget == targetNum) {
                    Log.i(TAG, "got target message by msg.callback" + msg.toString());
                    backupMessageQueue.add(Message.obtain(msg));

//                    targetMessageHandler = "";
                }else if(occurNumWaiting == waitingNum){
                    Log.i(TAG, "got waiting message by msg.callback" + msg.toString());

                    msg.getTarget().dispatchMessage(msg);
                    Message targetMessage = backupMessageQueue.poll();
                    if (targetMessage != null) {
                        Log.i(TAG, "targetMessage : " + targetMessage.toString());
                        targetMessage.getTarget().dispatchMessage(targetMessage);

                    }
                }else{
                    msg.getTarget().dispatchMessage(msg);
                }

            }else{
                msg.getTarget().dispatchMessage(msg);
            }
        }
        else{
            msg.getTarget().dispatchMessage(msg);
        }

    }





}

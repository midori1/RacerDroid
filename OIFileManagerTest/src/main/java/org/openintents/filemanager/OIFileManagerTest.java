package org.openintents.filemanager.appchecktest;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.RemoteException;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.test.ActivityInstrumentationTestCase2;
import android.support.test.espresso.intent.Intents;

import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiSelector;

import com.squareup.okhttp.mockwebserver.Dispatcher;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import org.junit.Before;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Queue;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Semaphore;

import com.appcheck.event.Instrumentor;

import android.support.test.espresso.Espresso;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.typeTextIntoFocusedView;
import static android.support.test.espresso.contrib.PickerActions.setTime;
//import static android.support.test.espresso.extension.action.OrientationChangeAction.orientationLandscape;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.matcher.ViewMatchers.withHint;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.isClickable;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.anyIntent;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static com.checkdroid.crema.EspressoPlus.*;

public class OIFileManagerTest extends ActivityInstrumentationTestCase2<Activity>{

    private static final String LAUNCHER_ACTIVITY_CLASSNAME = "org.openintents.filemanager.FileManagerActivity";
    private static Class<?> launchActivityClass;
    private Activity activity;
    private Instrumentation instrumentation;
    private MockWebServer mServer;
    private static Class<?> ella;
    private static Object ellaInstance;
    private static UiDevice mDevice;
    
    private Queue<String> locationQueue;
    private Queue<String> httpResponseQueue;
    
    static{
        try{
            launchActivityClass = Class.forName(LAUNCHER_ACTIVITY_CLASSNAME);
            ella =  Class.forName("com.apposcopy.ella.runtime.Ella");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
	public static Matcher<View> withId(final String resourceName) {
        return new TypeSafeMatcher<View>() {
            Resources resources = null;
            @Override
            public void describeTo(Description description) {
                String idDescription = resourceName;
                if (resources != null) {
                    try {
                        resources.getIdentifier(resourceName, null, null);
                    } catch (Resources.NotFoundException e) {
                        // No big deal, will just use the int value.
                        idDescription = String.format("%s (resource name not found)", resourceName);
                    }
                }
                description.appendText("with id: " + idDescription);
            }

            @Override
            public boolean matchesSafely(View view) {
                resources = view.getContext().getApplicationContext().getResources();
                if(view.getId() != -1){
                    String objectResourceName = resources.getResourceName(view.getId());
                    Log.d("MyMatcher", objectResourceName);
                    return TextUtils.equals(resourceName, objectResourceName);
                }
                return false;
            }
        };
    }
    @SuppressWarnings("unchecked")
    public OIFileManagerTest(){
        super((Class<Activity>)launchActivityClass);
    }

    final Dispatcher dispatcher = new Dispatcher(){
        @Override
        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
            if(request.getPath().equals("/location")){
                return new MockResponse().setResponseCode(200).setBody(locationQueue.peek());
            }else{
                return new MockResponse().setBody(httpResponseQueue.peek());
            }
        }
    };
    
    @Before
    public void setUp() throws Exception{
        super.setUp();
        ellaInstance = ella.newInstance();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        instrumentation = getInstrumentation();
        activity = getActivity();
        locationQueue = new ArrayDeque<>();
        httpResponseQueue = new ArrayDeque<>();
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mServer = new MockWebServer();
        mServer.setDispatcher(dispatcher);
        mServer.start(55555);
        
        //通过反射打开重放开关
        ellaInstance = Class.forName("com.apposcopy.ella.runtime.Ella").newInstance();
        Method startReplay = ellaInstance.getClass().getMethod("startReplay");
        startReplay.invoke(ellaInstance);
    }
    
    @Override
    protected void tearDown() throws Exception {
        mServer.shutdown();
        //通过反射关闭重放
        Method stopReplay = ellaInstance.getClass().getMethod("stopReplay");
        stopReplay.invoke(ellaInstance);    
        super.tearDown();
    }
    
    private static class SystemEventsServer implements Runnable{

        private ServerSocket server;
        private BufferedReader reader;
        private PrintWriter writer;
        private Socket socket;
        private int port = 7777;
        private String fromClient = "";
        private static String TAG = SystemEventsServer.class.getSimpleName();

        public void handleSystemEventsRequest() throws IOException, RemoteException {
            server = new ServerSocket(port);
            while(!Thread.currentThread().isInterrupted()){
                Log.i(TAG, "start listening");
                socket = server.accept();
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                while ((fromClient = reader.readLine()) != null) {
                    Log.i(TAG, "received message from client :" + fromClient);
                    if (fromClient.equals("EOF")) {//遇到eof时就结束接收
                        break;
                    }else if(fromClient.equals("ROTATE")){
                        mDevice.setOrientationRight();
                        mDevice.setOrientationNatural();
                    }else if(fromClient.equals("SLEEP")){
                        mDevice.sleep();
                        mDevice.wakeUp();
                    }
                }

                writer = new PrintWriter(socket.getOutputStream(), true);
                writer.println("OK");
                Log.i(TAG, "send reply to client");
                writer.close();
                reader.close();
                socket.close();
            }

        }

        @Override
        public void run() {
            try{
                try {
                    handleSystemEventsRequest();
                }catch(Exception ex){
                    ex.printStackTrace();
                }finally {
                    server.close();
                }
            }catch(IOException ex){
                ex.printStackTrace();
            }

        }
    }
    
    private static class loopRunnable implements Runnable {

        private static boolean flag;
        private static int num;
        private static Semaphore waitingThreadsSemaphore;
        private static Semaphore signalSemaphore;
        public static boolean startInstrument = false;
        static{
            try {
                waitingThreadsSemaphore = (Semaphore)ella.getMethod("getWaitingThreadsSemaphore").invoke(ellaInstance);
                signalSemaphore = (Semaphore)ella.getMethod("getSignalSemaphore").invoke(ellaInstance);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                do {
                    //更新flag和num的值
                    flag = (boolean)ella.getDeclaredField("isReplaying").get(ellaInstance);
                    num = (int)ella.getMethod("getWaitingThreads").invoke(ellaInstance);
                    if(num > 0){
                    	Log.d("TestCase", "acquire lock");
                    	//拿到互斥锁
	                    waitingThreadsSemaphore.acquire();
                        if(startInstrument){
		                    //do systematic events
                        	Espresso.pressBack();
                        }
	                    while(num > 0){
	                    	Log.d("TestCase", "" + num);
	                        //释放和记录数目相同的信号量
	                        signalSemaphore.release();
	                        ella.getMethod("decWaitingThreads").invoke(ellaInstance);
	                        num--;
	                    }
	                    //释放互斥锁
	                    waitingThreadsSemaphore.release();
	                    Log.d("TestCase", "release lock");
                    }else{
                        Thread.yield();
                    }
                }while(flag || num > 0);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
    
    public void test() throws Exception{
    	new Thread(new loopRunnable(), "loopThread").start();
    	Intents.init();
        Instrumentor.INSTRUMENT = true;
        onData(anything()).inAdapterView(withClassName(is("android.widget.ListView"))).atPosition(0).perform(click());
        //onView(withText(".com.taobao.dp")).perform(click());
        Thread.sleep(1000L);
    }

}
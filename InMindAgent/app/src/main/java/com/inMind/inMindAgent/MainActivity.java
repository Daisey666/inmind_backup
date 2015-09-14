package com.inMind.inMindAgent;

import java.util.ArrayList;
import java.util.Date;


import com.inMind.inMindAgent.InMindCommandListener.InmindCommandInterface;
import com.yahoo.inmind.middleware.control.MessageBroker;

import InMind.Consts;
import InMind.simpleUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.media.AudioTrack;
import android.media.AudioFormat;
import android.media.AudioManager;

/**
 * Created by Amos Azaria on 31-Dec-14.
 */
public class MainActivity extends ActionBarActivity
{

    TTScontroller ttsCont;
    LogicController logicController;
    InMindCommandListener inmindCommandListener;

    private ImageButton startButton;
    private Button stopButton;
    private Button resumeButton;
    private Button initButton;
    private Button localTestButton;

    private Handler userNotifierHandler, talkHandler, launchHandler, ttsCompleteHandler; // TODO: should
    // these all be
    // combined to
    // one handler?
    private LogicController.syncNotifiers startStopRecNotifier;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        userNotifierHandler = new Handler(new Handler.Callback()
        {

            @Override
            public boolean handleMessage(Message msg)
            {
                if (msg.arg1 == 1)
                {
                    boolean important = msg.arg2 == 1;
                    String toToast = msg.obj.toString();
                    toastWithTimer(toToast, important);
                    if (toToast.equals("Talk!")) //if needs to talk, set recording image. //TODO: should be done nicer (all strings should be refactorred).
                    {
                        ((ImageView) findViewById(R.id.image_recording)).setImageResource(R.drawable.rec_recording);
                    }
                    else
                        ((ImageView) findViewById(R.id.image_recording)).setImageResource(R.drawable.not_recording);
                }
                else if (msg.arg1 == 2)
                {
                    Log.d("Main", "Playing notification");
                    Uri notification = RingtoneManager
                            .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(
                            MainActivity.this, notification);
                    r.play();
                }
                else if (msg.arg1 == 0)
                    ((ImageView) findViewById(R.id.image_recording)).setImageResource(R.drawable.not_recording); //just turn off recording image.
                return false;
            }

        });

        ttsCompleteHandler = new Handler(new Handler.Callback()
        {

            @Override
            public boolean handleMessage(Message msg)
            {

                boolean isReconnecting = logicController.reconnectIfNeeded();
                return false;
            }
        });

        talkHandler = new Handler(new Handler.Callback()
        {

            @Override
            public boolean handleMessage(Message msg)
            {
                String toSay = msg.obj.toString();

                if (msg.arg1 == 1) //toast
                {
                    ttsCont.speakThis(toSay);
                    toastWithTimer(toSay, true);
                }
                if (msg.arg1 == 2){
                    return ttsCont.Speaking();
                }
                return false;
            }
        });

        launchHandler = new Handler(new Handler.Callback()
        {

            @Override
            public boolean handleMessage(Message msg)
            {
                if (msg.arg1 == 1)
                {
                    // Pattern p = Pattern.compile("(.*)/(.*)");
                    // Matcher m = p.matcher(msg.obj.toString());
                    // m.find();
                    String appToLaunch = msg.obj.toString();
                    Intent intent;
                    if (appToLaunch.equalsIgnoreCase("InMind agent"))
                    {
                        intent = new Intent(MainActivity.this,MainActivity.class);
                        //getconinical...
                        //intent = getIntent();
                    }
                    else
                    {
                        intent = MainActivity.this.getPackageManager()
                                .getLaunchIntentForPackage(appToLaunch);// m.group(1));
                    }
                    if (intent != null)
                    {
                        try
                        {
                            //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            MainActivity.this.startActivity(intent);
                            Log.d("Main Activity", "Launching intent");
                        }catch (Exception ex)
                        {
                            Log.e("MainActivity","error starting activity" + ex.getMessage());
                        }
                    }

                }
                return false;
            }
        });

        startStopRecNotifier = new LogicController.syncNotifiers()
        {
            @Override
            public void startStopRec(boolean start)
            {
                if (start)
                    inmindCommandListener.stopListening();
                else
                    inmindCommandListener.listenForInmindCommand();
            }
        };

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        //intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
                {
                    Log.d("Main Activity", Intent.ACTION_SCREEN_OFF);
                    //logicController.closeConnection();
                }
            }
        }, intentFilter);

        if (ttsCont == null)
        {
            ttsCont = new TTScontroller(this, ttsCompleteHandler);
        }

        MessageBroker messageBroker = null;
        try
        {
            messageBroker = MessageBroker.getInstance(this);
        }
        catch (Exception ex)
        {
            Log.e("Middleware", "Exception getting MW instance: " + ex.getMessage());
        }

        if (logicController == null)
        {
            final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
            String uniqueId = tm.getDeviceId();
            if (uniqueId == null)
                uniqueId = tm.getSimSerialNumber();
            if (uniqueId == null)
                uniqueId = "errorId";
            //logicController = new LogicController(userNotifierHandler, talkHandler,
             //      launchHandler, startStopRecNotifier, messageBroker, uniqueId);
            logicController = new LogicController(userNotifierHandler, talkHandler,
                    launchHandler, startStopRecNotifier, messageBroker, uniqueId, ttsCont);
        }

        if (inmindCommandListener == null)
        {
            inmindCommandListener = new InMindCommandListener(new InmindCommandInterface()
            {

                @Override
                public void commandDetected()
                {
                    // TODO Auto-generated method stub
                    connectAudioToServer();
                }
            }, this);
            inmindCommandListener.listenForInmindCommand();
        }

        startButton = (ImageButton) findViewById(R.id.button_rec);
        stopButton = (Button) findViewById(R.id.button_stop);
        resumeButton = (Button) findViewById(R.id.button_resume);
        initButton = (Button) findViewById(R.id.button_init);
        localTestButton = (Button) findViewById((R.id.button_local));

        resumeButton.setOnClickListener(resumeListener);
        startButton.setOnClickListener(startListener);
        stopButton.setOnClickListener(stopListener);
        initButton.setOnClickListener(initListener);
        localTestButton.setOnClickListener(localTestListener);

        // minBufSize += 2048;
        // System.out.println("minBufSize: " + minBufSize);

        // attach a Message. set msg.arg to 1 and msg.obj to string for toast.
    }


    void connectAudioToServer()
    {
        //inmindCommandListener.stopListening();
        logicController.ConnectToServer();
    }


    Date lastToastFinishes = new Date();

    private void toastWithTimer(String toToast, boolean important)
    {
        // toastCanceller.removeCallbacks(null);//make sure it won't be removed
        // by previous calls
        Date timeNow = new Date();
        boolean isAfter = timeNow.after(lastToastFinishes); //did we already pass the last toast finish time?

        int toastTime = important ? (int) ((toToast.length() / 75.0) * 2500 + 1000)
                : 1000;
        if (toastTime > 3500) // max toast time is 3500...
            toastTime = 3500;

        final int toastTimeFinal = toastTime;
        final Toast toast = Toast.makeText(this, toToast,
                Toast.LENGTH_LONG);


        if (isAfter)
        {
            toast.show();
            {
                Handler toastCanceller = new Handler();
                toastCanceller.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        toast.cancel();
                    }
                }, toastTimeFinal);
            }
            // set for when this toast will finish
            lastToastFinishes = simpleUtils.addMillisec(timeNow, toastTime);
        }
        else // if not, need to take care of delay for start as well
        {
            int startIn = simpleUtils.subtractDatesInMillisec(lastToastFinishes, timeNow);//lastToastFinishes - timeNow;
            Handler toastStarter = new Handler();
            toastStarter.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    toast.show();
                    Handler toastCanceller = new Handler();
                    toastCanceller.postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            toast.cancel();
                        }
                    }, toastTimeFinal);

                }
            }, startIn);
            // set for when this toast will finish
            lastToastFinishes = simpleUtils.addMillisec(lastToastFinishes, toastTime);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings)
        {
            return true;
        }
        if (id == R.id.action_toDesk)
        {
            if (logicController != null)
                logicController.changeInitIpAddr("128.2.213.163");
            return true;
        }
        if (id == R.id.action_toLap)
        {
            if (logicController != null)
                logicController.changeInitIpAddr("128.2.209.220");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when the user clicks the send button
     */
    public void sendText(View view)
    {

        EditText editText = (EditText) findViewById(R.id.text_to_send);
        String toSay = editText.getText().toString();
        logicController.ConnectToServer(toSay);
        //ttsCont.speakThis(toSay);
        //toastWithTimer(toSay, true);
    }

    public void closeSession(View view)
    {
        Log.d("Main", "close session clicked");
        logicController.closeSession();
    }

    public final OnClickListener localTestListener = new OnClickListener(){
        @Override
        public void onClick(View arg0)
        {
            Log.d("Main", "local test clicked");
            logicController.openLocalStream();
        }
    };

    private final OnClickListener stopListener = new OnClickListener()
    {

        @Override
        public void onClick(View arg0)
        {
            Log.d("Main", "Stop Clicked");
            // audioStreamer.stopStreaming();
            logicController.stopStreaming();
            //inmindCommandListener.stopListening();
        }

    };

    private final OnClickListener startListener = new OnClickListener()
    {

        @Override
        public void onClick(View arg0)
        {
            Log.d("Main", "Start Clicked");
            // audioStreamer.startStreaming();
            connectAudioToServer();
        }

    };

    private final OnClickListener initListener = new OnClickListener()
    {
        @Override
        public void onClick(View arg0)
        {
            Log.d("Main", "init session clicked");
            logicController.ConnectToServer(Consts.initSession,"");
        }
    };

    private final OnClickListener resumeListener = new OnClickListener()
    {

        @Override
        public void onClick(View arg0)
        {
            ttsCont.speakThis("You were distracted!");
            ttsCont.playPause();
            ttsCont.speakThis("I will repeat the previous email");
            ttsCont.playPause();
            try {
                Thread.sleep(500);
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            while(ttsCont.Speaking()){
                try {
                    Thread.sleep(1000);                 //1000 milliseconds is one second.
                } catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            logicController.ConnectToServer(Consts.distractionResumed,"");
            /*
            AudioTrack trackplay=new AudioTrack(AudioManager.STREAM_MUSIC, 16000,AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT,1280, AudioTrack.MODE_STREAM);
            byte[] buffer = new byte[1280];
            int byteread;
            File historyRaw=new File("/sdcard/history.raw");
            try(FileInputStream in = new FileInputStream(historyRaw)){
                trackplay.play();
                while((byteread = in.read(buffer))!=-1 ){
                    trackplay.write(buffer,0,1280);
                }
                trackplay.release();
            }
            catch (Exception ex){}
            */
        }
    };
}

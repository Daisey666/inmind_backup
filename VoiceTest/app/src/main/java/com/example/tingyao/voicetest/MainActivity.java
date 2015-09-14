package com.example.tingyao.voicetest;

import java.io.File;
import java.io.FileInputStream;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.media.AudioTrack;
import android.media.AudioFormat;
import android.media.AudioManager;



public class MainActivity extends ActionBarActivity {

    AudioStreamer audioStreamer;
    TTSController ttsCont;
    private Handler userNotifierHandler;
    private Handler distractionHandler;
    private Button startButton;
    private Button stopButton;
    private Button playbackButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
                    //toastWithTimer(toToast, important);
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

        distractionHandler = new Handler(new Handler.Callback()
        {

            @Override
            public boolean handleMessage(Message msg)
            {
                ttsCont.speakThis("you get distracted, system shutting down");
                try {
                    Thread.sleep(1000);
                } catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                audioStreamer.stopStreaming();

                return true;
            }
        });

        //run the opensmile manager
        //new Thread(new opensmileManager()).start();

        audioStreamer=new AudioStreamer(userNotifierHandler,distractionHandler);

        if (ttsCont == null)
        {
            ttsCont = new TTSController(this);
        }
        startButton = (Button) findViewById(R.id.button_start);
        stopButton = (Button) findViewById(R.id.button_stop);
        playbackButton = (Button) findViewById(R.id.button_play);

        startButton.setOnClickListener(startListener);
        stopButton.setOnClickListener(stopListener);
        playbackButton.setOnClickListener(playbackListener);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private final OnClickListener playbackListener = new OnClickListener()
    {

        @Override
        public void onClick(View arg0)
        {
            Log.d("Main", "Start Clicked");
            AudioTrack trackplay=new AudioTrack(AudioManager.STREAM_MUSIC, 16000,AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT,1280, AudioTrack.MODE_STREAM);
            //trackplay.setStereoVolume((float) volume,(float) volume);
            byte[] buffer = new byte[1280];
            int byteread;
            short tmpshort;
            File historyRaw=new File("/sdcard/history.raw");
            //connectAudioToServer();

            try(FileInputStream in = new FileInputStream(historyRaw)){
                trackplay.play();
                while((byteread = in.read(buffer))!=-1 ){

                    //amplification
                    for(int i = 0;i<640;i++){
                        tmpshort = (short) ((buffer[2*i] << 8) | buffer[2*i+1]);
                        //tmpshort *= 2;
                        buffer[2*i]=(byte) (tmpshort >>> 8);
                        buffer[2*i+1]=(byte) (tmpshort >>> 0);
                    }

                    trackplay.write(buffer,0,1280);
                }
                trackplay.release();
            }
            catch (Exception ex){}
        }

    };

    private final OnClickListener startListener = new OnClickListener()
    {

        @Override
        public void onClick(View arg0)
        {
            Log.d("Main", "Start Clicked");
            audioStreamer.startStreaming();
            //connectAudioToServer();
        }

    };

    private final OnClickListener stopListener = new OnClickListener()
    {
        @Override
        public void onClick(View arg0)
        {
            Log.d("Main", "stop clicked");
            audioStreamer.stopStreaming();
            //logicController.ConnectToServer(Consts.initSession,"");
        }
    };
/*
    public void ampRecord(double maximumEn){
        File historyRaw=new File("/sdcard/history.raw");
        byte[] buffer = new byte[1280];
        int byteread;
        short[] shortbuf = new short[640];
        try(FileInputStream in = new FileInputStream(historyRaw)){
            while((byteread = in.read(buffer))!=-1 ){
                shortbuf = byte2short(buffer, 1280);

            }
        }
        catch (Exception ex){}

    }

    public short[] byte2short(byte[] buf, int bufsize){
        short[] audioSeg=new short[bufsize/2];
        for (int i = 0; i <bufsize/2 ; i++) {
            audioSeg[i]=buf[i*2];
            audioSeg[i] = (short) ((buf[2*i] << 8) | buf[2*i+1]);
        }
        return audioSeg;
    }
    */
}

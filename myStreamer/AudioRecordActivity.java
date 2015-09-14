package com.inMind.inMindAgent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import com.yahoo.inmind.middleware.control.MessageBroker;
import com.yahoo.inmind.middleware.events.AudioRecordEvent;
import com.yahoo.inmind.middleware.events.MBRequest;
import com.yahoo.inmind.util.Constants;
import java.util.ArrayList;
/**
 * Created by tingyao on 3/26/15.
 */
public class AudioRecordActivity extends ActionBarActivity {
    private final int[] SAMPLE_RATE = new int[]{ 8000, 11025, 16000, 22050, 44100 };
    private final int[] CHANNEL_CONFIG = new int[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.CHANNEL_IN_STEREO };
    private final int[] ENCODING = new int[] { AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_DEFAULT, AudioFormat.ENCODING_PCM_8BIT };
    private ArrayList<Consumer> consumers;
    private int cont = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_activity_audio_record);
        consumers = new ArrayList<>();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        for( Consumer c : consumers ){
            c.stopRecording();
            c.interrupt();
        }
    }
    @Override
    protected void onResume(){
        super.onResume();
    }
    /**
     * Creates a new thread Consumer that subscribes to the audio stream updates
     * @param view
     */
    public void start( View view){
        Consumer c = new Consumer( "Consumer" + cont++ );
        c.start();
        consumers.add( c );
    }
    /**
     * Stops the execution of the consumer thread and as a consequence, no more audio stream
     * updates are sent to this thread.
     * @param view
     */
    public void stop( View view ){
        if( consumers.isEmpty() == false ) {
            Consumer c = consumers.remove( 0 );
            c.stopRecording();
            c.interrupt();
        }
    }
    /**
     * Helper class to demonstrate that we can have multiple subscribers (consumers) waiting
     * for audio stream updates. When the consumer receives the update, it play the buffer content
     * by using an AudioTrack object
     */
    class Consumer extends Thread{
        private AudioTrack track = null;
        private MessageBroker mb;
        private int requestedSampleRate;
        private int requestedChannelConfig;
        private int requestedAudioEncoding;
        public Consumer( String name ){
            super( name );
        }
        public void run(){
            mb = MessageBroker.getInstance( getApplicationContext() );
            mb.subscribe( this, AudioRecordEvent.class );
            startRecording();
        }
        /**
         * This method sends a subscription request to the Audio Recorder. If the Audio Recorder
         * is currently being used for another component, then your request (including the config you
         * have specified such as sample rate, channel config, audio encoding, etc.) will be enqueued
         * until the other component releases the audio recorder, however you will receive the recorded
         * stream in the meanwhile.
         */
        public void startRecording(){
            requestedSampleRate = SAMPLE_RATE[ (cont - 1) % SAMPLE_RATE.length ];
            requestedChannelConfig = CHANNEL_CONFIG[ (cont - 1) % CHANNEL_CONFIG.length ];
            requestedAudioEncoding = ENCODING[ (cont - 1) % ENCODING.length ];
            MBRequest request = new MBRequest( Constants.MSG_START_AUDIO_RECORD);
            request.put( Constants.SET_AUDIO_SAMPLE_RATE, requestedSampleRate );
            request.put( Constants.SET_AUDIO_CHANNEL_CONFIG, requestedChannelConfig);
            request.put( Constants.SET_AUDIO_ENCODING, requestedAudioEncoding );
// 2 bytes in 16bit format
            request.put( Constants.SET_AUDIO_BYTES_PER_ELEMENT, 2 );
// we want to play 2048 (2K). Since 2 bytes, we use only 1024
            request.put( Constants.SET_AUDIO_BUFFER_ELEMENTS_TO_REC, 1024 );
            request.put( Constants.SET_SUBSCRIBER, this );
            mb.send( request );
        }
        /**
         * On this method you can process the recorded audio stream. For testing purposes, the onEvent
         * method plays the audio stream by using the AudioTrack class.
         * @param event
         */
        public void onEvent( final AudioRecordEvent event ){
// add whatever you want here:
            if( event.getErrorMessage() != null ){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AudioRecordActivity.this, event.getErrorMessage(), Toast.LENGTH_LONG);
                    }
                });
            } else {
                try {
                    if (track == null || event.isNewConfiguration()) {
                        track = new AudioTrack(AudioManager.STREAM_MUSIC,
                                event.getSampleRate(),
                                AudioFormat.CHANNEL_OUT_MONO,
                                event.getAudioEncoding(),
                                event.getMinBufferSize(),
                                AudioTrack.MODE_STREAM);
                        track.play();
                    }
                    track.write(event.getBuffer(), 0, event.getMinBufferSize());
                    Log.e("TEST", "Receiving audio stream on " + this.getName() + ". Requested sample rate: "
                            + requestedSampleRate + " but using sample rate: " + event.getSampleRate());
                }catch(Exception e){
//TODO
                }
            }
        }
        /**
         * The main purpose of this method is to unsubscribe from receiving audio stream updates. It also
         * releases the resources (in this case, the audio track object).
         */
        public void stopRecording(){
            MBRequest request = new MBRequest( Constants.MSG_STOP_AUDIO_RECORD );
            request.put( Constants.SET_SUBSCRIBER, this );
            mb.send( request );
            if( track != null ) {
                track.pause();
                track.flush();
                track.release();
                track = null;
            }
            Log.e("TEST", "Stopping audio stream on " + this.getName());
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
// Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.app_menu_audio_record, menu);
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
}

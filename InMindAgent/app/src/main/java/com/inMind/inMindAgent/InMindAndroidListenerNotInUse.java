package com.inMind.inMindAgent;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

public class InMindAndroidListenerNotInUse
{

    interface InmindCommandInterface
    {
        void commandDetected();
    }

    boolean isListening = true;
    InmindCommandInterface inmindCommandInterface;
    Context context;

    InMindAndroidListenerNotInUse(InmindCommandInterface inmindCommandInterface, final Context context)
    {
        this.inmindCommandInterface = inmindCommandInterface;
        this.context = context;

        sr = SpeechRecognizer.createSpeechRecognizer(context);
        sr.setRecognitionListener(new RecListener());

        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        //mStreamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC); // getting system volume into var for later un-muting
    }

    public void stopListening()
    {
        isListening = false;
        sr.cancel();
        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
    }

    public void listenForInmindCommand()
    {
        isListening = true;
        testForInmindCommand();
    }

    private void testForInmindCommand()
    {
        if (isListening)
        {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "voice.recognition.test");

            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);

            sr.startListening(intent);
            //mStreamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC); // getting system volume into var for later un-muting
            //mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
        }
    }

    private SpeechRecognizer sr;
    //Android does not allow two intents to access the microphone :(
    //AudioRecord recorder;
    private AudioManager mAudioManager;
    private int mStreamVolume;

    class RecListener implements RecognitionListener
    {

        @Override
        public void onReadyForSpeech(Bundle params)
        {
            // TODO Auto-generated method stub
            //mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mStreamVolume, 0); // again setting the system volume back to the original, un-mutting
            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        }

        @Override
        public void onBeginningOfSpeech()
        {
            // TODO Auto-generated method stub
        }

        @Override
        public void onRmsChanged(float rmsdB)
        {
            // TODO Auto-generated method stub
        }

        @Override
        public void onBufferReceived(byte[] buffer)
        {
            // TODO Auto-generated method stub
        }

        @Override
        public void onEndOfSpeech()
        {
            // TODO Auto-generated method stub
        }

        @Override
        public void onError(int error)
        {
            // TODO Auto-generated method stub
            //mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mStreamVolume, 0);
            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            testForInmindCommand();
        }

        @Override
        public void onResults(Bundle results)
        {
            // TODO Auto-generated method stub
            //mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mStreamVolume, 0);
            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            String firstRes = data.get(0);

            if (firstRes.contains("mind agent"))
            {
                Log.d("Main", "got inmind command");
                inmindCommandInterface.commandDetected();
            }
            else
            {
                testForInmindCommand();
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults)
        {
            // TODO Auto-generated method stub

        }

        @Override
        public void onEvent(int eventType, Bundle params)
        {
            // TODO Auto-generated method stub

        }

    }
}

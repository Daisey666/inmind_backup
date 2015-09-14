package com.inMind.inMindAgent;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;


import com.yahoo.inmind.middleware.control.MessageBroker;
import com.yahoo.inmind.middleware.events.AudioRecordEvent;
import com.yahoo.inmind.middleware.events.MBRequest;
import com.yahoo.inmind.util.Constants;

import InMind.Consts;

/**
 * Created by tingyao on 3/25/15.
 */
public class MultiAudioStreamer {

    public static DatagramSocket socket;

    private final int[] SAMPLE_RATE = new int[]{ 8000, 11025, 16000, 22050, 44100 };
    private final int[] CHANNEL_CONFIG = new int[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.CHANNEL_IN_STEREO };
    private final int[] ENCODING = new int[] { AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_DEFAULT, AudioFormat.ENCODING_PCM_8BIT };
    private ArrayList<Consumer> consumers;
    private int cont = 0;

    String ipAddr;
    int portNum;
    private Handler userNotifierHandler;

    public MultiAudioStreamer(String ipAddr, int portNum, Handler userNotifierHandler)
    {
        this.ipAddr = ipAddr;
        this.portNum = portNum;
        this.userNotifierHandler = userNotifierHandler;
    }

}

package com.inMind.inMindAgent;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.LinkedList;
import java.util.Iterator;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import InMind.Consts;

/**
 * Created by Amos Azaria on 31-Dec-14.
 */
public class AudioStreamer
{

    AudioRecord recorder;

    public static DatagramSocket socket;

    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private boolean status = false;

    String ipAddr;
    int portNum;
    private Handler userNotifierHandler;

    //tingyao
    //announcement for distraction detection
    private File historyRaw = new File("/sdcard/history.raw");
    //private Queue<short[]> signalQE = new LinkedList<short[]>();
    private Queue<short[]> signalQE = new ConcurrentLinkedQueue<short[]>();
    private int queueSize = 40;
    private Handler distractionHandler;
    private double maximumEn;

    static{
        System.loadLibrary("opensmiletest");
    }
    public native String opensmilefunc();

    public AudioStreamer(String ipAddr, int portNum, Handler userNotifierHandler, Handler distractionHandler)
    {
        this.ipAddr = ipAddr;
        this.portNum = portNum;
        this.userNotifierHandler = userNotifierHandler;
        this.distractionHandler = distractionHandler;
    }

    public boolean DistractionDetection(){
        //VAD
        double energyTotal=0;

        Queue<short[]> signaltmp=signalQE;
        //for (int i = 0; i < signalQE.size(); i++) {
        for(short[] shorttmp: signaltmp){
            //Log.d("ddd", "frame1");
            for(int j=0; j < 640; j++) {
                energyTotal += shorttmp[j] * shorttmp[j];
            }
        }
        energyTotal/=signaltmp.size();
        if (energyTotal>maximumEn)
            maximumEn=energyTotal;
        //Filler Speech Detection
        System.out.println(energyTotal);
        if (energyTotal>60000000000.0)
            return false;
        else return true;
    }

    public short[] byte2short(byte[] buf, int bufsize){
        short[] audioSeg=new short[bufsize/2];
        for (int i = 0; i <bufsize/2 ; i++) {
            //audioSeg[i]=buf[i*2];
            audioSeg[i] = (short) ((buf[2*i] << 8) | buf[2*i+1]);
        }
        return audioSeg;
    }

    public boolean isStreaming()
    {
        return status;
    }

    @Override
    protected void finalize()
    {
        stopStreaming();
    }

    public void stopStreaming()
    {
        status = false;
        if (recorder != null)
        {
            recorder.stop();
            recorder.release();
            recorder = null;
        }
        Log.d("VS", "Recorder released");
        Message msgNotRecording = new Message();
        msgNotRecording.arg1 = 0;
        userNotifierHandler.sendMessage(msgNotRecording); //set not recording image.
    }


    //for local testing
    public void localStreaming(){
        status = true;

        //thread for distraction detection
        Thread distractionThread = new Thread(new Runnable(){

            @Override
            public void run(){
                try{
                    int eventcount=0;
                    boolean distracted=false;
                    Message msgTalk = new Message();
                    msgTalk.arg1 = 1; //important toast
                    msgTalk.obj = "distracted!";
                    Log.d("ddd", "detecting distraction!");
                    //Thread.sleep(3000);
                    //distractionHandler.sendMessage(msgTalk);
                    maximumEn=0;
                    while (status==true){
                        distracted = DistractionDetection();
                        Log.d("ddd", "distraction output:");
                        System.out.println(distracted);
                        if (distracted) {
                            eventcount+=1;
                            distracted=false;
                        }
                        if (eventcount>1 && !distracted) {
                            //a interface toward outside
                            Log.d("ddd", "get distracted!!!!!!!!!!!!!!");
                            //distractionHandler.sendMessage(msgTalk);
                            break;
                        }
                        Thread.sleep(500);
                    }
                }
                catch(Exception ex){}
            }
        });

        Thread streamThread = new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                try(FileOutputStream fop = new FileOutputStream(historyRaw))
                {
                    //Log.d("VS", "Before Creating socket");
                    //socket = new DatagramSocket();
                    //Log.d("VS", "Socket Created.");
                    int minBufSize = AudioRecord.getMinBufferSize(Consts.sampleRate, channelConfig, audioFormat);
                    //int minBufSize = 3584;//Consts.udpBufferSize;

                    Log.d("VS", "minBufSize:" + minBufSize);
                    byte[] buffer = new byte[minBufSize];

                    Log.d("VS", "Buffer created of size " + minBufSize);
                    //DatagramPacket packet;

                    //final InetAddress destination = InetAddress.getByName(ipAddr);
                    //Log.d("VS", "Address retrieved");


                    //recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, Consts.sampleRate, channelConfig, audioFormat, minBufSize * 10);
                    //Log.d("VS", "Recorder initialized");

                    //recorder.startRecording();

                    //Added to remove initial noise which Android (or at least Nexus 5) seems to have at the beginning. //can try using noise removal algorithms instead, look at Audacity.
//					Message msgWait = new Message();
//					msgWait.arg1 = 1;
//					msgWait.obj = "Wait!";
//					toasterHandler.sendMessage(msgWait);
//
//					Thread.sleep(1600);

                    Message msgTalk = new Message();
                    msgTalk.arg1 = 1;
                    msgTalk.arg2 = 1; //important toast
                    msgTalk.obj = "Talk!";
                    userNotifierHandler.sendMessage(msgTalk);
                    //Message msgPlayTone = new Message();
                    //msgPlayTone.arg1 = 2;
                    //toasterHandler.sendMessage(msgPlayTone);

//					Thread.sleep(300);
//					//couldn't find a better way to clear buffer.
//					byte[] tmpbuffer = new byte[minBufSize*1000];
//					recorder.read(tmpbuffer, 0, minBufSize*1000);
//					tmpbuffer=null;

                    for (int i=0;i < 5;i++) {
                        System.out.println(opensmilefunc());
                        System.out.println("some other thing here");
                    }
                    /*
                    while (status == true)
                    {

                        if (recorder != null)
                        {
                            //reading data from MIC into buffer
                            int bytesRead = recorder.read(buffer, 0, buffer.length);

                            if (bytesRead > 0)
                            {
                                //putting buffer in the packet
                                //packet = new DatagramPacket(buffer, buffer.length, destination, portNum);
                                //socket.send(packet);
                                System.out.println("Send_Packet: " + minBufSize);

                                signalQE.add(byte2short(buffer,buffer.length));
                                if (signalQE.size()>queueSize){
                                    //System.out.println("remove something");
                                    signalQE.remove();
                                }
                                //tmp save the history over here
                                fop.write(buffer);

                            }
                        }
                    }
                    //clean outputstream, added by tingyao
                    fop.flush();
                    fop.close();

                    //socket.close();
                    Log.d("VS", "Socket Closed");
*/

                }
                catch (Exception ex)
                {
                    //stopStreaming();
                    //if (socket != null)
                        //socket.close();
                }
            }

        });
        streamThread.start();
        //try{
        //    Thread.sleep(6000);
        //}
        //catch(Exception ex){
        //    Thread.currentThread().interrupt();
        //}
        //distractionThread.start();
    }


    public void startStreaming()
    {
        status = true;

        //thread for distraction detection
        Thread distractionThread = new Thread(new Runnable(){

            @Override
            public void run(){
                try{
                    int eventcount=0;
                    boolean distracted=false;
                    Message msgTalk = new Message();
                    msgTalk.arg1 = 1;
                    //msgTalk.obj = "distracted!";
                    Log.d("ddd", "detecting distraction!");
                    //Thread.sleep(3000);
                    //distractionHandler.sendMessage(msgTalk);

                    while (status==true){
                        distracted = DistractionDetection();
                        Log.d("ddd", "distraction output:");
                        System.out.println(distracted);
                        if (distracted) {
                            eventcount+=1;
                        }
                        if (eventcount>1 && !distracted) {
                            //a interface toward outside
                            msgTalk.obj = maximumEn;
                            distractionHandler.sendMessage(msgTalk);
                            break;
                        }
                        Thread.sleep(500);
                    }
                }
                catch(Exception ex){}
            }
        });

        Thread streamThread = new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                try(FileOutputStream fop = new FileOutputStream(historyRaw))
                {
                    Log.d("VS", "Before Creating socket");
                    socket = new DatagramSocket();
                    Log.d("VS", "Socket Created.");
                    int minBufSize = AudioRecord.getMinBufferSize(Consts.sampleRate, channelConfig, audioFormat);
                    //int minBufSize = 3584;//Consts.udpBufferSize;

                    Log.d("VS", "minBufSize:" + minBufSize);
                    byte[] buffer = new byte[minBufSize];

                    Log.d("VS", "Buffer created of size " + minBufSize);
                    DatagramPacket packet;

                    final InetAddress destination = InetAddress.getByName(ipAddr);
                    Log.d("VS", "Address retrieved");


                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, Consts.sampleRate, channelConfig, audioFormat, minBufSize * 10);
                    Log.d("VS", "Recorder initialized");

                    recorder.startRecording();

                    //Added to remove initial noise which Android (or at least Nexus 5) seems to have at the beginning. //can try using noise removal algorithms instead, look at Audacity.
//					Message msgWait = new Message();
//					msgWait.arg1 = 1;
//					msgWait.obj = "Wait!";
//					toasterHandler.sendMessage(msgWait);
//
//					Thread.sleep(1600);

                    Message msgTalk = new Message();
                    msgTalk.arg1 = 1;
                    msgTalk.arg2 = 1; //important toast
                    msgTalk.obj = "Talk!";
                    userNotifierHandler.sendMessage(msgTalk);
                    //Message msgPlayTone = new Message();
                    //msgPlayTone.arg1 = 2;
                    //toasterHandler.sendMessage(msgPlayTone);

//					Thread.sleep(300);
//					//couldn't find a better way to clear buffer.
//					byte[] tmpbuffer = new byte[minBufSize*1000];
//					recorder.read(tmpbuffer, 0, minBufSize*1000);
//					tmpbuffer=null;

                    while (status == true)
                    {

                        if (recorder != null)
                        {
                            //reading data from MIC into buffer
                            int bytesRead = recorder.read(buffer, 0, buffer.length);

                            if (bytesRead > 0)
                            {
                                //putting buffer in the packet
                                packet = new DatagramPacket(buffer, buffer.length, destination, portNum);
                                socket.send(packet);
                                //System.out.println("Send_Packet: " + minBufSize);

                                signalQE.add(byte2short(buffer,buffer.length));
                                if (signalQE.size()>queueSize){
                                    //System.out.println("remove something");
                                    signalQE.remove();
                                }
                                //tmp save the history over here
                                fop.write(buffer);

                            }
                        }
                    }
                    //clean outputstream, added by tingyao
                    fop.flush();
                    fop.close();

                    socket.close();
                    Log.d("VS", "Socket Closed");


                }
                catch (Exception ex)
                {
                    stopStreaming();
                    if (socket != null)
                        socket.close();
                }
            }

        });
        streamThread.start();
        try{
            Thread.sleep(2000);
        }
        catch(Exception ex){
            Thread.currentThread().interrupt();
        }
        distractionThread.start();
    }

    //tingyao
    //This function will be implemented in logicController
    public interface dealWithDistraction
    {
        public void dealWithDistraction(String message);
    }
}

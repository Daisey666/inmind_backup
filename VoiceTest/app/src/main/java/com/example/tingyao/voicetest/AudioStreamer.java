package com.example.tingyao.voicetest;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.LinkedList;
import java.util.Iterator;
import java.io.FileOutputStream;
import java.net.Socket;
import java.net.InetAddress;
import java.io.InputStreamReader;
import java.lang.Math.*;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Created by tingyao on 4/17/15.
 */
public class AudioStreamer {
    AudioRecord recorder;
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private boolean status = false;
    private int sampleRate=16000;
    private Handler userNotifierHandler;
    //private FileOutputStream fop;

    private File historyRaw = new File("/sdcard/history.raw");
    //private Queue<short[]> signalQE = new LinkedList<short[]>();
    private Queue<short[]> signalQE = new ConcurrentLinkedQueue<short[]>();
    private int queueSize = 40;
    private Handler distractionHandler;

    //amplification of playback
    double maximumEn; //record the maximum energy of the current utterance

    //variable for filler speech detection
    private double[] energyDist;
    private double[] energyDistDiff;
    //private float[] autocorrDist;

    //socket program, connecting to opensmile process
    //private final String ipaddr = "localhost";
    private final int port = 5000;
    private Socket socket;

    static{
        System.loadLibrary("opensmiletest");
    }
    public native float[] opensmilefunc(Object[] m);
    public native String jniUtilTest(Object[] m);

    public AudioStreamer(Handler userNotifierHandler ,Handler distractionHandler){
        this.distractionHandler = distractionHandler;
        this.userNotifierHandler = userNotifierHandler;

        //initialization for filler speech detection
        energyDist = new double[queueSize];
        energyDistDiff = new double[queueSize-1];
    }

    public int DistractionDetection(){
        //VAD
        double energyTotal=0;
        double energySeg=0;
        int segcount = 0;
        int distractEncode=0;
        final Queue<short[]> signaltmp=signalQE;
        Object[] filler_buf;
        float[] smile_output;
        float[] feature = new float[6];

        //for (int i = 0; i < signalQE.size(); i++) {
        for(short[] shorttmp: signaltmp){
            //Log.d("ddd", "frame1");
            energySeg = 0;
            for(int j=0; j < 640; j++) {
                energyTotal += shorttmp[j] * shorttmp[j];
                energySeg += shorttmp[j] * shorttmp[j];
            }
            if(segcount<queueSize)
                energyDist[segcount] = energySeg/640;
            segcount+=1;
        }
        energyTotal/=signaltmp.size();
        if (energyTotal>maximumEn)
            maximumEn=energyTotal;


        //Filler Speech Detection
        //for test
        //test opensmile
        filler_buf = signaltmp.toArray();
        smile_output=opensmilefunc(filler_buf);

        //my own functional statistics
        feature[0] = LinearCoefA(smile_output,39,smile_output.length/39,14);
        feature[1] = LinearCoefA(smile_output,39,smile_output.length/39,16);
        feature[2] = LinearCoefA(smile_output,39,smile_output.length/39,18);
        feature[3] = LinearCoefA(smile_output,39,smile_output.length/39,19);
        feature[4] = Stddev(smile_output,39,smile_output.length/39,16);
        feature[5] = Stddev(smile_output,39,smile_output.length/39,18);
        System.out.println(feature);
        //linear classification


        System.out.println(smile_output.length);
        //smile_output=opensmilefunc(filler_buf);
        //System.out.println("second opensmile");
        //jniUtilTest(filler_buf);

        System.out.println(energyTotal);
        if (energyTotal < 6.0E10)
            distractEncode+=1;
        //if (energyDiffVar < 3.0E14)
        //    distractEncode+=2;
        return distractEncode;
    }

    //some basic functionals
    public double Variance(double[] dSeq){
        double mean=0,var2=0;
        for(double doubletmp: dSeq){
            mean += doubletmp;
            var2 += (doubletmp*doubletmp);
        }
        mean/=dSeq.length;
        var2/=dSeq.length;

        return var2-(mean*mean);
    }

    public float Stddev(float[] mat, int rnum, int cnum, int idx){
        float mean = Mean(mat,rnum,cnum,idx),var = 0;
        for(int i = 0;i<cnum;i++)
            var+=mat[rnum*i+idx]*mat[rnum*i+idx];
        return (float) Math.sqrt((double)var);
    }

    public float Mean(float[] mat, int rnum, int cnum, int idx){
        float mean=0;
        for(int i = 0;i<cnum;i++)
            mean+=mat[rnum*i+idx];
        mean/=cnum;
        return mean;
    }

    public float LinearCoefA(float[] mat, int rnum, int cnum, int idx){
        float sumX = 0, sumXX=0, sumY=0, sumXY=0;
        for(int i = 0;i<cnum;i++){
            sumX+=i;
            sumXX+=i*i;
            sumY+=mat[rnum*i+idx];
            sumXY+=i*mat[rnum*i+idx];
        }
        return (sumXY-sumX*sumY/cnum)/(sumXX-sumX*sumX/cnum);
    }

    public short[] byte2short(byte[] buf, int bufsize){
        short[] audioSeg=new short[bufsize/2];
        for (int i = 0; i <bufsize/2 ; i++) {
            audioSeg[i]=buf[i*2];
            audioSeg[i] = (short) ((buf[2*i] << 8) | buf[2*i+1]);
        }
        return audioSeg;
    }

    public void startStreaming(){
        status = true;

        //thread for distraction detection
        Thread distractionThread = new Thread(new Runnable(){

            @Override
            public void run(){
                try{
                    int eventcount=0;
                    int fillerEvent=0;
                    //boolean distracted=false;
                    int distractEncode=0;
                    Message msgTalk = new Message();
                    msgTalk.arg1 = 1; //important toast
                    msgTalk.obj = "distracted!";
                    Log.d("ddd", "detecting distraction!");
                    //Thread.sleep(3000);
                    //distractionHandler.sendMessage(msgTalk);

                    /*
                    //initialization of socket here (client side)
                    InetAddress server_addr = InetAddress.getLocalHost();
                    socket = new Socket(server_addr,port);
                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    //InputStreamReader input = new InputStreamReader(socket.getInputStream());
                    PrintWriter out = new PrintWriter(new BufferedWriter( new OutputStreamWriter(socket.getOutputStream())),true);
                    */

                    maximumEn=0;
                    while (status==true){
                        distractEncode = DistractionDetection();
                        //out.println("a opensmile request!");
                        //System.out.println("socket msg: "+input.read());
                        Log.d("ddd", "distraction output:" + distractEncode);
                        //if (distractEncode==1 || distractEncode==3)
                        if (distractEncode>0)
                            eventcount+=1;


                        if (eventcount>2 && distractEncode==0) {
                            //a interface toward outside
                            Log.d("ddd","maximum energy: "+maximumEn);
                            distractionHandler.sendMessage(msgTalk);
                            status=false;
                            break;
                        }

                        Thread.sleep(300);
                    }
                    //out.println("terminate");
                }
                catch(Exception ex){
                    Log.e("VS", "exception: " + ex.getMessage());
                }
            }
        });

        Thread streamThread = new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                Log.d("VS", "in the beginning");
                try(FileOutputStream fop = new FileOutputStream(historyRaw))
                //try(fop = new FileOutputStream(historyRaw))
                {
                    //fop = new FileOutputStream(historyRaw);
                    int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
                    //int minBufSize = 3584;//Consts.udpBufferSize;

                    Log.d("VS", "minBufSize:" + minBufSize);
                    byte[] buffer = new byte[minBufSize];


                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBufSize * 10);
                    Log.d("VS", "Recorder initialized");

                    recorder.startRecording();

                    Message msgTalk = new Message();
                    msgTalk.arg1 = 1;
                    msgTalk.arg2 = 1; //important toast
                    msgTalk.obj = "Talk!";
                    userNotifierHandler.sendMessage(msgTalk);

                    while (status == true)
                    {

                        if (recorder != null)
                        {
                            //reading data from MIC into buffer
                            int bytesRead = recorder.read(buffer, 0, buffer.length);

                            if (bytesRead > 0)
                            {
                                //System.out.println("Get Packet: " + minBufSize);

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

                }
                catch (Exception ex)
                {
                    Log.e("VS", "exception: " + ex.getMessage());
                    stopStreaming();
                    signalQE.clear();
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
}

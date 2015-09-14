package com.inMind.inMindAgent;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.io.FileInputStream;

import InMind.Consts;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.media.AudioTrack;
import android.media.AudioFormat;
import android.media.AudioManager;

import com.yahoo.inmind.middleware.control.MessageBroker;

/*
 * This class is in-charge of all connections to the server. 
 * It first connects to the server (via TCP), authentication etc.
 * Then receives a port number and connects to it via UDP to stream the audio.
 * 
 * 
 * Created by Amos Azaria on 31-Dec-14.
 */
public class LogicController
{


    TCPClient tcpClient;
    AudioStreamer audioStreamer;
    TTScontroller ttsCont;

    String tcpIpAddr = "128.237.188.92";
    //String tcpIpAddr = "127.0.0.1";
    // String tcpIpAddr = "128.2.210.187";
    //String tcpIpAddr = "192.168.0.4";
    int tcpIpPort = Consts.serverPort;
    String udpIpAddr;
    int udpIpPort;
    String uniqueId;

    private Handler userNotifierHandler;
    private Handler talkHandler;
    private Handler launchHandler;
    syncNotifiers startStopRecNotifier;
    private boolean needToReconnect;
    private double maximumEn;

    //tingyao
    //distraction handler
    private Handler distractionHandler;

    private MessageController messageController;
    private Context context = null;
    MessageBroker messageBroker;

    private TcpPing tcpPing;

    interface syncNotifiers
    {
        void startStopRec(boolean start);
    }



    public LogicController(Handler userNotifierHandler, Handler talkHandler, Handler launchHandler, syncNotifiers startStopRecNotifier, MessageBroker messageBroker, String uniqueId, final TTScontroller ttsCont)
    {
        this.userNotifierHandler = userNotifierHandler;
        this.talkHandler = talkHandler;
        this.launchHandler = launchHandler;
        this.startStopRecNotifier = startStopRecNotifier;
        messageController = new MessageController();
        this.messageBroker = messageBroker;
        this.uniqueId = uniqueId;
        this.tcpPing = null;
        this.ttsCont=ttsCont;

        //tingyao
        //
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
                stopStreaming();
                //decide the ratio for playback amplification
                maximumEn=(double) msg.obj;
                Log.d("logic","maximum energy: "+maximumEn);
                ConnectToServer(Consts.distractionFound, "");
                return true;
            }
        });
    }

    public void ConnectToServer(String sendThisText)
    {
        //closeConnection();
        String messageToServer = uniqueId + Consts.commandChar + Consts.sendingText + Consts.commandChar + sendThisText;
        if (tcpClient != null)
            tcpClient.sendMessage(messageToServer);
        else
            startConnection(messageToServer);
    }

    public void ConnectToServer(String Command,String sendThisText)
    {
        //closeConnection();
        String messageToServer = uniqueId + Consts.commandChar + Command + Consts.commandChar + sendThisText;
        if (tcpClient != null)
            tcpClient.sendMessage(messageToServer);
        else
            startConnection(messageToServer);
    }

    public void ConnectToServer()
    {
        //if is currently streaming, ignore request.
        if (tcpClient != null && audioStreamer != null && audioStreamer.isStreaming())
            return;
        //closeConnection();
        startStopRecNotifier.startStopRec(true);//say that is starting the recording. must be called before starting.
        String messageToServer = uniqueId + Consts.commandChar + Consts.requestSendAudio + Consts.commandChar;
        if (tcpClient != null)
        {
            tcpClient.sendMessage(messageToServer);

        } else {
            startConnection(messageToServer);
        }
    }

    public void startConnection(String firstMessage)
    {
        new connectTask().execute(firstMessage);
        tcpPing = new TcpPing(this);
        Thread pingThread = new Thread(tcpPing);
        pingThread.start();
    }

    public void closeConnection()
    {
        stopStreaming();
        if (tcpClient != null)
        {
            tcpClient.closeConnection();
            tcpClient = null;
        }
        if (tcpPing != null)
        {
            tcpPing.stop();
            tcpPing = null;
        }
    }

    public void stopStreaming()
    {
        if (audioStreamer != null)
        {
            audioStreamer.stopStreaming();
            audioStreamer = null;
        }
    }

    public void changeInitIpAddr(String newIpAddr)
    {
        closeConnection();
        tcpIpAddr = newIpAddr;
    }

    public void changeInitPort(int newPort)
    {
        tcpIpPort = newPort;
    }

    private void openAudioStream()
    {
        audioStreamer = new AudioStreamer(udpIpAddr, udpIpPort, userNotifierHandler, distractionHandler);
        audioStreamer.startStreaming(); //TODO: must be async!!!
    }

    public void openLocalStream(){

        audioStreamer = new AudioStreamer(udpIpAddr, udpIpPort, userNotifierHandler, distractionHandler);
        audioStreamer.localStreaming(); //TODO: must be async!!!
    }

    private void dealWithMessage(String message)
    {
        Log.d("ServerConnector", "Dealing with message:" + message);
        Pattern p = Pattern.compile(Consts.serverMessagePattern);
        Matcher m = p.matcher(message);
        boolean found = m.find();
        Log.d("ServerConnector", "found:" + found);
        if (found)
        {
            if (m.group(1).equalsIgnoreCase(Consts.closeConnection))
            {
                Log.w("ServerConnector", "Session terminated from server");
                closeConnection();
            }
            else if (m.group(1).equalsIgnoreCase(Consts.startNewConnection))
            {
                //closeConnection();
                needToReconnect = true;
            }
            else if (m.group(1).equalsIgnoreCase(Consts.stopUdp))
            {
                stopStreaming();
                startStopRecNotifier.startStopRec(false); //say that is stopping the recording. must be called AFTER stopping.
            }
            else if (m.group(1).equalsIgnoreCase(Consts.connectUdp))
            {
                udpIpPort = 0;
                try
                {
                    udpIpAddr = tcpIpAddr;
                    Log.d("ServerConnector", "found:" + found);
                    //String protocol = m.group(1);
                    udpIpPort = Integer.parseInt(m.group(2).trim());
                    Log.d("ServerConnector", "Got port:" + udpIpPort);
                }
                catch (Exception e)
                {
                    Log.e("ServerConnector", "Error parsing message from server...");
                }
                if (udpIpPort > 0)
                    openAudioStream();
            }
            else if (m.group(1).equalsIgnoreCase(Consts.sayCommand))
            {
                Log.d("ServerConnector", "saying:" + m.group(2));
                ttsCont.volumeBack();
                Message msgTalk = new Message();
                msgTalk.arg1 = 1;
                msgTalk.obj = m.group(2).trim();
                talkHandler.sendMessage(msgTalk);
                ttsCont.playPause();
            }
            else if (m.group(1).equalsIgnoreCase(Consts.launchCommand))
            {
                Message msgLaunch = new Message();
                msgLaunch.arg1 = 1;
                msgLaunch.obj = m.group(2).trim();
                launchHandler.sendMessage(msgLaunch);
            }
            else if (m.group(1).equalsIgnoreCase(Consts.resumeCommand))
            {

            }
            else if (m.group(1).equalsIgnoreCase(Consts.playbackCommand))
            {
                boolean check=true;
                //saying "your latest reply is:"
                Message msgTalk = new Message();
                msgTalk.arg1 = 1;
                msgTalk.obj = "and your latest reply is";
                talkHandler.sendMessage(msgTalk);

                msgTalk.arg1 = 2;
                msgTalk.obj = "qwewer";

                try {
                    Thread.sleep(500);
                } catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }

                while(ttsCont.Speaking()){
                    try {
                        Thread.sleep(1000);
                    } catch(InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
/*
                while(talkHandler.sendMessage(msgTalk)){
                    try {
                        Thread.sleep(1000);                 //1000 milliseconds is one second.
                    } catch(InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }*/
                //playback
                //double volume=100000;
                AudioTrack trackplay=new AudioTrack(AudioManager.STREAM_MUSIC, 16000,AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT,1280, AudioTrack.MODE_STREAM);
                //trackplay.setStereoVolume((float) volume,(float) volume);
                byte[] buffer = new byte[1280];
                int byteread;
                short tmpshort;
                double totalEn;
                File historyRaw=new File("/sdcard/history.raw");

                //AudioManager am = (AudioManager) appContect.getSystemService(Context.AUDIO_SERVICE);
                //int amStreamMusicMaxVol = am.getStreamMaxVolume(am.STREAM_MUSIC);
                //add by tony, tingyao

                int endFrame = (int) (historyRaw.length()/2 - 10000);
                trackplay.setNotificationMarkerPosition(endFrame);
                trackplay.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                    @Override
                    public void onMarkerReached(AudioTrack track) {
                        Log.d("LogicControl", "Finished playing back user's response. Inform server");
                        ConnectToServer(Consts.userPlaybackDone,"");
                    }
                    @Override
                    public void onPeriodicNotification(AudioTrack track) {
                    }
                });
                ttsCont.volumeUp();
                try(FileInputStream in = new FileInputStream(historyRaw)){
                    trackplay.play();
                    while((byteread = in.read(buffer))!=-1 ){
                        totalEn=0;
                        for(int i = 0;i<640;i++){
                            tmpshort = (short) ((buffer[2*i] << 8) | buffer[2*i+1]);
                            totalEn+=tmpshort*tmpshort;
                        }
                        if(totalEn>7.0E10) {

                            for (int i = 0; i < 640; i++) {
                                tmpshort = (short) ((buffer[2 * i] << 8) | buffer[2 * i + 1]);
                                tmpshort *= 2;
                                buffer[2 * i] = (byte) (tmpshort >>> 8);
                                buffer[2 * i + 1] = (byte) (tmpshort >>> 0);
                            }
                        }
                        trackplay.write(buffer, 0, 1280);


                    }
                    trackplay.release();
                }
                catch (Exception ex){}

            }
            /*
            String command = m.group(1);
            String args = null;
            if (m.groupCount() > 1)
                args = m.group(2);
            try
            {
                messageController.dealWithMessage(command, args, messageBroker, talkHandler);
            }
            catch (Exception ex)
            {
                Log.e("messageController.dealWithMessage", "command=" + command + " args=" + args + " " + ex.toString());
                //ex.printStackTrace();
            }
            */
        }
    }

    /// connects to TCP server and sends the string as the first messages to send (use: obj.execute(message)).
    public class connectTask extends AsyncTask<String, String, TCPClient>
    {

        @Override
        protected TCPClient doInBackground(String... message)
        {

            //we create a TCPClient object and
            tcpClient = new TCPClient(tcpIpAddr, tcpIpPort, new TCPClient.OnMessageReceived()
            {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message)
                {
                    dealWithMessage(message); //TODO: make sure that runs on original thread. (avoid multithread unsafe access).
                    //publishProgress(message);//this method calls the onProgressUpdate
                }
            });

            try
            {
                tcpClient.run(message);
            }
            catch (IOException e)
            {

                Message msgNotConnect = new Message();
                msgNotConnect.arg1 = 1;
                msgNotConnect.arg2 = 1; //important message.
                msgNotConnect.obj = "Could not connect!";
                userNotifierHandler.sendMessage(msgNotConnect);

                Log.e("LogicControl", "C: Could not Connect!");
                tcpClient.closeConnection();
            }
            return null;
        }
    }

    /*
     * returns whether is reconnecting now.
     */
    public boolean reconnectIfNeeded()
    {
        Log.d("LogicControl", "Reconnecting if needed");

        boolean isReconnecting = needToReconnect;
        if (needToReconnect)
        {
            needToReconnect = false;
            ConnectToServer();
        }
        return isReconnecting;
    }

    /**
     * send close session message to the server
     */
    public void closeSession()
    {
        String messageToServer = uniqueId + Consts.commandChar + Consts.closeSession + Consts.commandChar;
        if (tcpClient != null)
            tcpClient.sendMessage(messageToServer);
        // close connection
    }

    public void initSession()
    {
        String messageToServer = uniqueId + Consts.commandChar + Consts.initSession + Consts.commandChar;
        if (tcpClient != null)
            tcpClient.sendMessage(messageToServer);
        // close connection
    }
}

package com.inMind.inMindAgent;

import InMind.Consts;
import android.util.Log;


/**
 * Created by Tony on 4/3/15.
 */
public class TcpPing implements  Runnable{
    LogicController logicController;
    boolean running;

    public TcpPing(LogicController logicController)
    {
        this.logicController = logicController;
        running = true;
    }

    public void run()
    {
        while (running)
        {
            try {
                //Log.w("tcpPing", "pinging");
                String pingMessage = logicController.uniqueId + Consts.commandChar + Consts.tcpPing + Consts.commandChar;
                if (logicController.tcpClient != null)
                {
                    logicController.tcpClient.sendMessage(pingMessage);
                    Thread.sleep(5000);                 //10 seconds
                }
            } catch(InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        Log.w("tcpPing", "pinging stopped");
    }

    public void stop()
    {
;        running = false;
    }


}

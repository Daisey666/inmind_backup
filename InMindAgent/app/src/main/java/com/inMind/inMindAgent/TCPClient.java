package com.inMind.inMindAgent;

import android.util.Log;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by Amos Azaria on 01-Nov-14.
 */
public class TCPClient
{

    static final int connectionTimeout = 2500; // in milliseconds

    private String serverMessage;
    String ipAddr;
    int portNum;
    private OnMessageReceived mMessageListener = null;
    private boolean mRun = false;

    PrintWriter out;
    BufferedReader in;

    /**
     * Constructor of the class. OnMessagedReceived listens for the messages
     * received from server
     */
    public TCPClient(String ipAddr, int portNum, OnMessageReceived listener)
    {
        mMessageListener = listener;
        this.ipAddr = ipAddr;
        this.portNum = portNum;
    }

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    public void sendMessage(String message)
    {
        if (out != null && !out.checkError())
        {
            out.println(message);
            out.flush();
        }
    }

    public void closeConnection()
    {
        mRun = false;
    }

    //connects and sends messages
    public void run(String[] messages) throws IOException
    {

        mRun = true;

        InetAddress serverAddr = InetAddress.getByName(ipAddr);
        Log.d("TCP Client", "C: Connecting...");

        // create a socket to make the connection with the server
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(serverAddr, portNum),
                connectionTimeout);

        Log.d("TCP Client", "C: Connected!");

        try
        {

            // send the message to the server
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    socket.getOutputStream())), true);

            Log.d("TCP Client", "C: Sent.");

            for (String message : messages)
            {
                if (!message.isEmpty())
                {
                    out.println(message);
                    out.flush();
                }
            }

            // receive the message which the server sends back
            in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));

            // in this while the client listens for the messages sent by the
            // server
            while (mRun)
            {
                serverMessage = in.readLine();

                if (serverMessage != null && mMessageListener != null)
                {
                    Log.d("TCP Client", "Got a message." + serverMessage);
                    // call the method messageReceived from MyActivity class
                    mMessageListener.messageReceived(serverMessage);
                }
                serverMessage = null;

            }

            Log.d("RESPONSE FROM SERVER", "S: Received Message: '"
                    + serverMessage + "'");

        }
        catch (Exception e)
        {

            Log.e("TCP", "S: Error", e);

        }
        finally
        {
            // the socket must be closed. It is not possible to reconnect to
            // this socket
            // after it is closed, which means a new socket instance has to be
            // created.
            try
            {
                socket.close();
            }
            catch (Exception e)
            {
                Log.e("TCP", "S: Could not close", e);
            }
        }

        // Log.e("TCP", "C: could not connect", e);

    }

    // Declare the interface. The method messageReceived(String message) will
    // must be implemented in the MyActivity
    // class at on asynckTask doInBackground
    public interface OnMessageReceived
    {
        public void messageReceived(String message);
    }

}

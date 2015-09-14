package com.example.tingyao.voicetest;

import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

/**
 * Created by tingyao on 9/3/15.
 */
public class opensmileManager implements Runnable {
    private Socket opensmile_sever;
    static{
        System.loadLibrary("opensmiletest");
    }
    public native String sockettest();


    @Override
    public void run(){
        sockettest();
    }
/*
        try{
            ServerSocket listener = new ServerSocket(5000);
            Socket socket = listener.accept();
            while(true){

                PrintWriter out = new PrintWriter(new BufferedWriter( new OutputStreamWriter(socket.getOutputStream())),true);
                out.println("fake opensmile output XD");
            }
        }
        catch(IOException ioe){
            System.out.println("io exception");
        }
        finally {
            System.out.println("end of run");
        }
    }*/
}

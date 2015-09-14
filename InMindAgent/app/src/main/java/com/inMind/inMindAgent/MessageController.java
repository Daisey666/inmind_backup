package com.inMind.inMindAgent;

import android.content.Context;
import android.os.Handler;

import com.yahoo.inmind.middleware.control.MessageBroker;

public class MessageController
{
    public void dealWithMessage(String command, String args, MessageBroker messageBroker, Handler talkHandler)
    {
        if (command.equalsIgnoreCase("News"))
        {
            NewsCommunicator.dealWithMessage(args, messageBroker, talkHandler);
        }
        //call middleware;
    }

}

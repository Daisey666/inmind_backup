package com.inMind.inMindAgent;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.yahoo.inmind.middleware.control.MessageBroker;
import com.yahoo.inmind.middleware.events.MBRequest;
import com.yahoo.inmind.model.NewsArticleVector;
import com.yahoo.inmind.reader.ReaderMainActivity;
import com.yahoo.inmind.util.Constants;


/**
 * Created by Amos Azaria on 03-Feb-15.
 */
public class NewsCommunicator
{

    public static void dealWithMessage(String args, MessageBroker messageBroker, Handler talkHandler)
    {
        if (messageBroker == null)
        {
            Log.e("NewsCommunicator", "Error messageBroker==null");
            return;
        }
        Log.d("Middleware", "Contacting News");

        try
        {
            if (args.equalsIgnoreCase("launch"))
            {
                MessageBroker.set( new MBRequest(Constants.SET_NEWS_LIST_SIZE, 40));
                MBRequest request = new MBRequest(Constants.MSG_LAUNCH_BASE_NEWS_ACTIVITY);
                messageBroker.send(request);

            }
            else if (args.equalsIgnoreCase("next"))
            {
                MBRequest request = new MBRequest(Constants.MSG_SHOW_NEXT_ARTICLE);
                messageBroker.send(request);
            }
            else if (args.equalsIgnoreCase("previous"))
            {
                    MBRequest request = new MBRequest(Constants.MSG_SHOW_PREVIOUS_ARTICLE);
                    messageBroker.send(request);
            }
            else if (args.equalsIgnoreCase("expand"))
            {
                MBRequest request = new MBRequest(Constants.MSG_EXPAND_ARTICLE);
                messageBroker.send(request);
            }
            else if (args.equalsIgnoreCase("read"))
            {
                MBRequest request = new MBRequest(Constants.MSG_GET_ARTICLE_POSITION);
                int position = (Integer) messageBroker.get(request);

                String title = NewsArticleVector.getInstance().get( position ).getTitle();
                String summary = NewsArticleVector.getInstance().get( position ).getSummary();
                Message msgTalk = new Message();
                msgTalk.arg1 = 0; //do not toast
                msgTalk.obj = title + "." + summary;
                talkHandler.sendMessage(msgTalk);
            }
        }
        catch (Exception ex)
        {
            Log.e("Middleware", "Excpetion creating News Activity: " + ex.getMessage());
        }

    }
}

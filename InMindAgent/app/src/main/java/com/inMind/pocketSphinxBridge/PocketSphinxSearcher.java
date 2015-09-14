package com.inMind.pocketSphinxBridge;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import android.content.Context;
import android.os.AsyncTask;
import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;



public class PocketSphinxSearcher {


    public interface SphinxRes
	{
		void keyDetected();				
	}
	
	private class MyListener implements RecognitionListener
	{
		SphinxRes sphinxRes = null;
		String keyPhrase;
		
		public MyListener(SphinxRes sphinxRes, String keyPhrase)
		{
			this.sphinxRes = sphinxRes;
			this.keyPhrase = keyPhrase;
		}
		
		public void testRes(Hypothesis hypothesis)
		{
	        String text = hypothesis.getHypstr();
	        if (text.equals(keyPhrase) && sphinxRes!= null)
	        {
	        	recognizer.stop();
	            sphinxRes.keyDetected();
	        }
		}

	    @Override
	    public void onPartialResult(Hypothesis hypothesis) {
            testRes(hypothesis);
	    }

	    @Override
	    public void onResult(Hypothesis hypothesis) {
	    	//testRes(hypothesis);
	    }

	    @Override
	    public void onBeginningOfSpeech() {
	    }

	    @Override
	    public void onEndOfSpeech() {
	    }
		
	}

    private static final String KWS_SEARCH = "wakeup";
    //private static final String KEYPHRASE = "in mind agent";//"isteveni";

    private SpeechRecognizer recognizer;
    private HashMap<String, Integer> captions;
    SphinxRes sphinxRes;
    String keyPhrase;
    Context context;

    public PocketSphinxSearcher(Context contextvar, String keyPhrase, SphinxRes sphinxRes) {

    	this.sphinxRes = sphinxRes;
    	this.keyPhrase = keyPhrase;
    	this.context = contextvar;
    }
    
    public void startListeningForKeyword()
    {
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task

        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(context);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                } else {
                	recognizer.startListening(KWS_SEARCH);
                }
            }
        }.execute();    	
    }


    private void setupRecognizer(File assetsDir) {
        File modelsDir = new File(assetsDir, "models");
        recognizer = defaultSetup()
                .setAcousticModel(new File(modelsDir, "hmm/en-us-semi"))
                .setDictionary(new File(modelsDir, "dict/cmu07a.dic"))
                .setRawLogDir(assetsDir).setKeywordThreshold(1e-20f)
                .getRecognizer();
        recognizer.addListener(new MyListener(sphinxRes, keyPhrase));

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, keyPhrase);

    }

    public void stopListening()
    {
        recognizer.stop();
    }
}

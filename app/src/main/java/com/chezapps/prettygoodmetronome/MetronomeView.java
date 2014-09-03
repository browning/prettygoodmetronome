package com.chezapps.prettygoodmetronome;
//freepik.com icon

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.graphics.Typeface;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.ImageView;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import android.content.Context;
import java.io.IOException;
import java.io.File;
import android.widget.Toast;


import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;
import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

public class MetronomeView extends Activity implements RecognitionListener {

    private MetronomeAsyncTask metroTask;
    private Handler mHandler;
    private Context mContext;

    private short bpm = 100;
    private short noteValue = 4;
    private short beats = 4;
    private short volume;
    private short initialVolume;
    private double beatSound = 400;
    private double sound = 380;
    private boolean started = false;
    SharedPreferences sharedPref;

    private static final String KEYPHRASE_FASTER = "faster";
    private static final String KEYPHRASE_SLOWER = "slower";

    private SpeechRecognizer recognizer;

    private AdView adView;

    /* Your ad unit id. Replace with your actual ad unit id. */
    private static final String AD_UNIT_ID = "INSERT_YOUR_AD_UNIT_ID_HERE";

    private int myprogress = 60;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metronome_view);
        mContext = this;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        metroTask = new MetronomeAsyncTask();

        Typeface tf = Typeface.createFromAsset(getAssets(),"digital-7.ttf");
        final TextView tv = (TextView) findViewById(R.id.bpmview);
        tv.setTypeface(tf);
        tv.setTextSize(100);
        tv.setText("60");

        final ImageView iv = (ImageView) findViewById(R.id.startview);
        iv.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                if (started) {
                    metroTask.stop();
                    metroTask = new MetronomeAsyncTask();
                    Runtime.getRuntime().gc();
                    started = false;
                    iv.setImageResource(R.drawable.play);
                }
                else {
                    String beats_per_measure = sharedPref.getString("beats", "4");
                    beats = (short)Integer.parseInt(beats_per_measure);

                    iv.setImageResource(R.drawable.stop);
                    started = true;
                    metroTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
                    metroTask.setBpm((short)(myprogress+1));
                    metroTask.setBeat(beats);
                }
            }
        });


        final SeekBar sk=(SeekBar) findViewById(R.id.seekBar);
        sk.setProgress(60);
        sk.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {

                myprogress = progress;
                tv.setText(Integer.toString(progress+1));
                metroTask.setBpm((short) (progress + 1));


            }
        });

        // Look up the AdView as a resource and load a request.
        AdView adView = (AdView) this.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(MetronomeView.this);
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
                    Toast.makeText(getApplicationContext(), "failed to init", Toast.LENGTH_SHORT).show();
                } else {
                    switchSearch("wakeup");
                }
            }
        }.execute();
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        String text = hypothesis.getHypstr();
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
            int inc = 0;
            if ( text.equals(KEYPHRASE_FASTER)) {
                inc = 5;
            }
            else if (text.equals(KEYPHRASE_SLOWER)) {
                inc = -5;
            }
            if ( myprogress + inc <1 || myprogress + inc > 250)
                return;
            myprogress = myprogress + inc;
            TextView tv = (TextView) findViewById(R.id.bpmview);
            tv.setText(Integer.toString(myprogress));
            metroTask.setBpm((short) (myprogress));
        }
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    @Override
    public void onEndOfSpeech() {
        switchSearch("wakeup");
    }

    private void switchSearch(String searchName) {
        recognizer.stop();
        recognizer.startListening("menu");

    }

    private void setupRecognizer(File assetsDir) {
        File modelsDir = new File(assetsDir, "models");
        recognizer = defaultSetup()
                .setAcousticModel(new File(modelsDir, "hmm/en-us-semi"))
                .setDictionary(new File(modelsDir, "dict/cmu07a.dic"))
                .setRawLogDir(assetsDir).setKeywordThreshold(1e-20f)
                .getRecognizer();
        recognizer.addListener(this);

        File menuGrammar = new File(modelsDir, "grammar/menu.gram");
        recognizer.addGrammarSearch("menu", menuGrammar);




    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.metronome_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MetronomeView.this,
                    SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Handler getHandler() {
        return new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String message = (String)msg.obj;

                boolean showflash = sharedPref.getBoolean("flashing_enabled", true);
                if (!showflash) {
                    return;
                }

                View someView = findViewById(R.id.seekBar);
                // Find the root view
                View root = someView.getRootView();
                // Set the color
                if (message.equals("1")) {
                    root.setBackgroundColor(Color.YELLOW);
                }
                else {
                    root.setBackgroundColor(Color.WHITE);
                }

            }
        };
    }

    public void onBackPressed() {
        metroTask.stop();
        recognizer.stop();
//    	metroTask = new MetronomeAsyncTask();
        Runtime.getRuntime().gc();
       // audio.setStreamVolume(AudioManager.STREAM_MUSIC, initialVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        finish();
    }

    public void onStart() {
        metroTask = new MetronomeAsyncTask();

        super.onStart();
    }

    public void onStop() {
        metroTask.stop();
        recognizer.stop();
//    	metroTask = new MetronomeAsyncTask();
        Runtime.getRuntime().gc();
        // audio.setStreamVolume(AudioManager.STREAM_MUSIC, initialVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        super.onStop();
    }

    private class MetronomeAsyncTask extends AsyncTask<Void,Void,String> {
        Metronome metronome;

        MetronomeAsyncTask() {
            mHandler = getHandler();
            metronome = new Metronome(mHandler, mContext);
        }

        protected String doInBackground(Void... params) {
            metronome.setBeat(beats);
            metronome.setNoteValue(noteValue);
            metronome.setBpm((short)(myprogress+1));
            metronome.setBeatSound(beatSound);
            metronome.setSound(sound);

            metronome.play();

            return null;
        }

        public void stop() {
            metronome.stop();
            metronome = null;
        }

        public void setBpm(short bpm) {
            metronome.setBpm(bpm);
            metronome.calcSilence();
        }

        public void setBeat(short beat) {
            if(metronome != null)
                metronome.setBeat(beat);
        }

    }
}

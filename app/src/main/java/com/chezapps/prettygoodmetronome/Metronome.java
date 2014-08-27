package com.chezapps.prettygoodmetronome;

import android.os.Handler;
import android.os.Message;
import java.io.InputStream;
import android.content.Context;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.io.BufferedInputStream;
import java.io.DataInputStream;


public class Metronome {

    private double bpm;
    private int beat;
    private int noteValue;
    private int silence;

    private double beatSound;
    private double sound;
    private final int tick = 5000; // samples of tick

    private Handler mHandler;

    private boolean play = true;

    private AudioGenerator audioGenerator = new AudioGenerator(44100);

    private byte[] soundTickArray = new byte[10000];
    private double[] soundTockArray;
    private double[] silenceSoundArray;
    private Message msg;
    private int currentBeat = 1;

    private byte[] drumnotes = new byte[8000];
    private byte[] ticknotes = new byte[8000];

    public Metronome(Handler handler, Context mContext) {
        InputStream is = mContext.getResources().openRawResource(R.raw.hat2);
        BufferedInputStream     bis = new BufferedInputStream   (is, 44100);
        DataInputStream         dis = new DataInputStream       (bis);

        try {
            int i = 0;                                                          //  Read the file into the "music" array
            while (dis.available() > 0 && i < 10000)
            {
                soundTickArray[i] = dis.readByte();                                      //  This assignment does not reverse the order
                i++;
            }

            dis.close();


        } catch (IOException e) {
            e.printStackTrace();
        }

        this.mHandler = handler;
        audioGenerator.createPlayer();
    }


    public void setBPM(double b) {
        bpm = b;
    }

    public double[] toDoubleArray(byte[] byteArray){
        int times = Double.SIZE / Byte.SIZE;
        double[] doubles = new double[byteArray.length / times];
        for(int i=0;i<doubles.length;i++){
            doubles[i] = ByteBuffer.wrap(byteArray, i*times, times).getDouble();
        }
        return doubles;
    }

    public void calcSilence() {
        silence = (int) (((60/bpm)*44100)-tick);
       // soundTickArray = new double[this.tick];
        soundTockArray = new double[this.tick];
        silenceSoundArray = new double[this.silence];
        msg = new Message();
        msg.obj = ""+currentBeat;
       // double[] tick = audioGenerator.getSineWave(this.tick, 8000, beatSound);
       // double[] tock = audioGenerator.getSineWave(this.tick, 8000, sound);
       // double[] tick = toDoubleArray(drumnotes);
      //  double[] tock = toDoubleArray(ticknotes);
      //  for(int i=0;i<this.tick;i++) {
       //     soundTickArray[i] = tick[i]*8;
        //    soundTockArray[i] = tick[i]*16;
       // }
        for(int i=0;i<silence;i++)
            silenceSoundArray[i] = 0;
    }

    public void play() {
        calcSilence();
        do {
            msg = new Message();
            msg.obj = ""+currentBeat;
            if(currentBeat == 1)

                audioGenerator.writeSoundBytes(soundTickArray);
            else
                audioGenerator.writeSoundBytes(soundTickArray);
            if(bpm <= 120)
                mHandler.sendMessage(msg);
            audioGenerator.writeSound(silenceSoundArray);
            if(bpm > 120)
                mHandler.sendMessage(msg);
            currentBeat++;
            if(currentBeat > beat)
                currentBeat = 1;
        } while(play);
    }

    public void stop() {
        play = false;
        audioGenerator.destroyAudioTrack();
    }

    public double getBpm() {
        return bpm;
    }

    public void setBpm(int bpm) {
        this.bpm = bpm;
    }

    public int getNoteValue() {
        return noteValue;
    }

    public void setNoteValue(int bpmetre) {
        this.noteValue = bpmetre;
    }

    public int getBeat() {
        return beat;
    }

    public void setBeat(int beat) {
        this.beat = beat;
    }

    public double getBeatSound() {
        return beatSound;
    }

    public void setBeatSound(double sound1) {
        this.beatSound = sound1;
    }

    public double getSound() {
        return sound;
    }

    public void setSound(double sound2) {
        this.sound = sound2;
    }

        /* Getters and Setters ... */
}

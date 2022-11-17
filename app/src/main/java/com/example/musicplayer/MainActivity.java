package com.example.musicplayer;


import android.content.Intent;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    TextView info, state;
    Button  buttonOpenNew;
    Button buttonPlay, buttonPause, buttonStop;
    SeekBar timeLine;
    LinearLayout timeFrame;
    TextView timePos, timeDur;
    ImageView img;

    final static int RQS_OPEN_AUDIO_MP3_NEW = 1;

    MediaPlayer mediaPlayer;
    String srcPath = null;

    enum MP_State {
        Idle, Initialized, Prepared, Started, Paused,
        Stopped, PlaybackCompleted, End, Error, Preparing
    }

    MP_State mediaPlayerState;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        buttonOpenNew = (Button) findViewById(R.id.OpenButton);
        buttonOpenNew.setOnClickListener(buttonOpenNewOnClickListener);

        info = (TextView) findViewById(R.id.info);
        state = (TextView) findViewById(R.id.state);

        buttonPlay = (Button) findViewById(R.id.play);
        buttonPlay.setOnClickListener(buttonPlayOnClickListener);
        buttonPause = (Button) findViewById(R.id.pause);
        buttonPause.setOnClickListener(buttonPauseOnClickListener);
        buttonStop = (Button) findViewById(R.id.stop);
        buttonStop.setOnClickListener(buttonStopOnClickListener);

        //
        timeLine = (SeekBar) findViewById(R.id.seekbartimeline);
        timeFrame = (LinearLayout) findViewById(R.id.timeframe);
        timePos = (TextView) findViewById(R.id.pos);
        timeDur = (TextView) findViewById(R.id.dur);

        ScheduledExecutorService myScheduledExecutorService
                = Executors.newScheduledThreadPool(1);

        myScheduledExecutorService.scheduleWithFixedDelay(
                new Runnable() {
                    @Override
                    public void run() {
                        monitorHandler.sendMessage(monitorHandler.obtainMessage());
                    }
                },
                200, //initialDelay
                200, //delay
                TimeUnit.MILLISECONDS);

    }

    Handler monitorHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            mediaPlayerMonitor();
        }

    };

    private void mediaPlayerMonitor() {
        if (mediaPlayer == null) {

            timeFrame.setVisibility(View.INVISIBLE);
        } else {
            if (mediaPlayer.isPlaying()) {
                timeLine.setVisibility(View.VISIBLE);
                timeFrame.setVisibility(View.VISIBLE);

                int mediaDuration = mediaPlayer.getDuration();
                int mediaPosition = mediaPlayer.getCurrentPosition();
                timeLine.setMax(mediaDuration);
                timeLine.setProgress(mediaPosition);
                timePos.setText(String.valueOf((float) mediaPosition / 1000) + "s");
                timeDur.setText(String.valueOf((float) mediaDuration / 1000) + "s");
            } else {

                timeFrame.setVisibility(View.INVISIBLE);
            }
        }
    }

    MediaPlayer.OnErrorListener mediaPlayerOnErrorListener
            = new MediaPlayer.OnErrorListener() {

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            // TODO Auto-generated method stub

            mediaPlayerState = MP_State.Error;
            showMediaPlayerState();

            return false;
        }
    };


    private void cmdReset() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnErrorListener(mediaPlayerOnErrorListener);
        }
        mediaPlayer.reset();
        mediaPlayerState = MP_State.Idle;
        showMediaPlayerState();
    }

    private void cmdSetDataSource(Uri uri) {
        if (mediaPlayerState == MP_State.Idle) {
            try {
                mediaPlayer.setDataSource(MainActivity.this, uri);
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
               // mmr.setDataSource(MainActivity.this, uri);
               // byte[] artBytes =  mmr.getEmbeddedPicture();
                //if(artBytes!=null)
                //{
                   //Bitmap bm = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.length);
                   //img.setImageBitmap(bm);
               // }

                mediaPlayerState = MP_State.Initialized;
            } catch (IllegalArgumentException e) {
                Toast.makeText(MainActivity.this,
                        e.toString(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            } catch (IllegalStateException e) {
                Toast.makeText(MainActivity.this,
                        e.toString(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            } catch (IOException e) {
                Toast.makeText(MainActivity.this,
                        e.toString(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        } else {
            Toast.makeText(MainActivity.this,
                    "Invalid State@cmdSetDataSource - skip",
                    Toast.LENGTH_LONG).show();
        }

        showMediaPlayerState();
    }


    private void cmdPrepare() {

        if (mediaPlayerState == MP_State.Initialized
                || mediaPlayerState == MP_State.Stopped) {
            try {
                mediaPlayer.prepare();
                mediaPlayerState = MP_State.Prepared;
            } catch (IllegalStateException e) {
                Toast.makeText(MainActivity.this,
                        e.toString(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            } catch (IOException e) {
                Toast.makeText(MainActivity.this,
                        e.toString(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        } else {
            Toast.makeText(MainActivity.this,
                    "Invalid State@cmdPrepare() - skip",
                    Toast.LENGTH_LONG).show();
        }

        showMediaPlayerState();
    }

    private void cmdStart() {
        if (mediaPlayerState == MP_State.Prepared
                || mediaPlayerState == MP_State.Started
                || mediaPlayerState == MP_State.Paused
                || mediaPlayerState == MP_State.PlaybackCompleted) {
            mediaPlayer.start();
            mediaPlayerState = MP_State.Started;
        } else {
            Toast.makeText(MainActivity.this,
                    "Invalid State@cmdStart() - skip",
                    Toast.LENGTH_LONG).show();
        }

        showMediaPlayerState();
    }

    private void cmdPause() {
        if (mediaPlayerState == MP_State.Started
                || mediaPlayerState == MP_State.Paused) {
            mediaPlayer.pause();
            mediaPlayerState = MP_State.Paused;
        } else {
            Toast.makeText(MainActivity.this,
                    "Invalid State@cmdPause() - skip",
                    Toast.LENGTH_LONG).show();
        }
        showMediaPlayerState();
    }

    private void cmdStop() {
        timeLine.setVisibility(View.INVISIBLE);
        if (mediaPlayerState == MP_State.Prepared
                || mediaPlayerState == MP_State.Started
                || mediaPlayerState == MP_State.Stopped
                || mediaPlayerState == MP_State.Paused
                || mediaPlayerState == MP_State.PlaybackCompleted) {
            mediaPlayer.stop();
            mediaPlayerState = MP_State.Stopped;
        } else {
            Toast.makeText(MainActivity.this,
                    "Invalid State@cmdStop() - skip",
                    Toast.LENGTH_LONG).show();
        }
        showMediaPlayerState();

    }

    private void showMediaPlayerState() {

        switch (mediaPlayerState) {
            case Idle:
                state.setText("Idle");
                break;
            case Initialized:
                state.setText("Initialized");
                break;
            case Prepared:
                state.setText("Prepared");
                break;
            case Started:
                state.setText("Started");
                break;
            case Paused:
                state.setText("Paused");
                break;
            case Stopped:
                state.setText("Stopped");
                break;
            case PlaybackCompleted:
                state.setText("PlaybackCompleted");
                break;
            case End:
                state.setText("End");
                break;
            case Error:
                state.setText("Error");
                break;
            case Preparing:
                state.setText("Preparing");
                break;
            default:
                state.setText("Unknown!");
        }
    }

    View.OnClickListener buttonPlayOnClickListener
            = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            if (srcPath == null) {
                Toast.makeText(MainActivity.this,
                        "No file selected",
                        Toast.LENGTH_LONG).show();
            } else {
                cmdPrepare();
                cmdStart();
            }

        }

    };

    View.OnClickListener buttonPauseOnClickListener
            = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (srcPath == null){
                Toast.makeText(MainActivity.this,
                        "No file selected",
                        Toast.LENGTH_LONG).show();
            }
            else {
                cmdPause();
            }
        }

    };

    View.OnClickListener buttonStopOnClickListener
            = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (srcPath == null){
            Toast.makeText(MainActivity.this,
                    "No file selected",
                    Toast.LENGTH_LONG).show();
        }
            else{

            cmdStop();}

        }

    };
    View.OnClickListener buttonOpenNewOnClickListener
            = new View.OnClickListener() {

        @Override
        public void onClick(View arg0) {
            Intent intent = new Intent();
            intent.setType("audio/mp3");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(
                    intent, "Open Audio (mp3) file"), RQS_OPEN_AUDIO_MP3_NEW);

        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Uri audioFileUri = data.getData();


            srcPath = audioFileUri.getPath();
            String[] array = srcPath.split("/");
            info.setText(array[array.length-1].substring(0,array[array.length-1].indexOf("_")));

            cmdReset();
            cmdSetDataSource(audioFileUri);
        }
    }
}
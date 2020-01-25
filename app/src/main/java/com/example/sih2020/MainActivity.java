package com.example.sih2020;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Cartesian;
import com.anychart.core.cartesian.series.Column;
import com.anychart.enums.Anchor;
import com.anychart.enums.HoverMode;
import com.anychart.enums.Position;
import com.anychart.enums.TooltipPositionMode;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.JsonArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import ai.api.AIDataService;
import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;

import static androidx.constraintlayout.widget.Constraints.TAG;

public class MainActivity extends AppCompatActivity implements AIListener {
    RecyclerView recyclerView;
    EditText messageBox;
    ImageView sendBtn;
    DatabaseReference ref;
    FirebaseRecyclerAdapter<ChatMessage, chat_rec> adapter;
    Boolean flagFab = true;
    FirebaseUser user;
    Button capture, analyze;

    private AIService aiService;
    private AnyChartView anyChartView;

    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        recyclerView = findViewById(R.id.recyclerView);
        messageBox = findViewById(R.id.messageBox);
        sendBtn = findViewById(R.id.sendBtn);
        capture = findViewById(R.id.capture);
        analyze = findViewById(R.id.analyze);
        progressBar = findViewById(R.id.progressBar);
        anyChartView = findViewById(R.id.anychartview);
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
        recyclerView.setHasFixedSize(true);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        ref = FirebaseDatabase.getInstance().getReference().child(user.getUid());
        ref.keepSynced(true);

        final AIConfiguration config = new AIConfiguration("ae29fbe93f274a5b925c6bb039667254",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        aiService = AIService.getService(this, config);
        aiService.setListener(this);

        final AIDataService aiDataService = new AIDataService(config);

        final AIRequest aiRequest = new AIRequest();

        String s = getIntent().getStringExtra("ingredients details");

        if (s != null) {
            try {
                s = s.replace(", ", "\n");
                s = s.toUpperCase();
                try {
                    s = s.substring(s.indexOf("INGRE"));
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "" + e, Toast.LENGTH_SHORT).show();
                }
                messageBox.setText(s);
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "" + e, Toast.LENGTH_SHORT).show();
                messageBox.setText(s);
            }

        }
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String message = messageBox.getText().toString().trim();

                if (!message.equals("")) {


                    ChatMessage chatMessage = new ChatMessage(message, "user");
                    ref.child("chat").push().setValue(chatMessage);

                    try {

                        aiRequest.setQuery(message);
                        new AsyncTask<AIRequest, Void, AIResponse>() {

                            @Override
                            protected AIResponse doInBackground(AIRequest... aiRequests) {
                                final AIRequest request = aiRequests[0];
                                try {
                                    final AIResponse response = aiDataService.request(aiRequest);
                                    return response;
                                } catch (AIServiceException e) {
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(AIResponse response) {
                                if (response != null) {

                                    Result result = response.getResult();
                                    String reply = result.getFulfillment().getSpeech();
                                    ChatMessage chatMessage = new ChatMessage(reply, "bot");
                                    ref.child("chat").push().setValue(chatMessage);
                                }
                            }
                        }.execute(aiRequest);
                    } catch (Exception e) {
                    }
                } else {
                    aiService.startListening();
                }

                messageBox.setText("");

            }
        });


        messageBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ImageView fab_img = findViewById(R.id.sendBtn);


                if (s.toString().trim().length() != 0 && flagFab) {
                    ImageViewAnimatedChange(MainActivity.this, fab_img, getResources().getDrawable(R.drawable.ic_send_black_24dp));
                    flagFab = false;

                } else if (s.toString().trim().length() == 0) {
                    ImageViewAnimatedChange(MainActivity.this, fab_img, getResources().getDrawable(R.drawable.ic_send_black_24dp));
                    flagFab = true;

                }


            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        adapter = new FirebaseRecyclerAdapter<ChatMessage, chat_rec>(ChatMessage.class, R.layout.msglist, chat_rec.class, ref.child("chat")) {
            @NonNull


            @Override
            protected void populateViewHolder(chat_rec viewHolder, ChatMessage model, int position) {
                if (model.getMsgUser().equals("user")) {


                    viewHolder.rightText.setText(model.getMsgText());
                    viewHolder.rightText.setVisibility(View.VISIBLE);
                    viewHolder.leftText.setVisibility(View.GONE);
                } else {
                    viewHolder.leftText.setText(model.getMsgText());
                    viewHolder.rightText.setVisibility(View.GONE);
                    viewHolder.leftText.setVisibility(View.VISIBLE);
                }
            }


        };

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);

                int msgCount = adapter.getItemCount();
                int lastVisiblePosition = linearLayoutManager.findLastCompletelyVisibleItemPosition();

                if (lastVisiblePosition == -1 ||
                        (positionStart >= (msgCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    recyclerView.scrollToPosition(positionStart);

                }

            }
        });

        recyclerView.setAdapter(adapter);

        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), TextRecogActivity.class));
                finish();
            }
        });
        analyze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String userName = user.getEmail();
                    assert userName != null;
                    userName = userName.substring(0, userName.indexOf("@"));
                    setThreadForApiCalling(userName);

                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, ""+e, Toast.LENGTH_SHORT).show();
                }
//                anyChartView.setVisibility(View.VISIBLE);
//                progressBar.setVisibility(View.VISIBLE);
//                anyChartView.setProgressBar(progressBar);
//
//
//                Cartesian cartesian = AnyChart.column();
//
//                List<DataEntry> data = new ArrayList<>();
//                data.add(new ValueDataEntry("NIACIN", 3));
//                data.add(new ValueDataEntry("REDUCED IRON", 3));
//                data.add(new ValueDataEntry("SUGAR", 4));
//                data.add(new ValueDataEntry("NIACIN, REDUCED IRON", 2));
//                data.add(new ValueDataEntry("NIACIN, SUGAR", 3));
//                data.add(new ValueDataEntry("REDUCED IRON, SUGAR", 1));
//                data.add(new ValueDataEntry("NIACIN, REDUCED IRON, SUGAR", 3));
//
//
//                Column column = cartesian.column(data);
//                column.tooltip()
//                        .titleFormat("{%X}")
//                        .position(Position.CENTER_BOTTOM)
//                        .anchor(Anchor.CENTER_BOTTOM)
//                        .offsetX(0d)
//                        .offsetY(5d)
//                        .format("number of intakes: {%Value}{groupsSeparator: }");
//
//                cartesian.animation(true);
//                cartesian.title("INGREDIENTS CONSUMED BY YOU DURING THIS ALLERGY PERIOD.");
//
//                cartesian.yScale().minimum(0d);
//
//                cartesian.yAxis(0).labels().format("{%Value}{groupsSeparator: }");
//
//                cartesian.tooltip().positionMode(TooltipPositionMode.POINT);
//                cartesian.interactivity().hoverMode(HoverMode.BY_X);
//
//                cartesian.xAxis(0).title("INGREDIENT");
//                cartesian.yAxis(0).title("FREQUENCY");
//
//                anyChartView.setChart(cartesian);

            }
        });

    }

    private void setThreadForApiCalling(String user) {
        anyChartView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        anyChartView.setProgressBar(progressBar);

        final String urlAdress = "https://fathomless-oasis-99930.herokuapp.com/rsrohanverma/return_json";


        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //JSONObject data = new JSONObject();


                    URL url = new URL(urlAdress);
                    Log.d(TAG, "run: " + urlAdress);
                    final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    //conn.connect();

//                    conn.setRequestProperty("Content-Type", "application/json");
//                    conn.setRequestProperty("Accept", "application/json");
//                    conn.setDoOutput(true);
//                    conn.setDoInput(true);




                    final Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    Log.d(TAG, "run: "+in);
                    StringBuilder sb = new StringBuilder();
                    for (int c; (c = in.read()) >= 0; )
                        sb.append((char) c);
                    final String response = sb.toString();

                    //Log.d(TAG, "run: STATUS" + String.valueOf(conn.getResponseCode()));
                    Log.d("MSG123", response);

                    Cartesian cartesian = AnyChart.column();

                    final JSONObject jsonObject = new JSONObject(response);

                    List<DataEntry> graphData = new ArrayList<>();
                    JSONArray array = jsonObject.getJSONArray("items");
                    JSONArray array2 = jsonObject.getJSONArray("freq");

                    for (int i = 0; i < array.length(); i++) {
                        graphData.add(new ValueDataEntry(array.getString(i), Integer.parseInt(array2.getString(i))));
                    }
                    Column column = cartesian.column(graphData);
                    column.tooltip()
                            .titleFormat("{%X}")
                            .position(Position.CENTER_BOTTOM)
                            .anchor(Anchor.CENTER_BOTTOM)
                            .offsetX(0d)
                            .offsetY(5d)
                            .format("number of intakes: {%Value}{groupsSeparator: }");

                    cartesian.animation(true);
                    cartesian.title("INGREDIENTS CONSUMED BY YOU DURING THIS ALLERGY PERIOD.");

                    cartesian.yScale().minimum(0d);

                    cartesian.yAxis(0).labels().format("{%Value}{groupsSeparator: }");

                    cartesian.tooltip().positionMode(TooltipPositionMode.POINT);
                    cartesian.interactivity().hoverMode(HoverMode.BY_X);

                    cartesian.xAxis(0).title("INGREDIENT");
                    cartesian.yAxis(0).title("FREQUENCY");

                    Cartesian cartesian1 = AnyChart.column();
                    cartesian1 = cartesian;
                    final Cartesian finalCartesian = cartesian1;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            anyChartView.setChart(finalCartesian);

                        }
                    });
                    Log.d(TAG, "run: " + jsonObject);

                    conn.disconnect();
                } catch (final Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "run: "+e);

                }


            }
        });

        thread.start();

    }


    @Override
    public void onError(AIError error) {

    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {

    }

    @Override
    public void onListeningCanceled() {

    }

    @Override
    public void onListeningFinished() {

    }

    public void ImageViewAnimatedChange(Context c, final ImageView v, final Drawable new_image) {
        final Animation anim_out = AnimationUtils.loadAnimation(c, R.anim.zoom_out);
        final Animation anim_in = AnimationUtils.loadAnimation(c, R.anim.zoom_in);
        anim_out.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                v.setImageDrawable(new_image);
                anim_in.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }
                });
                v.startAnimation(anim_in);
            }
        });
        v.startAnimation(anim_out);
    }

    @Override
    public void onResult(ai.api.model.AIResponse response) {


        Result result = response.getResult();

        String message = result.getResolvedQuery();
        ChatMessage chatMessage0 = new ChatMessage(message, "user");
        ref.child("chat").push().setValue(chatMessage0);


        String reply = result.getFulfillment().getSpeech();
        ChatMessage chatMessage = new ChatMessage(reply, "bot");
        ref.child("chat").push().setValue(chatMessage);


    }

    @Override
    public void onBackPressed() {
        try {
            anyChartView.setVisibility(View.GONE);
        } catch (Exception e) {
        }
    }

}


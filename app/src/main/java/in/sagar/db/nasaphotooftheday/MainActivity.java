package in.sagar.db.nasaphotooftheday;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private String BASE_URL = "https://api.nasa.gov/planetary/apod?api_key=DEMO_KEY&date=", explanation, selected_date, media_type, hdurl;
    private TextView mTitleTV, mDescriptionTV;
    private ImageView mDatePickerIV;
    private FloatingActionButton mPlayFAB;
    private ImageView mBackground;
    private SwipeRefreshLayout mRefreshLayout;
    private ProgressDialog dialog;
    private boolean isClicked = false;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setTitle(Html.fromHtml("<font color='#FFFFFF'>NASA Photo of the Day</font>"));

        getSystemTheme(); // Changing StatusBar color based on System Theme

        // Initializing Views
        mBackground = findViewById(R.id.activity_main_image);
        mRefreshLayout = findViewById(R.id.activity_main_swipe_to_refresh);
        mTitleTV = findViewById(R.id.activity_main_title);
        mDescriptionTV = findViewById(R.id.activity_main_description);
        mDatePickerIV = findViewById(R.id.activity_main_date_picker);
        mPlayFAB = findViewById(R.id.activity_main_floatingActionButton);


        String today_date = getDate(null);
        getNASA(today_date);

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getNASA(selected_date);
            }
        });

        mDescriptionTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isClicked){
                    mDescriptionTV.setText(explanation);
                    isClicked = false;
                }else {
                    String upToNCharacters = explanation.substring(0, Math.min(explanation.length(), 300));
                    mDescriptionTV.setText(upToNCharacters+".....");
                    isClicked = true;
                }
            }
        });

        mDatePickerIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getSelectedDate();
            }
        });

        mPlayFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, MediaViewActivity.class);
                intent.putExtra("media_type", media_type);
                intent.putExtra("url", hdurl);
                startActivity(intent);
            }
        });

    }

   /* ------- Calender View ----------- */
    private void getSelectedDate() {

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        int date = calendar.get(Calendar.DATE);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);


        final DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, dayOfMonth);
                String date = getDate(calendar);
                getNASA(date);
            }
        }, year, month, date);
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
        datePickerDialog.setTitle("SELECT DATE");

    }

    private String getDate(Calendar calendar) {
        if (calendar == null) {
            Calendar today_calendar = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            selected_date = dateFormat.format(today_calendar.getTime());
            return selected_date;
        }
        SimpleDateFormat simple_format = new SimpleDateFormat("yyyy-MM-dd");
        selected_date = simple_format.format(calendar.getTime()).toLowerCase().trim();
        return selected_date;

    }

    /* -------- NASA APOD ----------- */
    private void getNASA(final String date) {
        dialog = new ProgressDialog(MainActivity.this);
        dialog.setMessage("Please wait...");
        dialog.setIcon(R.mipmap.ic_launcher);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        StringRequest stringRequest = new StringRequest(Request.Method.GET, BASE_URL + date, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject object = new JSONObject(response);
                    String title = object.getString("title");
                    explanation = object.getString("explanation");
                    hdurl = object.getString("hdurl");      //Sample video for testing: "https://www.radiantmediaplayer.com/media/big-buck-bunny-360p.mp4";
                    media_type = object.getString("media_type");

                    if (!title.isEmpty()) {
                        mTitleTV.setText(title);
                    } else {
                        mTitleTV.setText("");
                    }

                    if (!explanation.isEmpty()) {
                        String upToNCharacters = explanation.substring(0, Math.min(explanation.length(), 300));
                        mDescriptionTV.setText(upToNCharacters+".....");
                        isClicked = true;
                    } else {
                        mDescriptionTV.setText("");
                    }

                    if (!media_type.isEmpty()) {
                        if (media_type.equals("video")) {
                            mPlayFAB.setImageDrawable(getDrawable(R.drawable.ic_twotone_play_arrow_24));
                            try {
                                Bitmap bitmap = getVideoFrameFromVideo(hdurl);
                                if (bitmap != null) {
                                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 40, out);
                                    mBackground.setImageBitmap(bitmap);
                                    dialog.hide();
                                    dialog.dismiss();
                                    mRefreshLayout.setRefreshing(false);
                                    mDatePickerIV.setVisibility(View.VISIBLE);
                                }
                            } catch (Throwable throwable) {
                                throwable.printStackTrace();
                            }
                        } else {
                            mPlayFAB.setImageDrawable(getDrawable(R.drawable.ic_twotone_zoom_in_24));
                            new ConvertURLImageToBitmap(mBackground).execute(hdurl);
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    mRefreshLayout.setRefreshing(false);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                dialog.dismiss();
                dialog.hide();
                mRefreshLayout.setRefreshing(false);
            }
        });

        VolleySingleton.getInstance(MainActivity.this).addToRequestQueue(stringRequest);
    }

    /* --------- Converting Image from URL to Bitmap ---------*/
    public class ConvertURLImageToBitmap extends AsyncTask<String, Void, Bitmap> {
        ImageView imageView;
        Bitmap bitmap;

        public ConvertURLImageToBitmap(ImageView img) {
            this.imageView = img;
        }

        @Override
        protected Bitmap doInBackground(String... url) {
            String stringUrl = url[0];
            bitmap = null;
            InputStream inputStream;
            try {
                inputStream = new java.net.URL(stringUrl).openStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                bitmap = BitmapFactory.decodeStream(inputStream);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 40, out);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            imageView.setImageBitmap(bitmap);
            mDatePickerIV.setVisibility(View.VISIBLE);
            dialog.dismiss();
            dialog.hide();
            mRefreshLayout.setRefreshing(false);
        }
    }

    /* ------- Getting Video Thumbnail ---------*/
    public static Bitmap getVideoFrameFromVideo(String videoPath) throws Throwable {
        Bitmap bitmap = null;
        MediaMetadataRetriever mediaMetadataRetriever = null;
        try {
            mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(videoPath, new HashMap<String, String>());
            bitmap = mediaMetadataRetriever.getFrameAtTime();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Throwable("Exception in retrieve VideoFrameFromVideo(String videoPath)" + e.getMessage());

        } finally {
            if (mediaMetadataRetriever != null) {
                mediaMetadataRetriever.release();
            }
        }
        return bitmap;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void getSystemTheme() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        switch (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
            case Configuration.UI_MODE_NIGHT_YES:
                window.setStatusBarColor(getColor(R.color.black));
                break;
            case Configuration.UI_MODE_NIGHT_NO:
                window.setStatusBarColor(getColor(R.color.colorPrimary));
                break;
        }
    }
}
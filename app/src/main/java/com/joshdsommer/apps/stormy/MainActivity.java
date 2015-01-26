package com.joshdsommer.apps.stormy;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends ActionBarActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    @InjectView(R.id.timeLabel)
    TextView mTimeLabel;
    @InjectView(R.id.temperatureLabel)
    TextView mTemperatureLabel;
    @InjectView(R.id.humidityValue)
    TextView mHumidityValue;
    @InjectView(R.id.precipValue)
    TextView mPrecipValue;
    @InjectView(R.id.summaryLabel)
    TextView mSummaryLabel;
    @InjectView(R.id.iconImageView)
    ImageView mIconImageView;
    @InjectView(R.id.refreshImageView)
    ImageView mRefreshImageView;
    @InjectView(R.id.progressBar)
    ProgressBar mProgressBar;
    @InjectView(R.id.locationLabel)
    TextView mLocationLabel;
    @InjectView(R.id.layout)
    RelativeLayout mRelativeLayout;


    private CurrentWeather mCurrentWeather;
    private String mCurrentLocation = "-";
    private double longitude = -81.3784349;
    private double latitude = 40.8428898;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stormy);

        ButterKnife.inject(this);

        mProgressBar.setVisibility(View.INVISIBLE);


        updateForecast();
        mRefreshImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateForecast();
            }
        });


    }
private LocationListener localListern = new LocationListener() {
    @Override
    public void onLocationChanged(Location location) {
        longitude = location.getLongitude();
        latitude = location.getLatitude();
        getForecast(latitude, longitude);
        getLocation(latitude, longitude);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
};

    private void updateForecast() {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                longitude = location.getLongitude();
                latitude = location.getLatitude();

            }

            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10,localListern);

            getForecast(latitude, longitude);
            getLocation(latitude, longitude);
            Log.d(TAG, "Main UI code.");
        } catch (Exception e) {
            Log.d(TAG, "WTF : ", e);
        }
    }

    private void getLocation(double latitude, double longitude) {
        String apiGoogleMapsURL = "http://maps.googleapis.com/maps/api/geocode/json?latlng=" + latitude + "," + longitude + "&sensor=false";

        if (isNetworkAvailable()) {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(apiGoogleMapsURL)
                    .build();

            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                        }
                    });
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    try {

                        String jsonData = response.body().string();
                        if (response.isSuccessful()) {
                            try {
                                JSONObject location = new JSONObject(jsonData);
                                JSONArray locInfo = location.getJSONArray("results")
                                        .getJSONObject(0)
                                        .getJSONArray("address_components");
                                mCurrentLocation = "Not Available";
                                if (locInfo.length() >= 3) {
                                    mCurrentLocation = locInfo
                                            .getJSONObject(2)
                                            .getString("long_name");
                                } else if (locInfo != null) {
                                    mCurrentLocation = locInfo
                                            .getJSONObject(1)
                                            .getString("long_name");
                                }


                                runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        mLocationLabel.setText(mCurrentLocation);
                                    }
                                });
                            } catch (JSONException lc) {
                                Log.d(TAG, "UGH! ", lc);
                            }

                        } else {
                            alertUserAboutError();
                        }
                    } catch (IOException e) {
                        Log.d(TAG, "Exception Caught : ", e);
                    }
                }
            });
        }
    }

    private void getForecast(double latitude, double longitude) {
        String apiKey = getString(R.string.api_key);

        String forecastUrl = getString(R.string.api_uri) + apiKey +
                "/" + latitude + "," + longitude;
        if (isNetworkAvailable()) {
            toggleRefresh();

            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(forecastUrl)
                    .build();

            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    try {

                        String jsonData = response.body().string();
                        if (response.isSuccessful()) {
                            mCurrentWeather = getCurrentDetails(jsonData);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplay();
                                }
                            });
                        } else {
                            alertUserAboutError();
                        }
                    } catch (IOException e) {
                        Log.d(TAG, "Exception Caught : ", e);
                    } catch (JSONException e) {
                        Log.d(TAG, "Exception Caught : ", e);
                    }
                }
            });
        }
    }

    private void toggleRefresh() {
        if (mProgressBar.getVisibility() == View.VISIBLE) {
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefreshImageView.setVisibility(View.VISIBLE);
        } else {
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshImageView.setVisibility(View.INVISIBLE);
        }

        int d = -1040175;
        Log.d(TAG, d + "");
    }

    private void updateDisplay() {
        mTemperatureLabel.setText(mCurrentWeather.getTemp() + "");
        mTimeLabel.setText("At " + mCurrentWeather.getFormattedTime() + " it will be");
        mHumidityValue.setText(mCurrentWeather.getHumidity() + "");
        mPrecipValue.setText(mCurrentWeather.getPrecipChance() + "%");
        mSummaryLabel.setText(mCurrentWeather.getSummary());

        Drawable drawable = getResources().getDrawable(mCurrentWeather.getIconId());

        mRelativeLayout.setBackgroundColor(Color.parseColor(mCurrentWeather.getBgColor()));
        mIconImageView.setImageDrawable(drawable);
    }

    private CurrentWeather getCurrentDetails(String jsonData) throws JSONException {
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");
        JSONObject currently = forecast.getJSONObject("currently");
        CurrentWeather currentWeather = new CurrentWeather();

        currentWeather.setHumidity(currently.getDouble("humidity"));
        currentWeather.setIcon(currently.getString("icon"));
        currentWeather.setPrecipChance(currently.getDouble("precipProbability"));
        currentWeather.setSummary(currently.getString("summary"));
        currentWeather.setTemp(currently.getDouble("temperature"));
        currentWeather.setTime(currently.getLong("time"));
        currentWeather.setTimeZone(timezone);

        Log.d(TAG, "current weather " + currentWeather.getSummary());
        return currentWeather;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();

        if (info != null && info.isConnected()) {
            return true;
        } else {
            Toast.makeText(this, getString(R.string.error_network_unavalable), Toast.LENGTH_LONG).show();
            return false;
        }
    }

   /* private void fadeToColor(String fromColor, String tooColor){
        ObjectAnimator colorFade = ObjectAnimator.ofObject(mRelativeLayout, "backgroundColor", new ArgbEvaluator(), Color.parseColor(fromColor), 0xff000000);
        colorFade.setDuration(7000);
        colorFade.start();
    }*/

    private void alertUserAboutError() {
        AlertDialogFragment alert = new AlertDialogFragment();
        alert.show(getFragmentManager(), "error_dialog");
    }

}

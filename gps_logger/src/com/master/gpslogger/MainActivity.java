package com.master.gpslogger;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import commaster.gpslogger.R;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final long MINIMUM_DISTANCE_CHANGE_FOR_UPDATES = 1; // in
																		// Meters
	private static final long MINIMUM_TIME_BETWEEN_UPDATES = 1000; // in
																	// Milliseconds
	private Future longRunningTaskFurure;
	private Future ajaxPOSTTask;
	private ExecutorService threadAjaxPOST;
	private Runnable runnable;

	
	
	private MyLocationListener myLocationListener;
	
	private Runnable ajaxPOSTRunnable;

	private String response;
	private String returnString;

	final Handler handler = new Handler();
	protected LocationManager locationManager;
	protected TextView textView;
	protected Button retrieveLocationButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		
		myLocationListener = new MyLocationListener();
		retrieveLocationButton = (Button) findViewById(R.id.button1);

		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		textView = (TextView) findViewById(R.id.textView1);
		textView.setMovementMethod(new ScrollingMovementMethod());

		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				MINIMUM_TIME_BETWEEN_UPDATES,
				MINIMUM_DISTANCE_CHANGE_FOR_UPDATES, myLocationListener);

		runnable = new Runnable() {

			private boolean killed = false;

			@Override
			public void run() {

				if (!killed) {
					try {
						showCurrentLocation();
					} catch (Exception e) {
						// TODO: handle exception
					} finally {
						handler.postDelayed(runnable, 5000);
					}
				}
			}

			public void kill() {
				killed = true;
			}
		};
		// handler.postDelayed(runnable, 1000);

		ajaxPOSTRunnable = new Runnable() {
			public void run() {
				try {
					submitData();
				} catch (Exception e) {
					// TODO: handle exception
				} finally {

				}
			}
		};

		threadAjaxPOST = Executors.newSingleThreadExecutor();

		ExecutorService threadPoolExecutor = Executors
				.newSingleThreadExecutor();

		// submit task to thread pool:
		longRunningTaskFurure = threadPoolExecutor.submit(runnable);

		// At some point in the future, if you want to kill the task:

		retrieveLocationButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ajaxPOSTTask = threadAjaxPOST.submit(ajaxPOSTRunnable);
			}
		});

	}

	@Override
	public void onBackPressed() {
		longRunningTaskFurure.cancel(true);
		    locationManager.removeUpdates(myLocationListener);
		finish();
	}

	protected void showCurrentLocation() {

		Location location = locationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		if (location != null) {
			String message = String.format("Lng: %1$s Lat: %2$s\n",
					location.getLongitude(), location.getLatitude());
			textView.append(message);

			final int scrollAmount = textView.getLayout().getLineTop(
					textView.getLineCount())
					- textView.getHeight();
			if (scrollAmount > 0) {
				textView.scrollTo(0, scrollAmount);
			}

		}

	}

	private class MyLocationListener implements LocationListener {

		public void onLocationChanged(Location location) {
			String message = String.format(
					"New Location \n Longitude: %1$s \n Latitude: %2$s",
					location.getLongitude(), location.getLatitude());
			
			submitLocation(location.getLatitude(), location.getLongitude());
			
			Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT)
					.show();
		}

		public void onStatusChanged(String s, int i, Bundle b) {
			Toast.makeText(MainActivity.this, "Provider status changed",
					Toast.LENGTH_LONG).show();
		}

		public void onProviderDisabled(String s) {
			Toast.makeText(MainActivity.this,
					"Provider disabled by the user. GPS turned off",
					Toast.LENGTH_LONG).show();
		}

		public void onProviderEnabled(String s) {
			Toast.makeText(MainActivity.this,
					"Provider enabled by the user. GPS turned on",
					Toast.LENGTH_LONG).show();
		}

	}

	private void submitLocation(double lat, double lng) {

		// declare parameters that are passed to PHP script i.e. the name
		// "birthyear" and its value submitted by user
		ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();


		// define the parameter
		postParameters.add(new BasicNameValuePair("lat", Double.toString(lat)));
		postParameters.add(new BasicNameValuePair("lng", Double.toString(lng)));
		
		String response = null;

		// call executeHttpPost method passing necessary parameters
		try {
			response = CustomHttpClient.executeHttpPost(
					"http://androidtests.ueuo.com/jsonscript_put.php", // your
																		// ip
																		// address
																		// if
																		// using
																		// localhost
																		// server
					// "http://omega.uta.edu/~kmr2464/jsonscript.php", // in
					// case of a remote server
					postParameters);

			// store the result returned by PHP script that runs MySQL query
			String result = response.toString();

			// parse json data
			try {
				returnString = "";
					JSONObject json_data = new JSONObject(result);
					Log.i("success", "status" + json_data.getBoolean("success"));
					returnString += "\n" + json_data.getBoolean("success");
			}
			catch (JSONException e) {
				Log.e("log_tag", "Error parsing data " + e.toString());
			}

			try {
				
				updateText(returnString);
				//textView.setText(returnString);
			} catch (Exception e) {
				Log.e("log_tag", "Error in Display!" + e.toString());
				;
			}
		} 
		catch (Exception e) {
			Log.e("log_tag", "Error in http connection!!" + e.toString());
		}

	}
	
	
	private void submitData() {

		// declare parameters that are passed to PHP script i.e. the name
		// "birthyear" and its value submitted by user
		ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();

		// define the parameter
		postParameters.add(new BasicNameValuePair("birthyear", "100"));

		String response = null;

		// call executeHttpPost method passing necessary parameters
		try {
			response = CustomHttpClient.executeHttpPost(
					"http://androidtests.ueuo.com/jsonscript.php", // your
																		// ip
																		// address
																		// if
																		// using
																		// localhost
																		// server
					// "http://omega.uta.edu/~kmr2464/jsonscript.php", // in
					// case of a remote server
					postParameters);

			// store the result returned by PHP script that runs MySQL query
			String result = response.toString();

			// parse json data
			try {
				returnString = "";
				JSONArray jArray = new JSONArray(result);
				for (int i = 0; i < jArray.length(); i++) {
					JSONObject json_data = jArray.getJSONObject(i);
					Log.i("log_tag", "id: " + json_data.getInt("id")
							+ ", name: " + json_data.getString("name")
							+ ", sex: " + json_data.getInt("sex")
							+ ", birthyear: " + json_data.getInt("birthyear"));
					// Get an output to the screen
					returnString += "\n" + json_data.getString("name") + " -> "
							+ json_data.getInt("birthyear");
				}
			} catch (JSONException e) {
				Log.e("log_tag", "Error parsing data " + e.toString());
			}

			try {
				
				updateText(returnString);
				//textView.setText(returnString);
			} catch (Exception e) {
				Log.e("log_tag", "Error in Display!" + e.toString());
				;
			}
		} 
		catch (Exception e) {
			Log.e("log_tag", "Error in http connection!!" + e.toString());
		}

	}
	
	private void updateText(String message){
		final String text = message;
		textView.post(new Runnable(){
		@Override
		public void run(){
			textView.append(text);
		}
		}
		);
	}

}
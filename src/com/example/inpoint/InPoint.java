package com.example.inpoint;

import java.util.HashMap;
import java.util.List;

import java.io.*;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HTTP;

public class InPoint extends Activity implements OnClickListener {
	private static final String TAG = "WifiScanner";
	// public static final String userID =
	// android.provider.Settings.Secure.ANDROID_ID;
	WifiManager wifi;
	// BroadcastReceiver receiver;

	TextView textStatus;
	Button buttonScan;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// for android version later than 2.3, enable processing httpclient in main thread
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
				.detectDiskReads().detectDiskWrites().detectNetwork()
				.penaltyLog().build());
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
				.detectLeakedSqlLiteObjects().penaltyLog().penaltyDeath()
				.build());
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Setup UI
		textStatus = (TextView) findViewById(R.id.textStatus);
		buttonScan = (Button) findViewById(R.id.buttonScan);
		buttonScan.setOnClickListener(this);

		// Setup WiFi
		wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		textStatus.setText("");
		textStatus.append("Press Scan to InPoint yourself!\n");
		Log.d(TAG, "onCreate()");
	}

	@Override
	public void onPause() {
		// unregisterReceiver(receiver);
		unregisterForContextMenu(textStatus);
		unregisterForContextMenu(buttonScan);
		super.onPause();
	}

	@Override
	public void onStop() {
		// unregisterReceiver(receiver);
		unregisterForContextMenu(textStatus);
		unregisterForContextMenu(buttonScan);
		super.onStop();
	}

	@Override
	public void onDestroy() {
		unregisterForContextMenu(textStatus);
		unregisterForContextMenu(buttonScan);
		super.onDestroy();
	}

	public void onClick(View view) {
		if (!wifi.isWifiEnabled()) {
			Toast.makeText(this, "WiFi is not open on this device",
					Toast.LENGTH_LONG).show();
		} else {
			textStatus = (TextView) findViewById(R.id.textStatus);
			textStatus.setText("");
			Toast.makeText(this, "Start Scan now!!", Toast.LENGTH_LONG).show();
			if (view.getId() == R.id.buttonScan) {
				Log.d(TAG, "onClick() wifi.startScan()");

			}
			wifi.startScan();
			try {
				Thread.sleep(700);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			ArrayList<List<ScanResult>> ScanList = new ArrayList<List<ScanResult>>(
					5);
			for (int scancount = 0; scancount < 5; scancount++) {
				wifi.startScan();
				try {
					Thread.sleep(700);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				List<ScanResult> results = this.wifi.getScanResults();
				ScanList.add(results);
				ScanResult bestSignal = null;
				for (ScanResult result : results) {
					if (bestSignal == null
							|| WifiManager.compareSignalLevel(bestSignal.level,
									result.level) < 0)
						bestSignal = result;
				}

//				for (int i = 0; i < results.size(); i++) {
//					String message1 = String
//							.format("AP num: %d\nessid: %s \nMAC: %s\nfrequency: %s\nSig: %d",
//									i + 1, results.get(i).SSID,
//									results.get(i).BSSID,
//									results.get(i).frequency,
//									results.get(i).level);
//					textStatus.append(message1 + '\n');
//				}
			}

			HashMap<String, Double> map_sig = new HashMap<String, Double>();
			HashMap<String, Double> map_num = new HashMap<String, Double>();
			// TODO Compare 5 scan results and Calculate an average value
			// List<ScanResult> average;
			for (int i = 0; i < 5; i++) {
				List<ScanResult> res = ScanList.get(i);
				for (int j = 0; j < res.size(); j++) {
					if (!map_sig.containsKey(res.get(j).BSSID)) {
						map_sig.put(res.get(j).BSSID,
								Double.valueOf(res.get(j).level));
						map_num.put(res.get(j).BSSID, Double.valueOf(1));
					} else {
						map_sig.put(res.get(j).BSSID,
								(Double.valueOf(map_sig.get(res.get(j).BSSID))
										.doubleValue() + res.get(j).level));
						map_num.put(res.get(j).BSSID,
								(Double.valueOf(map_num.get(res.get(j).BSSID))
										.doubleValue() + 1));
					}
				}
			}
			HashMap<String, Double> map_avg = new HashMap<String, Double>();
			for (String key : map_sig.keySet()) {
				if (Double.valueOf(map_num.get(key)) >= 3)
					map_avg.put(
							key,
							Double.valueOf(map_sig.get(key).doubleValue()
									/ map_num.get(key).doubleValue()));
				// textStatus.append(key + ":" + map_avg.get(key).doubleValue()
				// +
				// " "
				// + map_num.get(key).doubleValue() + "\n");
			}

			// create a xml formatted string
			String xml;
			String header = "<?xml version='1.0'?>\n";
			String session = "<session>\n <number>" + map_avg.size()
					+ "</number>\n";
			String content = " <content>\n";
			for (String key : map_avg.keySet()) {
				content += "  <";
				content += "item";
				content += ">\n";
				content += "   <MAC>";
				content += key;
				content += "</MAC>\n";
				content += "   <SIG>";
				content += map_avg.get(key);
				content += "</SIG>\n";
				content += "  </";
				content += "item";
				content += ">\n";
			}
			content += " </content>\n</session>\n";
			xml = header + session + content;
			textStatus.append("The XML below to be sent to server:\n\n");
			textStatus.append(xml);

			/* get IMEI */
			// TelephonyManager telephonyManager =
			// (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
			// textStatus.append(telephonyManager.getDeviceId());

			/* get Android ID */
			// String Id = Settings.Secure.getString(getContentResolver(),
			// Settings.Secure.ANDROID_ID);
			// textStatus.append(Id);

			ConnectivityManager mag = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo info = mag.getActiveNetworkInfo();
			if (info == null || !info.isConnected())
				Toast.makeText(this,
						"Warning: No Internet connection, please check...",
						Toast.LENGTH_LONG).show();
			else {
				try {
					HttpClient httpclient = new DefaultHttpClient();
					HttpPost httppost = new HttpPost(
							"http://inpoint.pdp.fi/wlan/wlan.php");

					// send xml through http post
					StringEntity se = new StringEntity(xml, HTTP.UTF_8);
					se.setContentType("text/xml");
					httppost.setHeader("Content-Type",
							"application/soap+xml;charset=UTF-8");
					httppost.setEntity(se);

					BasicHttpResponse httpResponse = (BasicHttpResponse) httpclient
							.execute(httppost);

					// read echo from server
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(httpResponse.getEntity()
									.getContent(), "UTF-8"));
					String json = reader.readLine();
					textStatus.append("\nResponse from server:\n\n");
					textStatus.append(json);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
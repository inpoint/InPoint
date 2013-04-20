package com.example.inpoint;

import java.util.HashMap;
import java.util.List;

import java.io.*;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

public class InPoint extends Activity {
	private static final String TAG = "WifiScanner";
	// public static final String userID =
	// android.provider.Settings.Secure.ANDROID_ID;
	public static WifiManager wifi;

	public static final int MENU_EXITAPPLICATION = Menu.FIRST;

	public static Handler mainHandler;
	public static TextView textStatus;
	public static ImageView imageView;
	
	private static ImageButton zoomInButton;
	private static ImageButton zoomOutButton;
	private DisplayMetrics dm;
	
	
	public static boolean should_scan;

	private Bitmap mapMark;
	private Bitmap mapCopy;
	private Bitmap mapTemp;

	public static final float mapActualX_Max = 100; // represent max coordinate
													// x, length
	public static final float mapActualY_Max = 40; // represent max coordinate
													// y, width
	private float serverReturn_x = 0; // server return calculated coordinate x,
	private float serverReturn_y = 0; // server return calculated coordinate y,

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);

		// Setup UI
		textStatus = (TextView) findViewById(R.id.textStatus);
		imageView = (ImageView) this.findViewById(R.id.imageView3);
		zoomInButton = (ImageButton)findViewById(R.id.zoomInButton);
        zoomOutButton = (ImageButton)findViewById(R.id.zoomOutButton);  

		// Setup WiFi
		wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		textStatus.setText("");

		// Setup imageView
		should_scan = true;

		// ImageView map = (ImageView)this.findViewById(R.id.imageView3);

		if (!wifi.isWifiEnabled()) {
			Toast.makeText(
					this,
					"WiFi is not open on this device, Please enable it and restart the app!",
					Toast.LENGTH_LONG).show();
		} else {
			Thread scan_thread = new Thread() {
				@Override
				public void run() {
					while (should_scan) {
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
							List<ScanResult> results = wifi.getScanResults();
							ScanList.add(results);
						}

						HashMap<String, Double> map_sig = new HashMap<String, Double>();
						HashMap<String, Double> map_num = new HashMap<String, Double>();
						// TODO Compare 5 scan results and Calculate an
						// average value
						// List<ScanResult> average;
						for (int i = 0; i < 5; i++) {
							List<ScanResult> res = ScanList.get(i);
							for (int j = 0; j < res.size(); j++) {
								if (!map_sig.containsKey(res.get(j).BSSID)) {
									map_sig.put(res.get(j).BSSID,
											Double.valueOf(res.get(j).level));
									map_num.put(res.get(j).BSSID,
											Double.valueOf(1));
								} else {
									map_sig.put(
											res.get(j).BSSID,
											(Double.valueOf(
													map_sig.get(res.get(j).BSSID))
													.doubleValue() + res.get(j).level));
									map_num.put(
											res.get(j).BSSID,
											(Double.valueOf(
													map_num.get(res.get(j).BSSID))
													.doubleValue() + 1));
								}
							}
						}
						HashMap<String, Double> map_avg = new HashMap<String, Double>();
						for (String key : map_sig.keySet()) {
							if (Double.valueOf(map_num.get(key)) >= 3)
								map_avg.put(key, Double.valueOf(map_sig
										.get(key).doubleValue()
										/ map_num.get(key).doubleValue()));
							// textStatus.append(key + ":" +
							// map_avg.get(key).doubleValue()
							// +
							// " "
							// + map_num.get(key).doubleValue() + "\n");
						}
						
						//obtain device mac address
						
						String address= wifi.getConnectionInfo().getMacAddress();

						// create a xml formatted string
						String xml;
						String header = "<?xml version='1.0'?>\n";
						String session = "<session>\n <number>"
								+ map_avg.size() + "</number>\n";
						String deviceMac = " <own_mac>" + address
								+ "</own_mac>\n";
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
						xml = header + session + deviceMac + content;

						// send msg to main thread to update UI
						// Message msg = mainHandler.obtainMessage();
						// msg.obj = xml;
						// mainHandler.sendMessage(msg);

						ConnectivityManager mag = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
						NetworkInfo info = mag.getActiveNetworkInfo();
						if (info == null || !info.isConnected()) {
							// send msg to main thread to update UI
							Message msg1 = mainHandler.obtainMessage();
							msg1.obj = "Error, no internet connection!\n";
							mainHandler.sendMessage(msg1);
						} else {
							try {
								HttpClient httpclient = new DefaultHttpClient();
								HttpPost httppost = new HttpPost(
										"http://inpoint.pdp.fi/wlan/wlan_relative.php");

								// send xml through http post
								StringEntity se = new StringEntity(xml,
										HTTP.UTF_8);
								se.setContentType("text/xml");
								httppost.setHeader("Content-Type",
										"application/soap+xml;charset=UTF-8");
								httppost.setEntity(se);

								BasicHttpResponse httpResponse = (BasicHttpResponse) httpclient
										.execute(httppost);

								// read echo from server
								BufferedReader reader = new BufferedReader(
										new InputStreamReader(httpResponse
												.getEntity().getContent(),
												"UTF-8"));
								String json = reader.readLine();
								// textStatus
								// .append("\nResponse from server:\n\n");
								// textStatus.append(json);

								// send msg to main thread to update UI
								Message msg1 = mainHandler.obtainMessage();
								msg1.obj = "Response from server:  " + json
										+ "\n";
								mainHandler.sendMessage(msg1);

								/*
								 * read server returned coordinates for Map UI
								 * display Here the String "TestString" is only
								 * for function Test, In actual use, please
								 * replace "TestString" below to "json"
								 */
								String TestString = "10_0,BrainStorm";
							    //readPositionFromServer(TestString);
								readPositionFromServer(json);

								// send msg to main thread to update UI
								Message msg_update_UI = mainHandler
										.obtainMessage();
								msg_update_UI.obj = "update UI!\n";
								mainHandler.sendMessage(msg_update_UI);

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
			};
			scan_thread.start();

			mainHandler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					if (msg.obj.toString() != "update UI!\n")
						textStatus.append(msg.obj.toString());
					else {
						// add mark to the map
						mapMark = BitmapFactory.decodeResource(getResources(),
								R.drawable.map_mark);
						mapCopy = BitmapFactory.decodeResource(getResources(),
								R.drawable.df_map_v2);
						// add the mark coordinates here, (x,y)
		// Version-1: Here using the calculated relative-coordinates, used to displace actual position	
		/*				mapTemp = addMark(mapCopy, mapMark, serverReturn_x
								/ mapActualX_Max, serverReturn_y
								/ mapActualY_Max);
		*/
						
		// Version-2: Here using the preset absolute coordinates, used to displace room-level-accuracy position	
						mapTemp = addMark_roomLevel(mapCopy, mapMark, (int)serverReturn_x);
						imageView.setImageBitmap(mapTemp);
						//modify the picture display according to the User Finger Gesture
						new ImageViewHelper(dm, imageView, mapTemp, zoomInButton, zoomOutButton); 
					}
				}
			};
		}
//		new ImageViewHelper(dm, imageView, mapTemp, zoomInButton, zoomOutButton);
		Log.d(TAG, "onCreate()");
	}

	@Override
	public void onResume() {
		should_scan = true;
		super.onResume();
	}

	@Override
	protected void onStart() {
		int flag = getIntent().getIntExtra("flag", 0);
		if (flag == SysUtil.EXIT_APPLICATION) {
			finish();
		}
		super.onResume();

	}

	@Override
	public void onPause() {
		unregisterForContextMenu(textStatus);
		unregisterForContextMenu(imageView);
		super.onPause();
	}

	@Override
	public void onStop() {
		unregisterForContextMenu(textStatus);
		unregisterForContextMenu(imageView);
		super.onStop();
	}

	@Override
	public void onDestroy() {
		should_scan = false;
		unregisterForContextMenu(textStatus);
		unregisterForContextMenu(imageView);
		super.onDestroy();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		int flag = getIntent().getIntExtra("flag", 0);
		if (flag == SysUtil.EXIT_APPLICATION) {
			finish();
		}
		super.onNewIntent(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_EXITAPPLICATION, 0, "Exit InPoint App");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == MENU_EXITAPPLICATION) {
			// exit app
			SysUtil mSysUtil = new SysUtil(InPoint.this);
			mSysUtil.exit();

		}
		return super.onOptionsItemSelected(item);
	}
	
	
	

	public void readPositionFromServer(String src) {
		String temp[] = src.split(",|\\_|\\ ");
		// textStatus.append("\nRead Result_x: ");
		// textStatus.append(temp[0]);
		// textStatus.append("  _y: ");
		// textStatus.append(temp[1]);

		try {
			serverReturn_x = Float.parseFloat(temp[0]);
		} catch (NumberFormatException e) {
			e.toString();
		}
		try {
			serverReturn_y = Float.parseFloat(temp[1]);
		} catch (NumberFormatException e) {
			e.toString();
		}
		// textStatus.append("\nTrans Result_x: ");
		// textStatus.append(Float.toString(serverReturn_x));
		// textStatus.append("  _y: ");
		// textStatus.append(Float.toString(serverReturn_y));

		// for debug
		// Message msg1 = mainHandler.obtainMessage();
		// msg1.obj = "Read Result_x: " + temp[0] + "  _y: " + temp[1]
		// + "\nTrans Result_x: " + Float.toString(serverReturn_x)
		// + "  _y: " + Float.toString(serverReturn_y) + "\n";
		// mainHandler.sendMessage(msg1);

	}

// Version-1: Here using the calculated relative-coordinates, used to displace actual position		
	public Bitmap addMark(Bitmap src, Bitmap mark, float relativePosX,
			float relativePosY) {
		// create a new figure, same size as original figure src
		Bitmap newb = Bitmap.createBitmap(src.getWidth(), src.getHeight(),
				Config.ARGB_8888);//
		// textStatus.append("\nmap_Width: ");
		// textStatus.append(Integer.toString(src.getWidth()));
		// textStatus.append("  _Height: ");
		// textStatus.append(Integer.toString(src.getHeight()));

		Canvas canvas = new Canvas(newb);
		canvas.drawBitmap(src, 0, 0, null);// insert original figure at
											// coordinate-(0,0)

		// insert mark onto the figure
		// canvas.drawBitmap(mark, (src.getWidth() - mark.getWidth()) / 2,
		// (src.getHeight() - mark.getHeight()) / 2, null);

	
		int posx = (int) (relativePosX * src.getWidth());
	    int posy = (int) (relativePosY * src.getHeight());	
		
		// textStatus.append("\nmapped_x: ");
		// textStatus.append(Integer.toString(posx));
		// textStatus.append("  _y: ");
		// textStatus.append(Integer.toString(posy));
		canvas.drawBitmap(mark, posx, posy, null);
		canvas.save(Canvas.ALL_SAVE_FLAG);
		canvas.restore();

		mark.recycle();
		mark = null;

		return newb;
	}
	
// Version-2: Here using the preset absolued coordinates, used to displace room-level-accuracy position	
	public Bitmap addMark_roomLevel(Bitmap src, Bitmap mark, int roomNum) {
		// create a new figure, same size as original figure src
		float posx = 0; 
		float posy = 0;
		
		float map_zoomed_x = src.getWidth()/2000;
		float map_zoomed_y = src.getHeight()/781;
		
		Bitmap newb = Bitmap.createBitmap(src.getWidth(), src.getHeight(),
				Config.ARGB_8888);//
		 textStatus.append("map_Width: ");
		 textStatus.append(Integer.toString(src.getWidth()));
		 textStatus.append("  _Height: ");
		 textStatus.append(Integer.toString(src.getHeight()));

		Canvas canvas = new Canvas(newb);
		canvas.drawBitmap(src, 0, 0, null);// insert original figure at
											// coordinate-(0,0)

		// insert mark onto the figure
		// canvas.drawBitmap(mark, (src.getWidth() - mark.getWidth()) / 2,
		// (src.getHeight() - mark.getHeight()) / 2, null);

		switch (roomNum){
			case 1:
				posx = 190;
				posy = 343;
				break;
			case 2:
				posx = 190;
				posy = 513;
				break;
			case 3:
				posx = 380;
				posy = 377;
				break;
			case 4:
				posx = 570;
				posy = 390;
				break;
			case 5:
				posx = 483;
				posy = 595;
				break;
			case 6:
				posx = 775;
				posy = 583;
				break;
			case 7:
				posx = 1083;
				posy = 571;
				break;
			case 8:
				posx = 1283;
				posy = 565;
				break;
			case 9:
				posx = 907;
				posy = 429;
				break;
			case 10:
				posx = 1280;
				posy = 350;
				break;				
			case 11:
				posx = 1370;
				posy = 350;
				break;
			case 12:
				posx = 1523;
				posy = 523;
				break;
			case 13:
				posx = 1523;
				posy = 369;
				break;
			case 14:
				posx = 1830;
				posy = 535;
				break;	
			default:
				posx = 15;
				posy = 15;
		}
		
		 int posx_rev = (int)((posx-15)*map_zoomed_x);
		 int posy_rev = (int)((posy-15)*map_zoomed_y);
		 textStatus.append("\nmapped_x: ");
		 textStatus.append(Integer.toString(posx_rev));
		 textStatus.append("  _y: ");
		 textStatus.append(Integer.toString(posy_rev));
		 textStatus.append("\n");
		canvas.drawBitmap(mark, posx_rev, posy_rev, null);
		canvas.save(Canvas.ALL_SAVE_FLAG);
		canvas.restore();

		mark.recycle();
		mark = null;

		return newb;
	}

}
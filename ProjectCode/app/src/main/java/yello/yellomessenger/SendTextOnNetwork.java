package yello.yellomessenger;


import android.app.ActivityManager;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by e on 11/24/2015.
 *
 * Extends AsyncTask because network operations can't be executed on the
 * main thread. doInBackground is executed on
 * SendTextOnNetwork(Context).execute(JSONObject);
 *
 * TODO: Fix isOnNetwork method to also check stuff in the commented link.
 * TODO: Test that the send on network functionality actually works...
 */
public class SendTextOnNetwork extends AsyncTask<JSONObject, Void, Void> {
	private String forwardIP;
	private Integer forwardPort;
	private String netName;
	private Context context;

	public SendTextOnNetwork(Context context) {
		this.context = context;
	}

	protected Void doInBackground(JSONObject... textMessages) {
		if (isOnNetwork()) {
			for (int i = 0; i < textMessages.length; i++ ){
				doTheForwardThing(textMessages[i]);
			}
		}
		return null;
	}

	private boolean isOnNetwork () {
		// https://developer.android.com/training/basics/network-ops/connecting.html
		return messengerServiceRunning();
	}

	private boolean messengerServiceRunning() {
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (MessengerService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private void doTheDBThing(String netName) {
		/* Database Connections */
		NetworkDBHelper mDBHelper = new NetworkDBHelper(context.getApplicationContext());
		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		/* The Columns to grab from the DB */
		String[] projection = {
				NetworkDB.NetworkDBEntries.COL_NAME_SSID,
				NetworkDB.NetworkDBEntries.COL_NAME_FOR_IP,
				NetworkDB.NetworkDBEntries.COL_NAME_FOR_PORT
		};

		/* The WHERE clause for SQLite */
		String selection = NetworkDB.NetworkDBEntries.COL_NAME_SSID+"=?";
		//Log.v("selection", selection);

		/* Replaces the "?" in the WHERE with variables in this array, respectively */
		String[] selectionArgs = new String[] {
				getNetName()
		};

		/* Build our cursor */
		Cursor c = db.query(
				NetworkDB.NetworkDBEntries.TABLE_NAME,
				projection,
				selection,
				selectionArgs,
				null,
				null,
				null
		);

		/* Replaces the text of the EditText's with the values from the DB */
		if (c.moveToFirst()) {
			String hahaIP = c.getString(c.getColumnIndex(NetworkDB.NetworkDBEntries.COL_NAME_FOR_IP));
			Integer hahaPort = c.getInt(c.getColumnIndex(NetworkDB.NetworkDBEntries.COL_NAME_FOR_PORT));
			//Log.v("IP", hahaIP);
			//Log.v("Port", hahaPort);
			setForwardIP(hahaIP);
			setForwardPort(hahaPort);
		}

		/* We can now close the DB */
		c.close();
		db.close();
		mDBHelper.close();

		//String[] results = {forwardIP, forwardPort};
		//return results;
	}

	private void doTheForwardThing(JSONObject textMessage) {
		/* Get the current wifi network */
		WifiManager wifimanager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		setNetName(wifimanager.getConnectionInfo().getSSID());

		doTheDBThing(getNetName());
		//forwardIP = stuff[0];
		//forwardPort = stuff[1];

		//Log.v("Net Stuff", getNetName() + getForwardIP() + String.valueOf(getForwardPort()) + textMessage.toString());
		doTheNetworkThing(textMessage);
	}

	private void doTheNetworkThing(JSONObject textMessage) {
		try {
			Socket toComputerSocket = new Socket(getForwardIP(), getForwardPort());
			PrintWriter outMessage = new PrintWriter(toComputerSocket.getOutputStream(), true);
			outMessage.println(textMessage.toString());
			outMessage.close();
			toComputerSocket.close();
		} catch (UnknownHostException e) {
			Log.e("Unknown Host", e.toString());
		} catch(IOException e) {
			Log.e("doTheForwardThing", e.toString());
		}
	}

	private void setNetName(String netName) {
		this.netName = netName.replaceAll("^\"|\"$", "");
	}

	private void setForwardIP(String forwardIP) {
		this.forwardIP = forwardIP;
	}

	private void setForwardPort(Integer forwardPort) {
		this.forwardPort = forwardPort;
	}

	private String getNetName() {
		return this.netName;
	}

	private String getForwardIP() {
		return this.forwardIP;
	}

	private Integer getForwardPort() {
		return this.forwardPort;
	}
}

package yello.yellomessenger;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by e on 10/15/2015.
 * TODO: Fix Spacing/Font Sizing/Padding on Current Network TextView
 */
public class MyActivity extends AppCompatActivity {
	public final static String EXTRA_MESSAGE = "com.yello.yellomessenger.MESSAGE";
	public boolean serviceRunning;
	public String currentNetwork;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

		setCurNet();
		populateNetworks();
		buildAddButton();

		Switch messageServiceSwitch = (Switch) findViewById(R.id.background_service_running);
		checkMessengerService(messageServiceSwitch);
		messageServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				toggleMessageService(isChecked);
			}
		});

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

	/* This causes the app to repopulate the network list after
	   the AddNetwork Activity finishes
	 */
	@Override
	public void onResume() {
		populateNetworks();
		setCurNet();
		checkMessengerService((Switch) findViewById(R.id.background_service_running));
		super.onResume();
	}

	/* This function populates the home screen with the network names in the DB */
	private void populateNetworks() {
		/* Gets current networks via populateNetworkList method and stores them in networks */
		final ArrayList<String> networks = populateNetworkList();

		/* Gets the ID of the the networkListView ListView object */
		ListView networkList = (ListView)findViewById(R.id.networkListView);

		/* Instances an ArrayAdapter to convert network names into something that
		   can be generated into a clickable list
		 */
		ArrayAdapter<String> networkAdapter =
				new ArrayAdapter<String>(this, android.R.layout.simple_list_item_activated_1, networks);

		/* Build the clickable list */
		networkList.setAdapter(networkAdapter);

		networkList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String networkName = networks.get(position);
				Intent intent = new Intent(MyActivity.this, AddNetwork.class);
				intent.putExtra("network_name", networkName);
				startActivity(intent);
				//Log.v("Network Name:", networkName);
			}
		});
	}

	/* This method retrieves the network names that currently are configured from the DB */
	private ArrayList<String> populateNetworkList() {
		/* Connect to the DB */
		NetworkDBHelper mDBHelper = new NetworkDBHelper(getApplicationContext());
		SQLiteDatabase DB = mDBHelper.getReadableDatabase();

		String[] projection = {
				NetworkDB.NetworkDBEntries.COL_NAME_SSID
		};

		Cursor c = DB.query(
				NetworkDB.NetworkDBEntries.TABLE_NAME,
				projection,
				null,
				null,
				null,
				null,
				null
		);

		/* Generate the ArrayList */
		ArrayList<String> networks = new ArrayList<String>();
		while (c.moveToNext()) {
			String networkName = c.getString(c.getColumnIndex(NetworkDB.NetworkDBEntries.COL_NAME_SSID));
			networks.add(networkName);
		}

		c.close();
		DB.close();
		mDBHelper.close();

		/* Return an ArrayList of Network names */
		return networks;
	}

	/* This method builds the FloatingActionButton used to add new networks */
	private void buildAddButton () {
		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(MyActivity.this, AddNetwork.class);
				startActivity(intent);
			}
		});
	}

	private void toggleMessageService (boolean isChecked) {
		Intent messageService = new Intent(this, MessengerService.class);
		if (isChecked) {
			startService(messageService);
		} else {
			stopService(messageService);
		}
	}

	private void setCurNet() {
		TextView currentNetworkBar = (TextView) findViewById(R.id.net_current);
		currentNetworkBar.setText(getCurNet());
	}

	public String getCurNet() {
		WifiManager wifimanager = (WifiManager) getSystemService(WIFI_SERVICE);
		return wifimanager.getConnectionInfo().getSSID().replaceAll("^\"|\"$", "");
	}

	public boolean isMessengerServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (MessengerService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private void checkMessengerService(Switch messageServiceSwitch) {
		if (isMessengerServiceRunning()) {
			messageServiceSwitch.setChecked(true);
		} else {
			messageServiceSwitch.setChecked(false);
		}
	}
}

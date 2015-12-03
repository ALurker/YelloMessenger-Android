package yello.yellomessenger;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;


/**
 * Created by e on 11/17/2015.
 * TODO: Make the default network name on the new network page be the current network name.
 */
public class AddNetwork extends AppCompatActivity {
	String existing_net_name;
	Boolean bool_modify = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_network);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null && extras.containsKey("network_name")) {
			bool_modify = (!("".equals(extras.getString("network_name"))));
		}


		if (bool_modify) {
			existing_net_name = extras.getString("network_name");
			getSupportActionBar().setTitle("Modify Network");
			//toolbar.setTitle("Modify Network");
			modifyNetwork();
		}
	}

	/* method is called when we are modifying an existing network instead of adding a new one */
	private void modifyNetwork() {
		replaceEditTextBoxes();

		/* If we are modifying, then change the add button to say "Save" instead of "Add" */
		Button addNetButton = (Button)findViewById(R.id.add_network_button);
		addNetButton.setText(getString(R.string.modify_add_button));

		/* The current RelativeLayout schema. We will need to add a "delete" button to it */
		RelativeLayout relLayout = (RelativeLayout)findViewById(R.id.addNetworkLayout);

		Button deleteButton = new Button(this);
		deleteButton.setText(getString(R.string.del_button));
		deleteButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				deleteNetwork();
			}
		});

		/* Create the Layout rules for the button */
		RelativeLayout.LayoutParams deleteLayoutParam = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT
		);

		/* Make the button appear below the forward_port EditText */
		deleteLayoutParam.addRule(RelativeLayout.BELOW, R.id.forward_port);

		/* Apply the Layout Rules defined above */
		deleteButton.setLayoutParams(deleteLayoutParam);

		/* Actually add the button to the existing Layout */
		relLayout.addView(deleteButton);
	}

	/* Replaces the EditText fields with the existing values in the case of modification */
	private void replaceEditTextBoxes(){
		EditText EditNetName = (EditText)findViewById(R.id.network_name);
		EditText EditIPAddress = (EditText)findViewById(R.id.forward_ip);
		EditText EditPort = (EditText)findViewById(R.id.forward_port);

		/* Database Connections */
		NetworkDBHelper mDBHelper = new NetworkDBHelper(getApplicationContext());
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
				existing_net_name
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
			EditNetName.setText(existing_net_name);
			EditIPAddress.setText(c.getString(c.getColumnIndex(NetworkDB.NetworkDBEntries.COL_NAME_FOR_IP)));
			EditPort.setText(
					String.valueOf(c.getInt(c.getColumnIndex(NetworkDB.NetworkDBEntries.COL_NAME_FOR_PORT)))
			);
		}

		/* We can now close the DB */
		c.close();
		db.close();
		mDBHelper.close();
	}

	/* This method is executed when the Add/Save button is pressed */
	public void saveNetwork(View view) {
		/* Get the Network Name, IP Address, and Port from the AddNetwork View */
		EditText EditNetName = (EditText)findViewById(R.id.network_name);
		EditText EditIPAddress = (EditText)findViewById(R.id.forward_ip);
		EditText EditPort = (EditText)findViewById(R.id.forward_port);

		String NetName;
		String IPAddress;
		Integer Port;

		/* If they typed something, get what they typed. Else get the hinted value */
		if (EditNetName.getText().toString().equals("")) {
			NetName = EditNetName.getHint().toString();
		} else {
			NetName = EditNetName.getText().toString();
		}

		if (EditIPAddress.getText().toString().equals("")) {
			IPAddress = EditIPAddress.getHint().toString();
		} else {
			IPAddress = EditIPAddress.getText().toString();
		}

		if (EditPort.getText().toString().equals("")) {
			Port = Integer.parseInt(EditPort.getHint().toString());
		} else {
			Port = Integer.parseInt(EditPort.getText().toString());
		}

		/* Database Connections */
		NetworkDBHelper mDBHelper = new NetworkDBHelper(getApplicationContext());
		SQLiteDatabase db = mDBHelper.getWritableDatabase();

		/* Time to build our insert/update statement */
		ContentValues values = new ContentValues();
		values.put(NetworkDB.NetworkDBEntries.COL_NAME_SSID, NetName);
		values.put(NetworkDB.NetworkDBEntries.COL_NAME_FOR_IP, IPAddress);
		values.put(NetworkDB.NetworkDBEntries.COL_NAME_FOR_PORT, Port);

		/* If we are not modifying, then this should update */
		if (!bool_modify) {
			/* Time to run the insert statement. The Primary key of inserted record
			   is returned by db.insert
			 */
				long insertID = db.insert(
						NetworkDB.NetworkDBEntries.TABLE_NAME,
						"null",
						values);
		} else {
			/* Update the DB */
			String selection = NetworkDB.NetworkDBEntries.COL_NAME_SSID + "=?";
			String[] selectionArgs = { NetName };

			int updateID = db.update(
					NetworkDB.NetworkDBEntries.TABLE_NAME,
					values,
					selection,
					selectionArgs
			);
		}

		db.close();
		mDBHelper.close();

		/* This returns us to the parent activity */
		finish();
	}

	/* This is called when the Delete button is pressed */
	private void deleteNetwork() {
		/* Get the Network Name, IP Address, and Port from the AddNetwork View */
		EditText EditNetName = (EditText)findViewById(R.id.network_name);

		String NetName;

		/* If they typed something, get what they typed. Else get the hinted value */
		if (EditNetName.getText().toString().equals("")) {
			NetName = EditNetName.getHint().toString();
		} else {
			NetName = EditNetName.getText().toString();
		}

		/* Database Connections */
		NetworkDBHelper mDBHelper = new NetworkDBHelper(getApplicationContext());
		SQLiteDatabase db = mDBHelper.getWritableDatabase();


		String selection = NetworkDB.NetworkDBEntries.COL_NAME_SSID + "=?";
		String[] selectionArgs = { NetName };

		db.delete(
				NetworkDB.NetworkDBEntries.TABLE_NAME,
				selection,
				selectionArgs
		);

		db.close();
		mDBHelper.close();

		finish();
	}
}

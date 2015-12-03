package yello.yellomessenger;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;
import android.os.Process;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by e on 11/24/2015.
 * TODO: Remove Extra methods in this class.
 * TODO: Add functionality to send out SMS messages.
 */
public class MessengerService extends Service {
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
	private static final Integer SERVERPORT = 1337;
	private ServerSocket yelloSocket;
	private Handler yelloHandler = new Handler();
	private Thread yelloThread;
	private final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(action.equals("android.provider.Telephony.SMS_RECEIVED")) {
				for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
					String messageBody = smsMessage.getDisplayMessageBody();   // The contents of the message.
					String sender = smsMessage.getDisplayOriginatingAddress(); // The phone number of the sender.

					try {
						JSONObject comeOn = new JSONObject();
						comeOn.put("phone_number", sender);
						comeOn.put("message", messageBody);
						comeOn.put("sha256_number", calcSha256(sender));
						comeOn.put("sha256_message", calcSha256(messageBody));

						SendTextOnNetwork message = new SendTextOnNetwork(context);
						message.execute(comeOn);
					} catch (org.json.JSONException|NoSuchAlgorithmException|UnsupportedEncodingException e) {
						Log.e("MessengerService", e.toString());
					}
				}
			}
		}
	};

	/**
	 * Private class inside of MessengerService.class. This is the service that handles,
	 * runs, and controls the ServerSocket that allows the phone app to send text messages.
	 *
	 * This is by far the biggest hack-together of the project. On the plus side, it works.
	 */
	private class YelloServer implements Runnable {
		/**
		 * This is the method run upon the instancing of any
		 * class implementing the Runnable class.
		 *
		 * This will set up a listener on port SERVERPORT,
		 * check the received JSON data for validity, and then
		 * send out that message to the phone number specified in the port
		 *
		 * TODO: Check for existence of JSON keys before checking SHA sums.
		 * TODO: Refactor this code into a much more readable series of methods.
		 */
		public void run() {
			try {
				JSONObject clientJSON;
				String phoneNumber;
				String phoneSha;
				String message;
				String messageSha;
				Boolean phoneShaEqual;
				Boolean messageShaEqual;
				SmsManager smsManager = SmsManager.getDefault();

				yelloHandler.post(new Runnable() {
					@Override
					public void run() {
						Log.v("Server ", "Running");
					}
				});
				yelloSocket = new ServerSocket(SERVERPORT);

				while (true) {
					Socket clientSocket = yelloSocket.accept();
					BufferedReader clientInput = new BufferedReader(
							new InputStreamReader(clientSocket.getInputStream())
					);
					String clientData = null;
					while ((clientData = clientInput.readLine()) != null) {
						try {
							//Log.v("YelloServerData", clientData);
							clientJSON = new JSONObject(clientData);

							phoneNumber = clientJSON.getString("phone_number");
							Log.v("Phone", phoneNumber);

							message = clientJSON.getString("message");
							Log.v("Message", message);

							phoneSha = clientJSON.getString("sha256_number");
							phoneShaEqual = calcSha256(phoneNumber).equals(phoneSha);
							Log.v("Phone_SHA", phoneShaEqual.toString());

							messageSha = clientJSON.getString("sha256_message");
							messageShaEqual = calcSha256(message).equals(messageSha);
							Log.v("Message_SHA", messageShaEqual.toString());

							if (phoneShaEqual && messageShaEqual) {
								smsManager.sendTextMessage(phoneNumber, null, message, null, null);
							} else {
								Log.e("SHA Sums don't match", "");
							}

						} catch (NoSuchAlgorithmException|UnsupportedEncodingException e) {
							Log.e("YelloServer", e.toString());
						}
					}
				}
			} catch (SocketException e) {
				Log.v("Server ", "Closing");
			} catch (IOException|JSONException e) {
				Log.e("YelloServer", e.toString());
			}
		}
	}

	/**
	 * This method is called upon instantiation of the MessengerService.class.
	 * As such, this gets loaded when MyActivity.class is loaded, as MessengerService.class
	 * is used by the R.id.background_service_running switch
	 */
	@Override
	public void onCreate() {
		// Start up the thread running the service.  Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block.  We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

		// Get the HandlerThread's Looper and use it for our Handler
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
		registerReceiver(receiver, intentFilter);
	}

	/**
	 * Handler that would in most circumstances receive message from the work thread.
	 * In our case however, we are not using a work thread.
	 * As such, this method actually does nothing.
	 *
	 * TODO: check to see if we can safely remove this method.
	 */
	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}
		@Override
		public void handleMessage(Message msg) {
		}
	}

	/**
	 * This method is run upon checking the Background Service Switch in MyActivity.class
	 * @param intent	An Intent
	 * @param flags		Some Flags
	 * @param startId	An Identifier
	 * @return			A return value
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Toast.makeText(this, "Service Starting", Toast.LENGTH_SHORT).show();

		// For each start request, send a message to start a job and deliver the
		// start ID so we know which request we're stopping when we finish the job
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		mServiceHandler.sendMessage(msg);

		yelloThread = new Thread(new YelloServer());
		yelloThread.start();

		// If we get killed, after returning from here, restart
		return START_STICKY;
	}

	/**
	 * We do not provide binding, so this method is moot for us.
	 * @param intent The intent to bind to.
	 * @return IBinder
	 */
	@Override
	public IBinder onBind(Intent intent) {
		// We don't provide binding, so return null
		return null;
	}

	/**
	 * This function is called on the closing of the MessengerService. This occurs
	 * when the BackgroundService Switch defined in MyActivity.class is changed from
	 * checked to not-checked.
	 */
	@Override
	public void onDestroy() {
		try {
			unregisterReceiver(receiver);
			Toast.makeText(this, "Service Done", Toast.LENGTH_SHORT).show();

			//closing a serverSocket while waiting for a clientSocket causes
			//a java.net.SocketException. As catch this stops execution,
			//we must have closing the socket be the final thing that we do.
			yelloSocket.close();
		} catch (SocketException e) {
		} catch (IOException e) {
			Log.e("onDestroy", e.toString());
		}
	}

	/**
	 * Returns the SHA256 hash of the input string. This code was published at
	 * http://stackoverflow.com/questions/9661008/compute-sha256-hash-in-android-java-and-c-sharp
	 * and was able to be essentially copy-and-pasted in.
	 * @param  input						The String to calculate the SHA256 of.
	 * @return      						The calculated SHA256
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	private String calcSha256(String input) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		digest.reset();

		byte[] byteData = digest.digest(input.getBytes("UTF-8"));
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < byteData.length; i++){
			sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
		}
		return sb.toString();
	}
}
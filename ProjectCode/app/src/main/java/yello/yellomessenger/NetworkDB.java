package yello.yellomessenger;

import android.provider.BaseColumns;

/**
 * Created by e on 11/17/2015.
 */
public class NetworkDB {
    public NetworkDB() {}

    public static abstract class NetworkDBEntries implements BaseColumns {
        public static final String TABLE_NAME = "NETWORK_YELLO";
        public static final String COL_NAME_SSID = "NETWORK_SSID";
        public static final String COL_NAME_FOR_IP = "NETWORK_FOR_IP";
        public static final String COL_NAME_FOR_PORT = "NETWORK_FOR_PORT";

        private static final String TEXT_TYPE = " TEXT";
		private static final String INT_TYPE = " INTEGER";
		private static final String PRI_KEY = " PRIMARY KEY";
		private static final String UNIQ = " UNIQUE";
		private static final String COMMA = ",";

		public static final String SQL_CREATE_ENTRIES =
			"CREATE TABLE " + NetworkDBEntries.TABLE_NAME + " (" +
			NetworkDBEntries._ID + INT_TYPE + PRI_KEY + COMMA +
			NetworkDBEntries.COL_NAME_SSID + TEXT_TYPE + UNIQ + COMMA +
			NetworkDBEntries.COL_NAME_FOR_IP + TEXT_TYPE + COMMA +
			NetworkDBEntries.COL_NAME_FOR_PORT + INT_TYPE + ")";

		public static final String SQL_DELETE_ENTRIES =
			"DROP TABLE IF EXISTS " + NetworkDBEntries.TABLE_NAME;
    }
}
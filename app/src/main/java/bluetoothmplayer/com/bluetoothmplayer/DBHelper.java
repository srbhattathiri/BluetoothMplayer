package bluetoothmplayer.com.bluetoothmplayer; /**
 * Created by Sethu on 10/30/2015.
 */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    public SQLiteDatabase DB;
    public String DBPath;
    public static String DBName = "sample1";
    public static final int version = '1';
    public static Context currentContext;
    public static String tableName = "SongList12";
    public static String relaytableName = "RelayDetails";

    public DBHelper(Context context) {
        super(context, DBName, null, version);
        currentContext = context;
        DBPath = "/data/data/" + context.getPackageName() + "/databases";
        createDatabase();

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
// TODO Auto-generated method stub

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
// TODO Auto-generated method stub

    }

    private void createDatabase() {
        // boolean dbExists = checkDbExists();


        DB = currentContext.openOrCreateDatabase(DBName, 0, null);
        DB.execSQL("CREATE TABLE IF NOT EXISTS " +
                tableName +
                " (SongName VARCHAR,Owner VARCHAR,MacId VARCHAR);");
        DB.execSQL("INSERT INTO " +
                tableName +
                " Values ('M','A','64:77:91:AB:D9:7E');");
        DB.execSQL("INSERT INTO " +
                tableName +
                " Values ('N','B','D8:31:CF:F9:5F:CA');");
        DB.execSQL("INSERT INTO " +
                tableName +
                " Values ('O','C','64:SJ:JS');");
        DB.execSQL("CREATE TABLE IF NOT EXISTS " +
                relaytableName +
                " (destination VARCHAR,relay VARCHAR);");
        DB.execSQL("INSERT INTO " +
                relaytableName +
                " Values ('64:77:91:AB:D9:7E','64:77:91:AB:D9:7E');");
        DB.execSQL("INSERT INTO " +
                relaytableName +
                " Values ('D8:31:CF:F9:5F:CA','D8:31:CF:F9:5F:CAAAAAAAAAAAAAA');");
        DB.execSQL("INSERT INTO " +
                relaytableName +
                " Values ('64:SJ:JS','D8:31:CF:F9:5F:CA');");
    }

    private boolean checkDbExists() {
        SQLiteDatabase checkDB = null;

        try {
            String myPath = DBPath + DBName;
            checkDB = SQLiteDatabase.openDatabase(myPath, null,
                    SQLiteDatabase.OPEN_READONLY);

        } catch (SQLiteException e) {

// database does't exist yet.

        }

        if (checkDB != null) {

            checkDB.close();

        }

        return checkDB != null ? true : false;
    }
}

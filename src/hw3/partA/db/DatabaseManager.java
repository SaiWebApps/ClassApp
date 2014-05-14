package hw3.partA.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.*;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

/**
 * Manage access to SQLite database and the Class & Student tables.
 * @author Sairam Krishnan (sbkrishn)
 */
public class DatabaseManager {

	private SQLiteDatabase database;
	private Context context;
	private final String DB_NAME = "QuizStats";
	private final String CLASS_TABLE = "Class";
	private final String STUDENTS_TABLE = "Students";
	private final int VERSION = 1;

	/**
	 * Initializes this database manager with the given context.
	 * @param context - Interface to global info about an application environment
	 */
	public DatabaseManager(Context context) {
		this.context = context;
	}

	/**
	 * Open connection to database.
	 */
	public void open() {
		DatabaseOpenHelper h = new DatabaseOpenHelper(context, DB_NAME, null, VERSION);
		database = h.getWritableDatabase();
	}

	/**
	 * Close connection to database.
	 */
	public void close() {
		if (database != null) {
			database.close();
		}
	}

	/**
	 * @param classId - Id of target class that we are retrieving name for
	 * @return the name of the class with the given class id; class ids
	 * are the primary key of the class table, so they should be unique.
	 */
	public String getClassName(int classId) {
		Cursor c = null;

		try {
			c = database.rawQuery("SELECT className FROM " + CLASS_TABLE +
					" WHERE classId=" + classId, null);
			c.moveToFirst();
			return c.getString(c.getColumnIndex("className"));
		} catch(Exception e) {
			return null;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	/**
	 * @param classId - Id of class that we are getting number of students for
	 * @return the number of students in the specified class
	 */
	public int getNumStudents(int classId) {
		Cursor c = null;

		try {
			c = database.rawQuery("SELECT COUNT(*) FROM " + STUDENTS_TABLE
					+ " WHERE classId=" + classId, null);
			c.moveToFirst();
			return c.getInt(c.getColumnIndex("COUNT(*)"));
		} catch (Exception e) {
			return 0;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	/**
	 * @return a list of Strings that describe created classes, null if none
	 */
	public List<String> getAllClasses() {
		Cursor c = null;
		List<String> allClasses = null;

		try {
			c = database.rawQuery("SELECT * FROM " + CLASS_TABLE, null);
			allClasses = new ArrayList<String>();

			while (c.moveToNext()) {
				int classId = c.getInt(c.getColumnIndex("classId"));
				String className = c.getString(c.getColumnIndex("className"));
				int numStudents = getNumStudents(classId);
				allClasses.add(classId + "\t\t" + className + "\t\t" + numStudents + " students");
			}

			return allClasses;
		} catch (Exception e) {
			return null;
		} finally {
			c.close();
		}
	}

	/**
	 * @param classId - Id of target class
	 * @param studentId - Id of target student
	 * @return true if the student w/ given id and class exists
	 */
	public boolean existsStudent(int classId, String studentId) {
		Cursor c = null;
		try {
			c = database.rawQuery("SELECT * FROM " + STUDENTS_TABLE +
					" WHERE classId=" + classId + " AND studentId=" + studentId, null);
			return (c.getCount() == 1); //If num rows == 1, student already exists.
		} catch (Exception e) {
			return false;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	/**
	 * Create a new class with the given id and name.
	 * @param className - Name of the new class
	 * @param classId - Id of new class
	 */
	public boolean createClass(int classId, String className) {
		//If a class already exists with given id, then don't recreate it.
		if (getClassName(classId) != null) {
			return false;
		}

		//Otherwise, create a new entry in the class table.
		ContentValues vals = new ContentValues();
		vals.put("classId", classId);
		vals.put("className", className);
		database.insert(CLASS_TABLE, null, vals);
		return true;
	}

	/**
	 * @param classId - Class that we are adding student to
	 * @param studentId - Id of student
	 * @param scores - 5 quiz scores for student
	 * @return true if student was successfully created, false otherwise
	 */
	public boolean createStudent(int classId, String studentId, List<Float> scores) {
		//Don't recreate student if she already exists. Also, don't create the
		//student if the user has not specified exactly 5 quiz scores.
		if (getClassName(classId) == null || existsStudent(classId, studentId)
				|| scores == null || scores.size() != 5) {
			return false;
		}

		//Otherwise, create student with the given id in the given class.
		ContentValues vals = new ContentValues();
		vals.put("studentId", studentId);
		vals.put("classId", classId);
		for (int i = 1; i<=scores.size(); i++) {
			vals.put("Q"+i, scores.get(i-1));
		}
		database.insert(STUDENTS_TABLE, null, vals);
		return true;
	}

	/**
	 * @param classId - Id of class that we are listing students for
	 * @return list of students in specified class
	 */
	public Map<String,List<Float>> getAllStudents(int classId) {
		Map<String, List<Float>> studentMap = new TreeMap<String,List<Float>>();
		Cursor c = null;
		try {
			c = database.rawQuery("SELECT studentId, Q1, Q2, Q3, Q4, Q5 FROM " + STUDENTS_TABLE +
					" WHERE classId=" + classId, null);
			while (c.moveToNext()) {
				String studentId = c.getString(c.getColumnIndex("studentId"));
				List<Float> scores = new ArrayList<Float>();
				for (int i = 1; i<=5; i++) {
					float s = c.getFloat(c.getColumnIndex("Q" + i));
					scores.add(s);
				}
				studentMap.put(studentId, scores);
			}
			return studentMap;
		} catch (Exception e) {
			return null;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	/**
	 * @param classId - Id of class we are fetching stats for
	 * @return the high, low, and average scores on the 5 quizzes for given class
	 */
	public Map<String, List<Float>> getStats(int classId) {
		HashMap<String,List<Float>> stats = new HashMap<String,List<Float>>();
		List<Float> highScores = new ArrayList<Float>();
		List<Float> lowScores = new ArrayList<Float>();
		List<Float> avgScores = new ArrayList<Float>();

		for (int i = 1; i<=5; i++) {
			Cursor max = database.rawQuery("SELECT MAX(Q"+ i + ") FROM " + 
					STUDENTS_TABLE + " WHERE classId=" + classId, null);
			Cursor min = database.rawQuery("SELECT MIN(Q" + i + ") FROM " + 
					STUDENTS_TABLE + " WHERE classId=" + classId, null);
			Cursor avg = database.rawQuery("SELECT AVG(Q" + i + ") FROM " + 
					STUDENTS_TABLE + " WHERE classId=" + classId, null);

			max.moveToFirst();
			min.moveToFirst();
			avg.moveToFirst();
			highScores.add(max.getFloat(0));
			lowScores.add(min.getFloat(0));
			avgScores.add(avg.getFloat(0));
			max.close();
			min.close();
			avg.close();
		}

		stats.put("max", highScores);
		stats.put("min", lowScores);
		stats.put("avg", avgScores);
		return stats;
	}

	/**
	 * Inner class that creates/upgrades and opens a connection to a database.
	 * @author Sairam Krishnan
	 */
	private class DatabaseOpenHelper extends SQLiteOpenHelper {

		public DatabaseOpenHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		/**
		 * Create a table to store all of the classes created thus far.
		 * Schema: class id, class name
		 * @param db - Android's SQLite database client
		 */
		private void createClassTable(SQLiteDatabase db) {
			StringBuffer buf = new StringBuffer("CREATE TABLE " + CLASS_TABLE + " (");
			buf.append("classId INTEGER PRIMARY KEY,");
			buf.append("className TEXT NOT NULL)");
			db.execSQL(buf.toString());
		}

		/**
		 * Create a table to store all of the students created so far.
		 * Schema: record id, student id, class id, q1-q5
		 * @param db - Android's SQLite database client
		 */
		private void createStudentsTable(SQLiteDatabase db) {
			StringBuffer buf = new StringBuffer("CREATE TABLE " + STUDENTS_TABLE + " (");
			buf.append("id INTEGER PRIMARY KEY AUTOINCREMENT,");
			buf.append("studentId TEXT NOT NULL,");
			buf.append("classId INTEGER NOT NULL,");
			buf.append("Q1 FLOAT NOT NULL,");
			buf.append("Q2 FLOAT NOT NULL,");
			buf.append("Q3 FLOAT NOT NULL,");
			buf.append("Q4 FLOAT NOT NULL,");
			buf.append("Q5 FLOAT NOT NULL)");
			db.execSQL(buf.toString());
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			//Create the student and class tables.
			createClassTable(db);
			createStudentsTable(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + CLASS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + STUDENTS_TABLE);
			db.setVersion(newVersion); //Update version
			onCreate(db); //Recreate the posts table.
		}
	}
}
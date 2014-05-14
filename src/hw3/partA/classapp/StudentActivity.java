package hw3.partA.classapp;

import java.util.List;
import java.util.Map;

import hw3.partA.db.DatabaseManager;
import hw3.partA.util.Util;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import android.os.Build;

/**
 * Display all of the students in the class, along with their quiz scores.
 * @author Sairam Krishnan (sbkrishn)
 */
public class StudentActivity extends ActionBarActivity {

	//Global info (conveyed through intent from MainActivity)
	private int classId;
	private String className;
	private String header;

	//Views
	private EditText studentIdView;
	private EditText q1View;
	private EditText q2View;
	private EditText q3View;
	private EditText q4View;
	private EditText q5View;
	private Button createStudentButton;
	private Button showStatsButton;
	private TextView headerView;
	private TextView errView;
	private TableLayout studentTable;

	//Utility tools
	private Util util = new Util();
	private DatabaseManager dbm = new DatabaseManager(StudentActivity.this);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_student);
		classId = getIntent().getIntExtra("classId", 0);
		className = getIntent().getStringExtra("className");
		header = classId + ": " + className;

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
			.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	/**
	 * Initialize fields.
	 */
	private void initializeFields() {
		studentIdView = (EditText) findViewById(R.id.studentId);
		q1View = (EditText) findViewById(R.id.q1);
		q2View = (EditText) findViewById(R.id.q2);
		q3View = (EditText) findViewById(R.id.q3);
		q4View = (EditText) findViewById(R.id.q4);
		q5View = (EditText) findViewById(R.id.q5);
		createStudentButton = (Button) findViewById(R.id.createStudent);
		showStatsButton = (Button) findViewById(R.id.showStatsButton);
		studentTable = (TableLayout) findViewById(R.id.studentTable);
		errView = (TextView) findViewById(R.id.err);
		headerView = (TextView) findViewById(R.id.header);
		headerView.setText(header);
	}

	/**
	 * Clear "Create Student" form's fields.
	 */
	private void clearFields() {
		studentIdView.setText("");
		q1View.setText("");
		q2View.setText("");
		q3View.setText("");
		q4View.setText("");
		q5View.setText("");
	}
	
	/**
	 * Add newly created student's information to table.
	 * @param studentId - Id of newly created student
	 * @param scores - Newly created student's quiz scores
	 */
	private void updateTable(String studentId, List<Float> scores) {
		TableRow newRow = new TableRow(StudentActivity.this);
		TextView newSid = new TextView(StudentActivity.this);
		newSid.setText(studentId);
		newRow.addView(newSid);

		for (float s : scores) {
			TextView newScore = new TextView(StudentActivity.this);
			newScore.setText(""+s);
			newRow.addView(newScore);
		}
		studentTable.addView(newRow);
	}

	/**
	 * Display table with all of the students in the class currently.
	 * Used when we resume this Activity.
	 */
	private void showTable() {
		dbm.open();
		Map<String, List<Float>> students = dbm.getAllStudents(classId);
		dbm.close();

		for (String sid : students.keySet()) {
			List<Float> scores = students.get(sid);
			TableRow row = new TableRow(StudentActivity.this);
			TextView idView = new TextView(StudentActivity.this);

			idView.setText(sid);
			row.addView(idView);
			for (float qs : scores) {
				TextView quizScore = new TextView(StudentActivity.this);
				quizScore.setText(""+qs);
				row.addView(quizScore);
			}
			studentTable.addView(row);
		}
	}

	/**
	 * Register on click listener for "Create Student" button.
	 */
	private void registerCreateStudentListener() {
		createStudentButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				List<Float> quizScores = util.convertToFloat(q1View, q2View, q3View, 
						q4View, q5View);
				String studentId = studentIdView.getText().toString();

				if (studentId == null || studentId.trim().length() != 4 || 
						!util.areValidScores(quizScores)) {
					errView.setText("Invalid inputs - Please enter a 4-digit student id "
							+ "and floating-point scores b/w 0 and 100.");
					return;
				}
				dbm.open();
				if (dbm.createStudent(classId, studentId, quizScores)) {
					updateTable(studentId, quizScores);
				}
				dbm.close();
				clearFields();
			}
		});
	}

	/**
	 * Display alert dialog with class statistics when user clicks on
	 * "Display Stats" button.
	 */
	private void registerStatsListener() {
		showStatsButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				//Get the statistics.
				dbm.open();
				Map<String, List<Float>> getStats = dbm.getStats(classId);
				dbm.close();

				//Display stats in dialog box.
				AlertDialog.Builder adb = new AlertDialog.Builder(StudentActivity.this);
				adb.setTitle(header + " Statistics");
				adb.setMessage("High Scores: " + getStats.get("max") + 
						"\nLow Scores: " + getStats.get("min") + "\nAverage Scores: "
						+ getStats.get("avg"));
				adb.setNegativeButton("Return to Home Page",new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,int id) {
						StudentActivity.this.finish();
					}
				});
				adb.setPositiveButton("Return to Class Page", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,int id) {
						dialog.cancel();
					}
				});
				
				adb.create().show();
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		initializeFields();
		registerCreateStudentListener();
		registerStatsListener();
		showTable();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.student, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_student,
					container, false);
			return rootView;
		}
	}

}
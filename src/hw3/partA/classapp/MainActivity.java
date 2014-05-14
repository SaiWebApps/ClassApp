package hw3.partA.classapp;

import hw3.partA.db.DatabaseManager;
import hw3.partA.util.Util;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.os.Build;

/**
 * Screen that allows user to create classes and view list of created classes.
 * @author Sairam Krishnan (sbkrishn)
 */
public class MainActivity extends ActionBarActivity {

	//Views
	private EditText classIdView;
	private EditText classNameView;
	private TextView errorMessageView;
	private Button createClassButton;
	private ListView classList;

	//Utility tools
	private	DatabaseManager dbm = new DatabaseManager(MainActivity.this);
	private Util util = new Util();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}
	
	/**
	 * Initialize the views.
	 */
	private void initializeFields() {
		classIdView = (EditText) findViewById(R.id.classId);
		classNameView = (EditText) findViewById(R.id.className);
		createClassButton = (Button) findViewById(R.id.createClass);
		errorMessageView = (TextView) findViewById(R.id.errorMessage);
		classList = (ListView) findViewById(R.id.createdClasses);
	}
	
	/**
	 * Update list of classes.
	 */
	private void updateList() {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, 
				android.R.layout.simple_list_item_1, dbm.getAllClasses());
		classList.setAdapter(adapter);
	}
	
	/**
	 * Invoke database to create class with given id and name.
	 * @param classId - Id of new class
	 * @param className - Name of new class
	 */
	private void createClass(int classId, String className) {
		dbm.open();
		//Create class if possible. Display error message if not.
		if (!dbm.createClass(classId, className)) {
			errorMessageView.setText("A class already exists for the given id. "
					+ "Please enter a different id for your class.");
		}
		//Update list view.
		updateList();
		dbm.close();	
	}
	
	/**
	 * @param classId - User input for class id
	 * @param className - User input for class name
	 * @return a String w/ error messages if there were validation errors
	 */
	private String validateFields(int classId, String className) {
		StringBuffer errorMessage = new StringBuffer();
		//Look for valid number for class id.
		if (classId == -1) {
			errorMessage.append("Please enter a valid numerical "
					+ "value for class id.\n");
		}
		//Look for non-null/non-blank input for class name.
		if (className == null || className.trim().isEmpty()) {
			errorMessage.append("Please enter a class name.");
		}
		return errorMessage.toString();
	}
	
	/**
	 * Register button click listener to "Create Class" button.
	 */
	private void registerButtonClickListener() {
		createClassButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				int classId = util.convertToInt(classIdView);
				String className = classNameView.getText().toString();
				String errors = validateFields(classId, className);
				
				//Display validation errors. Otherwise, create the class.
				if (!errors.isEmpty()) {
					errorMessageView.setText(errors);
					return;
				}
				createClass(classId, className);
				
				//Clear fields.
				classIdView.setText("");
				classNameView.setText("");
			}
		});
	}
	
	/**
	 * Listener that redirects to StudentActivity when the user selects one
	 * of the list items.
	 */
	private void registerListViewListener() {
		classList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				String text = ((TextView) view).getText().toString();
				String[] tokens = text.split("\t\t");
				int classId = util.convertToInt(tokens[0]);
				String className = tokens[1].trim();
				Intent intent = new Intent(MainActivity.this, StudentActivity.class);
				
				intent.putExtra("classId", classId);
				intent.putExtra("className", className);
				startActivity(intent);
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		initializeFields();
		registerButtonClickListener();
		registerListViewListener();
		dbm.open();
		updateList();
		dbm.close();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
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
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}
	}
}
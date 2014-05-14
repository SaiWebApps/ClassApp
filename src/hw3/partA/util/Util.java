package hw3.partA.util;

import java.util.ArrayList;
import java.util.List;
import android.widget.EditText;

public class Util {

	public Util() {}

	public int convertToInt(EditText et) {
		return convertToInt(et.getText().toString().trim());
	}

	public int convertToInt(String str) {
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException e) {
			return -1;
		}
	}
	
	public List<Float> convertToFloat(EditText...etList) {
		List<Float> result = new ArrayList<Float>();
		
		for (EditText et: etList) {
			try {
				result.add(Float.parseFloat(et.getText().toString().trim()));
			} catch (NumberFormatException e) {
				result.add(Float.NaN);
			}
		}
		return result;
	}
	
	public boolean areValidScores(List<Float> scores) {
		for (float s : scores) {
			if (Float.isNaN(s) || s < 0 || s > 100) {
				return false;
			}
		}
		return true;
	}
	
	public boolean areValidScores(EditText...etList) {
		List<Float> scores = convertToFloat(etList);
		return areValidScores(scores);
	}
}
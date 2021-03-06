package eu.loopit.f2011;

import org.apache.http.HttpStatus;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import eu.loopit.f2011.util.RestException;
import eu.loopit.f2011.util.RestHelper;
import eu.loopit.f2011.util.Validator;
import eu.loopit.f2011.welcome.WelcomeActivity;

public class PolePositionActivity extends BaseActivity {
	
	private EditText minutes;
	private EditText seconds;
	private EditText thousands;
	
	private Button submit;
	
	private Validator validator;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pole_position);
		
		minutes = (EditText) findViewById(R.id.minutes);
		seconds = (EditText) findViewById(R.id.seconds);
		thousands = (EditText) findViewById(R.id.thousands);
		
		minutes.setText("");
		minutes.setNextFocusLeftId(R.id.seconds);
		seconds.setText("");
		seconds.setNextFocusLeftId(R.id.thousands);
		thousands.setText("");
		
		OnEditorActionListener listener = new ActionListener();
		minutes.setOnEditorActionListener(listener);
		seconds.setOnEditorActionListener(listener);
		thousands.setOnEditorActionListener(listener);
		
		TextWatcher watcher = new TimeTextWatcher();
		minutes.addTextChangedListener(watcher);
		seconds.addTextChangedListener(watcher);
		
		validator = new Validator(this);
		
		Button previous = (Button) findViewById(R.id.previous);
		previous.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(getPreviousIntent());
			}
		});
		submit = (Button) findViewById(R.id.submit);
		submit.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				submitBid();
			}
		});
	}
	
	private void submitBid() {
		if (submit.isEnabled() == false) return; //Do not submit twice
		boolean result = validator.validateNumber(getString(R.string.label_minutes), minutes.getText().toString());
		if (result) result = validator.validateNumber(getString(R.string.label_seconds), seconds.getText().toString());
		if (result) result = validator.validateNumber(getString(R.string.label_thousands), thousands.getText().toString());
		if (result) {
			getF2011Application().getBid().setPolePositionTime(getPolePoistionTime());
			submit.setEnabled(false);
			if (result) new SubmitTask().execute();
		}
	}
	
	private Intent getNextIntent() {
		Intent intent = new Intent(this, WelcomeActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		intent.putExtra(WelcomeActivity.FORCE_REFRESH, true);
		return intent;
	}

	private Intent getPreviousIntent() {
		Intent intent = new Intent(this, FirstCrashActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		return intent;
	}
	
	private int getPolePoistionTime() {
		int result = Integer.parseInt(thousands.getText().toString());
		result += Integer.parseInt(seconds.getText().toString()) * 1000;
		result += Integer.parseInt(minutes.getText().toString()) * 1000 * 60;
		return result;
	}
	
	private class SubmitTask extends AsyncTask<Void, Void, Void> {
		
		private RestHelper helper = new RestHelper(getF2011Application());
		private RestException exception;

		@Override
		protected Void doInBackground(Void... params) {
			try {
				helper.postJSONData(String.format("/bid"), getF2011Application().getBid(), Void.class);
			} catch (RestException e) {
				exception = e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (exception != null && exception.getStatusCode() == HttpStatus.SC_PAYMENT_REQUIRED) {
				validator.showError(PolePositionActivity.this.getString(R.string.error_not_enough_money));
			} else if (exception != null) {
				validator.showError(PolePositionActivity.this.getString(R.string.error_server, exception.getStatusCode()));
			} else {
				Toast.makeText(PolePositionActivity.this, getString(R.string.bid_submitted), Toast.LENGTH_LONG).show();
				getF2011Application().removeBid();
				startActivity(getNextIntent());
			}
		}
	}
	
	private class ActionListener implements OnEditorActionListener {

		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			
			if (actionId == EditorInfo.IME_ACTION_DONE && v == thousands) {
				submitBid();
			}
			
			if (actionId != EditorInfo.IME_ACTION_NEXT) return false;
			
			if (v == minutes) {
				seconds.requestFocus();
			} else if (v == seconds) {
				thousands.requestFocus();
			}
			return true;
		}
	}
	
	private class TimeTextWatcher implements TextWatcher {

		public void afterTextChanged(Editable s) {
			if (s == minutes.getText()) {
				if (s.length() == 1) seconds.requestFocus();
			}
			if (s == seconds.getText()) {
				if (s.length() == 2) thousands.requestFocus();
			}
		}

		public void beforeTextChanged(CharSequence s, int start, int count,	int after) {
		}

		public void onTextChanged(CharSequence s, int start, int before, int count) {
		}
	}

}

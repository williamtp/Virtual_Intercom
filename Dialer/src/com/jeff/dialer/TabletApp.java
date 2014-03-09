package com.jeff.dialer;



import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;




public class TabletApp extends Activity implements OnClickListener {
	
	
	public static final int SERVER_PORT=6000;
	
	public TextView m_textView;
	private ImageButton btnSend, btnCancel;
	private EditText edtNumber;
	

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		Log.w(Util.LOG_TAG, "Starting Tablet app ...");
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialer_layout);
//		numbers = getResources().getStringArray(R.array.numbers);
		edtNumber = (EditText) findViewById(R.id.edtNumber1);
		edtNumber.setInputType(InputType.TYPE_NULL);
		btnSend = (ImageButton) findViewById(R.id.btnSend);
		btnCancel = (ImageButton) findViewById(R.id.btnCancel);
		btnSend.setOnClickListener(this);
		btnCancel.setOnClickListener(this);
		findViewById(R.id.btn_0).setOnClickListener(this);
		findViewById(R.id.btn_1).setOnClickListener(this);
		findViewById(R.id.btn_2).setOnClickListener(this);
		findViewById(R.id.btn_3).setOnClickListener(this);
		findViewById(R.id.btn_4).setOnClickListener(this);
		findViewById(R.id.btn_5).setOnClickListener(this);
		findViewById(R.id.btn_6).setOnClickListener(this);
		findViewById(R.id.btn_7).setOnClickListener(this);
		findViewById(R.id.btn_8).setOnClickListener(this);
		findViewById(R.id.btn_9).setOnClickListener(this);
		findViewById(R.id.btn_10).setOnClickListener(this);
		//findViewById(R.id.btnSetting).setOnClickListener(this);
		//findViewById(R.id.btnViewLog).setOnClickListener(this);
		

		
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	        case R.id.action_settings:
	            openSettings();
	            return true;
	        case R.id.action_viewlog:
	        	viewLog();
	        	return true;
	        case R.id.action_stop:
	        	stopAll();
	        	return true;
	        	
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	private void stopAll() {
		// TODO Auto-generated method stub
		android.os.Process.killProcess(android.os.Process.myPid());
	}

	private void openSettings()
	{
		Intent setting = new Intent(this,SettingsActivity.class);
		startActivity(setting);
	}
	
	private void viewLog()
	{
		Intent logView = new Intent();
		logView.setAction(Intent.ACTION_VIEW);
		String uri = String.format("http://%1$s/root/data/checklog.html", SettingMgr.getStringValue(this, SettingMgr.Preference.UNADDR));
		logView.setData(Uri.parse(uri));
		startActivity(logView);
	}
	

	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch (id) 
		{
		case R.id.btnSend:
			
			Log.w(Util.LOG_TAG, "Notify Resident about the vistor's call.");
			
			String unitNo = edtNumber.getText().toString();
			
			//start the calling activity.
			Intent callingActivity = new Intent(this,Calling.class);
			callingActivity.putExtra("UnitNO", unitNo);
			startActivity(callingActivity);
			
			
			break;

		case R.id.btnCancel:
			setEditValue();
			break;
		case R.id.btn_sound:
			
			break;
		default :
			Button btn = (Button)v;
			String text = btn.getText().toString();
			if (null == text || text.equals("")) return;
			Editable edit = edtNumber.getText();
			if (edit.length() > 0) {
				edit.insert(edit.length(), text);
			} else {
				edit.insert(0, text);
			}
			break;
		}
	}
	
	private void setEditValue() {
		int start = edtNumber.getSelectionStart();
		if (start > 0) {
			edtNumber.getText().delete(start - 1, start);
		}
	}

	

}









package com.mopoo;

import android.app.Activity;
import android.app.NotificationManager;
import android.os.Bundle;
import android.view.View;

import com.mopoo.action.FetchState;

public class NotificationActionActivity extends Activity implements View.OnClickListener {
	private View okBtn;
	private View cancelBtn;
	private MyApplication myApplication;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		myApplication = (MyApplication) this.getApplicationContext();
		this.setContentView(R.layout.notification_action_layout);
		okBtn = this.findViewById(R.id.ok_btn);
		okBtn.setOnClickListener(this);
		cancelBtn = this.findViewById(R.id.cancel_btn);
		cancelBtn.setOnClickListener(this);
		if (!FetchState.DOING.equals(myApplication.getFetchState())) {
			NotificationManager nm = (NotificationManager) myApplication
					.getSystemService(MyApplication.NOTIFICATION_SERVICE);
			nm.cancel(MyApplication.NOTI_ID_FETCH_CACHE);
			this.finish();
		}
	}

	@Override
	public void onClick(View v) {
		if (v == okBtn) {
			myApplication.setFetchState(FetchState.CANCEL);
			this.finish();
		} else if (v == cancelBtn) {
			this.finish();
		}
	}
}
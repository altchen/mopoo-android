package com.mopoo;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.mopoo.server.HttpClientHelper;
import com.mopoo.server.RemoteServer;
import com.mopoo.utils.StringUtils;

public class MainActivity extends Activity implements
		android.view.View.OnClickListener {
	EditText userEditText;
	EditText passEditText;
	Button loginBtn;
	MyApplication myApplication;
	String[] userAndPass;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		myApplication = (MyApplication) this.getApplicationContext();
		if(myApplication.isUsePortrait()){
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}			
		if (myApplication.isLogin()) {
			startTopicListActivity();
			return;
		}
		this.setContentView(R.layout.main_layout);
		userEditText = (EditText) this.findViewById(R.id.user_textedit);
		passEditText = (EditText) this.findViewById(R.id.pass_textedit);
		loginBtn = (Button) this.findViewById(R.id.login_btn);
		loginBtn.setOnClickListener(this);
		hanlder = this.createHander();
		userAndPass = myApplication.getUserAndPass();
		hanlder = createHander();
		if (userAndPass != null) {
			initTextEdit();
			doLogin(userAndPass[0], userAndPass[1]);
		}
	}

	private void initTextEdit() {
		if (userAndPass != null) {
			userEditText.setText(userAndPass[0]);
			passEditText.setText(userAndPass[1]);
		}
	}

	private void doLogin(final String user, final String pass) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					hanlder.sendEmptyMessage(MSG_LOGIN_PROGRESS);
					String cookie = RemoteServer.getServer(myApplication).login(user,
							pass);
					myApplication.saveRememberMeCookie(cookie);
					myApplication.saveUser(user);
					myApplication.savePass(pass);
					hanlder.sendEmptyMessage(MSG_LOGIN_SUCCESS);
				} catch (Exception e) {
					e.printStackTrace();
					String msg = e.getMessage();
					Message message = new Message();
					message.what = MSG_LOGIN_ERROR;
					message.getData().putString("msg", "登陆出现错误,请求信息如下:"+msg);
					hanlder.sendMessage(message);
				}

			}

		}).start();
	}

	private void setAllEnable(boolean value) {
		userEditText.setEnabled(value);
		passEditText.setEnabled(value);
		loginBtn.setEnabled(value);
	}

	private Handler hanlder = null;
	private int MSG_LOGIN_PROGRESS = 0;
	private int MSG_LOGIN_SUCCESS = 1;
	private int MSG_LOGIN_ERROR = 2;

	private Handler createHander() {
		return new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == MSG_LOGIN_PROGRESS) {
					loginBtn.setText("登录中...");
					setAllEnable(false);
				} else if (msg.what == MSG_LOGIN_SUCCESS) {
					startTopicListActivity();
				} else if (msg.what == MSG_LOGIN_ERROR) {
					String tip = msg.getData().getString("msg");
					myApplication.showMessage(tip);
					loginBtn.setText(R.string.login);
					setAllEnable(true);
				}
				super.handleMessage(msg);
			}
		};
	}

	private void startTopicListActivity() {
		HttpClientHelper.getInstance(myApplication).addCookies(myApplication.getRememberMeCookie());
		Intent i =null;
		if(myApplication.isUseRssMode()){
			i = new Intent(this, TopicListActivity.class);
		}else{
			i = new Intent(this, HtmlTopicListActivity.class);
		}
		MainActivity.this.startActivity(i);
		MainActivity.this.finish();
	}

	@Override
	public void onClick(View v) {
		if (v == loginBtn) {
			if (StringUtils.isBlank(userEditText.getEditableText().toString())) {
				return;
			}
			if (StringUtils.isBlank(passEditText.getEditableText().toString())) {
				return;
			}
			doLogin(userEditText.getEditableText().toString(), passEditText
					.getEditableText().toString());
		}

	}

}
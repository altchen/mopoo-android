package com.mopoo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.mopoo.server.RemoteServer;
import com.mopoo.utils.StringUtils;
import com.mopoo.utils.vo.DoNewTopicObject;
import com.mopoo.utils.vo.TopicObject;

public class TopicListActivity extends Activity implements OnClickListener {
	private MyAppAdapter myAppAdapter;
	MyApplication myApplication;
	private TextView loadingTextView;
	private ListView listView;
	private View menuBtn;
	private View refreshBtn;
	private View closeBtn;
	private LayoutInflater layoutInflater;
	private String todayString = "";
	private List<TopicObject> topicList;
	private int currentLongClickPosition;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTitle("帖子列表");
		myApplication = (MyApplication) this.getApplicationContext();
		if(myApplication.isUsePortrait()){
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}		
		this.setContentView(R.layout.topic_list_layout);
		layoutInflater = LayoutInflater.from(this);
		loadingTextView = (TextView) this.findViewById(R.id.list_loading_textview);
		listView = (ListView) this.findViewById(R.id.topiclist_list_view);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				startViewTopicActivity(topicList.get(position));
			}
		});
		listView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				currentLongClickPosition = position;
				showDialog(ID_LONG_CLICK_MENU);
				return true;
			}

		});
		menuBtn = this.findViewById(R.id.menu_btn);
		menuBtn.setOnClickListener(this);
		refreshBtn = this.findViewById(R.id.refresh_btn);
		refreshBtn.setOnClickListener(this);
		closeBtn = this.findViewById(R.id.close_btn);
		closeBtn.setOnClickListener(this);	
		loadDataAndShowUI();
	}

	private void startHtmlViewTopicActivity(String id) {
		Intent i = new Intent(this, HtmlTopicActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString("id", id);
		i.putExtras(bundle);
		startActivity(i);
	}

	private void startViewTopicActivity(TopicObject topicObject) {
		myApplication.setCurrentTopic(topicObject);
		Intent i = new Intent(this, TopicActivity.class);
		startActivity(i);
	}

	public void loadDataAndShowUI() {
		new AsyncTask<Object, Integer, List<TopicObject>>() {
			@Override
			protected void onPreExecute() {
				resetTodayString();
				loadingTextView.setText("加载中...");
				loadingTextView.setVisibility(View.VISIBLE);
			}

			@Override
			protected List<TopicObject> doInBackground(Object... arg0) {
				return getTopicListFromServer();
			}

			@Override
			protected void onPostExecute(List<TopicObject> result) {
				topicList = result;
				myAppAdapter = new MyAppAdapter(TopicListActivity.this, topicList);
				listView.setAdapter(myAppAdapter);
				if (topicList.size() > 0) {
					loadingTextView.setVisibility(View.GONE);
				}
			}

		}.execute();

	}

	private final static int ID_NEW_TOPIC_DIALOG = 0;
	private final static int ID_LONG_CLICK_MENU = 1;

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == ID_NEW_TOPIC_DIALOG) {
			final Dialog dialog = new Dialog(TopicListActivity.this);
			dialog.setContentView(R.layout.new_topic_view);
			dialog.getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
			dialog.setTitle("发贴");
			dialog.setCancelable(true);
			final EditText bodyEditText = (EditText) dialog.findViewById(R.id.body_edittext);
			bodyEditText.setText("");
			final EditText subjectEditText = (EditText) dialog.findViewById(R.id.subject_edittext);
			subjectEditText.setText("");
			final CheckBox noNameCheckbox = (CheckBox) dialog.findViewById(R.id.noname_checkbox);
			final Button cancelButton = (Button) dialog.findViewById(R.id.cancel_btn);
			cancelButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
				}
			});
			final Button sendBtn = (Button) dialog.findViewById(R.id.reply_btn);
			sendBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final String body = bodyEditText.getText().toString();
					final String subject = subjectEditText.getText().toString();
					if (StringUtils.isBlank(body) || StringUtils.isBlank(subject)) {
						myApplication.showMessage("主题和内容必填");
						return;
					}
					final DoNewTopicObject newTopic = new DoNewTopicObject();
					newTopic.subject = subject;
					newTopic.body = body;
					if (noNameCheckbox.isChecked()) {
						newTopic.isNoName = true;
					} else {
						newTopic.isNoName = false;
					}
					new AsyncTask<Object, Integer, String>() {
						@Override
						protected void onPreExecute() {
							sendBtn.setEnabled(false);
							cancelButton.setEnabled(false);
							bodyEditText.setEnabled(false);
							sendBtn.setText("发送中...");
						}

						@Override
						protected String doInBackground(Object... arg0) {
							String msg = "";
							try {
								RemoteServer.getServer(myApplication).newTopic(newTopic);
							} catch (Exception e) {
								msg = e.getMessage();
							}
							return msg;
						}

						@Override
						protected void onPostExecute(String result) {
							sendBtn.setText("   发送   ");
							sendBtn.setEnabled(true);
							cancelButton.setEnabled(true);
							bodyEditText.setEnabled(true);
							if (StringUtils.isNotBlank(result)) {
								myApplication.showMessage(result);
								return;
							}
							myApplication.showMessage("发送成功...");
							dialog.dismiss();

						}

					}.execute();

				}
			});

			return dialog;
		} else if (id == ID_LONG_CLICK_MENU) {
			return new AlertDialog.Builder(TopicListActivity.this).setTitle("操作").setItems(new String[] { "Html查看此贴" },
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {

							switch (which) {
							case 0:
								startHtmlViewTopicActivity(topicList.get(currentLongClickPosition).id);
								break;
							}
						}
					}).create();
		}
		return null;
	}

	private void showNewTopicDialog() {
		this.showDialog(ID_NEW_TOPIC_DIALOG);
	}

	private void resetTodayString() {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-M-d");
		todayString = df.format(new Date());
	}

	private List<TopicObject> getTopicListFromServer() {
		try {
			List<TopicObject> topics = RemoteServer.getServer(myApplication).getTopTopicList(
					myApplication.getSettingListCount());
			return topics;
		} catch (Exception e) {
			final String msg = e.getMessage();
			this.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					loadingTextView.setText("得到列表出错，请销等再试或者注销后重新登录：" + msg);
				}

			});

		}
		return new ArrayList<TopicObject>(0);
	}

	class MyAppAdapter extends ArrayAdapter<TopicObject> {

		public MyAppAdapter(Context context, List<TopicObject> objects) {
			super(context, 0, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TopicObject currObject = this.getItem(position);
			ViewHolder viewHolder = null;
			if (convertView == null) {
				convertView = layoutInflater.inflate(R.layout.topic_list_view, null);
				viewHolder = new ViewHolder();
				viewHolder.subject = (TextView) convertView.findViewById(R.id.subject_textview);
				viewHolder.author = (TextView) convertView.findViewById(R.id.author_textview);
				viewHolder.publicDate = (TextView) convertView.findViewById(R.id.public_date_textview);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}
			viewHolder.subject.setText(currObject.subjct);
			viewHolder.author.setText(currObject.author);
			if (currObject.publicDate != null) {
				viewHolder.publicDate.setText(currObject.publicDate.replace(todayString, ""));
			} else {
				viewHolder.publicDate.setText("");
			}

			return convertView;
		}

		class ViewHolder {
			TextView subject;
			TextView author;
			TextView publicDate;
		}
	}

	public static final int MENU_ID_NEW_TOPIC = 0;
	public static final int MENU_ID_LOGOUT = 1;
	public static final int MENU_ID_SETTING = 2;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_ID_NEW_TOPIC, 0, "发贴");
		menu.add(0, MENU_ID_LOGOUT, 0, "注销");
		menu.add(0, MENU_ID_SETTING, 0, "设置");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId() == MENU_ID_LOGOUT) {
			myApplication.saveRememberMeCookie("");
			myApplication.saveUser("");
			myApplication.savePass("");
			Intent i = new Intent(this, MainActivity.class);
			startActivity(i);
			this.finish();
		} else if (item.getItemId() == MENU_ID_NEW_TOPIC) {
			showNewTopicDialog();
		} else if (item.getItemId() == MENU_ID_SETTING) {
			Intent i = new Intent(this, MyPreferences.class);
			startActivity(i);
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public void onClick(View v) {
		if (v == menuBtn) {
			this.openOptionsMenu();
		} else if (v == refreshBtn) {
			doRefresh();
		} else if (v == closeBtn) {
			this.finish();
		}
	}

	public void doRefresh() {
		this.loadDataAndShowUI();
	}
}
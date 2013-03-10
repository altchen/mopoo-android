package com.mopoo;

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
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.mopoo.action.FetchToCacheAction;
import com.mopoo.server.RemoteServer;
import com.mopoo.utils.DateUtils;
import com.mopoo.utils.MoveLeftRightInterface;
import com.mopoo.utils.MyGestureDetector;
import com.mopoo.utils.MyWebView;
import com.mopoo.utils.StringUtils;
import com.mopoo.utils.vo.CacheObject;
import com.mopoo.utils.vo.DoNewTopicObject;
import com.mopoo.utils.vo.TopicObject;

public class HtmlTopicListActivity extends Activity implements OnClickListener, MoveLeftRightInterface {
	MyApplication myApplication;
	private TextView loadingTextView;
	private MyWebView topicListWebView;
	private View menuBtn;
	private View refreshBtn;
	private View closeBtn;
	private ToggleButton onlineBtn;
	private LayoutInflater layoutInflater;
	private String todayString = "";
	private static final String ORIG_TITLE = "帖子列表";
	private String lastViewTopicId = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTitle(ORIG_TITLE);
		myApplication = (MyApplication) this.getApplicationContext();
		if (myApplication.isUsePortrait()) {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		this.setContentView(R.layout.html_topic_list_layout);
		layoutInflater = LayoutInflater.from(this);
		loadingTextView = (TextView) this.findViewById(R.id.list_loading_textview);
		topicListWebView = (MyWebView) this.findViewById(R.id.topic_list_webview);
		if (myApplication.isUseGesture()){
			topicListWebView.setGestureDetector(new GestureDetector(this, new MyGestureDetector(this)));
		}
		menuBtn = this.findViewById(R.id.menu_btn);
		menuBtn.setOnClickListener(this);
		closeBtn = this.findViewById(R.id.close_btn);
		closeBtn.setOnClickListener(this);
		refreshBtn = this.findViewById(R.id.refresh_btn);
		refreshBtn.setOnClickListener(this);
		onlineBtn = (ToggleButton) this.findViewById(R.id.online_btn);
		onlineBtn.setOnClickListener(this);
		if (myApplication.isValidNetwork()) {
			onlineBtn.setChecked(true);
			myApplication.setUseOffline(false);
		} else {
			onlineBtn.setChecked(false);
			myApplication.setUseOffline(true);
		}
		initWebView();
		loadDataAndShowUI(false);
	}

	@Override
	public void rightToLeft() {
		if (StringUtils.isNotBlank(lastViewTopicId)) {
			startViewTopicActivity(lastViewTopicId);
		}
	}

	@Override
	public void leftToRight() {
	}

	private void initWebView() {
		final WebSettings webSettings = topicListWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setLoadsImagesAutomatically(myApplication.isShowNetworkImage());
		webSettings.setBlockNetworkImage(true);
		topicListWebView.addJavascriptInterface(new Object() {
			@SuppressWarnings("unused")
			public void fixedImage(final String[] srcArray) {
				webSettings.setBlockNetworkImage(false);
				if (!myApplication.isShowNetworkImage()) {
					return;
				}
				new Thread(new Runnable() {
					public void run() {
						for (int i = 0; i < srcArray.length; i++) {
							final String src = srcArray[i];
							final int currIndex = i;
							try {
								final String path = RemoteServer.getServer(myApplication).downloadToLocal(src,
										myApplication.isShowNetworkImage()).replaceFirst("/mnt/sdcard/", "");
								HtmlTopicListActivity.this.runOnUiThread(new Runnable() {
									public void run() {
										topicListWebView.loadUrl("javascript:mopoo_show_img(" + currIndex + ",\""
												+ path + "\");");
									}
								});
							} catch (Exception e) {
								Log.v("mopoo", "加载图片出错:" + e.getMessage());
							}
						}

					}
				}).start();
			}
		}, "mopoo");
		topicListWebView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (url.indexOf("info2.asp?id=") >= 0) {
					final String id = url.substring(url.lastIndexOf("=") + 1);
					HtmlTopicListActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							startViewTopicActivity(id);
						}
					});
				}
				return true;
			}
		});
	}

	private void startViewTopicActivity(String id) {
		lastViewTopicId = id;
		Intent i = new Intent(this, HtmlTopicActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString("id", id);
		i.putExtras(bundle);
		startActivity(i);
	}

	public void loadDataAndShowUI(final boolean showToast) {
		new AsyncTask<Object, Integer, CacheObject<String>>() {
			@Override
			protected void onPreExecute() {
				if (showToast) {
					HtmlTopicListActivity.this.setProgressBarIndeterminateVisibility(true);
					myApplication.showMessage("加载中...");
				} else {
					loadingTextView.setText("加载中...");
					loadingTextView.setVisibility(View.VISIBLE);
				}
			}

			@Override
			protected CacheObject<String> doInBackground(Object... arg0) {
				try {
					return RemoteServer.getServer(myApplication).getTopicListHtml(myApplication.isUseOffline(),
							!myApplication.isUseOffline());
				} catch (Exception e) {
					final String msg = e.getMessage();
					HtmlTopicListActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							String showError = "得到列表出错，请销等再试或者注销后重新登录：" + msg;
							if (showToast) {
								myApplication.showMessage(showError);
							}
							loadingTextView.setText(showError);
						}

					});
				}
				return null;
			}

			@Override
			protected void onPostExecute(CacheObject<String> cacheObject) {
				HtmlTopicListActivity.this.setProgressBarIndeterminateVisibility(false);
				if (cacheObject != null) {
					topicListWebView.loadDataWithBaseURL("file:///mnt/sdcard/", cacheObject.data, "text/html", "utf-8",
							null);
					loadingTextView.setVisibility(View.GONE);
					if (myApplication.getSettingUseCache()) {
						if (cacheObject.isCache) {
							HtmlTopicListActivity.this.setTitle(ORIG_TITLE + "(缓存:"
									+ DateUtils.getFriendlyDate(cacheObject.dataDateTime) + ")");
						} else {
							HtmlTopicListActivity.this.setTitle(ORIG_TITLE + "(非缓存)");
						}
					}
				}
			}

		}.execute();

	}

	private final static int ID_NEW_TOPIC_DIALOG = 0;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case ID_NEW_TOPIC_DIALOG:
			final Dialog dialog = new Dialog(HtmlTopicListActivity.this);
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
		}
		return null;
	}

	private void showNewTopicDialog() {
		this.showDialog(ID_NEW_TOPIC_DIALOG);
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
	public static final int MENU_ID_FETCH_CACHE = 3;
	public static final int MENU_ID_VIEW_CHAT = 4;
	public static final int MENU_ID_VIEW_MOREFUN = 5;
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_ID_FETCH_CACHE, 0, "获取离线数据");
		menu.add(0, MENU_ID_NEW_TOPIC, 0, "发贴");
		menu.add(0, MENU_ID_LOGOUT, 0, "注销");
		menu.add(0, MENU_ID_SETTING, 0, "设置");
		menu.add(0, MENU_ID_VIEW_CHAT, 0, "留言板");
		menu.add(0, MENU_ID_VIEW_MOREFUN, 0, "更多功能");	
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
		} else if (item.getItemId() == MENU_ID_FETCH_CACHE) {
			new AlertDialog.Builder(this).setTitle("确定?").setMessage(
					"确定获取离线数据(前" + myApplication.getSettingFetchCount() + "帖)?\n" + "注意:\n"
							+ "1.比较占流量建议WIFI下才使用(设置区可调整获取数量)\n" + "2.中途要取消可点击通知进度条\n" + "3.较占里层资源请不要频繁使用").setIcon(
					R.drawable.icon).setPositiveButton("确定", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					FetchToCacheAction action = new FetchToCacheAction(myApplication, HtmlTopicListActivity.this);
					action.doFetch(myApplication.getSettingFetchCount());
				}
			}).setNegativeButton("取消", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.dismiss();
				}
			}).show();

		} else if (item.getItemId() == MENU_ID_VIEW_CHAT) {
			Intent i = new Intent(this,ChatActivity.class);
			this.startActivity(i);
		} else if (item.getItemId() == MENU_ID_VIEW_MOREFUN){
			Intent i = new Intent(this,MoreFunctionActivity.class);
			this.startActivity(i);			
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
		} else if (v == onlineBtn) {
			if (onlineBtn.isChecked()) {
				myApplication.setUseOffline(false);
			} else {
				myApplication.setUseOffline(true);
			}
		}
	}

	public void doRefresh() {
		this.loadDataAndShowUI(true);
	}
}
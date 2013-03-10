package com.mopoo;

import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.mopoo.action.ButtonState;
import com.mopoo.action.GoDownButtonState;
import com.mopoo.action.ScrollViewContext;
import com.mopoo.server.RemoteServer;
import com.mopoo.utils.DateUtils;
import com.mopoo.utils.MoveLeftRightInterface;
import com.mopoo.utils.MyGestureDetector;
import com.mopoo.utils.MyWebView;
import com.mopoo.utils.StringUtils;
import com.mopoo.utils.vo.CacheObject;
import com.mopoo.utils.vo.DoReplyObject;
import com.mopoo.utils.vo.ReplyObject;
import com.mopoo.utils.vo.TopicObject;

public class HtmlTopicActivity extends Activity implements OnClickListener,ScrollViewContext,MoveLeftRightInterface {
	MyApplication myApplication;
	private MyWebView topicWebView;
	private View menuBtn;
	private View refreshBtn;
	private Button posiControlBtn;
	private View backBtn;
	private String id;
	private ButtonState posiBtnSate;
	private ReplyObject currentLcReply;
	private static final String ORIG_TITLE = "查看帖子";
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		myApplication = (MyApplication) this.getApplicationContext();
		if(myApplication.isUsePortrait()){
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}		
		this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);		
		this.setTitle(ORIG_TITLE);
		this.setContentView(R.layout.html_topic_layout);
		topicWebView = (MyWebView) this.findViewById(R.id.topic_webview);
		if (myApplication.isUseGesture()){		
			topicWebView.setGestureDetector(new GestureDetector(this,new MyGestureDetector(this)));
		}
		menuBtn = this.findViewById(R.id.menu_btn);
		menuBtn.setOnClickListener(this);
		refreshBtn = this.findViewById(R.id.refresh_btn);
		refreshBtn.setOnClickListener(this);
		posiControlBtn = (Button)this.findViewById(R.id.pagedown_btn);
		posiControlBtn.setOnClickListener(this);
		backBtn = this.findViewById(R.id.back_btn);
		backBtn.setOnClickListener(this);
		
		initTopic();
		loadDataAndShowUI(false,false);
	}
	@Override
	public void rightToLeft() {
	}
	@Override
	public void leftToRight() {
		this.finish();
	}
	private void initTopic() {
		id = this.getIntent().getExtras().getString("id");
		final WebSettings webSettings = topicWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setLoadsImagesAutomatically(myApplication.isShowNetworkImage());
		webSettings.setBlockNetworkImage(true);
		topicWebView.addJavascriptInterface(new Object() {
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
								HtmlTopicActivity.this.runOnUiThread(new Runnable() {
									public void run() {
										topicWebView.loadUrl("javascript:mopoo_show_img(" + currIndex + ",\"" + path
												+ "\");");
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
		topicWebView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (url.indexOf("/new/submitrhtml.asp")!=-1) {// 回复
					try{
						List<NameValuePair> nameValues=StringUtils.parseURL(url, "utf-8");
						url = "/new/submitrhtml.asp?" + URLEncodedUtils.format(nameValues, "gb2312");
					}catch(Exception e ){
						//nothing
						Log.d("mopoo", "得到回复出错", e);
						myApplication.showMessage("得到引用回复出错,请直接使用菜单->回复");
						return true;
					}
					doLcReply(url);
					return true;
				} else if (url.startsWith("https://www.253874.com/new/sssend.asp")) {// 短信https://www.253874.com/new/sssend.asp?yourname=
					return true;
				} else if (url.startsWith("https://www.253874.com/new/jpuserif.asp")) { // 查询https://www.253874.com/new/jpuserif.asp?id=&id2=
					int id2Posi = url.indexOf("id2=");
					if (id2Posi >= 0) {
						int end = url.indexOf("&", id2Posi);
						String id2 = "";
						if (end < 0) {
							id2 = url.substring(id2Posi + 4);
						} else {
							id2 = url.substring(id2Posi + 4, end);
						}
						final String replyId = id2;
						HtmlTopicActivity.this.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								doEditReply(replyId);
							}
						});
					}

					return true;
				} else if (url.indexOf("info2.asp?lmck=1") >= 0) {
					doRefresh(true);
					return true;
				}
				try {
					Uri uri = Uri.parse(url);
					Intent it = new Intent(Intent.ACTION_VIEW, uri);
					startActivity(it);
				} catch (Exception e) {
					Log.e("mopoo", e.getMessage());
				}
				return true;

			}
		});
	}
	
	public void doLcReply(final String url){
		new AsyncTask<Object, Integer, ReplyObject>() {
			@Override
			protected void onPreExecute() {
				HtmlTopicActivity.this.setProgressBarIndeterminateVisibility(true);					
				myApplication.showMessage("加载中...");
			}

			@Override
			protected ReplyObject doInBackground(Object... arg0) {
				try {
						return RemoteServer.getServer(myApplication).getOtherUserReplayByURL(url);
				} catch (Exception e) {
					final String errorMsg = e.getMessage();
					HtmlTopicActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							myApplication.showMessage("加载原回复出错:"+errorMsg);
						}

					});
				}
				return null;
			}

			@Override
			protected void onPostExecute(ReplyObject object) {
				HtmlTopicActivity.this.setProgressBarIndeterminateVisibility(false);					
				HtmlTopicActivity.this.setState(new GoDownButtonState());				
				if(object != null){
					currentLcReply = object;
					showReplyDialog();
				}
			}

		}.execute();
	}
	public void showReplyDialog(){
		removeDialog(ID_SHOW_REPLY_DIALOG);
		showDialog(ID_SHOW_REPLY_DIALOG);
	}
	public void loadDataAndShowUI(final boolean isViewPay,final boolean showToast) {
		final WebSettings webSettings = topicWebView.getSettings();
		webSettings.setBlockNetworkImage(true);
		new AsyncTask<Object, Integer, CacheObject<String>>() {
			@Override
			protected void onPreExecute() {
				if(showToast){
					HtmlTopicActivity.this.setProgressBarIndeterminateVisibility(true);					
					myApplication.showMessage("加载中...");
				}else{
					topicWebView.loadDataWithBaseURL("about:blank",
							"<div width=100% height=100% text-align=center >加载中...</div>", "text/html", "utf-8", null);
				}
			}

			@Override
			protected CacheObject<String> doInBackground(Object... arg0) {
				try {
					if(isViewPay){
						return RemoteServer.getServer(myApplication).getTopicHtmlOfPlay(id);
					}else{
						return RemoteServer.getServer(myApplication).getTopicHtml(id,myApplication.isUseOffline(),!myApplication.isUseOffline());
					}
				} catch (Exception e) {
					final String errorMsg = e.getMessage();
					HtmlTopicActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if(showToast){
								myApplication.showMessage("加载出错:"+errorMsg);
							}else{
								topicWebView.loadDataWithBaseURL("about:blank", "<hr align=left size=1> 加载出错:" + errorMsg,
										"text/html", "utf-8", null);
							}
						}

					});
				}
				return null;
			}

			@Override
			protected void onPostExecute(CacheObject<String> cacheObject) {
				HtmlTopicActivity.this.setProgressBarIndeterminateVisibility(false);					
				HtmlTopicActivity.this.setState(new GoDownButtonState());				
				if(cacheObject != null){					
					topicWebView.loadDataWithBaseURL("file:///mnt/sdcard/", cacheObject.data, "text/html", "utf-8", null);
					if(myApplication.getSettingUseCache()){
						if(cacheObject.isCache){
							HtmlTopicActivity.this.setTitle(ORIG_TITLE+"(缓存:"+DateUtils.getFriendlyDate(cacheObject.dataDateTime)+")");
						}else{
							HtmlTopicActivity.this.setTitle(ORIG_TITLE+"(非缓存)");
						}
					}
				}
			}

		}.execute();

	}

	private ProgressDialog progressDialog;
	private ReplyObject replyObject;

	private void doEditReply(final String replyId) {
		new AsyncTask<Object, Integer, ReplyObject>() {
			@Override
			protected void onPreExecute() {
				progressDialog = ProgressDialog.show(HtmlTopicActivity.this, "请稍候", "读取回复中...");
			}

			@Override
			protected ReplyObject doInBackground(Object... arg0) {
				try {
					return RemoteServer.getServer(myApplication).getReply(id, replyId);
				} catch (Exception e) {
					showMessageInUi("读取回复失败:" + e.getMessage());
					return null;
				}

			}

			@Override
			protected void onPostExecute(ReplyObject result) {
				replyObject = result;
				if (result != null) {
					showDialog(ID_SHOW_EDIT_REPLY_DIALOG);
				}
				progressDialog.dismiss();
			}

		}.execute();
	}

	private void showMessageInUi(final String msg) {
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				myApplication.showMessage(msg);
			}
		});
	}

	private final static int ID_SHOW_REPLY_DIALOG = 0;
	private final static int ID_SHOW_EDIT_REPLY_DIALOG = 1;

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == ID_SHOW_REPLY_DIALOG) {
			final Dialog dialog = new Dialog(HtmlTopicActivity.this);
			dialog.setContentView(R.layout.reply_view);
			dialog.getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
			dialog.setTitle("回复");
			dialog.setCancelable(true);
			final EditText replyBodyEditText = (EditText) dialog.findViewById(R.id.reply_body_edittext);
			replyBodyEditText.setText("");
			if(currentLcReply!=null){
				final TextView lcTextView = (TextView) dialog.findViewById(R.id.lcTextView);
				if (lcTextView!=null){
					lcTextView.setText("回复("+currentLcReply.lc+")楼:");
				}
			}
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
					String replyBody = replyBodyEditText.getText().toString();
					if (StringUtils.isBlank(replyBody)) {
						myApplication.showMessage("回复内容不能为空");
						return;
					}
					replyBody = replyBody.trim();
					final DoReplyObject reply = new DoReplyObject();
					reply.body = replyBody;
					if (noNameCheckbox.isChecked()) {
						reply.isNoName = true;
					} else {
						reply.isNoName = false;
					}
					
					if (currentLcReply !=null){
						reply.lc = currentLcReply.lc;
						reply.lcText = currentLcReply.body;
					}
						
					new AsyncTask<Object, Integer, String>() {
						@Override
						protected void onPreExecute() {
							sendBtn.setEnabled(false);
							cancelButton.setEnabled(false);
							// replyBodyEditText.setEnabled(false);
							sendBtn.setText("发送中...");
						}

						@Override
						protected String doInBackground(Object... arg0) {
							String msg = "";
							try {
								TopicObject topic = new TopicObject();
								topic.id = HtmlTopicActivity.this.id;
								RemoteServer.getServer(myApplication).reply(topic, reply);
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
							replyBodyEditText.setEnabled(true);
							if (StringUtils.isNotBlank(result)) {
								myApplication.showMessage(result);
								return;
							}
							myApplication.showMessage("发送成功...");
							dialog.dismiss();
							HtmlTopicActivity.this.removeDialog(ID_SHOW_REPLY_DIALOG);
							doRefresh(false);
						}

					}.execute();

				}
			});
			return dialog;
		} else if (id == ID_SHOW_EDIT_REPLY_DIALOG) {
			final Dialog editDialog = new Dialog(HtmlTopicActivity.this);
			editDialog.setContentView(R.layout.edit_reply_view);
			editDialog.getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
			editDialog.setTitle("编辑回复");
			editDialog.setCancelable(true);
			final EditText editText = (EditText) editDialog.findViewById(R.id.reply_body_edittext);
			editText.setText(replyObject.body);
			final Button editCancelButton = (Button) editDialog.findViewById(R.id.cancel_btn);
			final Button editSendBtn = (Button) editDialog.findViewById(R.id.save_btn);
			final Button delBtn = (Button) editDialog.findViewById(R.id.del_btn);
			editCancelButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					editDialog.dismiss();
					HtmlTopicActivity.this.removeDialog(ID_SHOW_EDIT_REPLY_DIALOG);
				}
			});

			editSendBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String replyBody = editText.getText().toString();
					if (StringUtils.isBlank(replyBody)) {
						myApplication.showMessage("回复内容不能为空");
						return;
					}
					replyObject.body = replyBody;
					new AsyncTask<Object, Integer, String>() {
						@Override
						protected void onPreExecute() {
							editSendBtn.setEnabled(false);
							editCancelButton.setEnabled(false);
							delBtn.setEnabled(false);
							editSendBtn.setText("修改中...");
						}

						@Override
						protected String doInBackground(Object... arg0) {
							String msg = null;
							try {
								RemoteServer.getServer(myApplication).saveEditReply(replyObject);
							} catch (Exception e) {
								msg = e.getMessage();
							}
							return msg;
						}

						@Override
						protected void onPostExecute(String result) {
							editSendBtn.setText("   修改   ");
							editSendBtn.setEnabled(true);
							editCancelButton.setEnabled(true);
							delBtn.setEnabled(true);
							if (StringUtils.isNotBlank(result)) {
								myApplication.showMessage(result);
								return;
							}
							myApplication.showMessage("修改成功...");
							editDialog.dismiss();
							HtmlTopicActivity.this.removeDialog(ID_SHOW_EDIT_REPLY_DIALOG);
							doRefresh(false);
						}

					}.execute();
				}
			});
			delBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					new AsyncTask<Object, Integer, String>() {
						@Override
						protected void onPreExecute() {
							delBtn.setText("   删除中...");
							editSendBtn.setEnabled(false);
							editCancelButton.setEnabled(false);
							delBtn.setEnabled(false);							
						}

						@Override
						protected String doInBackground(Object... arg0) {
							String msg = null;
							try {
								RemoteServer.getServer(myApplication).delReply(replyObject);
							} catch (Exception e) {
								msg = e.getMessage();
							}
							return msg;
						}

						@Override
						protected void onPostExecute(String result) {
							delBtn.setText("   删除   ");
							editSendBtn.setEnabled(true);
							editCancelButton.setEnabled(true);
							delBtn.setEnabled(true);
							if (StringUtils.isNotBlank(result)) {
								myApplication.showMessage(result);
								return;
							}
							myApplication.showMessage("删除成功...");
							editDialog.dismiss();
							HtmlTopicActivity.this.removeDialog(ID_SHOW_EDIT_REPLY_DIALOG);
							doRefresh(false);
						}

					}.execute();
				}
			});			
			return editDialog;
		}
		return null;
	}

	public void showReply() {
		this.showDialog(ID_SHOW_REPLY_DIALOG);
	}

	public static final int MENU_ID_NEW_REPLY = 0;
	public static final int MENU_ID_COLL = 1;
	public static final int MENU_ID_VIEW_NEED_REPLY = 2;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_ID_NEW_REPLY, 0, "回复");
		menu.add(0, MENU_ID_COLL, 0, "收藏");
		menu.add(0, MENU_ID_VIEW_NEED_REPLY, 0, "购买查看");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId() == MENU_ID_NEW_REPLY) {
			currentLcReply = null;
			showReplyDialog();
		} else if (item.getItemId() == MENU_ID_COLL) {
			new AsyncTask<Object, Integer, String>() {
				@Override
				protected void onPreExecute() {
					myApplication.showMessage("执行收藏中...");
				}

				@Override
				protected String doInBackground(Object... arg0) {
					try {
						RemoteServer.getServer(myApplication).collTopic(id);
						return null;
					} catch (Exception e) {
						return e.getMessage();
					}
				}

				@Override
				protected void onPostExecute(String result) {
					if (StringUtils.isNotBlank(result)) {
						myApplication.showMessage("收藏失败:" + result);
					} else {
						myApplication.showMessage("收藏成功");
					}
				}

			}.execute();

		} else if (item.getItemId() == MENU_ID_VIEW_NEED_REPLY) {
			doRefresh(true);
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public void onClick(View v) {
		if (v == menuBtn) {
			this.openOptionsMenu();
		} else if (v == refreshBtn) {
			doRefresh(false);
		} else if (v == posiControlBtn) {
			if(posiBtnSate != null){
				posiBtnSate.click(this);
			}
		} else if (v == backBtn) {
			this.finish();
		}
	}

	public void doRefresh(boolean isViewPay) {
		this.loadDataAndShowUI(isViewPay,true);
	}

	@Override
	public void goDown() {
		topicWebView.pageDown(true);
	}

	@Override
	public void goTop() {
		topicWebView.pageUp(true);
	}
	@Override
	public void setState(ButtonState state) {
		posiControlBtn.setText(state.getButtonText());
		this.posiBtnSate = state;
	}
}
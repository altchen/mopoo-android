package com.mopoo;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;

import com.mopoo.action.ButtonState;
import com.mopoo.action.GoDownButtonState;
import com.mopoo.action.ScrollViewContext;
import com.mopoo.server.RemoteServer;
import com.mopoo.utils.MoveLeftRightInterface;
import com.mopoo.utils.MyWebView;
import com.mopoo.utils.StringUtils;

public class ChatActivity extends Activity implements OnClickListener, ScrollViewContext, MoveLeftRightInterface {
	MyApplication myApplication;
	private MyWebView webView;
	private View newChatBtn;
	private View refreshBtn;
	private Button posiControlBtn;
	private View backBtn;
	private ButtonState posiBtnSate;
	private static final String ORIG_TITLE = "留言板";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		myApplication = (MyApplication) this.getApplicationContext();
		if (myApplication.isUsePortrait()) {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		this.setTitle(ORIG_TITLE);
		this.setContentView(R.layout.chat_layout);
		webView = (MyWebView) this.findViewById(R.id.chat_webview);

		newChatBtn = this.findViewById(R.id.new_chat_btn);
		newChatBtn.setOnClickListener(this);
		refreshBtn = this.findViewById(R.id.refresh_btn);
		refreshBtn.setOnClickListener(this);
		posiControlBtn = (Button) this.findViewById(R.id.pagedown_btn);
		posiControlBtn.setOnClickListener(this);
		backBtn = this.findViewById(R.id.back_btn);
		backBtn.setOnClickListener(this);

		initWebView();
		loadDataAndShowUI(false, false);
	}

	@Override
	public void rightToLeft() {
	}

	@Override
	public void leftToRight() {
		this.finish();
	}

	private void initWebView() {
		final WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setLoadsImagesAutomatically(myApplication.isShowNetworkImage());
		webSettings.setBlockNetworkImage(true);
		webView.addJavascriptInterface(new Object() {
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
								ChatActivity.this.runOnUiThread(new Runnable() {
									public void run() {
										webView.loadUrl("javascript:mopoo_show_img(" + currIndex + ",\"" + path
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
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
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

	public void loadDataAndShowUI(final boolean isViewPay, final boolean showToast) {
		final WebSettings webSettings = webView.getSettings();
		webSettings.setBlockNetworkImage(true);
		new AsyncTask<Object, Integer, String>() {
			@Override
			protected void onPreExecute() {
				if (showToast) {
					ChatActivity.this.setProgressBarIndeterminateVisibility(true);
					myApplication.showMessage("加载中...");
				} else {
					webView.loadDataWithBaseURL("about:blank",
							"<div width=100% height=100% text-align=center >加载中...</div>", "text/html", "utf-8", null);
				}
			}

			@Override
			protected String doInBackground(Object... arg0) {
				try {
					return RemoteServer.getServer(myApplication).getChatHtml();
				} catch (Exception e) {
					final String errorMsg = e.getMessage();
					ChatActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (showToast) {
								myApplication.showMessage("加载出错:" + errorMsg);
							} else {
								webView.loadDataWithBaseURL("about:blank", "<hr align=left size=1> 加载出错:" + errorMsg,
										"text/html", "utf-8", null);
							}
						}

					});
				}
				return null;
			}

			@Override
			protected void onPostExecute(String html) {
				ChatActivity.this.setProgressBarIndeterminateVisibility(false);
				ChatActivity.this.setState(new GoDownButtonState());
				if (html != null) {
					webView.loadDataWithBaseURL("file:///mnt/sdcard/", html, "text/html", "utf-8", null);
				}
			}

		}.execute();

	}

	private final static int ID_SHOW_CHAT_DIALOG = 0;

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == ID_SHOW_CHAT_DIALOG) {
			final Dialog dialog = new Dialog(ChatActivity.this);
			dialog.setContentView(R.layout.new_chat_view);
			dialog.getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
			dialog.setTitle("留言");
			dialog.setCancelable(true);
			final EditText editText = (EditText) dialog.findViewById(R.id.message_edittext);
			editText.setText("");
			final Button cancelButton = (Button) dialog.findViewById(R.id.cancel_btn);
			cancelButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
				}
			});
			final Button sendBtn = (Button) dialog.findViewById(R.id.send_btn);
			sendBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final String message = editText.getText().toString();
					if (StringUtils.isBlank(message)) {
						myApplication.showMessage("留言内容不能为空");
						return;
					}
					new AsyncTask<Object, Integer, String>() {
						@Override
						protected void onPreExecute() {
							sendBtn.setEnabled(false);
							cancelButton.setEnabled(false);
							sendBtn.setText("发送中...");
						}

						@Override
						protected String doInBackground(Object... arg0) {
							String msg = "";
							try {
								RemoteServer.getServer(myApplication).sendChatMessage(message);
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
							if (StringUtils.isNotBlank(result)) {
								myApplication.showMessage(result);
								return;
							}
							myApplication.showMessage("发送成功...");
							dialog.dismiss();
							ChatActivity.this.removeDialog(ID_SHOW_CHAT_DIALOG);
							doRefresh(false);
						}

					}.execute();

				}
			});

			return dialog;
		}
		return null;
	}

	@Override
	public void onClick(View v) {
		if (v == refreshBtn) {
			doRefresh(false);
		} else if (v == posiControlBtn) {
			if (posiBtnSate != null) {
				posiBtnSate.click(this);
			}
		} else if (v == backBtn) {
			this.finish();
		} else if (v == newChatBtn) {
			showDialog(ID_SHOW_CHAT_DIALOG);
		}
	}

	public void doRefresh(boolean isViewPay) {
		this.loadDataAndShowUI(isViewPay, false);
	}

	@Override
	public void goDown() {
		webView.pageDown(true);
	}

	@Override
	public void goTop() {
		webView.pageUp(true);
	}

	@Override
	public void setState(ButtonState state) {
		posiControlBtn.setText(state.getButtonText());
		this.posiBtnSate = state;
	}
}
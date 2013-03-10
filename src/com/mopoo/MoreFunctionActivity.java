package com.mopoo;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.mopoo.server.RemoteServer;
import com.mopoo.utils.MoveLeftRightInterface;
import com.mopoo.utils.MyWebView;
import com.mopoo.utils.StringUtils;
import com.mopoo.utils.vo.ReplyObject;

public class MoreFunctionActivity extends Activity implements OnClickListener, MoveLeftRightInterface {
	MyApplication myApplication;
	private MyWebView webView;
	private Button favBtn;
	private Button selfBtn;
	private Button replyBtn;	
	private View backBtn;
	private static final String ORIG_TITLE = "更多功能";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		myApplication = (MyApplication) this.getApplicationContext();
		if (myApplication.isUsePortrait()) {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		this.setTitle(ORIG_TITLE);
		this.setContentView(R.layout.more_function_layout);
		webView = (MyWebView) this.findViewById(R.id.webview);
		favBtn = (Button) this.findViewById(R.id.fav_btn);
		favBtn.setOnClickListener(this);
		selfBtn = (Button) this.findViewById(R.id.self_btn);
		selfBtn.setOnClickListener(this);
		replyBtn = (Button) this.findViewById(R.id.reply_btn);
		replyBtn.setOnClickListener(this);		
		backBtn = this.findViewById(R.id.back_btn);
		backBtn.setOnClickListener(this);
		initWebView();
		showInitView();
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
								MoreFunctionActivity.this.runOnUiThread(new Runnable() {
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
			@SuppressWarnings("unused")
			public void search(final String key,final String type) {
				MoreFunctionActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						showSearch(key, type);
					}
				});
			}
		}, "mopoo");
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (url.indexOf("info2.asp?id=") >= 0) {
					final String id = url.substring(url.lastIndexOf("=") + 1);
					MoreFunctionActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							startViewTopicActivity(id);
						}
					});
				} else if (url.indexOf("infonewdel.asp") >= 0 ){
					String topicId = StringUtils.getQueryValueByString(url, "sid");
					String replyId = StringUtils.getQueryValueByString(url, "id");
					try {
						ReplyObject replyObject = new ReplyObject();
						replyObject.topicId = topicId;
						replyObject.id = replyId;
						RemoteServer.getServer(myApplication).delReply(replyObject);
						MoreFunctionActivity.this.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								myApplication.showMessage("删除成功");
							}
						});						
					} catch (Exception e) {
						MoreFunctionActivity.this.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								String showError = "删除失败";
								myApplication.showMessage(showError);
							}
						});
					}

				}				
				
				return true;
			}
//				if ([url rangeOfString:@"infonew.asp?iio="].length >0){
//		            NSDictionary * dict = [[request URL] queryDictionary];
//		            NSString * iio = [dict valueForKey:@"iio"];
//		            NSString * lastUrl = [@"infonew.asp?iio=" stringByAppendingString:iio];
//		            dispatch_async(dispatch_get_main_queue(), ^{
//		                [MBProgressHUD showHUDAddedTo:self.view animated:YES]; 
//		                dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
//		                    NSString * html = [[MopooRemoteServer sharedRemoteServer] fetchInfoNewHtml:lastUrl];
//		                    dispatch_async(dispatch_get_main_queue(), ^{
//		                        [self showHtmlToWebView:html addTopLink:TRUE];
//		                        [MBProgressHUD hideHUDForView:self.view animated:YES]; 
//		                    });
//		                });
//		            }); 
//				return true;
//
//			}
		});
	}
	private void startViewTopicActivity(String id) {
		Intent i = new Intent(this, HtmlTopicActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString("id", id);
		i.putExtras(bundle);
		startActivity(i);
	}	
	private void showInitView(){
		webView.loadDataWithBaseURL("file:///mnt/sdcard/",htmlOfTopLink(), "text/html", "utf-8",
				null);
	}
	private String htmlOfTopLink(){
	    return "<form action='msearch.asp' method='get'><div align='left'><p>"
	    +"<input id='T1' name='T1' size='10'  value=''><span >&nbsp;<select id='D1' name='D1' size='1' >"
	    +"<option value='帖子标题' selected=''>帖子标题</option>"
	    +"<option value='发帖人'>发帖人</option>"
	    +"<option value='发言人'>发言人</option>"
	    +"</select></span>&nbsp;<input name='B1' c type='button' onclick='search();' value='查找'></span></p>"
	    +"</div>"
	    +"</form><script>function search(){window.mopoo.search(document.getElementById('T1').value,document.getElementById('D1').value);};</script>";
	}
	private void showSearch(final String key,final String searchType){
		new AsyncTask<Object, Integer, String>() {
			@Override
			protected void onPreExecute() {
				MoreFunctionActivity.this.setProgressBarIndeterminateVisibility(true);
					myApplication.showMessage("加载中...");
			}

			@Override
			protected String doInBackground(Object... arg0) {
				try {
					return RemoteServer.getServer(myApplication).search(key, searchType);
				} catch (Exception e) {
					final String msg = e.getMessage();
					MoreFunctionActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							String showError = "得到列表出错，请销等再试或者注销后重新登录：" + msg;
							myApplication.showMessage(showError);
						}

					});
				}
				return null;
			}

			@Override
			protected void onPostExecute(String html) {
				MoreFunctionActivity.this.setProgressBarIndeterminateVisibility(false);
				if (html != null) {
					html = MoreFunctionActivity.this.htmlOfTopLink() + html;
					webView.loadDataWithBaseURL("file:///mnt/sdcard/", html, "text/html", "utf-8",
							null);
				}
			}

		}.execute();
	}
	public void showInfoNewUrl(final String url) {
		new AsyncTask<Object, Integer, String>() {
			@Override
			protected void onPreExecute() {
				MoreFunctionActivity.this.setProgressBarIndeterminateVisibility(true);
					myApplication.showMessage("加载中...");
			}

			@Override
			protected String doInBackground(Object... arg0) {
				try {
					return RemoteServer.getServer(myApplication).getInfoNew(url);
				} catch (Exception e) {
					final String msg = e.getMessage();
					MoreFunctionActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							String showError = "得到列表出错，请销等再试或者注销后重新登录：" + msg;
							myApplication.showMessage(showError);
						}

					});
				}
				return null;
			}

			@Override
			protected void onPostExecute(String html) {
				MoreFunctionActivity.this.setProgressBarIndeterminateVisibility(false);
				if (html != null) {
					html = MoreFunctionActivity.this.htmlOfTopLink() + html;
					webView.loadDataWithBaseURL("file:///mnt/sdcard/", html, "text/html", "utf-8",
							null);
				}
			}

		}.execute();

	}	
	@Override
	public void onClick(View v) {
		if (v == backBtn) {
			this.finish();
		} else if (v == favBtn){
			showInfoNewUrl("/new/infonew.asp?iio=2");
		}else if (v == selfBtn){
			showInfoNewUrl("/new/infonew.asp?iio=3");
		}else if (v == replyBtn){
			showInfoNewUrl("/new/infonew.asp?iio=4");
		}
	}
}
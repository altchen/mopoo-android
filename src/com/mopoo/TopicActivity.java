package com.mopoo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
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
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mopoo.action.ButtonState;
import com.mopoo.action.GoDownButtonState;
import com.mopoo.action.ScrollViewContext;
import com.mopoo.server.RemoteServer;
import com.mopoo.utils.StringUtils;
import com.mopoo.utils.vo.DoReplyObject;
import com.mopoo.utils.vo.ReplyObject;
import com.mopoo.utils.vo.TopicObject;

public class TopicActivity extends Activity implements OnClickListener,ScrollViewContext {
 	MyApplication myApplication;	
	private WebView topicWebView; 
	private WebView replyWebView; 
	private View menuBtn;
	private View refreshBtn;
	private Button pageDownBtn;
	private View backBtn;
	private ScrollView scrollView;
	private LayoutInflater layoutInflater;
	private String todayString = "";
	private List<TopicObject> replyTopicList;
	private TopicObject topic;
	private ButtonState scrollControlState;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myApplication=(MyApplication)this.getApplicationContext(); 
        topic = myApplication.getCurrentTopic();
		if(myApplication.isUsePortrait()){
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}        
        this.setTitle(topic.subjct);
        this.setContentView(R.layout.topic_layout);
        layoutInflater=LayoutInflater.from(this);
        scrollView = (ScrollView)this.findViewById(R.id.scrollview);
        topicWebView=(WebView)this.findViewById(R.id.topic_webview); 
        replyWebView=(WebView)this.findViewById(R.id.reply_webview); 
        menuBtn=this.findViewById(R.id.menu_btn);
        menuBtn.setOnClickListener(this);
        refreshBtn=this.findViewById(R.id.refresh_btn);
        refreshBtn.setOnClickListener(this);
		pageDownBtn = (Button)this.findViewById(R.id.pagedown_btn);
		pageDownBtn.setOnClickListener(this);
		backBtn = this.findViewById(R.id.back_btn);
		backBtn.setOnClickListener(this); 
        initTopic();
    	loadDataAndShowUI();    
    }
    private void initTopic(){
    	StringBuilder sb = new StringBuilder();
    	String temp;
		if(StringUtils.isNotBlank(topic.body)){
			int posi = topic.body.indexOf("<hr align=left size=1>");
			if(posi >=0){
				temp = topic.body.substring(posi+22);
			}else{
				temp = topic.body;
			}
			posi = temp.lastIndexOf("(发帖时间:");
			if(posi>=0){
				sb.append(temp.substring(0,posi));
				sb.append(topic.author);
				sb.append("&nbsp");
				sb.append(temp.substring(posi));
			}else{
				sb.append(temp);
			}
		}
		topicWebView.getSettings().setLoadsImagesAutomatically(myApplication.isShowNetworkImage());
    	topicWebView.loadDataWithBaseURL("about:blank",sb.toString(), "text/html", "utf-8",null);
    	
    	replyWebView.getSettings().setLoadsImagesAutomatically(myApplication.isShowNetworkImage());
    	replyWebView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (url.startsWith("/modify/")) { // 查询https://www.253874.com/new/jpuserif.asp?id=&id2=
					int id2Posi = url.lastIndexOf("/");
					if (id2Posi >= 0) {
						String id2 = url.substring(id2Posi+1);
						final String replyId = id2;
						TopicActivity.this.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								doEditReply(replyId);
							}
						});
						return true;
					}
				}
				return false;
			}
		});    	
    }
	private ProgressDialog progressDialog;
	private ReplyObject replyObject;

	private void doEditReply(final String replyId) {
		new AsyncTask<Object, Integer, ReplyObject>() {
			@Override
			protected void onPreExecute() {
				progressDialog = ProgressDialog.show(TopicActivity.this, "请稍候", "读取回复中...");
			}

			@Override
			protected ReplyObject doInBackground(Object... arg0) {
				try {
					return RemoteServer.getServer(myApplication).getReply(topic.id, replyId);
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
	public void loadDataAndShowUI(){
    	new AsyncTask<Object,Integer,List<TopicObject>>(){
    		@Override
    		protected void onPreExecute() {
		    	resetTodayString();
		    	replyWebView.loadDataWithBaseURL("about:blank","<hr align=left size=1>加载回复中....", "text/html", "utf-8",null);
    		}
			@Override
			protected List<TopicObject> doInBackground(Object... arg0) {
		        return getReplyTopicListFromServer();
			}
			@Override
			protected void onPostExecute(List<TopicObject> result) {
				TopicActivity.this.setState(new GoDownButtonState());
		    	replyTopicList = result;
		    	StringBuilder sb = new StringBuilder();
		    	int size =replyTopicList.size();
		    	if(size ==0){
		    		sb.append("<hr align=left size=1>");
		    		sb.append("暂无回复");
		    	}else{
			    	for(int i=size-1;i>=0;i--){
			    		TopicObject reply = replyTopicList.get(i);
			    		sb.append("<br>");
			    		sb.append("<hr align=left size=1>");
			    		sb.append("回复(");
			    		sb.append(size-i);
			    		sb.append("):");
			    		if(StringUtils.isNotBlank(reply.body)){
			    			int posi = reply.body.indexOf("<hr align=left size=1>");
			    			if(posi >=0){
			    				sb.append(reply.body.substring(posi+22));
			    			}else{
			    				sb.append(reply.body);
			    			}
			    		}
			    		sb.append("<br>");
			    		sb.append("---");
			    		sb.append(reply.author);
			    		sb.append("&nbsp;&nbsp;&nbsp;&nbsp;");
			    		if(StringUtils.isNotBlank(reply.subjct)){
			    			int posi = reply.subjct.indexOf("title='");
			    			if(posi>=0){
				    			int endPosi = reply.subjct.indexOf("'",posi+7);
				    			if(endPosi>=0){
				    				sb.append(reply.subjct.substring(posi+7,endPosi));
				    			}
			    			}
			    		}
			    		if(i==0){
				    		sb.append("<hr align=left size=1><div style='text-align:center'>共("+size+")个回复 </div>");
			    		}
			    	}
		    	}
		    	replyWebView.loadDataWithBaseURL("about:blank",sb.toString(), "text/html", "utf-8",null);
			}
    		
    	}.execute();

    }
    private void resetTodayString(){
    	SimpleDateFormat df =new SimpleDateFormat("yyyy-M-d");
    	todayString = df.format(new Date());
    }
    private List<TopicObject> getReplyTopicListFromServer(){
    	try{
    		List<TopicObject> topics = RemoteServer.getServer(myApplication).getReplyTopicList(topic.id);
    		return topics;
    	}catch(Exception e){
    		final String errorMsg = e.getMessage();
    		this.runOnUiThread(new Runnable(){
				@Override
				public void run() {
					replyWebView.loadDataWithBaseURL("about:blank","<hr align=left size=1> 加载出错"+errorMsg, "text/html", "utf-8",null); 
				}
			
    		});
    	}
		return new ArrayList<TopicObject>(0);
    }
    
    class MyAppAdapter extends ArrayAdapter<TopicObject>{

		public MyAppAdapter(Context context,List<TopicObject> objects) {
			super(context, 0, objects);
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TopicObject currObject=this.getItem(position);
			ViewHolder viewHolder=null;
			if(convertView==null){
				convertView=layoutInflater.inflate(R.layout.reply_topic_list_view, null);
				viewHolder=new ViewHolder();
				viewHolder.subject=(TextView)convertView.findViewById(R.id.subject_textview);
				viewHolder.author=(TextView)convertView.findViewById(R.id.author_textview);
				viewHolder.publicDate=(TextView)convertView.findViewById(R.id.public_date_textview);
				convertView.setTag(viewHolder);
			}else{
				viewHolder=(ViewHolder)convertView.getTag();
			}
			viewHolder.subject.setText(currObject.subjct);
			viewHolder.author.setText(currObject.author);
			if(currObject.publicDate != null){
				viewHolder.publicDate.setText(currObject.publicDate.replace(todayString,""));
			}else{
				viewHolder.publicDate.setText("");
			}
			
			return convertView;
		}
		class ViewHolder{
			TextView subject;
			TextView author;
			TextView publicDate;
		}
    }
	private final static int ID_SHOW_REPLY_DIALOG=0;
	private final static int ID_SHOW_EDIT_REPLY_DIALOG = 1;	
	@Override
	protected Dialog onCreateDialog(int id) {
		if(id==ID_SHOW_REPLY_DIALOG){
	        final Dialog dialog = new Dialog(TopicActivity.this);
	        dialog.setContentView(R.layout.reply_view);
	        dialog.getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
	        dialog.setTitle("回复");
	        dialog.setCancelable(true);
			final EditText replyBodyEditText = (EditText)dialog.findViewById(R.id.reply_body_edittext);
			replyBodyEditText.setText("");
			final CheckBox noNameCheckbox = (CheckBox)dialog.findViewById(R.id.noname_checkbox);
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
					String replyBody=replyBodyEditText.getText().toString();
					if(StringUtils.isBlank(replyBody)){
						myApplication.showMessage("回复内容不能为空");
						return;
					}
					replyBody=replyBody.trim();
					final DoReplyObject reply = new DoReplyObject();
					reply.body = replyBody;
					if(noNameCheckbox.isChecked()){
						reply.isNoName=true;
					}else{
						reply.isNoName=false;
					}
			    	new AsyncTask<Object,Integer,String>(){
			    		@Override
			    		protected void onPreExecute() {
			    			sendBtn.setEnabled(false);
			    			cancelButton.setEnabled(false);
			    			replyBodyEditText.setEnabled(false);
			    			sendBtn.setText("发送中...");
			    		}
						@Override
						protected String doInBackground(Object... arg0) {
							String msg = "";
							try{
								RemoteServer.getServer(myApplication).reply(topic,reply);
							}catch(Exception e){
								msg =e.getMessage();
							}
					        return msg;
						}
						@Override
						protected void onPostExecute(String result) {
							sendBtn.setText("   发送   ");
			    			sendBtn.setEnabled(true);
			    			cancelButton.setEnabled(true);
			    			replyBodyEditText.setEnabled(true);
							if(StringUtils.isNotBlank(result)){
								myApplication.showMessage(result);
								return;
							}
							myApplication.showMessage("发送成功...");
							dialog.dismiss();
							TopicActivity.this.removeDialog(ID_SHOW_REPLY_DIALOG);
							doRefresh();
						}
			    		
			    	}.execute();


	            }
	        });

	        return dialog;
		} else if (id == ID_SHOW_EDIT_REPLY_DIALOG) {
			final Dialog editDialog = new Dialog(TopicActivity.this);
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
					TopicActivity.this.removeDialog(ID_SHOW_EDIT_REPLY_DIALOG);
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
							TopicActivity.this.removeDialog(ID_SHOW_EDIT_REPLY_DIALOG);
							doRefresh();
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
							TopicActivity.this.removeDialog(ID_SHOW_EDIT_REPLY_DIALOG);
							doRefresh();
						}

					}.execute();
				}
			});			
			return editDialog;
		}
		return null;
	}    
	public void showReply(){
		this.showDialog(ID_SHOW_REPLY_DIALOG);
	}
	public static final int MENU_ID_NEW_REPLY=0;
	public static final int MENU_ID_COLL = 1;
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0,MENU_ID_NEW_REPLY,0,"回复");
		menu.add(0, MENU_ID_COLL, 0, "收藏");
		return super.onCreateOptionsMenu(menu);
	}
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if(item.getItemId() == MENU_ID_NEW_REPLY){
			showReply();
		} else if (item.getItemId() == MENU_ID_COLL) {
			new AsyncTask<Object, Integer, String>() {
				@Override
				protected void onPreExecute() {
					myApplication.showMessage("执行收藏中...");
				}

				@Override
				protected String doInBackground(Object... arg0) {
					try {
						RemoteServer.getServer(myApplication).collTopic(topic.id);
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

		}
		return super.onMenuItemSelected(featureId, item);
	}	

	@Override
	public void onClick(View v) {
		if(v==menuBtn){
			this.openOptionsMenu();
		}else if(v==refreshBtn){
			doRefresh();
		}else if (v == pageDownBtn) {
			if(scrollControlState!=null){
				scrollControlState.click(this);
			}
		} else if (v == backBtn) {
			this.finish();
		}
	}
	public void doRefresh(){
		this.loadDataAndShowUI();
	}
	@Override
	public void goDown() {
		scrollView.fullScroll(View.FOCUS_DOWN);
		
	}
	@Override
	public void goTop() {
		scrollView.fullScroll(View.FOCUS_UP);		
	}
	@Override
	public void setState(ButtonState state) {
		pageDownBtn.setText(state.getButtonText());
		scrollControlState = state;
	}
}
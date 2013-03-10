package com.mopoo.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicNameValuePair;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;
import android.util.Xml;

import com.mopoo.MyApplication;
import com.mopoo.utils.StringUtils;
import com.mopoo.utils.vo.CacheObject;
import com.mopoo.utils.vo.DoNewTopicObject;
import com.mopoo.utils.vo.DoReplyObject;
import com.mopoo.utils.vo.ReplyObject;
import com.mopoo.utils.vo.TopicObject;

public class RemoteServer {
	
	public static final String REAL_MOPOO_IP = "REAL_MOPOO_IP"; //替换成真实的服务器ip,为了安全起见不直接写ip,请修改后不要提交到git上
	public static final String URI_ROOT = "https://"+REAL_MOPOO_IP;
	public static final String LOGIN_URI = URI_ROOT + "/inyourname.asp";
	public static final String TOP_TOPIC_URI = URI_ROOT + "/rss.asp?top=";
	public static final String REPLY_TOPIC_URI = URI_ROOT + "/rss.asp?ttid=";
	public static final String REPLY_URI = URI_ROOT + "/new/info2.asp";
	public static final String NEW_TOPIC_URI = URI_ROOT + "/new/fatiezi.asp";
	public static final String INDEX_URI = URI_ROOT + "/index.asp";
	private MyApplication myApplication;
	public static RemoteServer server;

	public static RemoteServer getServer(MyApplication myApplication) {
		if (server == null) {
			server = new RemoteServer(myApplication);
		}
		return server;
	}

	private RemoteServer(MyApplication myApplication) {
		this.myApplication = myApplication;
		try {
			// visitIndexIfNeed();rss模式列表和看贴不用这个，所以推迟到各自的方法了
		} catch (Exception e) {
			e.printStackTrace();
			// nothing
		}
	}

	private boolean evenVisitIndex = false;

	public void visitIndexIfNeed() throws Exception {
		if (!evenVisitIndex) {
			HttpGet httpGet = new HttpGet(INDEX_URI);
			executeRequest(httpGet, true);
			evenVisitIndex = true;
		}
	}

	public String login(String user, String pass) {
		try {
			HttpPost httpPost = new HttpPost(LOGIN_URI);
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(7);
			nameValuePairs.add(new BasicNameValuePair("h", "4544745"));
			nameValuePairs.add(new BasicNameValuePair("name", user));
			nameValuePairs.add(new BasicNameValuePair("pass1", pass));
			nameValuePairs.add(new BasicNameValuePair("B1", "登录"));
			nameValuePairs.add(new BasicNameValuePair("C1", "1"));
			nameValuePairs.add(new BasicNameValuePair("tjm", ""));
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "gb2312"));
			processRequest(httpPost);
			String rememberCookie = HttpClientHelper.getInstance(myApplication).getCurrentCookie();
			if (StringUtils.isNotBlank(rememberCookie) && rememberCookie.indexOf("yourname2") >= 0) { // 简单验证下cookie
				return rememberCookie;
			}
			throw new RuntimeException("登录失败！");
		} catch (UnknownHostException uke) {
			throw new RuntimeException("网络无效！");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static String TOPIC_LIST_HTML_URI = URI_ROOT + "/new/info.asp";

	private static String TOPIC_LIST_CACHE_FILE_NAME = "topic_list.html";

	public CacheObject<String> getTopicListHtml(boolean useCache,boolean useRemote) throws Exception {
		CacheObject<String> listCacheObject = new CacheObject<String>();
		if (useCache) {
			if (getCacheDataFromFile(TOPIC_LIST_CACHE_FILE_NAME, listCacheObject)) {
				listCacheObject.isCache = true;
				return listCacheObject;
			}
		}
		if(!useRemote){
			throw new RuntimeException("没有离线数据");
		}
		listCacheObject.isCache = false;
		listCacheObject.data = this.getTopicListHtmlFromServer();
		listCacheObject.dataDateTime = new Date();
		saveDataToCacheFile(TOPIC_LIST_CACHE_FILE_NAME, listCacheObject.data); //保存缓存
		return listCacheObject;
	}

	private boolean getCacheDataFromFile(String path, CacheObject<String> cacheObject) {
		String absolutePath = this.getAbsolutePath(path);
		File dataFile = new File(absolutePath);
		try {
			if (dataFile.exists()) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile),
						"gb2312"), 1024 * 40);
				String line = null;
				StringBuilder sb = new StringBuilder();
				while (true) {
					line = reader.readLine();
					if (line == null) {
						break;
					}
					sb.append(line);
					sb.append("\n");
				}
				cacheObject.data = sb.toString();
				cacheObject.dataDateTime = new Date(dataFile.lastModified());
				sb = null;
				return true;
			}
		} catch (Exception e) {
			cacheObject.errorMsg = e.getMessage();
			Log.e("mopoo", "得到缓存文件时出错:" + e.getMessage());
		}
		return false;
	}

	private boolean saveDataToCacheFile(String path, String data) {
		boolean isWrite = false;
		String localAbsolutePath = this.getAbsolutePath(path);
		OutputStreamWriter out = null;
		File parent = new File(localAbsolutePath).getParentFile();
		if (!parent.exists()) {
			parent.mkdirs();
		}
		try {
			out = new OutputStreamWriter(new FileOutputStream(new File(localAbsolutePath)), "gb2312");
			out.write(data);
			isWrite = true;
		} catch (Exception e) {
			Log.e("mopoo", "写入缓存数据时出错:" + e.getMessage());
		}
		closeStream(out);
		return isWrite;
	}

	public String getTopicListHtmlFromServer() throws Exception {
		visitIndexIfNeed();
		HttpGet request = new HttpGet(TOPIC_LIST_HTML_URI);
		InputStream is = getInputStreamFromRequest(request);
		StringBuilder serverSb = new StringBuilder(1024 * 30);
		BufferedReader reader = new BufferedReader(new InputStreamReader(is, "gb2312"), 1024 * 40);
		String line = null;
		boolean start = false;
		boolean isValidTopicListHtml = false;
		try {
			while ((line = reader.readLine()) != null) {
				if(!isValidTopicListHtml){
					if(line.indexOf("info2.asp?id=")>=0){ //判断，如果有这一句当作是正常的列表
						isValidTopicListHtml = true;
					}
				}
				if (line.startsWith("[<span style=")) {
					start = true;
					serverSb.append(StringUtils.getSubStringFromStart(line, "</font>帖<p>"));
					serverSb.append("\n");
					continue;
				}
				if (start && line.startsWith("<SCRIPT LANGUAGE=\"JavaScript\">")) {
					break;
				}
				serverSb.append(line);
				serverSb.append("\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (is != null) {
					is.close();
				}
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(!isValidTopicListHtml){
			throw new RuntimeException("帖子列表读取错误,服务器返回:"+serverSb.toString());
		}
		StringBuilder sb = new StringBuilder(1024 * 35);
		sb.append(serverSb.toString());
		sb.append(myApplication.getLeftCss());		
		sb.append(myApplication.getFixJavascript());
		return sb.toString();
	}

	private static String TOPIC_HTML_URI = URI_ROOT + "/new/info2.asp?id=";

	public CacheObject<String> getTopicHtml(String id, boolean useCache,boolean useRemote) throws Exception {
		return getTopicHtml(id, useCache, useRemote,false);
	}
	private static String TOPIC_CACHE_PRE_FILE_NAME = "topic-";

	private String getTopicCacheFilePath(String id) {
		return TOPIC_CACHE_PRE_FILE_NAME + id;
	}
	public CacheObject<String> getTopicHtmlOfPlay(String id) throws Exception {
		return this.getTopicHtml(id,false,true,true);
	}
	private CacheObject<String> getTopicHtml(String id,boolean useCache,boolean useRemote, boolean isViewPay) throws Exception {
		CacheObject<String> topicCacheObject = new CacheObject<String>();
		String cachePath = this.getTopicCacheFilePath(id);		
		if (useCache) {
			if (getCacheDataFromFile(cachePath, topicCacheObject)) {
				topicCacheObject.isCache = true;
				return topicCacheObject;
			}
		}
		if(!useRemote){
			throw new RuntimeException("没有离线数据");
		}
		topicCacheObject.isCache = false;
		topicCacheObject.data = this.getTopicHtmlFromServer(id, isViewPay);
		topicCacheObject.dataDateTime = new Date();
		saveDataToCacheFile(cachePath,topicCacheObject.data); //保存缓存
		return topicCacheObject;
	}
	public String getInfoNew(String url) throws Exception {
		visitIndexIfNeed();
		url = URI_ROOT + url;
		HttpGet get = new HttpGet(url);
		String html = this.executeRequest(get);
		html = StringUtils.getSubStringFromStart(html, "</form>");
		if (html == null) {
			throw new RuntimeException("返回数据格式有误");
		}
		return myApplication.getRigthcss() + html + myApplication.getFixJavascript();

	}
	
	public String search(String key , String searchType){
		try {
			visitIndexIfNeed();
			HttpPost httpPost = new HttpPost(URI_ROOT + "/new/msearch.asp");
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(7);
			nameValuePairs.add(new BasicNameValuePair("T1", key));
			nameValuePairs.add(new BasicNameValuePair("D1", searchType));
			nameValuePairs.add(new BasicNameValuePair("B1", "查找"));
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "gb2312"));
			String html = executeRequest(httpPost, true);
			html = StringUtils.getSubStringFromStart(html, "</form>");
			if (html == null) {
				throw new RuntimeException("返回数据格式有误");
			}
			return myApplication.getRigthcss() + html + myApplication.getFixJavascript();
		} catch (UnknownHostException uke) {
			throw new RuntimeException("network invalid");
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}   
	}
	
	public String getTopicHtmlFromServer(String id) throws Exception {
		return this.getTopicHtmlFromServer(id, false);
	}

	
	
	public String getTopicHtmlFromServer(String id, boolean isViewPay) throws Exception {
		visitIndexIfNeed();
		String url = TOPIC_HTML_URI + id;
		if (isViewPay) {
			url = url + "&lmck=1";
		}
		HttpGet get = new HttpGet(url);
		String html = this.executeRequest(get);
		html = StringUtils.getMiddleString(html, "</title>", "<script ");
		if (html == null) {
			throw new RuntimeException("返回帖子的数据格式有误");
		}

		return myApplication.getRigthcss() + html + myApplication.getFixJavascript();
	}
	private static String CHAT_HTML_URI = URI_ROOT + "/new/mmm.asp";	
	public String getChatHtml() throws Exception {
		visitIndexIfNeed();
		String url = CHAT_HTML_URI;
		
		HttpGet get = new HttpGet(url);
		String html = this.executeRequest(get);
		html = StringUtils.getMiddleString(html, "<body topmargin=\"5\" vlink=\"#008080\">", "<script language=\"JavaScript\">");
		if (html == null) {
			throw new RuntimeException("返回留言板内容有误");
		}

		return this.getChatCssString() + html;
	}
	public void sendChatMessage(String message) {
		try {
			visitIndexIfNeed();
			HttpPost httpPost = new HttpPost(CHAT_HTML_URI);
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(7);
			nameValuePairs.add(new BasicNameValuePair("B1", "OK"));
			nameValuePairs.add(new BasicNameValuePair("T1",message));
			nameValuePairs.add(new BasicNameValuePair("T2",message));
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "gb2312"));
			String result = executeRequest(httpPost, true);
			if (StringUtils.isNotBlank(result)) {
				return;
			}
			throw new RuntimeException("留言失败！");
		} catch (UnknownHostException uke) {
			throw new RuntimeException("network invalid");
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}	
	
	private String getChatCssString(){
		return "<style>BODY  {font-size : 12pt; line-height: 20px;} a { text-decoration : none;} a:hover { text-decoration: underline;}</style> ";
	}
	public List<TopicObject> getTopTopicList(int count) throws Exception {
		String uri = TOP_TOPIC_URI + count;
		InputStream is = this.getInputStreamFromRequest(new HttpGet(uri));
		try {
			return getTopicsFromInputStream(is);
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	public static List<TopicObject> getTopTopicListForWidget(int count) throws Exception {
		RemoteServer server = new RemoteServer(null);
		return server.getTopTopicList(count);
	}

	public List<TopicObject> getReplyTopicList(String id) throws Exception {
		String uri = REPLY_TOPIC_URI + id.trim();
		InputStream is = this.getInputStreamFromRequest(new HttpGet(uri));
		try {
			return getTopicsFromInputStream(is);
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	public void newTopic(DoNewTopicObject newTopic) {
		try {
			visitIndexIfNeed();
			HttpPost httpPost = new HttpPost(NEW_TOPIC_URI);
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(7);
			nameValuePairs.add(new BasicNameValuePair("title", newTopic.subject));
			nameValuePairs.add(new BasicNameValuePair("Rqz", "0"));
			nameValuePairs.add(new BasicNameValuePair("acgs", "no"));
			nameValuePairs.add(new BasicNameValuePair("neirong", newTopic.body.replaceAll("\n", "<br>")));
			nameValuePairs.add(new BasicNameValuePair("huifubz", "1"));
			nameValuePairs.add(new BasicNameValuePair("ttype", "普通"));

			if (newTopic.isNoName) {
				nameValuePairs.add(new BasicNameValuePair("nimin", "1"));
			}
			nameValuePairs.add(new BasicNameValuePair("B1", "按此处发帖"));
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "gb2312"));
			String result = executeRequest(httpPost, true);
			if (StringUtils.isNotBlank(result)) {
				return;
			}
			throw new RuntimeException("发帖失败！");
		} catch (UnknownHostException uke) {
			throw new RuntimeException("network invalid");
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public void reply(TopicObject mainTopic, DoReplyObject doReplyObject) {
		try {
			visitIndexIfNeed();
			HttpPost httpPost = new HttpPost(REPLY_URI);
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(7);
			nameValuePairs.add(new BasicNameValuePair("h", "34236367464234525"));
			// nameValuePairs.add(new BasicNameValuePair("Rqz", "0"));
			// nameValuePairs.add(new BasicNameValuePair("address", "http://"));
			// nameValuePairs.add(new BasicNameValuePair("address2",
			// "http://"));
			// nameValuePairs.add(new BasicNameValuePair("face1", "0"));
			// nameValuePairs.add(new BasicNameValuePair("face2", "0"));
			// nameValuePairs.add(new BasicNameValuePair("face3", "0"));
			// nameValuePairs.add(new BasicNameValuePair("papapa", "2"));
			// nameValuePairs.add(new BasicNameValuePair("xzsl", ""));
			// nameValuePairs.add(new BasicNameValuePair("zhuid", ""));
			// nameValuePairs.add(new BasicNameValuePair("zmppm", ""));
			nameValuePairs.add(new BasicNameValuePair("id", mainTopic.id));
			nameValuePairs.add(new BasicNameValuePair("neirong2", doReplyObject.body.replaceAll("\n", "<br>")));
			if (doReplyObject.isNoName) {
				nameValuePairs.add(new BasicNameValuePair("nimin", "1"));
			}
			if (StringUtils.isNotBlank(doReplyObject.lc)&&StringUtils.isNotBlank(doReplyObject.lcText)){
				nameValuePairs.add(new BasicNameValuePair("lc", doReplyObject.lc));
				nameValuePairs.add(new BasicNameValuePair("neirongy", doReplyObject.lcText));
			}

			nameValuePairs.add(new BasicNameValuePair("B1", "提交回复"));
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "gb2312"));
			String result = executeRequest(httpPost, true);
			if (StringUtils.isNotBlank(result)) {
				return;
			}
			throw new RuntimeException("回复失败！");
		} catch (UnknownHostException uke) {
			throw new RuntimeException("network invalid");
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public void saveEditReply(ReplyObject object) {
		try {
			visitIndexIfNeed();
			HttpPost httpPost = new HttpPost(REPLY_URI);
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(7);
			nameValuePairs.add(new BasicNameValuePair("h", "23468845456"));
			nameValuePairs.add(new BasicNameValuePair("id", object.topicId));
			nameValuePairs.add(new BasicNameValuePair("id2", object.id));
			nameValuePairs.add(new BasicNameValuePair("neirong", object.body.replaceAll("\n", "<br>")));
			nameValuePairs.add(new BasicNameValuePair("B1", "修改"));
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "gb2312"));
			String result = executeRequest(httpPost, true);
			if (StringUtils.isNotBlank(result)) {
				return;
			}
			throw new RuntimeException("修改回复失败！");
		} catch (UnknownHostException uke) {
			throw new RuntimeException("网络无效");
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public void delReply(ReplyObject object) {
		try {
			visitIndexIfNeed();
			HttpGet get = new HttpGet(URI_ROOT + "/new/infonewdel.asp?delhf=1&sid="+object.topicId+"&id="+object.id);
			String html = this.executeRequest(get);
			if (html != null && html.indexOf("删除成功") != -1) {
				return;
			}
			throw new RuntimeException("删除失败！");
		} catch (UnknownHostException uke) {
			throw new RuntimeException("网络无效");
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public ReplyObject getReply(String topicId, String replyid) throws Exception {
		visitIndexIfNeed();
		String uri = URI_ROOT + "/new/editlthf.asp?id=" + topicId + "&id2=" + replyid;
		String html = this.executeRequest(new HttpGet(uri));
		if (StringUtils.isNotBlank(html)) {
			String content = StringUtils.getMiddleString(html, "<textarea rows='10' name='neirong' cols='38'>",
					"</textarea>");
			if (StringUtils.isNotBlank(content)) {
				ReplyObject obj = new ReplyObject();
				obj.topicId = topicId;
				obj.id = replyid;
				obj.body = content;
				return obj;
			}
		}
		throw new RuntimeException("没有相关信息");
	}
	
	public ReplyObject getOtherUserReplayByURL(String url) throws Exception{
		visitIndexIfNeed();
		String uri = URI_ROOT + url;
		String html = this.executeRequest(new HttpGet(uri));
		if (StringUtils.isNotBlank(html)) {
			String lc = StringUtils.getMiddleString(html, "name='lc' value=",
					" >");
			String text = StringUtils.getMiddleString(html, "<textarea rows='4' name='neirongy' cols='36' onkeydown=ctlent2()>",
					"</textarea>");
			if (lc != null && text !=null){
				ReplyObject obj = new ReplyObject();
				obj.lc = lc;
				obj.body = text;
				return obj;
			}
		}
		throw new RuntimeException("没有相关信息,或者里屋的限制：如果前面回复字数太少需过会才能再回复");
	}

	private static String COLL_TOPIC_URI = URI_ROOT + "/new/ltcollect.asp?collid=";

	public void collTopic(String id) throws Exception {
		visitIndexIfNeed();
		HttpGet request = new HttpGet(COLL_TOPIC_URI + id);
		String result = this.executeRequest(request);
		if (result.indexOf("已添加到收藏夹") >= 0) {
			return;
		} else {
			throw new RuntimeException("失败，信息如下:" + result);
		}
	}

	public List<TopicObject> getTopicsFromInputStream(InputStream is) {
		RssHandler handler = new RssHandler();
		try {
			InputStreamReader streamReader = new InputStreamReader(is, "gb2312");
			Xml.parse(streamReader, handler);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return handler.getTopics();
	}

	public class RssHandler extends DefaultHandler {
		private List<TopicObject> topics;
		private TopicObject currentTopic;
		private StringBuilder builder;

		public List<TopicObject> getTopics() {
			return this.topics;
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			super.characters(ch, start, length);
			builder.append(ch, start, length);
		}

		static final String AUTHOR = "author";
		static final String DESCRIPTION = "description";
		static final String LINK = "link";
		static final String TITLE = "title";
		static final String ITEM = "item";

		@Override
		public void endElement(String uri, String localName, String name) throws SAXException {
			super.endElement(uri, localName, name);
			if (this.currentTopic != null) {
				String value = builder.toString();
				if (value != null) {
					value = value.trim();
				}
				if (localName.equalsIgnoreCase(TITLE)) {
					currentTopic.subjct = value;
				} else if (localName.equalsIgnoreCase(LINK)) {
					currentTopic.link = value;
					if (StringUtils.isNotBlank(currentTopic.link)) {
						int posi = currentTopic.link.lastIndexOf("=");
						if (posi >= 0) {
							currentTopic.id = currentTopic.link.substring(posi + 1, currentTopic.link.length());
						}
					}
				} else if (localName.equalsIgnoreCase(DESCRIPTION)) {
					currentTopic.body = value;
					if (StringUtils.isNotBlank(currentTopic.body)) {
						int posi = currentTopic.body.indexOf("(发帖时间:");
						if (posi >= 0) {
							int posiEnd = currentTopic.body.indexOf(")", posi);
							if (posiEnd >= 0) {
								currentTopic.publicDate = currentTopic.body.substring(posi + 6, posiEnd);
							}
						}
					}
				} else if (localName.equalsIgnoreCase(AUTHOR)) {
					currentTopic.author = value;
					if (StringUtils.isNotBlank(currentTopic.author)) {
						int posi = currentTopic.author.indexOf("【");
						if (posi >= 0) {
							currentTopic.author = currentTopic.author.substring(0, posi); // 去除签名
						}
					}
				} else if (localName.equalsIgnoreCase(ITEM)) {
					topics.add(currentTopic);
				}
			}
			builder.setLength(0);
		}

		@Override
		public void startDocument() throws SAXException {
			super.startDocument();
			topics = new ArrayList<TopicObject>();
			builder = new StringBuilder();
		}

		@Override
		public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
			super.startElement(uri, localName, name, attributes);
			if (localName.equalsIgnoreCase(ITEM)) {
				this.currentTopic = new TopicObject();
			}
		}
	}

	private HttpResponse processRequest(HttpRequestBase request) throws Exception {
		request.setHeader("Host", "www.253874.com");
		request.setHeader("Referer", "https://www.253874.com/inyourname.asp");
		request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:2.0.1) Gecko/20100101 Firefox/4.0.1");
		request.addHeader("Accept-Encoding", "gzip");
//		request.addHeader("Keep-Alive", "60");
//		request.addHeader("Connection", "keep-alive");
		request.setHeader("Cookie", HttpClientHelper.getInstance(myApplication).getCurrentCookie());
		if (request instanceof HttpPost) {
			request.setHeader("Content-Type", "application/x-www-form-urlencoded");
		}
		Log.i("mopoo", "请求服务器:" + request.getURI());
		return HttpClientHelper.getInstance(myApplication).execute(request);
	}

	private InputStream getInputStreamFromRequest(HttpRequestBase request) throws Exception {
		HttpResponse httpResponse = processRequest(request);
		HttpEntity entity = httpResponse.getEntity();
		if (entity != null) {
			InputStream instream = entity.getContent();
			Header contentEncoding = httpResponse.getFirstHeader("Content-Encoding");
			if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
				instream = new GZIPInputStream(instream);
			}
			return instream;
		}
		throw new RuntimeException("从服务器得到数据失败，未知原因");
	}

	public String executeRequest(HttpRequestBase request) throws Exception {
		return this.executeRequest(request, true);
	}

	public String executeRequest(HttpRequestBase request, boolean needReturnResult) throws Exception {
		long start = System.currentTimeMillis();
		InputStream instream = getInputStreamFromRequest(request);
		if (!needReturnResult) {
			return null;
		}
		String httpResult = null;
		try {
			httpResult = convertStreamToString(instream);
			long end = System.currentTimeMillis();
			Log.v("mopoo", "请求" + request.getURI() + "共用:" + (end - start));
		} finally {
			if (instream != null) {
				instream.close();
			}
		}
		return httpResult;
	}

	public static String convertStreamToString(InputStream is) throws UnsupportedEncodingException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is, "gb2312"), 1024 * 35);
		StringBuilder sb = new StringBuilder(1024 * 35);

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		return sb.toString();
	}

	private String sdcardRootPath;

	public String getExternalStoragePath() {
		if (StringUtils.isNotBlank(sdcardRootPath)) {
			return sdcardRootPath;
		}
		String state = android.os.Environment.getExternalStorageState();
		if (android.os.Environment.MEDIA_MOUNTED.equals(state)) {
			if (android.os.Environment.getExternalStorageDirectory().canWrite()) {
				sdcardRootPath = android.os.Environment.getExternalStorageDirectory().getPath();
				return sdcardRootPath;
			} else {
				new RuntimeException("sdcard不可写");
			}
		} else {
			new RuntimeException("sdcard无效");
		}

		return null;
	}

	private String getAppFileRootPath() {
		String root = this.getExternalStoragePath() + "/mopoo_temp";
		File rootFile = new File(root);
		if (!rootFile.exists()) {
			rootFile.mkdirs();
		}
		return root;
	}

	private String getAbsolutePath(String path) {
		String realPath = this.getAppFileRootPath() + "/" + path.replace('/', '-');
		return realPath;
	}

	public String downloadToLocal(String path, boolean downlaodFromNetwork) throws Exception {
		if (path.startsWith("https://")) {
			path = path.substring(path.indexOf("/", 9));
		}
		if (path.startsWith("file:///mnt/sdcard")) {
			path = path.substring("file:///mnt/sdcard".length());
		}
		if (path.startsWith("file:///mnt")) {
			path = path.substring("file:///mnt".length());
		}
		if (path.startsWith("file://")) {
			path = path.substring("file://".length());
		}
		String localAbsolutePath = this.getAbsolutePath(path);
		if (new File(localAbsolutePath).exists()) {
			return localAbsolutePath;
		}
		if (!downlaodFromNetwork) {
			return "";
		}
		String serverUri = path;
		if (serverUri.startsWith("/")) {
			serverUri = URI_ROOT + serverUri;
		} else {
			serverUri = URI_ROOT + "/" + serverUri;
		}
		Log.i("mopoo", "下载:" + serverUri);
		HttpGet request = new HttpGet(serverUri);
		InputStream is = getInputStreamFromRequest(request);
		BufferedInputStream in = null;
		BufferedOutputStream out = null;
		try {
			File parent = new File(localAbsolutePath).getParentFile();
			if (!parent.exists()) {
				parent.mkdirs();
			}
			in = new BufferedInputStream(is, 20 * 1024);
			out = new BufferedOutputStream(new FileOutputStream(new File(localAbsolutePath)), 1024 * 20);
			byte[] buffer = new byte[1024];
			int read = 0;
			while (true) {
				read = in.read(buffer);
				if (read == -1) {
					break;
				}
				out.write(buffer, 0, read);
			}
			out.flush();
			if (new File(localAbsolutePath).exists()) {
				Log.v("mopoo", new File(localAbsolutePath).getAbsolutePath());
			}
			return localAbsolutePath;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		} finally {
			closeStream(in);
			closeStream(out);
		}
	}

	private void closeStream(Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
				Log.e("appscompare", e.getMessage());
			}
		}
	}
}

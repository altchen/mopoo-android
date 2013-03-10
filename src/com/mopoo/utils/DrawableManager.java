package com.mopoo.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;

import com.mopoo.utils.vo.TopicObject;

public class DrawableManager {
	private final HashMap<String, Drawable> drawableMap;
	private static DrawableManager self;
	public synchronized static DrawableManager getInstance() {
		if (self == null) {
			self = new DrawableManager();
		}
		return self;
	}

	private DrawableManager() {
		drawableMap = new HashMap<String, Drawable>();
	}
    public void downloadFile(String url,String savePath){
    	BufferedInputStream in=null;
    	BufferedOutputStream out=null;
        try {
        	File parent=new File(savePath).getParentFile();
        	if(!parent.exists()){
        		parent.mkdirs();
        	}
            in = new BufferedInputStream(new URL(url).openStream(), 4 * 1024);
            out=new BufferedOutputStream(new FileOutputStream(new File(savePath)));
            byte [] buffer=new byte [1024];
            int read=0;
            while(true){
            	read=in.read(buffer);
            	if(read==-1){
            		break;
            	}
            	out.write(buffer, 0, read);
            }
            out.flush();
        }catch(Exception e){
        	e.printStackTrace();
        	throw new RuntimeException(e.getMessage());
        }finally{
        	closeStream(in);
        	closeStream(out);
        }
    }
    private  void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
              Log.e("appscompare", e.getMessage());
            }
        }
    }
    private final static String ICON_ROOT=android.os.Environment.getExternalStorageDirectory()+"/.appscompare/icons";
    public static String getIconFilePath(TopicObject topicObject){
    	return ICON_ROOT+"/"+topicObject.id+".png";
    }
    public String getIconHttpPath(TopicObject topicObject){
		return "http://cache.cyrket.com/p/android/" + topicObject.id+ "/icon";
    }
    public Drawable getDrawableFromFile(TopicObject appObject){
    	try{
	    	String filePath=getIconFilePath(appObject);
	    	File file=new File(filePath);
	    	if(!file.exists()||file.isDirectory()){
	    		return null;
	    	}else{
	    		return Drawable.createFromPath(filePath);
	    	}
	    }catch(Exception e){
	    	return null;
	    }
    }
    public Drawable getDrawableFromSystemFile(Context context,TopicObject appObject){
    	try{
	    	return Drawable.createFromStream(context.openFileInput(appObject.id+".png"),"");
	    }catch(Exception e){
	    	return null;
	    }
    }    
	public Drawable fetchDrawableFromNetwork(TopicObject appObject) {		
		String urlString =getIconHttpPath(appObject);
		BufferedInputStream is=null;
		try {
			is=new BufferedInputStream(new URL(urlString).openStream(), 4 * 1024);
		} catch (Exception e) {
			return null;
		}
		BufferedOutputStream out=null;
		Drawable drawable=null;
		try{
			String savePath=getIconFilePath(appObject);
			File file=new File(savePath);
			if(!file.getParentFile().exists()){
				file.getParentFile().mkdirs();
			}
			out=new BufferedOutputStream(new FileOutputStream(file));
			
	        byte [] buffer=new byte [1024];
	        int read=0;
	        while(true){
	        	read=is.read(buffer);
	        	if(read==-1){
	        		break;
	        	}
	        	out.write(buffer, 0, read);
	        }
	        is.close();
	        out.flush();
	        out.close();
	        drawable=Drawable.createFromPath(savePath);	
		}catch(Exception e){
			try{
				drawable = Drawable.createFromStream(is, "src");
			}catch(Exception e2){
				return null;
			}
			
		}
		drawableMap.put(appObject.id, drawable);
		return drawable;
	
	}
	
	public void fetchDrawableOnThread(Context context,final TopicObject appObject,
			final ImageView imageView,boolean needLoadNetwork) {
		if (drawableMap.containsKey(appObject.id)) {
			imageView.setImageDrawable(drawableMap.get(appObject.id));
			return;
		}
		Drawable drawable=this.getDrawableFromSystemFile(context, appObject);
		if(drawable==null){
			drawable=this.getDrawableFromFile(appObject);
		}
		if(drawable!=null){
			imageView.setImageDrawable(drawable);
			drawableMap.put(appObject.id, drawable);			
			return;			
		}
		if(!needLoadNetwork){
			return;
		}
		final Handler handler = new Handler() {
			@Override
			public void handleMessage(Message message) {
				if(message.obj!=null){
					imageView.setImageDrawable((Drawable) message.obj);
				}
			}
		};
		Thread thread = new Thread() {
			@Override
			public void run() {
				Drawable drawable = fetchDrawableFromNetwork(appObject);
				Message message = handler.obtainMessage(1, drawable);
				handler.sendMessage(message);
			}
		};
		thread.start();
	}

	public InputStream fetch(String urlString) throws MalformedURLException,
			IOException {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet request = new HttpGet(urlString);
		HttpResponse response = httpClient.execute(request);
		return response.getEntity().getContent();
	}

	public static void saveDrawableToFile(TopicObject obj, Drawable drawable) {
		Bitmap  bitmap = drawableToBitmap(drawable);
		try {
			String savePath = getIconFilePath(obj);
        	File parent=new File(savePath).getParentFile();
        	if(!parent.exists()){
        		parent.mkdirs();
        	}
			FileOutputStream fos=new FileOutputStream(new File(savePath));
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
			fos.close();
		} catch (Exception e) {
			Log.e("appshare", e.getMessage());
		}		 
	}
	public static void saveDrawableToSystemFile(Context context,TopicObject obj, Drawable drawable) {
		Bitmap  bitmap = drawableToBitmap(drawable);
		try {
			FileOutputStream fos=context.openFileOutput(obj.id+".png",Context.MODE_PRIVATE);
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
			fos.close();
		} catch (Exception e) {
			Log.e("appshare", e.getMessage());
		}		 
	}
	public static void deleteSystemFileDrawable(Context context,String packageName) {
		try {
			context.deleteFile(packageName+".png");
		} catch (Exception e) {
			Log.e("appshare", e.getMessage());
		}		 
	}	
	public static Bitmap drawableToBitmap(Drawable drawable) {  
        
        Bitmap bitmap = Bitmap  
                        .createBitmap(  
                                        drawable.getIntrinsicWidth(),  
                                        drawable.getIntrinsicHeight(),  
                                        drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888  
                                                        : Bitmap.Config.RGB_565);  
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());  
        drawable.draw(canvas);  
        return bitmap;  
} 
}

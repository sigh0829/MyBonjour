package com.audric.bonjour;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class WebServiceClient {
	static final int MAX_NB_MADAMES = 200;
	private static final String TAG = WebServiceClient.class.getSimpleName();
	public static String ip = "192.168.1.67";//"192.168.2.200"; "10.5.18.228"  192.168.2.70
	public static String port = "8080";
	
	private static boolean suffixesHasChanged = false;
	


	private static ArrayList<String> suffixes = new ArrayList<String>();

	private static WebServiceClient instance = null;

	public static String prefix =  "http://" + ip + ":" + port;
	private static  String urlGetAvailableMadameOnServer = prefix + "/all_images/FULL";




	public interface OnSuffixesLoadingListener {
		public abstract void suffixesLoadingFinished(boolean isOK, ArrayList<String> suffixes);
	}

	public interface OnUrlsLoadingListener {
		public abstract void urlsLoadingFinished(boolean isOK, ArrayList<String> urls);
	}
	
	public static void setSuffixesHasChanged(boolean suffixesHasChanged) {
		WebServiceClient.suffixesHasChanged = suffixesHasChanged;
	}

	
	public static boolean getSuffixesHasChanged() {
		return suffixesHasChanged;
	}
	

	
	

	public static WebServiceClient getInstance() {
		if (instance== null) {
			instance = new WebServiceClient();
		}
		return instance;
	}




	private String getAvailableMadameOnServer() {
		HttpResponse response;
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet(urlGetAvailableMadameOnServer);
		try {
			StringBuilder builder = new StringBuilder();
			response =  client.execute(request);

			InputStream in = response.getEntity().getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in), 65536);
			String line;
			while ( (line = reader.readLine()) != null) {
				builder.append(line);
			}

			setSuffixesHasChanged(true);
			in.close();
			return builder.toString();

		}
		catch (IOException io) {
			io.printStackTrace();
		}

		return null;
	}



	public void retrieveMadamesUrlsInThread(final OnUrlsLoadingListener callback) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				ArrayList<String> urls = getAvailableMadameOnServerURL();
				if(callback!=null) {
					Log.d(TAG, "urls in webserviceclient:"+ (urls==null?"NULL":"not null"));
					if(urls!=null) {
						callback.urlsLoadingFinished(true, urls);
					}
					else {
						callback.urlsLoadingFinished(false, null);
					}
				}
				else
					Log.e(TAG, "urlsListener not set!");
			}
		}).start();

	}




	public static ArrayList<String> getSuffixes() {
		return suffixes;
	}




	private ArrayList<String> getAvailableMadameOnServerSuffix() {
		if(suffixes == null || (suffixes!=null && suffixes.isEmpty() )) {
			String madamesAsString = getAvailableMadameOnServer();
			suffixes = new ArrayList<String>();
			if(madamesAsString != null) { 
				try {
					JSONObject jsonobj = new JSONObject(madamesAsString);
					JSONArray array = jsonobj.getJSONArray("all_madames");

					if(array!=null) {
						for (int i = 0; i < MAX_NB_MADAMES && i<array.length(); i++) {
							suffixes.add(array.getString(i));
						}
					}  

					return suffixes;  
				} catch (JSONException e) {
					e.printStackTrace();
					return null;
				}
			}
			else
				return null;
		}
		else {
			return suffixes;
		}


	}


	private ArrayList<String> getAvailableMadameOnServerURL() {
		ArrayList<String> urls = new ArrayList<String>();

		suffixes = getAvailableMadameOnServerSuffix();
		int indice = 0;
		if(suffixes!=null && !suffixes.isEmpty()) {
			while(indice < MAX_NB_MADAMES && indice < suffixes.size()) {
				urls.add(prefix + suffixes.get(indice));
				indice++;
			}
		}
		else {
			return null;
		}
		return urls;
	}



	public static void setIp(String ip) {
		WebServiceClient.ip = ip;
		prefix =  "http://" + ip + ":" + port;
		urlGetAvailableMadameOnServer = prefix + "/all_images/FULL";
	}




	public void resetSuffixes() {
		suffixes = null;
	}



}

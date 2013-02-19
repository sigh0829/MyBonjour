package com.audric.bonjour;

import java.util.ArrayList;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class PreferencesManager {
	private static final String TAG = PreferencesManager.class.getSimpleName();
	private SharedPreferences preferences = null;

	public static final String PREF_FILENAME = "MyBonjourPrefs";
	public static final String IMAGES_URLS_PREFIX = "imageSuffixes.";

	public PreferencesManager(Context context) {
		preferences = context.getSharedPreferences(PREF_FILENAME, 0);
	}


	public ArrayList<String> getUrlsFromPref() {
		ArrayList<String> urls = new ArrayList<String>();
		Map<String, ?> presetDataMap = preferences.getAll();

		int indice = 0;
		Log.e(TAG, "size of pref! "+ presetDataMap.keySet().size());
		for (Object key : presetDataMap.keySet()) {
			String key_s = (String) key;
			int indiceToAdd = 0;
			Log.d(TAG, "key:"+key_s);
			if(indice <= WebServiceClient.MAX_NB_MADAMES && key_s.startsWith(IMAGES_URLS_PREFIX)) {
				indiceToAdd = Integer.parseInt(key_s.substring(key_s.indexOf('.')+1));
				if(indiceToAdd>urls.size())
					indiceToAdd = urls.size();

				Log.d(TAG, "loading prefs:"+indice+" value:" + presetDataMap.get(key));
				urls.add(indiceToAdd, WebServiceClient.prefix + presetDataMap.get(key));
				indice++;

			}
		}

		if (urls.isEmpty())
			urls = null;
		return urls;
	}

	public void setSuffixesToPref(ArrayList<String> suffixes) {
		if( suffixes!=null) {
			SharedPreferences.Editor editor = preferences.edit();

			Map<String, ?> presetDataMap = preferences.getAll();

			for (Object key : presetDataMap.keySet()) {
				String key_s = (String) key;
				if(key_s.startsWith(IMAGES_URLS_PREFIX)) {
					//Log.d(TAG, "erasing prefs: value:" + presetDataMap.get(key));
					editor.remove(key_s);
				}
			}
			editor.commit();

			int i = 0; 

			for(String suffixe : suffixes) {
				Log.d(TAG, "saving : " + IMAGES_URLS_PREFIX + i+ " as " +suffixe);
				editor.putString(IMAGES_URLS_PREFIX + i, suffixe);
				i++;
			}
			editor.commit();
		}
	}

	public void setIpToPref(String ip) {
		if(ip != null) {
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString("ip", ip);
			editor.commit();
		}
	}

	public String getIpFromPref() {
		return preferences.getString("ip", null);
	}
}

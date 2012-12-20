package com.audric.bonjour;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

class ThumbnailsAdapter extends BaseAdapter {
	

    private Context mContext; 
    private ArrayList<Bitmap> photos = new ArrayList<Bitmap>();

    public ThumbnailsAdapter(Context context) { 
        mContext = context; 
    } 

    public void addBitmap(Bitmap bitmap) { 
        photos.add(bitmap); 
    } 

    public int getCount() { 
        return photos.size(); 
    } 

    public Object getItem(int position) { 
        return photos.get(position); 
    } 

    public long getItemId(int position) { 
        return position; 
    } 

    public View getView(int position, View convertView, ViewGroup parent) { 
        final ImageView imageView; 
        if (convertView == null) { 
            imageView = new ImageView(mContext); 
        } else { 
            imageView = (ImageView) convertView; 
        } 
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setPadding(2, 2, 2, 2);
        imageView.setImageBitmap(photos.get(position));
        return imageView; 
    } 
}

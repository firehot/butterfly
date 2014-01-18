package com.butterfly.adapter;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.butterfly.R;
import com.loopj.android.image.SmartImageView;

public class GridViewAdapter extends BaseAdapter
{
    private List<String> items = new ArrayList<String>();
    private LayoutInflater inflater;
    private final Activity context;
    
    static class ViewHolder {

		public SmartImageView imageView;
	}

    public GridViewAdapter(Activity context)
    {
        inflater = LayoutInflater.from(context);
        this.context = context;

    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public String getItem(int i)
    {
        return items.get(i);
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup)
    {
		View rowView = view;
		if (rowView == null) {
			
			rowView = inflater.inflate(R.layout.gridview_item, null);
			ViewHolder viewHolder = new ViewHolder();
			
			viewHolder.imageView = (SmartImageView) rowView
					.findViewById(R.id.stream_image_video);

			rowView.setTag(viewHolder);
		}

		ViewHolder holder = (ViewHolder) rowView.getTag();
		String file_url = this.context.getString(R.string.image_url)+getItem(i);

		holder.imageView.setImageUrl(file_url);
		
		return rowView;
    }

	@Override
	public long getItemId(int position) {
		
		return 0;
	}
	
	public void addAll(ArrayList<String> videoList)
	{
		this.items.addAll(videoList);
	}
	
	public void clear()
	{
		this.items.clear();
	}
}

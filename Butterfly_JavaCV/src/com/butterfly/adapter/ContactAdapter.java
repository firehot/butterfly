package com.butterfly.adapter;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.provider.ContactsContract.Data;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.butterfly.R;
import com.butterfly.adapter.ContactAdapter.Contact;

public class ContactAdapter extends ArrayAdapter<ContactAdapter.Contact> {
	
	public static class ViewHolder {
		TextView displayNameView;
		TextView emailView;
		ImageView photoView;
		ImageView deleteView;
	}
	
	public static class Contact {
		public String displayName;
		public String email;
		public String photoUri;
	
		public Contact(String displayName, String email, String photoUri) {
			super();
			this.displayName = displayName;
			this.email = email;
			this.photoUri = photoUri;
		}
	
	
		@Override
		public String toString() {
			return displayName;
		}
	}

	private OnClickListener removeListener = null;
	public static final String DISPLAY_NAME = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? Data.DISPLAY_NAME_PRIMARY
	: Data.DISPLAY_NAME;

	public ContactAdapter(Context context, int resource) {
		super(context, resource);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.contact_list_item  , null);
			holder = new ViewHolder();
			holder.displayNameView = (TextView) convertView.findViewById(R.id.display_name);
			holder.emailView = (TextView) convertView.findViewById(R.id.email_address);
			holder.photoView = (ImageView) convertView.findViewById(R.id.photo_uri);
			holder.deleteView = (ImageView) convertView.findViewById(R.id.remove_contact);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		ContactAdapter.Contact item = getItem(position);
		holder.displayNameView.setText(item.displayName);
		//holder.emailView.setText(item.email);
		if (item.photoUri != null) {
			holder.photoView.setImageURI(Uri.parse(item.photoUri));
		}
		else {
			holder.photoView.setImageResource(R.drawable.ic_contact_picture_2);
		}
		
		if (this.removeListener  != null) {
			holder.deleteView.setVisibility(View.VISIBLE);
			holder.deleteView.setOnClickListener(this.removeListener);
		}
		return convertView;
	}

	public void setRemoveListener(OnClickListener Listener) {
		this.removeListener = Listener;
	}
}
package com.butterfly.fragment;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.CursorToStringConverter;
import android.widget.TextView;
import android.widget.Toast;

import com.butterfly.MainActivity;
import com.butterfly.R;
import com.butterfly.RecordActivity;

public class ContactsListFragment extends Fragment {

	public static final String MAILS_TO_BE_NOTIFIED = "mails_to_be_notified";

	SimpleCursorAdapter mAdapter;

	private ListView contactList;

	private SelectedContactAdapter selectedContactAdapter;

	private TextView searchEditText;

	public View fragmentView;

	private static String displayName = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? Data.DISPLAY_NAME_PRIMARY
			: Data.DISPLAY_NAME;

	private ArrayList<Contact> frequentContacts;

	public class Contact {
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

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.contact_list, container, false);

		searchEditText = (TextView) v.findViewById(R.id.searchText);
		contactList = (ListView) v.findViewById(R.id.selectedContactList);
		selectedContactAdapter = new SelectedContactAdapter(getActivity(),
				R.layout.contact_list_item);
		contactList.setAdapter(selectedContactAdapter);
		
		mAdapter = new FilteredContactListAdapter(getActivity(),
				R.layout.contact_list_item, null, new String[] {
			displayName, displayName, Data.PHOTO_THUMBNAIL_URI }, new int[] {
			R.id.display_name, R.id.email_address, R.id.photo_uri}, 0, true);

		frequentContacts = getFrequentContacts();
		selectedContactAdapter.addAll(frequentContacts);

		mAdapter.setCursorToStringConverter(new CursorToStringConverter() {

			@Override
			public CharSequence convertToString(Cursor cursor) {
				int index = cursor.getColumnIndex(displayName);
				return cursor.getString(index);
			}
		});
		searchEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
			@Override
			public void afterTextChanged(Editable s) {
				String text = searchEditText.getText().toString();
				if (text.length() > 1) {
					contactList.setAdapter(mAdapter);
					mAdapter.changeCursor(getFilteredContacts(text));
					System.out.println(" text :" + text);
				}
				else {
					contactList.setAdapter(selectedContactAdapter);					
				}
				
			}
		});
		
		contactList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int position,
					long id) {
				ListAdapter adapter = contactList.getAdapter();
				String email;
				if (adapter.equals(mAdapter)) {
					Cursor cursor = (Cursor) mAdapter.getItem(position);
					email = cursor.getString(cursor.getColumnIndex(Email.ADDRESS));
				}
				else {
					Contact contact = selectedContactAdapter.getItem(position);
					email = contact.email;
				}
				
				Intent intent = new Intent(getActivity(), RecordActivity.class);
				intent.putExtra(MAILS_TO_BE_NOTIFIED, email);
				startActivity(intent);
			}
			
		});
		
		fragmentView = v;
		return v;
	}

	private Cursor getFilteredContacts(CharSequence constraint) {
		String[] selectArgs = null;

		String select = 
				"(" + Email.ADDRESS + " NOTNULL " + " AND " +
						Data.MIMETYPE + " = '" + Email.CONTENT_ITEM_TYPE + "'" + " AND "
						+ displayName + " NOTNULL ) ";

		if (constraint != null) {
			select += " AND (" + Email.ADDRESS + " LIKE ? " + " OR "
					+ displayName + " LIKE ? )";
			selectArgs = new String[2];
			selectArgs[0] = "%" + constraint + "%";
			selectArgs[1] = "%" + constraint + "%";
		}

		
		Cursor cursor = getActivity().getContentResolver().query(Data.CONTENT_URI,
				CONTACTS_SUMMARY_PROJECTION, select, selectArgs,
				Contacts.TIMES_CONTACTED + " DESC"				
				);
		
		
		return cursor;
				
	}
	
	public ArrayList<Contact> getFrequentContacts() {

		String[] CONTACTS_SUMMARY_PROJECTION = new String[] {
			Contacts._ID,
			// The primary display name
			displayName,
			Contacts.PHOTO_THUMBNAIL_URI,
		};
		
		ContentResolver resolver = getActivity().getContentResolver();
		Cursor cursor = resolver.query(Contacts.CONTENT_STREQUENT_URI,
				CONTACTS_SUMMARY_PROJECTION, null, null, null 	
				);
		
		cursor.moveToFirst();
		int displayNameIndex = cursor.getColumnIndex(displayName);
		int photoIndex = cursor.getColumnIndex(Contacts.PHOTO_THUMBNAIL_URI);
		int idIndex = cursor.getColumnIndex(Contacts._ID);
		
		String[] EMAIL_PROJECTION = new String[] {
				Email.ADDRESS
		};
		String email, displayName, photoUri;
		ArrayList<Contact> frequentContacts = new ArrayList<Contact>();
		String allMails = new String();
		while (cursor.isAfterLast() == false) 
		{
			String select = Data.MIMETYPE + " = '"+ Email.CONTENT_ITEM_TYPE +"' AND " + 
					Email.ADDRESS + " NOTNULL AND " 
    				+ Data.CONTACT_ID + "= " + cursor.getInt(idIndex) + " ";
			 
			displayName = cursor.getString(displayNameIndex);
		    Cursor emailCursor = resolver.query(Data.CONTENT_URI, EMAIL_PROJECTION, 
		    				select, 
		    				null, Email.TYPE + " ASC ");
		    emailCursor.moveToFirst();
		    String userMails = new String();
		    while(emailCursor.isAfterLast() == false) {
		    	email = emailCursor.getString(emailCursor.getColumnIndex(Email.ADDRESS));
		    	if (allMails.contains(email) == false) {
		    		allMails += email;
		    		userMails += email + ",";
		    	}
		    	emailCursor.moveToNext();
		    }
		    if (userMails.length()>0) {
		    	displayName = cursor.getString(displayNameIndex);
	    		photoUri = cursor.getString(photoIndex);
	    		frequentContacts.add(new Contact(displayName, userMails.substring(0, userMails.length()-1) , photoUri));
		    }
	    	cursor.moveToNext();
		}
		
		
		return frequentContacts;
	}

	// These are the Contacts rows that we will retrieve.
	static final String[] CONTACTS_SUMMARY_PROJECTION = new String[] {
		Data._ID,
		// The primary display name
		displayName,
		// The contact's _ID, to construct a content URI
		Data.CONTACT_ID,
		// The contact's LOOKUP_KEY, to construct a content URI
		Data.LOOKUP_KEY, 
		Email.ADDRESS ,
		Data.PHOTO_THUMBNAIL_URI,

	};


	static class ViewHolder {
		TextView displayNameView;
		TextView emailView;
		ImageView photoView;
	}

	public class SelectedContactAdapter extends ArrayAdapter<Contact> {

		public SelectedContactAdapter(Context context, int resource) {
			super(context, resource);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = getActivity().getLayoutInflater().inflate(R.layout.contact_list_item  , null);
				holder = new ViewHolder();
				holder.displayNameView = (TextView) convertView.findViewById(R.id.display_name);
				holder.emailView = (TextView) convertView.findViewById(R.id.email_address);
				holder.photoView = (ImageView) convertView.findViewById(R.id.photo_uri);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			Contact item = getItem(position);
			holder.displayNameView.setText(item.displayName);
			//holder.emailView.setText(item.email);
			if (item.photoUri != null) {
				holder.photoView.setImageURI(Uri.parse(item.photoUri));
			}
			else {
				holder.photoView.setImageResource(R.drawable.ic_contact_picture_2);
			}
			return convertView;
		}
	}

	public class FilteredContactListAdapter extends SimpleCursorAdapter {

		private int emailColumnIndex;
		private int displayNameColumnIndex;
		private int photoUriColumnIndex;

		public FilteredContactListAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to, int flag, boolean isSearching) {
			super(context, layout, c, from, to, flag);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			Cursor cursor = getCursor();
			cursor.moveToPosition(position);

			ViewHolder holder;
			if (convertView == null) {
				convertView = getActivity().getLayoutInflater().inflate(R.layout.contact_list_item  , null);
				holder = new ViewHolder();
				holder.displayNameView = (TextView) convertView.findViewById(R.id.display_name);
				holder.emailView = (TextView) convertView.findViewById(R.id.email_address);
				holder.photoView = (ImageView) convertView.findViewById(R.id.photo_uri);
				convertView.setTag(holder);

				emailColumnIndex = cursor.getColumnIndex(Email.ADDRESS);
				displayNameColumnIndex = cursor.getColumnIndex(displayName);
				photoUriColumnIndex = cursor.getColumnIndex(Data.PHOTO_THUMBNAIL_URI);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			String email = "";
			if (emailColumnIndex != -1) {
				email = cursor.getString(emailColumnIndex);
			}
			holder.emailView.setText(email);

			String displayName = cursor.getString(displayNameColumnIndex);
			holder.displayNameView.setText(displayName);

			String photoUri = cursor.getString(photoUriColumnIndex);
			if (photoUri != null) {
				holder.photoView.setImageURI(Uri.parse(photoUri));
			}
			else {
				holder.photoView.setImageResource(R.drawable.ic_contact_picture_2);
			}

			return convertView;
		}

	}
}

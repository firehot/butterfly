package com.butterfly.adapter;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.SimpleCursorAdapter.CursorToStringConverter;

import com.butterfly.R;
import com.butterfly.adapter.ContactAdapter.ViewHolder;
import com.butterfly.fragment.ContactsListFragment;

public class FilteredContactListAdapter extends SimpleCursorAdapter {
	
	// These are the Contacts rows that we will retrieve.
	public static final String[] CONTACTS_SUMMARY_PROJECTION = new String[] {
		Data._ID,
		// The primary display name
		ContactAdapter.DISPLAY_NAME,
		// The contact's _ID, to construct a content URI
		Data.CONTACT_ID,
		// The contact's LOOKUP_KEY, to construct a content URI
		Data.LOOKUP_KEY, 
		Email.ADDRESS ,
		Data.PHOTO_THUMBNAIL_URI,

	};
	
	private int emailColumnIndex;
	private int displayNameColumnIndex;
	private int photoUriColumnIndex;
	private Context context;

	public FilteredContactListAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to, int flag, boolean isSearching) {
		super(context, layout, c, from, to, flag);
		this.context = context;
		
		this.setCursorToStringConverter(new CursorToStringConverter() {

			@Override
			public CharSequence convertToString(Cursor cursor) {
				int index = cursor.getColumnIndex(ContactAdapter.DISPLAY_NAME);
				return cursor.getString(index);
			}
		});
	}
	
	

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		Cursor cursor = getCursor();
		cursor.moveToPosition(position);
		
		

		ViewHolder holder;
		if (convertView == null) {
			convertView = LayoutInflater.from(this.context).inflate(R.layout.contact_list_item  , null);
			holder = new ViewHolder();
			holder.displayNameView = (TextView) convertView.findViewById(R.id.display_name);
			holder.emailView = (TextView) convertView.findViewById(R.id.email_address);
			holder.photoView = (ImageView) convertView.findViewById(R.id.photo_uri);
			convertView.setTag(holder);

			emailColumnIndex = cursor.getColumnIndex(Email.ADDRESS);
			displayNameColumnIndex = cursor.getColumnIndex(ContactAdapter.DISPLAY_NAME);
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
	
	public Cursor getCursor(CharSequence constraint) {
		String[] selectArgs = null;

		String select = 
				"(" + Email.ADDRESS + " NOTNULL " + " AND " +
						Data.MIMETYPE + " = '" + Email.CONTENT_ITEM_TYPE + "'" + " AND "
						+ ContactAdapter.DISPLAY_NAME + " NOTNULL AND " +
						Data.IS_PRIMARY + " == 1 ) ";

		if (constraint != null) {
			select += " AND (" + Email.ADDRESS + " LIKE ? " + " OR "
					+ ContactAdapter.DISPLAY_NAME + " LIKE ? )";
			selectArgs = new String[2];
			selectArgs[0] = "%" + constraint + "%";
			selectArgs[1] = "%" + constraint + "%";
			;
		}

		return context.getContentResolver().query(Data.CONTENT_URI,
				CONTACTS_SUMMARY_PROJECTION, select, selectArgs,
				Contacts.TIMES_CONTACTED + " DESC "
				);
				//displayName + " COLLATE LOCALIZED ASC"
		
	}
	
	public ArrayList<ContactAdapter.Contact> getFrequentContacts() {

		String[] CONTACTS_SUMMARY_PROJECTION = new String[] {
				Contacts._ID,
				// The primary display name
				ContactAdapter.DISPLAY_NAME,
				Contacts.PHOTO_THUMBNAIL_URI,
		};

		ContentResolver resolver = this.context.getContentResolver();
		Cursor cursor = resolver.query(Contacts.CONTENT_STREQUENT_URI,
				CONTACTS_SUMMARY_PROJECTION, null, null, null 	
				);

		cursor.moveToFirst();
		int displayNameIndex = cursor.getColumnIndex(ContactAdapter.DISPLAY_NAME);
		int photoIndex = cursor.getColumnIndex(Contacts.PHOTO_THUMBNAIL_URI);
		int idIndex = cursor.getColumnIndex(Contacts._ID);

		String[] EMAIL_PROJECTION = new String[] {
				Email.ADDRESS
		};
		String email, displayName, photoUri;
		ArrayList<ContactAdapter.Contact> frequentContacts = new ArrayList<ContactAdapter.Contact>();
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
			if (emailCursor.getCount() > 0) {
				emailCursor.moveToFirst();
				email = emailCursor.getString(emailCursor.getColumnIndex(Email.ADDRESS));
				if (allMails.contains(email) == false) {
					allMails += email;

					displayName = cursor.getString(displayNameIndex);
					photoUri = cursor.getString(photoIndex);
					frequentContacts.add(new ContactAdapter.Contact(displayName, email, photoUri));
				}
			}
			emailCursor.close();
			cursor.moveToNext();
		}
		cursor.close();
		return frequentContacts;
	}

}
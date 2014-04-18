package com.butterfly.adapter;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
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
		Data.DISPLAY_NAME_PRIMARY,
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
				int index = cursor.getColumnIndex(Data.DISPLAY_NAME_PRIMARY);
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
			displayNameColumnIndex = cursor.getColumnIndex(Data.DISPLAY_NAME_PRIMARY);
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
		if (displayName != null && displayName.length() > 0) {
			holder.displayNameView.setText(displayName);
		}
		else {
			holder.displayNameView.setText("");
		}

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
						+ Data.DISPLAY_NAME_PRIMARY + " NOTNULL " +
						" ) ";

		if (constraint != null) {
			select += " AND (" + Email.ADDRESS + " LIKE ? " + " OR "
					+ Data.DISPLAY_NAME_PRIMARY + " LIKE ? )";
			selectArgs = new String[2];
			selectArgs[0] = "%" + constraint + "%";
			selectArgs[1] = "%" + constraint + "%";
			;
		}

		return context.getContentResolver().query(Data.CONTENT_URI,
				CONTACTS_SUMMARY_PROJECTION, select, selectArgs,
				Contacts.TIMES_CONTACTED + " DESC "
				);
	}

	/**
	 * Returns the frequent contacts in an array list. 
	 * If any exception occurs it returns null
	 * @return
	 */
	public ArrayList<ContactAdapter.Contact> getFrequentContacts() {

		ArrayList<ContactAdapter.Contact> frequentContacts = null;
		try {
			String[] CONTACTS_SUMMARY_PROJECTION = new String[] {
					Contacts._ID,
					// The primary display name
					Data.DISPLAY_NAME_PRIMARY,
					Contacts.PHOTO_THUMBNAIL_URI,
			};

			ContentResolver resolver = this.context.getContentResolver();
			Cursor cursor = resolver.query(Contacts.CONTENT_STREQUENT_URI,
					CONTACTS_SUMMARY_PROJECTION, null, null, null 	
					);

			cursor.moveToFirst();
			int displayNameIndex = cursor.getColumnIndex(Data.DISPLAY_NAME_PRIMARY);
			int photoIndex = cursor.getColumnIndex(Contacts.PHOTO_THUMBNAIL_URI);
			int idIndex = cursor.getColumnIndex(Contacts._ID);

			String[] EMAIL_PROJECTION = new String[] {
					Email.ADDRESS
			};
			String email, displayName, photoUri;
			frequentContacts = new ArrayList<ContactAdapter.Contact>();
			String allMails = new String();
			while (cursor.isAfterLast() == false) 
			{
				String select = Data.MIMETYPE + " = '"+ Email.CONTENT_ITEM_TYPE +"' AND " + 
						Email.ADDRESS + " NOTNULL AND " 
						+ Data.CONTACT_ID + "= " + cursor.getInt(idIndex) + " ";

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
		}
		catch (SQLiteException e) {
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return frequentContacts;
	}

}
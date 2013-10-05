package com.butterfly;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Data;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.CursorToStringConverter;
import android.widget.TextView;

import com.bugsense.trace.BugSenseHandler;
import com.butterfly.debug.BugSense;

public class ContactsList extends Activity {

	public static final String MAILS_TO_BE_NOTIFIED = "mails_to_be_notified";

	SimpleCursorAdapter mAdapter;

	private ListView selectedContactList;

	private ContactAdapter selectedContactAdapter;

	private AutoCompleteTextView textView;

	private static String displayName = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? Data.DISPLAY_NAME_PRIMARY
			: Data.DISPLAY_NAME;

	public class Contact {
		public String displayName;
		public String email;

		public Contact(String displayName, String email) {
			super();
			this.displayName = displayName;
			this.email = email;
		}

		@Override
		public String toString() {
			return displayName;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		BugSenseHandler.initAndStartSession(this, BugSense.API_KEY);

		setContentView(R.layout.contact_list);

		textView = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView);
		selectedContactList = (ListView) findViewById(R.id.selectedContactList);
		selectedContactAdapter = new ContactAdapter(getApplicationContext(),
				R.layout.contact_list_item);
		selectedContactList.setAdapter(selectedContactAdapter);

		mAdapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_2, null, new String[] {
						displayName, Email.ADDRESS }, new int[] {
						android.R.id.text1, android.R.id.text2 }, 0);

		textView.setAdapter(mAdapter);

		mAdapter.setFilterQueryProvider(new FilterQueryProvider() {

			@Override
			public Cursor runQuery(CharSequence constraint) {
				if (constraint != null) {
					return getCursor(constraint);
				}
				return null;
			}
		});
		mAdapter.setCursorToStringConverter(new CursorToStringConverter() {

			@Override
			public CharSequence convertToString(Cursor cursor) {
				int index = cursor.getColumnIndex(displayName);
				return cursor.getString(index);
			}
		});

		textView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapterView, View arg1,
					int position, long id) {

				int count = selectedContactAdapter.getCount();

				Cursor cursor = (Cursor) adapterView
						.getItemAtPosition(position);
				String name = cursor.getString(cursor
						.getColumnIndex(displayName));
				String mail = cursor.getString(cursor
						.getColumnIndex(Email.ADDRESS));

				boolean found = false;
				for (int i = 0; i < count; i++) {
					if (selectedContactAdapter.getItem(i).email.equals(mail)) {
						found = true;
						break;
					}
				}
				if (found == false) {
					selectedContactAdapter.add(new Contact(name, mail));
					selectedContactAdapter.notifyDataSetChanged();
				}
				textView.setText("");

			}
		});

	}

	private Cursor getCursor(CharSequence constraint) {
		String[] selectArgs = null;

		String select = /*
						 * Searches for an email address that matches the search
						 * string
						 */
		"(" + Email.ADDRESS + " NOTNULL " + " AND " +
		/*
		 * Searches for a MIME type that matches the value of the constant
		 * Email.CONTENT_ITEM_TYPE. Note the single quotes surrounding
		 * Email.CONTENT_ITEM_TYPE.
		 */
		Data.MIMETYPE + " = '" + Email.CONTENT_ITEM_TYPE + "'" + " AND "
				+ displayName + " NOTNULL" + ") ";

		if (constraint != null) {
			select += " AND (" + Email.ADDRESS + " LIKE ? " + " OR "
					+ displayName + " LIKE ? )";
			selectArgs = new String[2];
			selectArgs[0] = "%" + constraint + "%";
			selectArgs[1] = "%" + constraint + "%";
			;
		}
		// + " AND " +
		//
		// Data.DISPLAY_NAME + "=?";

		return getContentResolver().query(Data.CONTENT_URI,
				CONTACTS_SUMMARY_PROJECTION, select, selectArgs,
				displayName + " COLLATE LOCALIZED ASC");

	}

	// These are the Contacts rows that we will retrieve.
	static final String[] CONTACTS_SUMMARY_PROJECTION = new String[] {
			Data._ID,
			// The primary display name
			displayName,
			// The contact's _ID, to construct a content URI
			Data.CONTACT_ID,
			// The contact's LOOKUP_KEY, to construct a content URI
			Data.LOOKUP_KEY, Email.ADDRESS,

	/*
	 * Contacts._ID, Contacts.DISPLAY_NAME, Contacts.CONTACT_STATUS,
	 * Contacts.CONTACT_PRESENCE, Contacts.PHOTO_ID, Contacts.LOOKUP_KEY,
	 */};

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();

		BugSenseHandler.closeSession(this);
	}

	public void removeContact(View view) {

		int position = selectedContactList.getPositionForView((View) view
				.getParent());

		selectedContactAdapter.remove(selectedContactAdapter.getItem(position));
		selectedContactAdapter.notifyDataSetChanged();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.contact_list_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.action_contact_list_done:
			int count = selectedContactAdapter.getCount();
			Intent intent = new Intent(this, RecordActivity.class);

			if (selectedContactAdapter.getCount() > 0) {
				String mails = new String();
				for (int i = 0; i < count; i++) {
					mails += selectedContactAdapter.getItem(i).email + ",";
				}

				mails = mails.substring(0, mails.length() - 1);
				intent.putExtra(MAILS_TO_BE_NOTIFIED, mails);

			}
			startActivity(intent);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}

	}

	public class ContactAdapter extends ArrayAdapter<Contact> {

		private int view;

		public ContactAdapter(Context context, int resource) {
			super(context, resource);
			this.view = resource;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			if (convertView == null) {
				convertView = getLayoutInflater().inflate(view, null);
				TextView tv = (TextView) (convertView
						.findViewById(R.id.contact_display_name));
				ViewHolder viewHolder = new ViewHolder();
				viewHolder.tv = tv;
				convertView.setTag(viewHolder);
			}

			ViewHolder viewHolder = (ViewHolder) convertView.getTag();
			viewHolder.tv.setText(getItem(position).toString());

			return convertView;
		}

		public class ViewHolder {
			TextView tv;
		}

	}
}

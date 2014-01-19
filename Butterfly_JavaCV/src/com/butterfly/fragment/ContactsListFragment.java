package com.butterfly.fragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Data;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
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

	private ListView selectedContactList;

	private SelectedContactAdapter selectedContactAdapter;

	private AutoCompleteTextView autoCompleteEditText;

	private RemoveContactListener removeContactListener;

	private ImageView startBroadcastView;

	private static String displayName = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? Data.DISPLAY_NAME_PRIMARY
			: Data.DISPLAY_NAME;

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

	public class RemoveContactListener implements OnClickListener {

		@Override
		public void onClick(View view) {
			int position = selectedContactList.getPositionForView((View) view
					.getParent());

			selectedContactAdapter.remove(selectedContactAdapter.getItem(position));
			selectedContactAdapter.notifyDataSetChanged();
		}

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.contact_list, container, false);

		removeContactListener = new RemoveContactListener();
		autoCompleteEditText = (AutoCompleteTextView) v.findViewById(R.id.autoCompleteTextView);
		startBroadcastView = (ImageView) v.findViewById(R.id.start_broadcast_button); 
		startBroadcastView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				int batteryLevel = ((MainActivity)getActivity()).getBatteryLevel();
				if(batteryLevel > 0 && batteryLevel < 10)
				{
					Toast.makeText(getActivity(), getString(R.string.batteryLow), Toast.LENGTH_LONG).show();
				}
				else {
					int count = selectedContactAdapter.getCount();
					Intent intent = new Intent(getActivity(), RecordActivity.class);

					if (selectedContactAdapter.getCount() > 0) {
						String mails = new String();
						for (int i = 0; i < count; i++) {
							mails += selectedContactAdapter.getItem(i).email + ",";
						}

						mails = mails.substring(0, mails.length() - 1);
						intent.putExtra(MAILS_TO_BE_NOTIFIED, mails);

					}
					startActivity(intent);
				}
			}
		});

		selectedContactList = (ListView) v.findViewById(R.id.selectedContactList);
		selectedContactAdapter = new SelectedContactAdapter(getActivity(),
				R.layout.contact_list_item);
		selectedContactList.setAdapter(selectedContactAdapter);
		
		mAdapter = new FilteredContactListAdapter(getActivity(),
				R.layout.contact_list_item, null, new String[] {
			displayName, displayName, Data.PHOTO_THUMBNAIL_URI }, new int[] {
			R.id.display_name, R.id.email_address, R.id.photo_uri}, 0, true);


		autoCompleteEditText.setAdapter(mAdapter);

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

		autoCompleteEditText.setOnItemClickListener(new OnItemClickListener() {

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

				String photoUri = cursor.getString(cursor.getColumnIndex(Data.PHOTO_THUMBNAIL_URI));

				boolean found = false;
				for (int i = 0; i < count; i++) {
					if (selectedContactAdapter.getItem(i).email.equals(mail)) {
						found = true;
						break;
					}
				}
				if (found == false) {
					selectedContactAdapter.add(new Contact(name, mail, photoUri));
					selectedContactAdapter.notifyDataSetChanged();
				}
				autoCompleteEditText.setText("");
			}
		});
		return v;
	}

	private Cursor getCursor(CharSequence constraint) {
		String[] selectArgs = null;

		String select = 
				"(" + Email.ADDRESS + " NOTNULL " + " AND " +
						Data.MIMETYPE + " = '" + Email.CONTENT_ITEM_TYPE + "'" + " AND "
						+ displayName + " NOTNULL" + " AND "
						+ Data.IS_PRIMARY + "='1' ) ";

		if (constraint != null) {
			select += " AND (" + Email.ADDRESS + " LIKE ? " + " OR "
					+ displayName + " LIKE ? )";
			selectArgs = new String[2];
			selectArgs[0] = "%" + constraint + "%";
			selectArgs[1] = "%" + constraint + "%";
			;
		}

		return getActivity().getContentResolver().query(Data.CONTENT_URI,
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
		Data.LOOKUP_KEY, 
		Email.ADDRESS ,
		Data.PHOTO_THUMBNAIL_URI,

	};


	static class ViewHolder {
		TextView displayNameView;
		TextView emailView;
		ImageView photoView;
		ImageView deleteView;
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
				holder.deleteView = (ImageView) convertView.findViewById(R.id.remove_contact);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			Contact item = getItem(position);
			holder.displayNameView.setText(item.displayName);
			holder.emailView.setText(item.email);
			holder.deleteView.setOnClickListener(removeContactListener);
			if (item.photoUri != null) {
				holder.photoView.setImageURI(Uri.parse(item.photoUri));
			}
			else {
				holder.photoView.setImageResource(R.drawable.ic_action_user);
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
				holder.deleteView = (ImageView) convertView.findViewById(R.id.remove_contact);
				convertView.setTag(holder);

				emailColumnIndex = cursor.getColumnIndex(Email.ADDRESS);
				displayNameColumnIndex = cursor.getColumnIndex(displayName);
				photoUriColumnIndex = cursor.getColumnIndex(Data.PHOTO_THUMBNAIL_URI);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			String email = cursor.getString(emailColumnIndex);
			holder.emailView.setText(email);

			String displayName = cursor.getString(displayNameColumnIndex);
			holder.displayNameView.setText(displayName);

			String photoUri = cursor.getString(photoUriColumnIndex);
			if (photoUri != null) {
				holder.photoView.setImageURI(Uri.parse(photoUri));
			}
			else {
				holder.photoView.setImageResource(R.drawable.ic_action_user);
			}
			holder.deleteView.setVisibility(View.GONE);

			return convertView;
		}

	}
}

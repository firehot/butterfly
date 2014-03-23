package com.butterfly.fragment;

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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
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
import com.butterfly.adapter.ContactAdapter;
import com.butterfly.adapter.ContactAdapter.Contact;
import com.butterfly.adapter.FilteredContactListAdapter;

public class ContactsGroupFragment extends Fragment {

	public static final String MAILS_TO_BE_NOTIFIED = "mails_to_be_notified";

	FilteredContactListAdapter mAdapter;

	private ListView selectedContactList;

	private ContactAdapter selectedContactAdapter;

	private AutoCompleteTextView autoCompleteEditText;

	private RemoveContactListener removeContactListener;

	
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
		View v = inflater.inflate(R.layout.contacts_group_list, container, false);

		removeContactListener = new RemoveContactListener();
		autoCompleteEditText = (AutoCompleteTextView) v.findViewById(R.id.autoCompleteTextView);

		selectedContactList = (ListView) v.findViewById(R.id.selectedContactList);
		selectedContactAdapter = new ContactAdapter(getActivity(),
				R.layout.contact_list_item);
		selectedContactList.setAdapter(selectedContactAdapter);
		selectedContactAdapter.setRemoveListener(removeContactListener);
		
		mAdapter = new FilteredContactListAdapter(getActivity(),
				R.layout.contact_list_item, null, new String[] {
			ContactAdapter.DISPLAY_NAME, ContactAdapter.DISPLAY_NAME, Data.PHOTO_THUMBNAIL_URI }, new int[] {
			R.id.display_name, R.id.email_address, R.id.photo_uri}, 0, true);


		autoCompleteEditText.setAdapter(mAdapter);

		mAdapter.setFilterQueryProvider(new FilterQueryProvider() {

			@Override
			public Cursor runQuery(CharSequence constraint) {
				if (constraint != null) {
					return mAdapter.getCursor(constraint);
				}
				return null;
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
						.getColumnIndex(ContactAdapter.DISPLAY_NAME));
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
					selectedContactAdapter.add(new ContactAdapter.Contact(name, mail, photoUri));
					selectedContactAdapter.notifyDataSetChanged();
				}
				autoCompleteEditText.setText("");
			}
		});
		
		
	
		return v;
	}

	public void openRecordActivity() {
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
	
	@Override
	public void onPause() {
		Cursor cursor = mAdapter.getCursor();
		if (cursor != null && cursor.isClosed() == false) {
			cursor.close();
		}
		super.onPause();
	}
	
}

package com.butterfly.fragment;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Data;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.butterfly.R;
import com.butterfly.RecordActivity;
import com.butterfly.adapter.ContactAdapter;
import com.butterfly.adapter.FilteredContactListAdapter;

public class ContactsListFragment extends Fragment {

	public static final String MAILS_TO_BE_NOTIFIED = "mails_to_be_notified";

	FilteredContactListAdapter mAdapter;

	private ListView contactList;

	private ContactAdapter selectedContactAdapter;

	private EditText searchEditText;

	private ArrayList<ContactAdapter.Contact> frequentContacts;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.contact_list, container, false);

		searchEditText = (EditText) v.findViewById(R.id.searchText);
		contactList = (ListView) v.findViewById(R.id.selectedContactList);
		selectedContactAdapter = new ContactAdapter(getActivity(),
				R.layout.contact_list_item);
		contactList.setAdapter(selectedContactAdapter);

		mAdapter = new FilteredContactListAdapter(getActivity(),
				R.layout.contact_list_item, null, new String[] {
			Data.DISPLAY_NAME_PRIMARY, Data.DISPLAY_NAME_PRIMARY, Data.PHOTO_THUMBNAIL_URI }, new int[] {
			R.id.display_name, R.id.email_address, R.id.photo_uri}, 0, true);

		frequentContacts = mAdapter.getFrequentContacts();

		if (frequentContacts != null) {
			selectedContactAdapter.addAll(frequentContacts);
		}
		else
		{
			frequentContacts = mAdapter.getContactsWithPhoneAndEmail();
			if(frequentContacts != null)
				selectedContactAdapter.addAll(frequentContacts);
		}


		searchEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
			@Override
			public void afterTextChanged(Editable s) {
				String text = searchEditText.getText().toString();
				if (text.length() > 2) {
					contactList.setAdapter(mAdapter);
					mAdapter.changeCursor(mAdapter.getCursor(text));
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
					Cursor cursor = (Cursor) contactList.getItemAtPosition(position);
					email = cursor.getString(cursor.getColumnIndex(Email.ADDRESS));
				}
				else {
					ContactAdapter.Contact contact = selectedContactAdapter.getItem(position);
					email = contact.email;
				}

				Intent intent = new Intent(getActivity(), RecordActivity.class);
				intent.putExtra(MAILS_TO_BE_NOTIFIED, email);
				startActivity(intent);
			}

		});

		return v;
	}
	
}

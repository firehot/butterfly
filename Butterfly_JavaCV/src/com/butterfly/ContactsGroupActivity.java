package com.butterfly;

import com.butterfly.fragment.ContactsGroupFragment;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class ContactsGroupActivity extends FragmentActivity {

	private ContactsGroupFragment contactGroupFragment;



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contacts_group);
		
		contactGroupFragment = (ContactsGroupFragment)(getSupportFragmentManager().findFragmentById(R.id.contactGroupFragment));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.contact_chooser, menu);
		return true;
	}
	

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.record) {
			contactGroupFragment.openRecordActivity();
			
			return true;
			
		}
		return super.onOptionsItemSelected(item);
	}
	


}

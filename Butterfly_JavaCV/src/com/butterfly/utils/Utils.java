package com.butterfly.utils;

import java.util.regex.Pattern;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.util.Patterns;

public class Utils {

	public static String getMailList(Activity activity) {
		Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
		Account[] accounts = AccountManager.get(activity).getAccounts();
		String mails = new String();
		for (Account account : accounts) {
			if (emailPattern.matcher(account.name).matches()) {
				if (mails.length() > 0) {
					if (mails.contains(account.name)) {
						continue;
					}
					mails += ",";
				}
				mails += account.name;
			}
		}
		return mails;
	}

}

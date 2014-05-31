package com.butterfly.listeners;

public interface IAsyncTaskListener {
	
	public void onPreExecute();
	
	public void onPostExecute(Object object);
	
	public void onProgressUpdate(Object... progress);
 
}

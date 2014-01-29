package com.butterfly.listeners;

import java.util.ArrayList;

import com.butterfly.fragment.StreamListFragment;
import com.butterfly.fragment.StreamListFragment.Stream;

public interface IStreamListUpdateListener {
	
	public void streamListUpdated(ArrayList<Stream> streamList);

}
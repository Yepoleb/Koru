package io.anuke.koru.components;

import io.anuke.koru.network.syncing.SyncData.Synced;

@Synced
public class ChildComponent implements KoruComponent{
	public long parent = -1;
}

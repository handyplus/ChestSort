package de.jeffclan.JeffChestSort;

import org.bukkit.inventory.Inventory;

public class JeffChestSortPlayerSetting {
	
	// Represents the information regarding a player
	// That includes:
	// - Does this player has sorting enabled?
	// - Did this player see the message on how to use ChestSort (message-when-using-chest in config.yml)

	// Sorting enabled for this player?
	boolean sortingEnabled;
	
	// Inventory sorting enabled for this player?
	boolean invSortingEnabled;
	
	// Hotkey settings
	boolean middleClick, shiftClick, doubleClick, shiftRightClick;
	
	Inventory guiInventory = null;

	// Did we already show the message how to activate sorting?
	boolean hasSeenMessage = false;
	
	// Do we have to save these settings?
	boolean changed = false;

	JeffChestSortPlayerSetting(boolean sortingEnabled, boolean invSortingEnabled, boolean middleClick, boolean shiftClick, boolean doubleClick, boolean shiftRightClick, boolean changed) {
		this.sortingEnabled = sortingEnabled;
		this.middleClick = middleClick;
		this.shiftClick = shiftClick;
		this.doubleClick = doubleClick;
		this.shiftRightClick = shiftRightClick;
		this.invSortingEnabled = invSortingEnabled;
		this.changed = changed;
	}
	
	void toggleMiddleClick() {
		middleClick = !middleClick;
		changed = true;
	}
	void toggleShiftClick() {
		shiftClick = !shiftClick;
		changed = true;
	}
	void toggleDoubleClick() {
		doubleClick = !doubleClick;
		changed = true;
	}
	void toggleShiftRightClick() {
		shiftRightClick = !shiftRightClick;
		changed = true;
	}
	void enableChestSorting() {
		sortingEnabled = true;
		changed = true;
	}
	void disableChestSorting() {
		sortingEnabled = false;
		changed = true;
	}
	void toggleChestSorting() {
		sortingEnabled = !sortingEnabled;
		changed = true;
	}
	void enableInvSorting() {
		invSortingEnabled = true;
		changed = true;
	}
	void disableInvSorting() {
		invSortingEnabled = false;
		changed = true;
	}
	void toggleInvSorting() {
		invSortingEnabled = !invSortingEnabled;
		changed = true;
	}

}

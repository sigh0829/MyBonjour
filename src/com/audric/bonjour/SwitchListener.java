package com.audric.bonjour;

public interface SwitchListener {
	public enum Direction {UP, DOWN, RIGHT, LEFT};
	
	public void onSwitch(Direction direction);
}

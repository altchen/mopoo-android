package com.mopoo.action;

public class GoDownButtonState implements ButtonState {

	@Override
	public void click(ScrollViewContext action) {
		action.goDown();
		action.setState(new GoTopButtonState());
	}

	@Override
	public String getButtonText() {
		return "到底";
	}

}

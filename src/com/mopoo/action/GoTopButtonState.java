package com.mopoo.action;

public class GoTopButtonState implements ButtonState {

	@Override
	public void click(ScrollViewContext action) {
		action.goTop();
		action.setState(new GoDownButtonState());
	}

	@Override
	public String getButtonText() {
		return "到顶";
	}

}

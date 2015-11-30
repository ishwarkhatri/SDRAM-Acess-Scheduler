package edu.binghamton.my.common;

public enum OPERATION {

	ACTIVATE,
	READ,
	WRITE,
	PRECHARGE;

	public static OPERATION getValue(String prevOperation) {
		switch (prevOperation) {
		case Constants.ACTIVATE:
			return ACTIVATE;

		case Constants.READ:
			return READ;

		case Constants.WRITE:
			return WRITE;

		case Constants.PRECHARGE:
			return PRECHARGE;

		default:
			break;
		}
		return null;
	}
}

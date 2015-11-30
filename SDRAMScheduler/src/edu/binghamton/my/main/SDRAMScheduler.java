package edu.binghamton.my.main;

import static edu.binghamton.my.common.Constants.ACTIVATE;
import static edu.binghamton.my.common.Constants.IDLE;
import static edu.binghamton.my.common.Constants.LOAD;
import static edu.binghamton.my.common.Constants.PRECHARGE;
import static edu.binghamton.my.common.Constants.READ;
import static edu.binghamton.my.common.Constants.STORE;
import static edu.binghamton.my.common.Constants.WRITE;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import edu.binghamton.my.common.OPERATION;

public class SDRAMScheduler {
	private static List<String> outputList = new ArrayList<>();
	private static int[][] DELAY_COUNT_ARRAY = new int[4][4];
	private static List<String> DATA_LIST = new LinkedList<>();

	public static void main(String[] args) {
		if(args.length < 2) {
			echoError("Invalid command line arguments!");
			System.exit(1);
		}

		init();

		String inputFileName = args[0];
		String outputFileName = args[1];

		File inputFile = new File(inputFileName);
		File outputFile = new File(outputFileName);

		int activateIndex = 0;
		int readWriteIndex = 0;
		int prechargeIndex = 0;

		readFile(inputFile);
		String currentOperation = null;
		String prevOperation = null;
		int idleCycleCount;
		String idleCmd;

		for(int i = 0; i < DATA_LIST.size();) {
			String input = DATA_LIST.get(i);

			String actionType = input.substring(0, 1);
			String rowAddress = input.substring(1, 6);
			String colAddress = input.substring(6);

			currentOperation = ACTIVATE;
			if(prevOperation !=  null) {
				idleCycleCount = getIdleCycleCount(prevOperation, currentOperation);

				idleCmd = getIdleCommand((idleCycleCount - 1));
				registerOutput(idleCmd);
			}

			//Operation -> Activate
			activateIndex = 10;
			String activateCommand = initiateActivate(rowAddress);
			registerOutput(activateCommand);
			prevOperation = currentOperation;
			activateIndex++;

			//first process currently read line
			//Operation -> Read/Write
			currentOperation = getOperationType(actionType);
				
			idleCycleCount = getIdleCycleCount(prevOperation, currentOperation);

			idleCmd = getIdleCommand((idleCycleCount - 1));
			registerOutput(idleCmd);

			String readWriteCommand = getReadWriteCommand(actionType, colAddress);
			registerOutput(readWriteCommand);
			activateIndex++;

			while(true) {
				if(++i == DATA_LIST.size()) {
					break;
				}

				String nextInput = DATA_LIST.get(i);
				if(hasSameRowAddress(nextInput, rowAddress)) {
					actionType = input.substring(0, 1);
					prevOperation = getOperationType(actionType);

					actionType = nextInput.substring(0, 1);
					currentOperation = getOperationType(actionType);
					colAddress = nextInput.substring(6);

					idleCycleCount = getIdleCycleCount(prevOperation, currentOperation);

					if(idleCycleCount > 1) {
						idleCmd = getIdleCommand((idleCycleCount - 1));
						registerOutput(idleCmd);
						activateIndex += (idleCycleCount - 1);
					}

					readWriteCommand = getReadWriteCommand(actionType, colAddress);
					registerOutput(readWriteCommand);
					activateIndex++;
					input = nextInput;
				} else {
					break;
				}
			}

			actionType = input.substring(0, 1);
			prevOperation = getOperationType(actionType);
			currentOperation = PRECHARGE;

			idleCycleCount = getIdleCycleCount(prevOperation, currentOperation);
			//activateIndex += idleCycleCount;

			int prechargeAndActivateDelay = getIdleCycleCount(ACTIVATE, PRECHARGE);

			if(activateIndex >= prechargeAndActivateDelay) {
				if((idleCycleCount - 1) != 0) {
					idleCmd = getIdleCommand((idleCycleCount - 1));
					registerOutput(idleCmd);
				}
			} else {
				int temp = 0;
				for(;activateIndex < prechargeAndActivateDelay; activateIndex++) {
					temp++;
				}
				if(temp < idleCycleCount) {
					temp = idleCycleCount;
				}

				idleCmd = getIdleCommand((temp - 1));
				registerOutput(idleCmd);
			}
			
			registerOutput(PRECHARGE);
			prevOperation = currentOperation;
		}

		generateOutputFile(outputFile);
		
	}


	private static void generateOutputFile(File outputFile) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(outputFile));
			for(int i = 0; i<outputList.size(); i++) {
				if(i == (outputList.size() - 1)) {
					writer.write(outputList.get(i));
				} else {
					writer.write(outputList.get(i) + "\n");
				}
			}
		} catch (Exception e) {
			echoError("Error while writing data to file: " + e.getMessage());
		} finally {
			try {
				writer.flush();
				writer.close();
			} catch(Exception e) {
				echoError("Error while closing BufferedWriter: " + e.getMessage());
			}
		}
	}


	private static boolean hasSameRowAddress(String nextInput, String rowAddress) {
		String newRowAddress = nextInput.substring(1, 6);
		return newRowAddress.equalsIgnoreCase(rowAddress);
	}


	private static void readFile(File inputFile) {
		Scanner scan = null;
		try {
			scan = new Scanner(inputFile);

			while(scan.hasNextLine()) {
				String input = scan.nextLine();
				if(input == null || "".equals(input.trim()) || input.length() < 3) {
					continue;
				}

				DATA_LIST.add(input);
			}
		} catch (IOException e) {
			echoError("Error in tournament predictor: " + e.getMessage());
		} finally {
			try {
				scan.close();
			} catch(Exception e) {
				echoError("Unable to close bufferred writer: " + e.getMessage());
			}
		}
	}


	private static String getIdleCommand(int idleCycleCount) {
		return IDLE + idleCycleCount;
	}


	private static int getIdleCycleCount(String prevOperation, String currentOperation) {
		OPERATION prevOp = OPERATION.getValue(prevOperation);
		OPERATION currOp = OPERATION.getValue(currentOperation);
		int row = prevOp.ordinal();
		int col = currOp.ordinal();

		return DELAY_COUNT_ARRAY[row][col];
	}


	private static String getOperationType(String actionType) {
		if(LOAD.equalsIgnoreCase(actionType)) {
			return READ;
		} else if (STORE.equalsIgnoreCase(actionType)) {
			return WRITE;
		}
		return null;
	}


	private static String getReadWriteCommand(String actionType, String colAddress) {
		String readWriteCmd = "";
		if(LOAD.equalsIgnoreCase(actionType)) {
			readWriteCmd = READ + colAddress;
		} else if (STORE.equalsIgnoreCase(actionType)) {
			readWriteCmd = WRITE + colAddress;
		}

		return readWriteCmd;
	}


	private static String initiateActivate(String rowAddress) {
		return ACTIVATE + rowAddress;
	}

	private static void registerOutput(String activateCommand) {
		outputList.add(activateCommand.toUpperCase());
	}

	private static void init() {
		DELAY_COUNT_ARRAY[0][0] = -1;
		DELAY_COUNT_ARRAY[0][1] = 12;
		DELAY_COUNT_ARRAY[0][2] = 12;
		DELAY_COUNT_ARRAY[0][3] = 30;
		DELAY_COUNT_ARRAY[1][0] = -1;
		DELAY_COUNT_ARRAY[1][1] = 1;
		DELAY_COUNT_ARRAY[1][2] = 4;
		DELAY_COUNT_ARRAY[1][3] = 3;
		DELAY_COUNT_ARRAY[2][0] = -1;
		DELAY_COUNT_ARRAY[2][1] = 1;
		DELAY_COUNT_ARRAY[2][2] = 1;
		DELAY_COUNT_ARRAY[2][3] = 1;
		DELAY_COUNT_ARRAY[3][0] = 20;
		DELAY_COUNT_ARRAY[3][1] = -1;
		DELAY_COUNT_ARRAY[3][2] = -1;
		DELAY_COUNT_ARRAY[3][3] = -1;
	}

	private static void echoError(String data) {
		System.err.println(data);
	}
	private static void echo(String data) {
		System.out.println(data);
	}
}

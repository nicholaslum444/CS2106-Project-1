import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;

public class Manager {

	private static class Process implements Comparable<Process> {

		String mName;
		int mPriority;
		Process mParent;
		ArrayList<Process> mChildren;
		State mState;

		public Process(String name, int priority, Process parent) {
			mName = name;
			mPriority = priority;
			mState = State.READY;
			mChildren = new ArrayList<Process>();
			
			link(parent, this);
		}

		public void link(Process parent, Process child) {
			if (parent != null) {
				parent.mChildren.add(child);
			}
			child.mParent = parent;
		}

		@Override
		public int compareTo(Process other) {
			if (this.mPriority > other.mPriority) {
				return -1;
			} else if (this.mPriority < other.mPriority) {
				return 1;
			} else {
				return 0;
			}
		}
		
		public String info() {
			String info = "[name=%1$s;priority=%2$d;state=%3$s;parent=%4$s;children=%5$s]";
			return String.format(info, mName, mPriority, mState, mParent.mName, mChildren.toString());
		}

		@Override
		public String toString() {
			return "[" + mName + "]";
		}
	}

	private static class ReadyList {

		ArrayList<Process> queue;

		public ReadyList() {
			queue = new ArrayList<Process>();
		}

		public void insert(Process p) {
			queue.add(p);
			Collections.sort(queue);
		}

		public Process checkNext() {
			return queue.get(0);
		}

		public Process getNext() {
			return queue.remove(0);
		}

		public void remove(String name) {
			for (int i = 0; i < queue.size(); i++) {
				if (queue.get(i).mName.equals(name)) {
					queue.remove(i);
				}
			}
		}
		
		public int size() {
			return queue.size();
		}

		@Override
		public String toString() {
			return queue.toString();
		}
	}

	private static enum State {
		READY, RUNNING, BLOCKED
	}

	private static ReadyList readyList = new ReadyList();
	private static HashMap<String, Process> allProcesses = new HashMap<String, Process>();
	private static Scanner sc = new Scanner(System.in);
	private static Process runningProcess = new Process("init", 0, null);

	public static void main(String[] args) {
		runInit();
		printRunningProcess();
		while (true) {
			String input = sc.nextLine();
			parseInput(input);
		}
	}

	private static void parseInput(String cmdLine) {
		String[] cmdArray = cmdLine.toLowerCase().split("\\s+");
		String cmd = cmdArray[0];
		switch (cmd) {
			case "init":
				runInit();
				break;
			case "cr":
				runCreate(cmdArray);
				break;
			case "de":
				runDestroy(cmdArray);
				break;
			case "req":
				runRequest(cmdArray);
				break;
			case "rel":
				runRelease(cmdArray);
				break;
			case "to":
				runTimeout(cmdArray);
				break;
			case " ":
				// do nothing
				return;
			default:
				// do nothing
				return;
		}
		printRunningProcess();
	}

	private static void runTimeout(String[] cmdArray) {
		Process kickedProcess = runningProcess;
		runningProcess = null;
		kickedProcess.mState = State.READY;
		readyList.insert(kickedProcess);
		reschedule();
	}

	private static void runRelease(String[] cmdArray) {
		
	}

	private static void runRequest(String[] cmdArray) {

	}

	private static void runDestroy(String[] cmdArray) {
		String name = cmdArray[1];
		Process doomedProcess = allProcesses.get(name);
		destroyBranch(doomedProcess);
		reschedule();

	}
	
	private static void destroyBranch(Process p) {
		//println(p.info());
		for (Process pc : p.mChildren) {
			destroyBranch(pc);
		}
		
		if (runningProcess != null) {
			String rName = runningProcess.mName;
			String pName = p.mName;
			if (rName.equals(pName)) {
				runningProcess = null;
			}
		}
		readyList.remove(p.mName);
		allProcesses.remove(p.mName);
	}

	private static void runCreate(String[] cmdArray) {
		String name = cmdArray[1];
		int priority = Integer.parseInt(cmdArray[2]);
		Process newProcess = new Process(name, priority, runningProcess);
		allProcesses.put(name, newProcess);
		readyList.insert(newProcess);

		reschedule();
	}

	private static void runInit() {
		readyList = new ReadyList();
		runningProcess = new Process("init", 0, null);
	}

	private static void reschedule() {
		// find highest priority task.
		// switch current task with it if highest priority > own priority.
		Process nextInLine = readyList.checkNext();
		if (runningProcess == null) {
			runningProcess = readyList.getNext();
		} else if (nextInLine.mPriority > runningProcess.mPriority) {
			Process newRunningProcess = readyList.getNext();
			newRunningProcess.mState = State.RUNNING;
			Process oldRunningProcess = runningProcess;
			oldRunningProcess.mState = State.READY;
			runningProcess = newRunningProcess;
			readyList.insert(oldRunningProcess);
		}
	}

	static void printRunningProcess() {
		print(runningProcess.mName);
	}

	static void print(String s) {
		System.out.print(s + " ");
	}

	static void println(String s) {
		System.out.println(s);
	}

}

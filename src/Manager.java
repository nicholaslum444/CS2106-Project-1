import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;

public class Manager {
	
	private static class ProcessPair {
		
		Process mProcess;
		int mUnits;
		public ProcessPair(Process process, int units) {
			mProcess = process;
			mUnits = units;
		}
		
		@Override
		public String toString() {
			return String.format("[%1$s, %2$d]", mProcess.mName, mUnits);
		}
	}
	
	private static class ResourcePair {
		
		Resource mResource;
		int mUnits;
		public ResourcePair(Resource r, int units) {
			mResource = r;
			mUnits = units;
		}
		
		@Override
		public String toString() {
			return String.format("[%1$s, %2$d]", mResource.mName, mUnits);
		}
	}
	
	private static class Resource {
		
		String mName;
		int mUnitsTotal;
		int mUnitsAvailable;
		HashMap<String, ProcessPair> mProcessesAttached;
		HashMap<String, ProcessPair> mProcessesBlocked;
		
		public Resource(String name, int unitsTotal) {
			mName = name;
			mUnitsTotal = unitsTotal;
			mUnitsAvailable = unitsTotal;
			mProcessesAttached = new HashMap<String, ProcessPair>();
			mProcessesBlocked = new HashMap<String, ProcessPair>();
		}
		
		public void block(Process p, int unitsRequested) {
			if (mProcessesBlocked.containsKey(p.mName)) {
				int unitsCurrentlyOccupied = mProcessesBlocked.remove(p.mName).mUnits;
				int unitsNowOccupied = unitsCurrentlyOccupied + unitsRequested;
				mProcessesBlocked.put(p.mName, new ProcessPair(p, unitsNowOccupied));
			} else {
				mProcessesBlocked.put(p.mName, new ProcessPair(p, unitsRequested));
			}
			p.blockOnResource(this, unitsRequested);
		}
		
		public void unblock(Process p) {
			mProcessesBlocked.remove(p.mName);
			p.unblockOnResource(this);
		}
		
		public void updateBlockedList() {
			ProcessPair candidate = null;
			for (ProcessPair pair : mProcessesBlocked.values()) {
				int unitsWanted = pair.mUnits;
				if (unitsWanted <= mUnitsAvailable) {
					candidate = pair;
					break;
				}
			}
			if (candidate != null) {
				int unitsWanted = candidate.mUnits;
				Process pr = candidate.mProcess;
				unblock(pr);
				attach(pr, unitsWanted);
				updateBlockedList();
			}
		}

		public void attach(Process p, int unitsToAttach) {
			if (unitsToAttach > mUnitsTotal) {
				print("error");
				noError = false;
			} else {
				if (mProcessesAttached.containsKey(p.mName)) {
					int unitsCurrentlyOccupied = mProcessesAttached.get(p.mName).mUnits;
					int unitsPotentialOccupied = unitsCurrentlyOccupied + unitsToAttach;
					if (unitsPotentialOccupied > mUnitsTotal) {
						print("error");
						noError = false;
					} else if (unitsToAttach > mUnitsAvailable) {
						block(p, unitsToAttach);
					} else {
						mProcessesAttached.put(p.mName, new ProcessPair(p, unitsPotentialOccupied));
						allocateUnits(unitsToAttach);
						p.attachResource(this, unitsToAttach);
					}
				} else if (unitsToAttach > mUnitsAvailable) {
					block(p, unitsToAttach);
				} else {
					mProcessesAttached.put(p.mName, new ProcessPair(p, unitsToAttach));
					allocateUnits(unitsToAttach);
					p.attachResource(this, unitsToAttach);
				}
			}
		}
		
		public void release(Process p, int unitsToRelease) {
			if (unitsToRelease > mUnitsTotal) {
				print("error");
				noError = false;
			} else {
				if (!mProcessesAttached.containsKey(p.mName)) {
					print("error");
					noError = false;
				} else {
					int unitsOccupied = mProcessesAttached.remove(p.mName).mUnits;
					int unitsStillOccupied = unitsOccupied - unitsToRelease;
					if (unitsStillOccupied < 0) {
						println("error");
					} else {
						if (unitsStillOccupied > 0) {
						mProcessesAttached.put(p.mName, new ProcessPair(p, unitsStillOccupied));
						}
						deallocateUnits(unitsToRelease);
						p.releaseResource(this, unitsToRelease);
					}
				}
			}
			updateBlockedList();
		}
		
		public void removeFromBlockedList(Process p) {
			ProcessPair pp = mProcessesBlocked.remove(p.mName);
			p.removeFromBlockedList(this);
		}
		
		public void allocateUnits(int unitsRequested) {
			mUnitsAvailable = mUnitsAvailable - unitsRequested;
		}
		public void deallocateUnits(int unitsRequested) {
			mUnitsAvailable = mUnitsAvailable + unitsRequested;
		}
		
		@Override
		public String toString() {
			String info = "[%1$s, %2$s, %3$s, %4$s, %5$s]";
			return String.format(info, mName, mUnitsTotal, mUnitsAvailable, mProcessesAttached.toString(), mProcessesBlocked.toString());
		}
		
	}

	private static class Process implements Comparable<Process> {

		String mName;
		int mPriority;
		Process mParent;
		ArrayList<Process> mChildren;
		HashMap<String, ResourcePair> mResourcesAttached;
		State mState;
		Resource mResourceBlockedOn;
		int mUnitsBlockedOn;

		public Process(String name, int priority, Process parent) {
			mName = name;
			mPriority = priority;
			mState = State.READY;
			mChildren = new ArrayList<Process>();
			mResourcesAttached = new HashMap<String, ResourcePair>();
			mResourceBlockedOn = null;
			mUnitsBlockedOn = 0;
			
			linkProcesses(parent, this);
		}
		
		public void attachResource(Resource r, int units) {
			if (mResourcesAttached.containsKey(r.mName)) {
				int unitsCurrentlyOccupied = mResourcesAttached.remove(r.mName).mUnits;
				int unitsNowOccupied = unitsCurrentlyOccupied + units;
				mResourcesAttached.put(r.mName, new ResourcePair(r, unitsNowOccupied));
			} else {
				mResourcesAttached.put(r.mName, new ResourcePair(r, units));
			}
		}
		
		public void releaseResource(Resource r, int units) {
			if (mResourcesAttached.containsKey(r.mName)) {
				mResourcesAttached.remove(r.mName);
			}
			
		}
		
		public void releaseAllResources() {
			if (mResourceBlockedOn != null) {
				mResourceBlockedOn.removeFromBlockedList(this);
			}
			
			if (mResourcesAttached.isEmpty()) {
				return;
			} else {
				ResourcePair candidate = mResourcesAttached.values().iterator().next();
				candidate.mResource.release(this, candidate.mUnits);
				releaseAllResources();
			}
		}
		
		public void blockOnResource(Resource r, int units) {
			mState = State.BLOCKED;
			mResourceBlockedOn = r;
			mUnitsBlockedOn = units;
			readyList.remove(r.mName);
		}
		
		public void unblockOnResource(Resource r) {
			removeFromBlockedList(r);
			readyList.insert(this);
		}
		
		public void removeFromBlockedList(Resource r) {
			mState = State.READY;
			mResourceBlockedOn = null;
			mUnitsBlockedOn = 0;
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
		
		public String toString() {
			String info = "[name=%1$s;priority=%2$d;state=%3$s;parent=%4$s;children=%5$s;resources=%6$s]";
			return String.format(info, mName, mPriority, mState, mParent.mName, mChildren.toString(), mResourcesAttached.toString());
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
			if (queue.isEmpty()) {
				return null;
			}
			return queue.get(0);
		}

		public Process getNext() {
			if (queue.isEmpty()) {
				return null;
			}
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
	private static HashMap<String, Resource> allResources = new HashMap<String, Resource>();
	private static Scanner sc = new Scanner(System.in);
	private static Process runningProcess = new Process("init", 0, null);
	private static boolean noError = true;

	public static void main(String[] args) {
		runInit();
		printRunningProcess();
		while (true) {
			try {
				String input = sc.nextLine();
				parseInput(input);
				if (noError) {
					printRunningProcess();
				}
				noError = true;
			} catch (Exception e) {
				print("error");
				noError = true;
			}
		}
	}

	private static void parseInput(String cmdLine) {
		if (runningProcess == null) {
			noError = false;
			return;
		}
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
			case "quit":
				System.exit(0);
				break;
			case "pp":
				printProcess(cmdArray);
				break;
			case "pr":
				printResource(cmdArray);
				break;
			case "":
				println("");
				println("");
				noError = false;
				return;
			default:
				print("error");
				noError = false;
				return;
		}
		
	}

	private static void runTimeout(String[] cmdArray) {
		Process kickedProcess = runningProcess;
		runningProcess = null;
		kickedProcess.mState = State.READY;
		readyList.insert(kickedProcess);
		reschedule();
	}

	private static void runRequest(String[] cmdArray) {
		if (cmdArray.length < 3) {
			print("error");
			noError = false;
			return;
		}
		Process requestingProcess = runningProcess;
		String resourceName = cmdArray[1];
		Resource requestedResource = allResources.get(resourceName);
		int requestedUnits = Integer.parseInt(cmdArray[2]);
		requestedResource.attach(requestingProcess, requestedUnits);
		reschedule();
	}

	private static void runRelease(String[] cmdArray) {
		if (cmdArray.length < 3) {
			print("error");
			noError = false;
			return;
		}
		Process targetProcess = runningProcess;
		String resourceName = cmdArray[1];
		Resource targetResource = allResources.get(resourceName);
		int targetUnits = Integer.parseInt(cmdArray[2]);
		targetResource.release(targetProcess, targetUnits);
		reschedule();
	}

	private static void runDestroy(String[] cmdArray) {
		if (cmdArray.length < 2) {
			print("error");
			noError = false;
			return;
		}
		String name = cmdArray[1];
		Process doomedProcess = allProcesses.get(name);
		if (doomedProcess == null) {
			print("error");
			noError = false;
		} else {
			doomedProcess.releaseAllResources();
			destroyBranch(doomedProcess);
			reschedule();
		}

	}
	
	private static void destroyBranch(Process p) {
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
		p.releaseAllResources();
		Resource r = p.mResourceBlockedOn;
		readyList.remove(p.mName);
		allProcesses.remove(p.mName);
	}

	private static void runCreate(String[] cmdArray) {
		if (cmdArray.length < 3) {
			print("error");
			noError = false;
			return;
		}
		String name = cmdArray[1];
		int priority = Integer.parseInt(cmdArray[2]);
		Process newProcess = new Process(name, priority, runningProcess);
		if (allProcesses.containsKey(name)) {
			print("error");
			noError = false;
			return;
		}
		allProcesses.put(name, newProcess);
		readyList.insert(newProcess);
		reschedule();
	}

	private static void runInit() {
		allResources.clear();
		allProcesses.clear();
		readyList = new ReadyList();
		
		// add the 4 resources R1-4
		for (int i = 1; i < 5; i++) {
			allResources.put("r"+i, new Resource("r"+i, i));
		}
		Process init = new Process("init", 0, null);
		allProcesses.put(init.mName, init);
		runningProcess = init;
	}

	private static void reschedule() {
		// find highest priority task.
		// switch current task with it if highest priority > own priority.
		Process nextInLine = readyList.checkNext();
		if (nextInLine == null && runningProcess == null) {
			print("error");
			noError = false;
		} else if (runningProcess == null) {
			runningProcess = readyList.getNext();
		} else if (nextInLine == null) {
			// do nothing.
		} else if (runningProcess.mState == State.BLOCKED) {
			runningProcess = readyList.getNext();
		} else if (runningProcess.mPriority < nextInLine.mPriority) {
			Process newRunningProcess = readyList.getNext();
			newRunningProcess.mState = State.RUNNING;
			Process oldRunningProcess = runningProcess;
			oldRunningProcess.mState = State.READY;
			runningProcess = newRunningProcess;
			readyList.insert(oldRunningProcess);
		}
	}
	


	public static void linkProcesses(Process parent, Process child) {
		if (parent != null) {
			parent.mChildren.add(child);
		}
		child.mParent = parent;
	}

	static void printRunningProcess() {
		print(runningProcess.mName);
	}
	
	static void printProcess(String[] args) {
		String processName = args[1];
		println(allProcesses.get(processName).toString());
	}
	
	static void printResource(String[] args) {
		String resourceName = args[1];
		println(allResources.get(resourceName).toString());
	}

	static void print(String s) {
		System.out.print(s + " ");
	}

	static void println(String s) {
		System.out.println(s);
	}

}

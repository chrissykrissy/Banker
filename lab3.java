import java.io.*;
import java.util.*;
import java.util.stream.IntStream;

import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;

/**
 * This is to store the resource id and its units in one data structure.
 */
class resource{
    int id;
    int units;

    resource(int id, int units){
        this.id = id;
        this.units = units;
    }
}

/**
 * This program is implementation of banker's algorithm and optimistic resource manager.
 * This program processes both algorithms in one file.
 * @author Chrissy Jeon[jj2174]
 */
public class lab3 {

    //these are static variables
    public static int processNumFIFO;
    public static int processNUMBanker;
    public static ArrayList<resource> FIFOrsrc = new ArrayList<>();
    public static ArrayList<resource> Bankrsrc = new ArrayList<>();
    public static ArrayList<ArrayList<String[]>> activityFIFO = new ArrayList<>(processNumFIFO);
    public static ArrayList<ArrayList<String[]>> activityBanker = new ArrayList<>(processNUMBanker);


    public static void main(String[] args) throws IOException {
        //reading command-line input of file name
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        String fileName = in.readLine().trim();

        //reading all of the file data and storing it as a String
        String input = new String(readAllBytes(get(fileName)));

        //scanner to go through the file
        Scanner sc = new Scanner(input);

        //store the values twice(once for resource manager algorithm, once for banker)
        processNumFIFO = sc.nextInt();
        processNUMBanker = processNumFIFO;
        int resourceNum = sc.nextInt();
        int c = 0;
        while(c < resourceNum){
            int units = sc.nextInt();
            resource rc = new resource(c+1, units);
            FIFOrsrc.add(rc);
            Bankrsrc.add(rc);
            c++;
        }

        sc.nextLine(); //dummy to read the \n after the integer

        //initialize two arrayList of ArrayList
        for (int i = 0; i < processNumFIFO; i++) {
            activityFIFO.add(new ArrayList<>());
            activityBanker.add(new ArrayList<>());

        }


        //read the rest of the data and organize
        while(sc.hasNext()){
            String line = sc.nextLine();
            while (line.length() == 0){ //skip empty lines
                line = sc.nextLine();
            }
            Scanner lineSC = new Scanner(line);
            String activity = lineSC.next();
            int id = lineSC.nextInt();
            String[] str = line.trim().split("\\s+");
            activityFIFO.get(id-1).add(str);
            activityBanker.get(id-1).add(str);
        }

        //variables needed for FIFO algorithm
        int counterFIFO = 0;
        int remainingFIFO;
        int runtimeFIFO = 0;
        int waitFIFO = 0;
        int deadlockFIFO = 0;
        int dlockitr;
        Integer[] cycleFIFO = new Integer[processNumFIFO];
        Arrays.fill(cycleFIFO,0);
        int[] waitingFIFO = new int[processNumFIFO];
        int[] delayFIFO = new int[processNumFIFO];
        int[] release = new int[processNumFIFO];
        Queue<Integer> runQueue = new LinkedList<>();
        HashMap<Integer, int[]> jobs = new HashMap<>();

        //Resource Manager Algorithm
        //while not all the processes are terminated
        while (counterFIFO != processNumFIFO){
            dlockitr = 0;
            remainingFIFO = processNumFIFO - counterFIFO;

            //deadlock checking
            if (deadlockFIFO == remainingFIFO){
                dlockitr++;
                int i = 0;
                while (i < processNumFIFO) {
                    if (activityFIFO.get(i).isEmpty()) {
                        i++;
                    } else {
                        cycleFIFO[i] = null; //abort & terminate
                        int j = 0;
                        while (j < resourceNum) {
                            FIFOrsrc.get(j).units += jobs.get(i)[j]; //release resources
                            jobs.get(i)[j] = 0;
                            j++;
                        }
                        activityFIFO.get(i).clear();
                        counterFIFO++;
                        break;
                    }
                }
            }
            deadlockFIFO = 0;

            //go through the task and add them in FIFO queue
            int qCount = 0;
            while (true) {
                if (qCount >= processNumFIFO) break;
                if (!runQueue.contains(qCount) && !activityFIFO.isEmpty()) {
                    runQueue.add(qCount);
                }
                qCount++;
            }

            //for remaining tasks
            remainingFIFO = processNumFIFO - counterFIFO;
            for (int i = 0; i < remainingFIFO; i++) {
                int id = runQueue.poll();
                if (!activityFIFO.get(id).isEmpty()){
                    String activity = activityFIFO.get(id).get(0)[0];
                    int taskID = Integer.parseInt(activityFIFO.get(id).get(0)[1]);
                    int delay = Integer.parseInt(activityFIFO.get(id).get(0)[2]);
                    int rsrcID = Integer.parseInt(activityFIFO.get(id).get(0)[3]);
                    int claim = Integer.parseInt(activityFIFO.get(id).get(0)[4]);

                    //process the activity accordingly
                    if (delay == delayFIFO[id]) {
                        delayFIFO[id] = 0;
                        switch(activity){
                            case "initiate" :
                                cycleFIFO[id]++;
                                jobs.put(id, new int[resourceNum]);
                                jobs.get(id)[rsrcID-1] = 0;
                                activityFIFO.get(id).remove(0);
                                break;
                            case "request":
                                if (claim <= FIFOrsrc.get(rsrcID - 1).units) {
                                    jobs.get(id)[rsrcID-1] += claim;
                                    FIFOrsrc.get(rsrcID-1).units -= claim;
                                    cycleFIFO[id]++;
                                    activityFIFO.get(id).remove(0);
                                    break;
                                } else {
                                    if (dlockitr == 0){
                                        cycleFIFO[id]++;
                                        waitingFIFO[id]++;
                                    }
                                    runQueue.add(id);
                                    deadlockFIFO++;
                                    break;
                                }
                            case "release" :
                                cycleFIFO[id]++;
                                release[rsrcID -1] += claim;
                                jobs.get(id)[rsrcID-1] -= claim;
                                activityFIFO.get(id).remove(0);
                                break;
                            case "terminate" :
//                                cycleFIFO[id]++;
                                counterFIFO++;
                                activityFIFO.get(id).remove(0);
                                break;
                        }
                    } else {
                        //if delayed then make it wait
                        delayFIFO[id]++;
                        cycleFIFO[id]++;
                    }
                }else{
                    i--;
                }
            }

            //resource managing (make it available by adding released resources)
            IntStream.range(0, resourceNum).forEach(i -> FIFOrsrc.get(i).units += release[i]);
            Arrays.fill(release, 0);

        }

        //printing
        System.out.println("FIFO Algorithm: ");
        int k = 0;
        for (int i = 0, cycleFIFOLength = cycleFIFO.length; i < cycleFIFOLength; i++) {
            Integer cycle = cycleFIFO[i];
            System.out.print("Task " + (k + 1) + ": ");
            if (cycle == null) {
                System.out.println("aborted  ");
            } else {
                System.out.println(cycle + " " + waitingFIFO[k] + " " + Math.round(waitingFIFO[k] / (double) cycle * 100) + "%");
                runtimeFIFO += cycle;
                waitFIFO += waitingFIFO[k];
            }
            k++;
        }
        System.out.println("Total: " + runtimeFIFO + " " + waitFIFO + " " + Math.round(waitFIFO / (double) runtimeFIFO * 100) + "%\n");




        //variables needed for Banker's algorithm
        int safe = 0;
        runQueue.clear();
        int runtimeBK = 0;
        int waitBK = 0;
        int counterBK = 0;
        int remainBK = 0;
        Integer[] cycleBK = new Integer[processNUMBanker];
        Arrays.fill(cycleBK,0);
        int[] waitingBK = new int[processNUMBanker];
        int[] delayBK = new int[processNUMBanker];
        int[] released = new int[processNUMBanker];
        Integer[][] claims = new Integer[processNUMBanker][resourceNum];
        for (Integer[] i : claims){
            Arrays.fill(i,0);
        }
        Integer[][] Orgin = new Integer[processNUMBanker][resourceNum];
        jobs.clear();

        //Banker's algorithm
        //while not all tasks are terminated
        while (counterBK != processNUMBanker) {
            //add the tasks to the queue
            IntStream.range(0, processNUMBanker).filter(i -> !runQueue.contains(i) && !(activityBanker.get(i).isEmpty())).forEach(runQueue::add);

            remainBK = processNUMBanker - counterBK;
            for (int task = 0; task < remainBK; task++) {
                int id = runQueue.poll();
                if (!(activityBanker.get(id).isEmpty())) {
                    String activity = activityBanker.get(id).get(0)[0];
                    int taskID = Integer.parseInt(activityBanker.get(id).get(0)[1]);
                    int delay = Integer.parseInt(activityBanker.get(id).get(0)[2]);
                    int rsrcID = Integer.parseInt(activityBanker.get(id).get(0)[3]);
                    int claim = Integer.parseInt(activityBanker.get(id).get(0)[4]);

                    //if not delayed
                    if (delay == delayBK[id]) {
                        //process activity accordingly
                        switch(activity){
                            case "initiate" :
                                if (claim <= Bankrsrc.get(rsrcID - 1).units) { //initiate
                                    delayBK[id] = 0;
                                    jobs.put(id, new int[resourceNum]);
                                    jobs.get(id)[rsrcID - 1] = 0;
                                    cycleBK[id]++;
                                    activityBanker.get(id).remove(0);
                                    //purpose is to better keep track of the claims
                                    claims[id][rsrcID - 1] = -claim;
                                    Orgin[id][rsrcID - 1] = claim;
                                    break;
                                } else {
                                    //if claim is more than the available resources then abort and print warning
                                    System.out.println("Banker aborts task " + (id + 1) + " before run begins:" + "\n\tclaim for resource " + rsrcID + " (" + claim + ") exceeds number of units present (" + Bankrsrc.get(rsrcID - 1).units + ")");
                                    cycleBK[id] = null;
                                    activityBanker.get(id).clear();
                                    counterBK++;
                                    break;
                                }
                            case "request" :
                                //grant it since the claim is less than what we have but still check whether the result is safe
                                if (claim <= Bankrsrc.get(rsrcID - 1).units || claims[id][rsrcID - 1] >= 0) {
                                    jobs.get(id)[rsrcID - 1] += claim;
                                    cycleBK[id]++;
                                    activityBanker.get(id).remove(0);

                                    //resource tracking
                                    if (claims[id][rsrcID - 1] >= 0) {
                                        claims[id][rsrcID - 1] -= claim;
                                        if (claims[id][rsrcID - 1] < 0) {
                                            //resource tracking
                                            for (int resource = 0; resource < resourceNum; resource++) {
                                                released[resource] += jobs.get(id)[resource];
                                                Bankrsrc.get(resource).units += claims[id][rsrcID - 1];
                                                jobs.get(task)[resource] = 0;
                                            }
                                            System.out.println("During cycle " + (cycleBK[id]-1) + "-" + cycleBK[id] + " of Banker's algorithms" + "\n\tTask " + (id+1) + "'s request exceeds its claim; aborted; "+ released[rsrcID-1] + " units available next cycle");
                                            cycleBK[id] = null; //abort/terminate
                                            activityBanker.get(id).clear();
                                            counterBK++;
                                        }
                                    } else {
                                        claims[id][rsrcID - 1] += claim;
                                        Bankrsrc.get(rsrcID - 1).units -= claim;
                                    }

                                    //safety checking
                                    safe += IntStream.range(0, resourceNum).filter(resource -> Bankrsrc.get(resource).units >= Math.abs(claims[id][resource])).count();
                                    if (safe == resourceNum) {
                                        int resource = 0;
                                        while (resource < resourceNum) {
                                            claims[id][resource] = Math.abs(claims[id][resource]);
                                            Bankrsrc.get(resource).units -= claims[id][resource];
                                            resource++;
                                        }
                                    }
                                    safe = 0;
                                    delayBK[id] = 0;
                                    break;
                                } else { //since claim is larger than what we have, make it wait for now
                                    cycleBK[id]++;
                                    waitingBK[id]++;
                                    runQueue.add(id);
                                    break;
                                }

                            case "release" :
                                delayBK[id] = 0;
                                if (claims[id][rsrcID - 1] < 0) {
                                    claims[id][rsrcID - 1] -= claim;
                                } else {
                                    claims[id][rsrcID - 1] *= -1;
                                    safe += IntStream.range(0, resourceNum).filter(i -> claims[id][i] <= 0 || Math.abs(claims[id][i]) == Orgin[id][i]).count();
                                    if (safe == resourceNum) {
                                        released[rsrcID - 1] += claim;
                                        int i = 0;
                                        while (i < resourceNum) {
                                            if (claims[id][i] > 0)
                                                claims[id][i] *= -1;
                                            Bankrsrc.get(i).units -= claims[id][i];
                                            i++;
                                        }
                                    }
                                    safe = 0;
                                    claims[id][rsrcID - 1] -= claim;
                                }
                                jobs.get(id)[rsrcID - 1] -= claim;
                                cycleBK[id]++;
                                activityBanker.get(id).remove(0);
                                break;
                            case "terminate" :
                                delayBK[id] = 0;
                                counterBK++;
                                activityBanker.get(id).remove(0);
                                break;
                        }
                    }
                    //if delayed
                    else {
                        delayBK[id]++;
                        cycleBK[id]++;
                    }

                } else {
                    task--;
                }
            }

            //add released back
            IntStream.range(0, resourceNum).forEach(resource -> Bankrsrc.get(resource).units += released[resource]);
            Arrays.fill(released, 0);

        }

        //printing
        System.out.println("Banker's Algorithm:");
        int j = 0;
        for (int i = 0, cycleBKLength = cycleBK.length; i < cycleBKLength; i++) {
            Integer cycle = cycleBK[i];
            System.out.print("Task " + (j + 1) + ": ");
            if (cycle == null) {
                System.out.println("aborted");
            } else {
                System.out.println(cycle + " " + waitingBK[j] + " " + Math.round(waitingBK[j] / (double) cycle * 100) + "%");
                runtimeBK += cycle;
                waitBK += waitingBK[j];
            }
            j++;
        }
        System.out.println("Total: " + runtimeBK + " " + waitBK + " " + Math.round(waitBK / (double) runtimeBK * 100) + "%\n");

    }
}

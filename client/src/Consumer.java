import java.util.concurrent.Semaphore;

public class Consumer extends Thread {
    private final Semaphore full;
    private final Semaphore empty;
    private final Semaphore buffer;
    private final LRUObject[] lruObjects;
    private final SecondChanceObject[] secondChanceObjects;
    private final int[] fifo;
    private final int[] bufferArray;
    private final int size;
    private int fifoFirst = 0;
    private int fifoLast = 0;
    private int secondFirst = 0;
    private int secondSize = 0;
    private int bufferIndex = 0;
    private int dataCount = 0;
    private int lruIndex = 0;
    private int lruPageFault = 0, fifoPageFault = 0, secondPageFault = 0;

    public Consumer(Semaphore full, Semaphore empty, Semaphore buffer, int size, int[] bufferArray) {
        super();
        this.full = full;
        this.empty = empty;
        this.buffer = buffer;
        this.size = size;
        this.bufferArray = bufferArray;
        lruObjects = new LRUObject[size];
        secondChanceObjects = new SecondChanceObject[size];
        fifo = new int[size];
    }

    @Override
    public void run() {
        while(true) {
            try {
                full.acquire();
                buffer.acquire();
                int data = bufferArray[bufferIndex];
                bufferIndex = (bufferIndex+1)%size;
                empty.release();
                buffer.release();
                System.out.println("consumer received the data");
                if(data != 0) {
                    LRU(data, ++dataCount);
                    secondChance(data);
                    FIFO(data);
                    printTables(data);
                } else {
                    System.out.println("LRU : " + lruPageFault + ", FIFO : " + fifoPageFault + ", Second-chance : " + secondPageFault);
                    break;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void LRU(int data, int count) {
        for (int i = 0; i < lruIndex; i++) {
            if(lruObjects[i] != null) {
                if(lruObjects[i].data == data) {
                    lruObjects[i].clock = count;
                    return;
                }
            }
        }
        if(lruIndex < size) {
            lruObjects[lruIndex] = new LRUObject();
            lruObjects[lruIndex].data = data;
            lruObjects[lruIndex].clock = count;
            ++lruIndex;
        } else {
            int minClk = lruObjects[0].clock;
            int minI = 0;
            for (int i = 1; i < size; i++) {
                if(lruObjects[i].clock < minClk) {
                    minI = i;
                    minClk = lruObjects[i].clock;
                }
            }
            lruObjects[minI].clock = count;
            lruObjects[minI].data = data;
        }
        ++lruPageFault;
    }

    private void secondChance(int data) {
        for (int i = 0; i < secondSize; i++) {
            if(secondChanceObjects[i] != null) {
                if(secondChanceObjects[i].data == data) {
                    secondChanceObjects[i].referenceBit = 1;
                    return;
                }
            }
        }
        if(secondSize < size) {
            secondChanceObjects[secondFirst] = new SecondChanceObject();
            secondChanceObjects[secondFirst].data = data;
            secondChanceObjects[secondFirst].referenceBit = 0;
            secondFirst = (secondFirst+1)%size;
            ++secondSize;
        } else {
            while(secondChanceObjects[secondFirst].referenceBit != 0) {
                secondChanceObjects[secondFirst].referenceBit = 0;
                secondFirst = (secondFirst+1)%size;
            }
            secondChanceObjects[secondFirst].data = data;
            secondChanceObjects[secondFirst].referenceBit = 0;
            secondFirst = (secondFirst+1)%size;
        }
        ++secondPageFault;
    }

    private void FIFO(int data) {
        for (int i = 0; i < fifoLast; i++) {
            if (fifo[i] == data)
                return;
        }
        if(fifoLast < size) {
            fifo[fifoLast++] = data;
        } else {
            fifo[fifoFirst] = data;
            fifoFirst = (fifoFirst+1)%size;
        }
        ++fifoPageFault;
    }

    private void printTables(int data) {
        System.out.println("for data : " + data);
        /* printing LRU results : */
        System.out.print("LRU :\n[");
        for (int i = 0; i < size-1; i++) {
            if(lruObjects[i] != null)
                System.out.print(" " + lruObjects[i].data + ",");
            else
                System.out.print(" free table,");
        }
        if(lruObjects[size-1] != null)
            System.out.println(" " + lruObjects[size-1].data + "]");
        else
            System.out.println(" free table]");
        /* printing Second-chance results : */
        System.out.print("Second-chance :\n[");
        for (int i = 0; i < size-1; i++) {
            if(secondChanceObjects[i] != null)
                System.out.print(" " + secondChanceObjects[i].data + ",");
            else
                System.out.print(" free table,");
        }
        if(secondChanceObjects[size-1] != null)
            System.out.println(" " + secondChanceObjects[size-1].data + "]");
        else
            System.out.println(" free table]");
        /* printing FIFO results : */
        System.out.print("FIFO :\n[");
        for (int i = 0; i < size-1; i++) {
            if(fifo[i] != 0)
                System.out.print(" " + fifo[i] + ",");
            else
                System.out.print(" free table,");
        }
        if(fifo[size-1] != 0)
            System.out.println(" " + fifo[size-1] + "]");
        else
            System.out.println(" free table]");

    }
}

class LRUObject {
    public int data;
    public int clock;
}

class SecondChanceObject {
    public int data;
    public int referenceBit = 0;
}

/* ///////////////////////////////////////
        System.out.print("clock :\n[");
        for (int i = 0; i < size-1; i++) {
            if(lruObjects[i] != null)
                System.out.print(" " + lruObjects[i].clock + ",");
            else
                System.out.print(" free table,");
        }
        if(lruObjects[size-1] != null)
            System.out.println(" " + lruObjects[size-1].clock + "]");
        else
            System.out.println(" free table]");*/
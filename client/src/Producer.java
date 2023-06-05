import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class Producer {
    public static void main(String[] args) {
        int[] bufferArray;
        int buffer_index = 0, data, n;
        Semaphore full, empty, buffer;
        try(Socket socket = new Socket("localhost", 8080);
            DataInputStream din = new DataInputStream(socket.getInputStream())) {
            n = din.readInt();
            System.out.println("n is : " + n);
            full = new Semaphore(0);
            empty = new Semaphore(n);
            buffer = new Semaphore(1);
            bufferArray = new int[n];
            Consumer consumer = new Consumer(full, empty, buffer, n, bufferArray);
            consumer.start();
            while(true) {
                data = din.readInt();
                System.out.println("producer received the data");
                empty.acquire();
                buffer.acquire();
                //System.out.println("buffer index : " + buffer_index);
                bufferArray[buffer_index] = data;
                buffer_index = (buffer_index+1)%n;
                //System.out.println("new buffer index : " + buffer_index);
                full.release();
                buffer.release();
                if(n == 0) {
                    consumer.join();
                    break;
                }
            }
        }catch (Exception e) {
            System.out.println("process finished");
            //e.printStackTrace();
        }
    }
}

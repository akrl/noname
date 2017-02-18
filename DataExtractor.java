import java.io.DataInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.LinkedList;
import java.io.FileReader;
import java.io.FileNotFoundException;

/*TODO add data checks to flag corrupted stock values*/


//needs to be modularised 
//need to add proper exception handling
public class DataExtractor {

    public static void main(String args[]) throws Exception {

        /*uses args[0] to determine type of program usage*/
        /*TODO alow live and historical data to be controled via inline options*/
        //
        if (args.length == 0) {
            instructions();
            return;
        } else if (args[0].equals("-h") && (args.length == 2)) {
            System.out.println("This is the historical data option.");
            historicalData(args[1]);
            return;
        } else if (args[0].equals("-l")) {
            System.out.println("This is the live data option.");
            liveData();
            return;
        } else {
            System.out.println("Invalid options. Give no arguments to see useage instructions.");
            return;
        }
        //
    }

    public static LinkedList<Stock> extract(String args[]) throws Exception {
        if (args.length == 0) {
            instructions();
            return null;
        } else if (args[0].equals("-h") && (args.length == 2)) {
            System.out.println("This is the historical data option.");
            return historicalData(args[1]);
        } else if (args[0].equals("-l")) {
            System.out.println("This is the live data option.");
            liveData();
            return null;
        } else {
            System.out.println("Invalid options. Give no arguments to see useage instructions.");
            return null;
        }
    }
        

    //provides instructions for usage
    private static void instructions(){
        System.out.println("DataExtractor usage Guide:");
        System.out.println("DataExtractor -[option] [arguments]...\n");
        System.out.println("Options:");
        System.out.println("-h      Takes a single argument, [FileName]");
        System.out.println("        Processes stock data from file(assumes first line is context line, does not process first line)");
        System.out.println("-l      Currently takes no arguments.");
        System.out.println("        Connects to the FTSE100 stream at \"cs261.dcs.warwick.ac.uk\" on port 80 and processes the first 2 values");    //TODO later change this to all values to message queue once asynchronus finished
        System.out.println("\nExample usage:");
        System.out.println("DataExtractor -h file       Will process the stock data in file.");
    }

    private static void liveData() throws Exception{
        String value;
        LinkedList<String> queue = new LinkedList<String>();

        Socket s = new Socket("cs261.dcs.warwick.ac.uk", 80);   //establishes a server socket

        InputStream is = s.getInputStream();
        BufferedReader dis = new BufferedReader(new InputStreamReader(is));

        /*appears to pick up all data, but may miss some if the market speeds up*/
        /*reads values from server feed and places them in a queue*/
        /*TODO implement asynchronus usage*/
        for (int i = 0; i < 3;i++) {
            value = dis.readLine();

            System.out.println(value);
            queue.add(value);
        }

        s.close();

        Stock[] stocks = new Stock[2];
        String[] str;
        int i = 0;
        queue.removeFirst();    //skips first line (reference line contains no data)
        while (queue.size() != 0) {
            stocks[i] = stockBuilder(queue.removeFirst());
            i++;
        }
        System.out.println(stocks[0].getTime());
    }

    private static LinkedList<Stock> historicalData(String fileName) throws Exception{
        String str;
        LinkedList<Stock> queue = new LinkedList<Stock>();
        if ((fileName == null) || (fileName.equals(""))) {
            System.out.println("Error: No file name entered");
        }
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            in.readLine();
            while ((str = in.readLine()) != null)  {
                queue.add(stockBuilder(str));
            }
            in.close();
        } catch (FileNotFoundException e) {
            System.out.println("Error: Cannot find file: " + fileName);
            return null;
        }
        return queue;
        //System.out.println(queue.getFirst().getTime());
    }

    /*splits the string by commas, then builds and returns a Stock object using the stock values*/
    private static Stock stockBuilder(String stock){
        String[] str = stock.split(",");
        return new Stock(str[0], str[1], str[2], Double.parseDouble(str[3]), Integer.parseInt(str[4]), str[5], str[6], str[7], Double.parseDouble(str[8]), Double.parseDouble(str[9]));
    }

}





//tests:
//ensure all transactions are being picked up by the system

//problems encountered in implementation:
//any uneccasary calls in the retevial loop cause misses on the data, as such it is ideal to have the loop perform on a seperate cpu with a shared data queue to any further processing
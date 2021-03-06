import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.Locale;
import java.util.Date;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.Arrays;
import java.lang.Math;


class PumpAndDump implements ICheck{

	LinkedList<Double> prices;			//TODO decide max length for this
	LinkedList<Double> pmas;
	long period;			//determines current period for grouping, currently minutes
	int k = 100;			//number of stocks to pass to anomaly
	boolean flag; 	//has this time period already been flagged
	int limit; //= 5;		//used to gauge the difference between periods for flagging		TODO make this dynamic to pma, but not overly sensitive at small numbers
	double pma;
	boolean startFlag;
	long periodLength = 900000;
	double a = 0.18;			//alpha equal to 2/(1+N) where N is the number of periods in this case 10	TODO choose dynamicaly based on k
	int diff;					//used in the detection of time period gaps
	int channel = 0 ; // default value

	public PumpAndDump(Stock stock, int channel) throws ParseException{
		this.channel = channel;
		long time = stock.getTime();
		period = time - (time % periodLength) + periodLength;

		prices = new LinkedList<Double>() ;
		prices.add(stock.getPrice());

		flag = true;
		startFlag = true;
	}

	public void update(Stock stock){


		try {
			/*if time within period add to current price list*/
			if (stock.getTime() < period) {
				prices.set((prices.size() - 1), (prices.getLast() + stock.getPrice()) );
			} else {
				// section for coping with empty time periods	(works in theory has yet to be tested with gaps)
				diff = (int) ((stock.getTime() - period) / periodLength);
				if (diff >= 1) {
					for (int i = 0; i < diff ; i++) {
						calculatePma();
						prices.add(0.0);
						period += periodLength;
						if (prices.size() > k) {
							prices.removeFirst();
							pmas.removeFirst();
						}
					}
				}

		    	calculatePma();

				prices.add(1.0);
				period += periodLength;
				flag = false;
				if (prices.size() > k) {
					prices.removeFirst();
					pmas.removeFirst();
				}
			}

		} catch (Exception e) {
			return;
		}

	}

	public Anomaly check(Stock stock, Client client) {
		
		
		if(flag || pmas.size() < 60) {
			return null;
		}
		else { // there is a sufficient amount of averages to analyse for a pump and dump
			
			boolean	dumpingState = false ;
			// check for dumpingState
			// look at latest and previous price averages
			//look at percentage increase/decrease, if there is a large enough decrease then dumping state is true

			double difference = pmas.getLast() -  pmas.get(pmas.size()-2) ;
			double percentage_decrease = (difference/pmas.get(pmas.size()-2)) *100 ;

			if (percentage_decrease < 0 ){ // it is negative
				if(Math.abs(percentage_decrease) >= 10){// if there is a 10% or more percentage decrease flag a dumping state
					
					System.out.println("PERCENTAGE DECREASE IS GREATER THAN 10");
					System.out.println(percentage_decrease);
					
					dumpingState = true;
				}
			}

			//check for Pumping before the Dumping
			double[] Yarray = new double[50];
			int t = pmas.size()-1 ;
			int pumping = 0 ; // this variable will keep track of pumps that occured before the dump state if the
			if (dumpingState) {
				for (int i = t ; i >= t - 50 ; i--) { // for the last 50 transactions
					if(pmas.get(i)>pmas.get(i-1)){
						pumping++ ;
					}
				}
				if (pumping >= 20) {
					
					// if code reaches here, then pumping state is true
					// build array so it contains last 50 recent prices
					long tStart = period - (pmas.size() * periodLength) ;
					int counter =  0 ;
					for (int x = pmas.size() - 50; x < (pmas.size()) ; x++) {
						Yarray[counter] = pmas.get(x) ;
						counter++;
					}
					// send dat shiz off
					PDAnomaly anomaly = new PDAnomaly(client.getCounter(), channel, stock.getSymbol(), Yarray, tStart, this.periodLength);
					flag = true;
					
					
					return anomaly;
				}

			}
			flag = true;
		}
		return null ;

	}

private void calculatePma() {

		if (startFlag) {
			pma = prices.getLast();
			startFlag = false;
			pmas = new LinkedList<Double>();
			pmas.add(pma);
		}

		else {
			pma = (int) Math.ceil((a * prices.getLast()) + ((1 - a) * pma));			//Exponential Moving Average where a is alpha
			limit = (int) Math.ceil(0.7 * pma);
			pmas.add(pma);
		}

	}



}

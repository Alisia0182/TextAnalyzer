import scala.Tuple2;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.api.java.JavaSparkContext;

import com.google.common.collect.Lists;
import java.io.*;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class WordGraph {
	private static final Pattern SPACE = Pattern.compile(" ");

	/*
	 * The main function needs to create a word graph of the text files provided in
	 * arg[0]
	 * The output of the word graph should be written to arg[1]
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.err.println("Usage: JavaWordGraph <file>");
			System.exit(1);
		}

		PrintStream ps = new PrintStream(new FileOutputStream(args[1]));
		System.setOut(ps);

		SparkSession spark = SparkSession
				.builder()
				.appName("JavaWordGraph")
				.getOrCreate();

		JavaRDD<String> files = spark.read().textFile(args[0]).javaRDD();

		JavaRDD<String> convertedLines = files
		.flatMap(file->Arrays.asList(file.split("\n")).iterator())
		.map(line -> line.toLowerCase().replaceAll("[^A-Za-z0-9]", " "));
		// convert to lower case
		// non-alphanumeric characters ==> white-space

        // create a pair RDD where the key is a pair of words that co-occur and the value is 1
        JavaPairRDD<Tuple2<String, String>, Integer> pairRdd = convertedLines.flatMapToPair(line -> {
			String[] words = line.split(" ");
			List<Tuple2<Tuple2<String, String>, Integer>> coOccurCountTuples = new LinkedList<>();
			int len = words.length;
			if(len >= 2){
				for(int i = 0; i < len - 1; ++ i){
					if(words[i].length() != 0){
						int j = i + 1;
						while(j < len && words[j].length() == 0) ++ j;
						if(j < len)
							coOccurCountTuples.add(new Tuple2<>(new Tuple2<>(words[i], words[j]),1));
						else
							break;
					}
				}
			}
			return coOccurCountTuples.iterator();
		})
		.reduceByKey((x,y) -> x + y);
        // ((word1, word2), # of pairs)

		// (word1, (word2, # of pairs))
		JavaPairRDD<String, Tuple2<String, Integer>> prePairRDD = pairRdd.mapToPair(
			pair -> {
				return new Tuple2<>(pair._1()._1(),new Tuple2<>(pair._1()._2(), pair._2()));
			}
		);

		// (word1, # of word1)
		JavaPairRDD<String, Integer> preCounterRDD = prePairRDD.mapToPair(
			pair -> {
				return new Tuple2<>(pair._1(), pair._2()._2());
			}
		)
		.reduceByKey((x,y)->x + y);
		
		// (word1, # of edges-of-word1)
		JavaPairRDD<String, Tuple2<String, Integer>> cntEdgeRDD = pairRdd.mapToPair(
			pair -> {
				return new Tuple2<>(pair._1()._1(),new Tuple2<>(pair._1()._2(), 1));
			}
		);
		JavaPairRDD<String, Integer> edgeCounterRDD = cntEdgeRDD.mapToPair(
			pair -> {
				return new Tuple2<>(pair._1, pair._2()._2());
			}
		)
		.reduceByKey((x, y) -> x + y);
		/*
		// print out
		for(Tuple2<String, Integer> entry : edgeCounterRDD.collect()){
			String key = entry._1();
			Integer num = entry._2();
			System.out.println(key + " " + num);
		}
		
		for(int i = 0; i < 40; ++ i) {
			Sytem.out.print("#");
		}
		Sytem.out.println("");
		*/
		// join: (word1, ((word2, # of pairs), # of word1))
		JavaPairRDD<String, Tuple2< Tuple2<String, Integer>, Integer>> joinedRDD = prePairRDD.join(preCounterRDD);

		// mapToPair: to calculate fractions
		// (word1, ((word2, # of pairs / # of word1), # of word1))
		JavaPairRDD<String, Tuple2 <Tuple2<String, Double>, Integer>> calculatedRDD = joinedRDD.mapToPair(
			pair -> {
				Double fraction = pair._2()._1()._2().doubleValue()/pair._2()._2().doubleValue();
				return new Tuple2<>(pair._1, new Tuple2<>(new Tuple2<>(pair._2()._1()._1(), fraction),pair._2()._2()));
			}
		);
		
		// (word1, (word2, # of pairs / # of word1))
		JavaPairRDD<String, Tuple2<String, Double> > fracRDD = calculatedRDD.mapToPair(
			pair -> {
				return new Tuple2<>(pair._1, pair._2()._1());
			}
		);
		
		// (word1, (word2, # of pairs / # of word1), # of edges-of-word1)
		JavaPairRDD<String, Tuple2< Tuple2<String, Double>, Integer>> finalRDD = fracRDD.join(edgeCounterRDD);

		 /* How to print out, with the output grouped by word1 <== groupByKey
		 * Print out
		 */
		DecimalFormat df = new DecimalFormat("#.000");
		JavaPairRDD<String, Iterable<Tuple2 <Tuple2<String, Double>, Integer>>> groupedRDD = finalRDD.groupByKey();

		for(Tuple2<String, Iterable<Tuple2 <Tuple2<String, Double>, Integer>>>group : groupedRDD.collect()){
			String key = group._1();
			Iterable<Tuple2 <Tuple2<String, Double>, Integer>> pairs = group._2();// Weird
			boolean firstPair = true;
			for(Tuple2 <Tuple2<String, Double>, Integer> pair:pairs){
				
				if(firstPair){
					System.out.println(key + " " + pair._2());
					firstPair = false;
				}
				System.out.println("<" + pair._1()._1() + "," + df.format(pair._1()._2()) + ">");
			}
		}

		spark.stop();
	}
}

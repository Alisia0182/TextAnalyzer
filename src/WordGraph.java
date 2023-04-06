import scala.Tuple2;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.api.java.JavaSparkContext;

import com.google.common.collect.Lists;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class WordGraph {
	private static final Pattern SPACE = Pattern.compile(" ");
	/*
	* The main function needs to create a word graph of the text files provided in arg[0]
	* The output of the word graph should be written to arg[1]
	*/
    public static void main(String[] args) throws Exception {
		if(args.length < 1) {
			System.err.println("Usage: JavaWordCount <file>");
			System.exit(1);
		}
		
        PrintStream ps = new PrintStream(new FileOutputStream(args[1]));
		System.setOut(ps);

		SparkSession spark = SparkSession
							.builder()
							.appName("JavaWordGraph")
							.getOrCreate();
		
		JavaRDD<String> lines = spark.read().textFile(args[0]).javaRDD();
		JavaRDD<String> words = lines.flatMap(s -> Arrays.asList(SPACE.split(s)).iterator());

		JavaPairRDD<String, Integer> ones = words.mapToPair(s -> new Tuple2<>(s,1));
		JavaPairRDD<String, Integer> counts = ones.reduceByKey((i1, i2) -> i1 + i2);

		List<Tuple2<String, Integer>> output = counts.collect();
		
		for(Tuple2<?,?> tuple: output){
			System.out.println(tuple._1() + ": " + tuple._2());

		}
		spark.stop();
    }
}

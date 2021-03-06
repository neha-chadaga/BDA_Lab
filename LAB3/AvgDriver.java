package com.hadoop.imcdp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class AvgDriver extends Configured implements Tool{

	public static class ImcdpMap extends Mapper<LongWritable, Text, IntWritable, IntWritable> {
		
		String record;
		
		protected void map(LongWritable key, Text value, Mapper.Context context) throws IOException, InterruptedException {
			record = value.toString();
			String[] fields = record.split(",");
			
			Integer s_id = new Integer(fields[0]);
			Integer marks = new Integer(fields[2]);
			context.write(new IntWritable(s_id), new IntWritable(marks));
		} // end of map method
	} // end of mapper class
	

	public static class ImcdpReduce extends Reducer<IntWritable, IntWritable, IntWritable, FloatWritable> {
		
		protected void reduce(IntWritable key, Iterable<IntWritable> values, Reducer<IntWritable, IntWritable, IntWritable, FloatWritable>.Context context) throws IOException, InterruptedException {
			Integer s_id = key.get();
			Integer sum = 0;
			Integer cnt = 0;
			
			for (IntWritable value:values) {
				sum = sum + value.get();
				cnt = cnt + 1;
			}
			
			Float avg_m = (float) sum/cnt;
			context.write(new IntWritable(s_id), new FloatWritable(avg_m));
		}
	}
	
	@Override
	public int run(String[] args) throws Exception {
		Configuration conf = new Configuration();
		args = new GenericOptionsParser(conf, args).getRemainingArgs();
		String input = args[0];
		String output = args[1];
		
		Job job = new Job(conf, "Avg");
		job.setJarByClass(ImcdpMap.class);
		job.setInputFormatClass(TextInputFormat.class);
		job.setMapperClass(ImcdpMap.class);
		job.setMapOutputKeyClass(IntWritable.class);
		job.setMapOutputValueClass(IntWritable.class);
		
		job.setReducerClass(ImcdpReduce.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(FloatWritable.class);
		
		FileInputFormat.setInputPaths(job, new Path(input));
		Path outPath = new Path(output);
		FileOutputFormat.setOutputPath(job, outPath);
		outPath.getFileSystem(conf).delete(outPath, true);
		
		job.waitForCompletion(true);
		return (job.waitForCompletion(true) ? 0 : 1);
	}
	
    public static void main(String[] args) throws Exception {
        int exitCode = ToolRunner.run(new AvgDriver(), args);
        System.exit(exitCode);
    }
}

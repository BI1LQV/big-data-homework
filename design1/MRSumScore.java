package design1;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;

public class MRSumScore {
	public static Put Row(String rowkey, String colFamily, String... kv) {
		Put row = new Put(Bytes.toBytes(rowkey));
		byte[] family = Bytes.toBytes(colFamily);
		// for (int i = 0; i < kv.length; i += 2)
		// 	// 循环体内需要添加一行代码
			
		return row;
	}

	public static class doScoreMapper extends TableMapper<Text, FloatWritable> {
		protected void map(ImmutableBytesWritable key, Result value, Context context)
				throws IOException, InterruptedException {
			String studID = Bytes.toString(key.get());
			for (Cell c : value.rawCells()) {
				// 需要完成以下代码（10行以内）




			}
		}
	}

	public static class doScoreReducer extends
			TableReducer<Text, FloatWritable, NullWritable> {
		protected void reduce(Text key, Iterable<FloatWritable> values,
				Context context) throws IOException, InterruptedException {
			// 需要完成以下代码（10行以内）

		
		
		
		
		}
	}

	public static void main(String[] args) throws IOException,
			ClassNotFoundException, InterruptedException {
		Configuration conf = HBaseConfiguration.create();
		conf.set("hbase.rootdir", "hdfs://BigData1:9000/hbase");
		conf.set("hbase.zookeeper.quorum", "BigData1");

		Job job = Job.getInstance(conf);
		job.setJarByClass(MRSumScore.class);
		job.setJobName("EEA.MRSumScore");
		Scan scan = new Scan();
		scan.setCaching(500);
		scan.setCacheBlocks(false);
		scan.addFamily(Bytes.toBytes("subScore"));

		TableMapReduceUtil.initTableMapperJob("student", scan,
				doScoreMapper.class, Text.class, FloatWritable.class, job);
		TableMapReduceUtil.initTableReducerJob("student", doScoreReducer.class, job);

		boolean res = job.waitForCompletion(true);
		System.out.println("Score-MapReduce " + (res ? "Completed" : "Failure"));
		System.exit(res ? 0 : 1);
	}
}
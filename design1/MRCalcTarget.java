package design1;

import java.io.IOException;
import java.util.function.Consumer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.ColumnPrefixFilter;
import org.apache.hadoop.hbase.filter.PrefixFilter;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;

public class MRCalcTarget {
	private static Configuration conf;

	public static Put Row(String rowkey, String colFamily, String... kv) {
		Put row = new Put(Bytes.toBytes(rowkey));
		byte[] family = Bytes.toBytes(colFamily);
		for (int i = 0; i < kv.length; i += 2)
			// 循环体内需要添加一行代码

			return row;
	}

	public static class doTargetMapper extends TableMapper<Text, FloatWritable> {
		protected void map(ImmutableBytesWritable key, Result value,
				Context context) throws IOException, InterruptedException {
			String course = Bytes.toString(key.get());
			int nPaperSn = 0, nQuesSn = 1, nScore = 0, nTargetNo = 1;
			HTable tbStud = new HTable(conf, "student");
			for (Cell c : value.rawCells()) {
				String ck[] = new String(CellUtil.cloneQualifier(c)).split(":");
				String cv[] = new String(CellUtil.cloneValue(c)).split("\t");
				String PaperSn = ck[0], QuesSn = ck[1], QScore = cv[0], TargetNo[] = cv[1].split(";");
				String mapkey = course + ":" + PaperSn.substring(0, 2) + ":";
				for (String tar : TargetNo) {
					// System.out.println(mapkey+tar+"\t-"+QScore);
					context.write(new Text(mapkey + tar), new FloatWritable(-Float.parseFloat(QScore)));
				}

				Scan scan = new Scan();
				scan.addFamily(Bytes.toBytes("subScore"));
				// 需要完成以下代码（10行以内）

			}
		}
	}

	public static class doTargetReducer extends TableReducer<Text, FloatWritable, NullWritable> {
		protected void reduce(Text key, Iterable<FloatWritable> values,
				Context context) throws IOException, InterruptedException {
			// 需要完成以下代码（10行以内）

		}
	}

	public static void main(String[] args) throws ClassNotFoundException,
			IOException, InterruptedException {
		conf = HBaseConfiguration.create();
		conf.set("hbase.rootdir", "hdfs://BigData1:9000/hbase");
		conf.set("hbase.zookeeper.quorum", "BigData1");

		Job job = Job.getInstance(conf);
		job.setJarByClass(MRCalcTarget.class);
		job.setJobName("EEA.MRCalcTarget");
		Scan scan = new Scan();
		scan.setCaching(500);
		scan.setCacheBlocks(false);
		scan.addFamily(Bytes.toBytes("paper"));

		TableMapReduceUtil.initTableMapperJob("course", scan, doTargetMapper.class,
				Text.class, FloatWritable.class, job);
		TableMapReduceUtil.initTableReducerJob("course", doTargetReducer.class, job);

		boolean res = job.waitForCompletion(true);
		System.out.println("Target-MapReduce " + (res ? "Completed" : "Failure"));
		System.exit(res ? 0 : 1);
	}
}
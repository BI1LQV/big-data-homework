package design1;

import java.io.IOException;
// import java.util.function.Consumer;

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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MRCalcTarget {
	private static Configuration conf;

	public static Put Row(String rowkey, String colFamily, String... kv) {
		Put row = new Put(Bytes.toBytes(rowkey));
		byte[] family = Bytes.toBytes(colFamily);
		for (int i = 0; i < kv.length; i += 2) {
			row.add(family, Bytes.toBytes(kv[i]), Bytes.toBytes(kv[i + 1]));
		}
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
				String cv[] = new String(CellUtil.cloneValue(c)).split(" ");
				String PaperSn = ck[0], QuesSn = ck[1], QScore = cv[1], TargetNo[] = cv[0].split(";");
				String mapkey = course + ":" + PaperSn.substring(0, 2) + ":";
				for (String tar : TargetNo) {
					context.write(new Text(mapkey + tar), new FloatWritable(-Float.parseFloat(QScore)));
				}

				Scan scan = new Scan();
				scan.addColumn(Bytes.toBytes("subScore"), Bytes.toBytes(course + ":" + PaperSn + ":" + QuesSn));
				ResultScanner ress = tbStud.getScanner(scan);
				float sum = 0;
				int count = 0;
				for (Result res : ress) {
					sum += Float.parseFloat(new String(res.value()));
				}
				float average = sum / count;
				for (String tar : TargetNo) {
					context.write(new Text(mapkey + tar), new FloatWritable(average));
				}
			}
		}
	}

	public static class doTargetReducer extends TableReducer<Text, FloatWritable, NullWritable> {
		protected void reduce(Text key, Iterable<FloatWritable> values,
				Context context) throws IOException, InterruptedException {
			int positiveCount = 0;
			int negativeCount = 0;
			float positiveSum = 0;
			float negativeSum = 0;
			for (FloatWritable score : values) {
				float thisScore = score.get();
				if (thisScore > 0) {
					positiveCount += thisScore;
					positiveCount++;
				} else {
					negativeCount += thisScore;
					negativeCount++;
				}
			}
			String fullKey = key.toString();
			Matcher matcher = Pattern.compile("^(.+):(\\d+:\\d+)$").matcher(fullKey);
			matcher.find();
			String courseName = matcher.group(1);
			String targetId = matcher.group(2);
			context.write(NullWritable.get(), Row(courseName, "target", targetId, (positiveSum / negativeSum)
					+ ""));
		}
	}

	public static void main(String[] args) throws ClassNotFoundException,
			IOException, InterruptedException {
		conf = HBaseConfiguration.create();
		conf.set("hbase.rootdir", "hdfs://bd:9000/hbase");
		conf.set("hbase.zookeeper.quorum", "bd");

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
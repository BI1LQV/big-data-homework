package design1;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class MRRankScore {
	public static class paperScore implements WritableComparable<paperScore> {
		public String paper;
		public float score;

		@Override
		public int compareTo(paperScore ps) {
			int res = paper.compareTo(ps.paper);
			return res != 0 ? res : (score == ps.score ? 0 : (score > ps.score ? -1 : 1));
		}

		@Override
		public void write(DataOutput out) throws IOException {
			out.writeUTF(paper);
			out.writeFloat(score);
		}

		@Override
		public void readFields(DataInput in) throws IOException {
			paper = in.readUTF();
			score = in.readFloat();
		}
	}

	public static class paperPartitioner extends Partitioner<paperScore, FloatWritable> {
		@Override
		public int getPartition(paperScore key, FloatWritable value, int numPartitions) {
			return (key.paper.hashCode() & Integer.MAX_VALUE) % numPartitions;
		}
	}

	public static class GroupingComparator extends WritableComparator {
		protected GroupingComparator() {
			super(paperScore.class, true);
		}

		@Override
		public int compare(WritableComparable a, WritableComparable b) {
			return ((paperScore) a).paper.compareTo(((paperScore) b).paper);
		}
	}

	public static class doRankMapper extends TableMapper<paperScore, Text> {
		private final paperScore ps = new paperScore();

		protected void map(ImmutableBytesWritable key, Result value, Context context)
				throws IOException, InterruptedException {
			String studID = Bytes.toString(key.get());
			for (Cell c : value.rawCells()) {
				String cellKey = new String(CellUtil.cloneQualifier(c));
				String cellValue = new String(CellUtil.cloneValue(c));
				ps.paper = cellKey;
				ps.score = Float.parseFloat(cellValue);
				context.write(ps, new Text(studID));
			}
		}
	}

	public static class doRankReducer extends Reducer<paperScore, Text, Text, FloatWritable> {
		private String group = "";
		private int count = 0;

		public void reduce(paperScore key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			context.write(new Text(key.paper), new FloatWritable(key.score));
		}
	}

	public static void main(String[] args) throws IOException,
			ClassNotFoundException, InterruptedException, URISyntaxException {
		Configuration conf = HBaseConfiguration.create();
		conf.set("hbase.rootdir", "hdfs://bd:9000/hbase");
		conf.set("hbase.zookeeper.quorum", "bd");

		Job job = Job.getInstance(conf);
		job.setJarByClass(MRRankScore.class);
		job.setJobName("EEA.MRRankScore");

		Scan scan = new Scan();
		scan.setCaching(500);
		scan.setCacheBlocks(false);
		scan.addFamily(Bytes.toBytes("score"));

		TableMapReduceUtil.initTableMapperJob("student", scan, doRankMapper.class,
				paperScore.class, Text.class, job);

		job.setReducerClass(doRankReducer.class);
		job.setOutputKeyClass(paperScore.class);
		job.setOutputValueClass(Text.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		Path out = new Path("hdfs://bd:9000/out/");
		FileSystem fs = FileSystem.get(new URI("hdfs://bd:9000/"), conf);
		if (fs.exists(out))
			fs.delete(out, true);
		FileOutputFormat.setOutputPath(job, out);

		boolean res = job.waitForCompletion(true);
		System.out.println("Rank-MapReduce " + (res ? "Completed" : "Failure"));
		System.exit(res ? 0 : 1);
	}
}
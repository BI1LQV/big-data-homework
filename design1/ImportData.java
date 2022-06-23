package design1;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

public class ImportData {
	// 建表，通过可变参数，接收多列族名称
	public static void createTable(Configuration conf, String TableName,
			String... Family) throws IOException {
		HTableDescriptor tableDesc = new HTableDescriptor(TableName);
		for (String family : Family) {
			tableDesc.addFamily(new HColumnDescriptor(family));
		}
		HBaseAdmin admin = new HBaseAdmin(conf);
		if (admin.tableExists(TableName)) {
			System.out.println("Table exists,trying drop first!");
			admin.disableTable(TableName);
			admin.deleteTable(TableName);
		}
		System.out.println("create table: " + TableName);
		admin.createTable(tableDesc);
	}

	// 产生Put对象，用于插入数据行，键值对通过Map结构传递
	public static Put Row(String rowkey, String colFamily, Map<String, String> kv) {
		Put row = new Put(Bytes.toBytes(rowkey));
		byte[] family = Bytes.toBytes(colFamily);
		for (Map.Entry<String, String> i : kv.entrySet()) {
			row.add(family, Bytes.toBytes(i.getKey()), Bytes.toBytes(i.getValue()));
		}
		return row;
	}

	// 产生Put对象，用于插入数据行，键值对通过可变数组参数传递
	public static Put Row(String rowkey, String colFamily, String... kv) {
		Put row = new Put(Bytes.toBytes(rowkey));
		byte[] family = Bytes.toBytes(colFamily);
		for (int i = 0; i < kv.length; i += 2) {
			row.add(family, Bytes.toBytes(kv[i]), Bytes.toBytes(kv[i + 1]));
		}
		return row;
	}

	private static Result courRow = new Result();
	private static String preCourse = "";

	private static void putData(HTable tbStud, HTable tbCourse, String stud, String paper, String nowCourse,
			Map<String, String> subScore) throws IOException {
		if (nowCourse.equals("")) {
			return;
		}
		// 课程变化时需要读取course表中的课程数 据
		if (!nowCourse.equals(preCourse)) {
			courRow = tbCourse.get(new Get(Bytes.toBytes(nowCourse)));
			preCourse = nowCourse;
		}
		for (Cell c : courRow.rawCells()) {
			String key = new String(CellUtil.cloneQualifier(c));
			if (key.startsWith(paper)) {
				String mapKey = nowCourse + ":" + paper + ":" + key.split(":")[1];
				String thisScore = subScore.get(mapKey);
				Float calcedScore = Float.parseFloat(new String(CellUtil.cloneValue(c)).split(" ")[1]);
				if (thisScore != null) {
					calcedScore += Float.parseFloat(thisScore);
				}
				subScore.put(mapKey, calcedScore + "");
			}
		}
		tbStud.put(Row(stud, "subScore", subScore));
	}

	// 导入试卷数据
	public static void ImportPaper(FileSystem fs, Configuration conf)
			throws IOException {
		FSDataInputStream in = fs.open(new Path("/in/EEA_Paper.csv"));
		HTable tbCourse = new HTable(conf, "course");
		int nCourse = 0, nPaperSn = 1, nQuesSn = 2, nTargetNo = 3, nScore = 4;
		String line = in.readLine();
		while ((line = in.readLine()) != null) {
			// 解决导入时编码问题需要getBytes("ISO8859-1")
			String i[] = new String(line.getBytes("ISO8859-1")).split(",");
			tbCourse.put(Row(i[nCourse], "paper", i[nPaperSn] + ':' + i[nQuesSn], i[nTargetNo] + ' ' + i[nScore]));
		}
	}

	// 导入成绩数据
	public static void ImportScore(FileSystem fs, Configuration conf)
			throws IOException {
		FSDataInputStream in = fs.open(new Path("/in/EEA_Score.csv"));
		HTable tbCourse = new HTable(conf, "course");
		HTable tbStud = new HTable(conf, "student");
		Map<String, String> subScore = new HashMap<String, String>();

		String course = "", paper = "", stud = "";
		int nCourse = 0, nPaperSn = 1, nStudID = 2, nName = 3, nQuesSn = 4, nAns = 5;
		String line = in.readLine();
		while ((line = in.readLine()) != null) {
			String i[] = new String(line.getBytes("ISO8859-1")).split(",");
			// 学生或试卷编号变化时需要写入学生数据，更新score原数据
			if (!stud.equals(i[nStudID]) || !paper.equals(i[nPaperSn])) {
				if (!stud.isEmpty()) {
					putData(tbStud, tbCourse, stud, paper, course, subScore);
				}
				stud = i[nStudID];
				paper = i[nPaperSn];
				subScore = new HashMap<String, String>();
				tbStud.put(Row(i[nStudID], "info", "Name", i[nName]));
			}
			// 逐行处理数据，计算score值。
			String key = i[nCourse] + ":" + paper + ":" + i[nQuesSn];
			subScore.put(key, i[nAns]);
			course = i[nCourse];
		}
		// 最后一个学生成绩入库
		putData(tbStud, tbCourse, stud, paper, course, subScore);
	}

	public static void main(String[] args) throws ClassNotFoundException,
			IOException, InterruptedException {
		Configuration conf = HBaseConfiguration.create();
		conf.set("hbase.rootdir", "hdfs://bd:9000/hbase");
		conf.set("hbase.zookeeper.quorum", "bd");
		FileSystem hdfs = FileSystem.get(URI.create("hdfs://bd:9000"), conf);

		createTable(conf, "course", "target", "paper");
		ImportPaper(hdfs, conf);
		createTable(conf, "student", "info", "subScore", "score");
		ImportScore(hdfs, conf);
		System.out.println("Import Data Finish.");
	}
}
package design1.test;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

public class testImport {
    public static void main(String[] args) throws IOException{
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.rootdir", "hdfs://bd:9000/hbase");
        conf.set("hbase.zookeeper.quorum", "bd");
        HTable tbCourse = new HTable(conf, "course");
        HTable tbStud = new HTable(conf, "student");
        Result stuIns = tbStud.get(new Get(Bytes.toBytes("2012010241")));
        int count=0;
        for (Cell stu : stuIns.rawCells()) {
            String key = new String(CellUtil.cloneQualifier(stu));
            String value=new String(CellUtil.cloneValue(stu));
            System.out.println(key);
            System.out.println(value);
            count++;
        }
        System.out.println("found:"+count);
    }
}

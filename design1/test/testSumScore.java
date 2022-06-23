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
import java.util.Map;

public class testSumScore {
    public static void main(String[] args) throws IOException {
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.rootdir", "hdfs://bd:9000/hbase");
        conf.set("hbase.zookeeper.quorum", "bd");
        HTable tbCourse = new HTable(conf, "course");
        HTable tbStud = new HTable(conf, "student");
        Result stuIns = tbStud.get(new Get(Bytes.toBytes("2012010241")));
        int count = 0;
        Map<byte[], byte[]> map = stuIns.getFamilyMap(Bytes.toBytes("score"));
        for(Map.Entry<byte[], byte[]> entry:map.entrySet()){
            count++;
            System.out.println(Bytes.toString(entry.getKey())+" "+Bytes.toString(entry.getValue()));
        }
        System.out.println("found:" + count);
    }
}

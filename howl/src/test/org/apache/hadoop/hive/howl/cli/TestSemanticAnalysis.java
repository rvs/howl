package org.apache.hadoop.hive.howl.cli;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.conf.HiveConf.ConfVars;
import org.apache.hadoop.hive.howl.cli.SemanticAnalysis.HowlSemanticAnalyzer;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.MetaStoreUtils;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.hive.ql.Driver;
import org.apache.hadoop.hive.ql.processors.CommandProcessorResponse;
import org.apache.thrift.TException;

public class TestSemanticAnalysis extends TestCase{

  private Driver howlDriver;
  private Driver hiveDriver;
  private HiveMetaStoreClient msc;

  @Override
  protected void setUp() throws Exception {

    HiveConf howlConf = new HiveConf(this.getClass());
    howlConf.set(ConfVars.PREEXECHOOKS.varname, "");
    howlConf.set(ConfVars.POSTEXECHOOKS.varname, "");
    howlConf.set(ConfVars.HIVE_SUPPORT_CONCURRENCY.varname, "false");

    HiveConf hiveConf = new HiveConf(howlConf,this.getClass());
    hiveDriver = new Driver(hiveConf);

    howlConf.set(ConfVars.SEMANTIC_ANALYZER_HOOK.varname, HowlSemanticAnalyzer.class.getName());
    howlDriver = new Driver(howlConf);

    msc = new HiveMetaStoreClient(howlConf);
  }

  String query;
  private final String tblName = "junit_sem_analysis";

  public void testAlterTblTouch(){

    howlDriver.run("drop table junit_sem_analysis");
    howlDriver.run("create table junit_sem_analysis (a int) partitioned by (b string) stored as RCFILE");
    CommandProcessorResponse response = howlDriver.run("alter table junit_sem_analysis touch");
    assertEquals(10, response.getResponseCode());
    assertTrue(response.getErrorMessage().contains("Operation not supported."));

    howlDriver.run("alter table junit_sem_analysis touch partition (b='12')");
    assertEquals(10, response.getResponseCode());
    assertTrue(response.getErrorMessage().contains("Operation not supported."));

    howlDriver.run("drop table junit_sem_analysis");
  }

  public void testChangeColumns(){
    howlDriver.run("drop table junit_sem_analysis");
    howlDriver.run("create table junit_sem_analysis (a int, c string) partitioned by (b string) stored as RCFILE");
    CommandProcessorResponse response = howlDriver.run("alter table junit_sem_analysis change a a1 int");
    assertEquals(10, response.getResponseCode());
    assertTrue(response.getErrorMessage().contains("Operation not supported."));

    response = howlDriver.run("alter table junit_sem_analysis change a a string");
    assertEquals(10, response.getResponseCode());
    assertTrue(response.getErrorMessage().contains("Operation not supported."));

    response = howlDriver.run("alter table junit_sem_analysis change a a int after c");
    assertEquals(10, response.getResponseCode());
    assertTrue(response.getErrorMessage().contains("Operation not supported."));
    howlDriver.run("drop table junit_sem_analysis");
  }

  public void testAddReplaceCols() throws IOException, MetaException, TException, NoSuchObjectException{

    howlDriver.run("drop table junit_sem_analysis");
    howlDriver.run("create table junit_sem_analysis (a int, c string) partitioned by (b string) stored as RCFILE");
    CommandProcessorResponse response = howlDriver.run("alter table junit_sem_analysis replace columns (a1 tinyint)");
    assertEquals(10, response.getResponseCode());
    assertTrue(response.getErrorMessage().contains("Operation not supported."));

    response = howlDriver.run("alter table junit_sem_analysis add columns (d tinyint)");
    assertEquals(0, response.getResponseCode());
    assertNull(response.getErrorMessage());
    Table tbl = msc.getTable(MetaStoreUtils.DEFAULT_DATABASE_NAME, tblName);
    List<FieldSchema> cols = tbl.getSd().getCols();
    assertEquals(3, cols.size());
    assertTrue(cols.get(0).equals(new FieldSchema("a", "int", "from deserializer")));
    assertTrue(cols.get(1).equals(new FieldSchema("c", "string", "from deserializer")));
    assertTrue(cols.get(2).equals(new FieldSchema("d", "tinyint", null)));
    howlDriver.run("drop table junit_sem_analysis");
  }

  public void testAlterTblClusteredBy(){

    howlDriver.run("drop table junit_sem_analysis");
    howlDriver.run("create table junit_sem_analysis (a int) partitioned by (b string) stored as RCFILE");
    CommandProcessorResponse response = howlDriver.run("alter table junit_sem_analysis clustered by (a) into 7 buckets");
    assertEquals(10, response.getResponseCode());
    assertTrue(response.getErrorMessage().contains("Operation not supported."));
    howlDriver.run("drop table junit_sem_analysis");
  }

  public void testAlterTableSetFF() throws IOException, MetaException, TException, NoSuchObjectException{

    howlDriver.run("drop table junit_sem_analysis");
    howlDriver.run("create table junit_sem_analysis (a int) partitioned by (b string) stored as RCFILE");

    Table tbl = msc.getTable(MetaStoreUtils.DEFAULT_DATABASE_NAME, tblName);
    assertEquals("org.apache.hadoop.hive.ql.io.RCFileInputFormat",tbl.getSd().getInputFormat());
    assertEquals("org.apache.hadoop.hive.ql.io.RCFileOutputFormat",tbl.getSd().getOutputFormat());
    Map<String, String> tblParams = tbl.getParameters();
    assertEquals("org.apache.hadoop.hive.howl.rcfile.RCFileInputStorageDriver", tblParams.get("howl.isd"));
    assertEquals("org.apache.hadoop.hive.howl.rcfile.RCFileOutputStorageDriver", tblParams.get("howl.osd"));

    howlDriver.run("alter table junit_sem_analysis set fileformat INPUTFORMAT 'org.apache.hadoop.hive.ql.io.RCFileInputFormat' OUTPUTFORMAT " +
        "'org.apache.hadoop.hive.ql.io.RCFileOutputFormat' inputdriver 'mydriver' outputdriver 'yourdriver'");
    howlDriver.run("desc extended junit_sem_analysis");

    tbl = msc.getTable(MetaStoreUtils.DEFAULT_DATABASE_NAME, tblName);
    assertEquals("org.apache.hadoop.hive.ql.io.RCFileInputFormat",tbl.getSd().getInputFormat());
    assertEquals("org.apache.hadoop.hive.ql.io.RCFileOutputFormat",tbl.getSd().getOutputFormat());
    tblParams = tbl.getParameters();
    assertEquals("mydriver", tblParams.get("howl.isd"));
    assertEquals("yourdriver", tblParams.get("howl.osd"));

    howlDriver.run("drop table junit_sem_analysis");
  }

  public void testAddPartFail(){

    hiveDriver.run("drop table junit_sem_analysis");
    hiveDriver.run("create table junit_sem_analysis (a int) partitioned by (b string) stored as RCFILE");
    CommandProcessorResponse response = howlDriver.run("alter table junit_sem_analysis add partition (b='2') location '/some/path'");
    assertEquals(10, response.getResponseCode());
    assertTrue(response.getErrorMessage().contains("FAILED: Error in semantic analysis: Operation not supported. Partitions can be added only in a table created through Howl. It seems table junit_sem_analysis was not created through Howl."));
    hiveDriver.run("drop table junit_sem_analysis");
  }

  public void testAddPartPass() throws IOException{

    howlDriver.run("drop table junit_sem_analysis");
    howlDriver.run("create table junit_sem_analysis (a int) partitioned by (b string) stored as RCFILE");
    CommandProcessorResponse response = howlDriver.run("alter table junit_sem_analysis add partition (b='2') location '/tmp'");
    assertEquals(0, response.getResponseCode());
    assertNull(response.getErrorMessage());
    howlDriver.run("drop table junit_sem_analysis");
  }

  public void testCTAS(){
    howlDriver.run("drop table junit_sem_analysis");
    query = "create table junit_sem_analysis (a int) as select * from tbl2";
    CommandProcessorResponse response = howlDriver.run(query);
    assertEquals(10, response.getResponseCode());
    assertTrue(response.getErrorMessage().contains("FAILED: Error in semantic analysis: Operation not supported. Create table as Select is not a valid operation."));
    howlDriver.run("drop table junit_sem_analysis");
  }

  public void testStoredAs(){
    howlDriver.run("drop table junit_sem_analysis");
    query = "create table junit_sem_analysis (a int)";
    CommandProcessorResponse response = howlDriver.run(query);
    assertEquals(10, response.getResponseCode());
    assertTrue(response.getErrorMessage().contains("FAILED: Error in semantic analysis: STORED AS specification is either incomplete or incorrect."));
    howlDriver.run("drop table junit_sem_analysis");
  }

  public void testAddDriverInfo() throws IOException, MetaException, TException, NoSuchObjectException{

    howlDriver.run("drop table junit_sem_analysis");
    query =  "create table junit_sem_analysis (a int) partitioned by (b string)  stored as " +
    		"INPUTFORMAT 'org.apache.hadoop.hive.ql.io.RCFileInputFormat' OUTPUTFORMAT " +
    		"'org.apache.hadoop.hive.ql.io.RCFileOutputFormat' inputdriver 'mydriver' outputdriver 'yourdriver' ";
    assertEquals(0,howlDriver.run(query).getResponseCode());

    Table tbl = msc.getTable(MetaStoreUtils.DEFAULT_DATABASE_NAME, tblName);
    assertEquals("org.apache.hadoop.hive.ql.io.RCFileInputFormat",tbl.getSd().getInputFormat());
    assertEquals("org.apache.hadoop.hive.ql.io.RCFileOutputFormat",tbl.getSd().getOutputFormat());
    Map<String, String> tblParams = tbl.getParameters();
    assertEquals("mydriver", tblParams.get("howl.isd"));
    assertEquals("yourdriver", tblParams.get("howl.osd"));

    howlDriver.run("drop table junit_sem_analysis");
  }

  public void testInvalidateNonStringPartition() throws IOException{

    howlDriver.run("drop table junit_sem_analysis");
    query =  "create table junit_sem_analysis (a int) partitioned by (b int)  stored as RCFILE";

    CommandProcessorResponse response = howlDriver.run(query);
    assertEquals(10,response.getResponseCode());
    assertEquals("FAILED: Error in semantic analysis: Operation not supported. Howl only supports partition columns of type string. For column: b Found type: int",
        response.getErrorMessage());

  }

  public void testInvalidateSeqFileStoredAs() throws IOException{

    howlDriver.run("drop table junit_sem_analysis");
    query =  "create table junit_sem_analysis (a int) partitioned by (b string)  stored as SEQUENCEFILE";

    CommandProcessorResponse response = howlDriver.run(query);
    assertEquals(10,response.getResponseCode());
    assertEquals("FAILED: Error in semantic analysis: Operation not supported. Howl doesn't support Sequence File by default yet. You may specify it through INPUT/OUTPUT storage drivers.",
        response.getErrorMessage());

  }

  public void testInvalidateTextFileStoredAs() throws IOException{

    howlDriver.run("drop table junit_sem_analysis");
    query =  "create table junit_sem_analysis (a int) partitioned by (b string)  stored as TEXTFILE";

    CommandProcessorResponse response = howlDriver.run(query);
    assertEquals(10,response.getResponseCode());
    assertEquals("FAILED: Error in semantic analysis: Operation not supported. Howl doesn't support Text File by default yet. You may specify it through INPUT/OUTPUT storage drivers.",
        response.getErrorMessage());

  }

  public void testInvalidateClusteredBy() throws IOException{

    howlDriver.run("drop table junit_sem_analysis");
    query =  "create table junit_sem_analysis (a int) partitioned by (b string) clustered by (a) into 10 buckets stored as TEXTFILE";

    CommandProcessorResponse response = howlDriver.run(query);
    assertEquals(10,response.getResponseCode());
    assertEquals("FAILED: Error in semantic analysis: Operation not supported. Howl doesn't allow Clustered By in create table.",
        response.getErrorMessage());
  }

  public void testCTLFail() throws IOException{

    hiveDriver.run("drop table junit_sem_analysis");
    query =  "create table junit_sem_analysis (a int) partitioned by (b string) stored as RCFILE";

    hiveDriver.run(query);
    query = "create table like_table like junit_sem_analysis";
    CommandProcessorResponse response = howlDriver.run(query);
    assertEquals(10,response.getResponseCode());
    assertEquals("FAILED: Error in semantic analysis: Operation not supported. Table junit_sem_analysis should have been created through Howl. Seems like its not.",
        response.getErrorMessage());
  }

  public void testCTLPass() throws IOException, MetaException, TException, NoSuchObjectException{

    try{
      howlDriver.run("drop table junit_sem_analysis");
    }
    catch( Exception e){
      System.err.println(e.getMessage());
    }
    query =  "create table junit_sem_analysis (a int) partitioned by (b string) stored as RCFILE";

    howlDriver.run(query);
    String likeTbl = "like_table";
    howlDriver.run("drop table "+likeTbl);
    query = "create table like_table like junit_sem_analysis";
    assertEquals(0, howlDriver.run(query).getResponseCode());
    Table tbl = msc.getTable(MetaStoreUtils.DEFAULT_DATABASE_NAME, likeTbl);
    assertEquals(likeTbl,tbl.getTableName());
    List<FieldSchema> cols = tbl.getSd().getCols();
    assertEquals(1, cols.size());
    assertEquals(new FieldSchema("a", "int", null), cols.get(0));
    assertEquals("org.apache.hadoop.hive.ql.io.RCFileInputFormat",tbl.getSd().getInputFormat());
    assertEquals("org.apache.hadoop.hive.ql.io.RCFileOutputFormat",tbl.getSd().getOutputFormat());
    Map<String, String> tblParams = tbl.getParameters();
    assertEquals("org.apache.hadoop.hive.howl.rcfile.RCFileInputStorageDriver", tblParams.get("howl.isd"));
    assertEquals("org.apache.hadoop.hive.howl.rcfile.RCFileOutputStorageDriver", tblParams.get("howl.osd"));

    howlDriver.run("drop table junit_sem_analysis");
    howlDriver.run("drop table "+likeTbl);
  }
}
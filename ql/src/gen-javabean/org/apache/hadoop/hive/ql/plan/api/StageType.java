/**
 * Autogenerated by Thrift
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 */
package org.apache.hadoop.hive.ql.plan.api;


import java.util.HashMap;
import java.util.Map;

import org.apache.thrift.IntRangeSet;

public class StageType {
  public static final int CONDITIONAL = 0;
  public static final int COPY = 1;
  public static final int DDL = 2;
  public static final int MAPRED = 3;
  public static final int EXPLAIN = 4;
  public static final int FETCH = 5;
  public static final int FUNC = 6;
  public static final int MAPREDLOCAL = 7;
  public static final int MOVE = 8;
  public static final int STATS = 9;

  public static final IntRangeSet VALID_VALUES = new IntRangeSet(
    CONDITIONAL,
    COPY,
    DDL,
    MAPRED,
    EXPLAIN,
    FETCH,
    FUNC,
    MAPREDLOCAL,
    MOVE,
    STATS);

  public static final Map<Integer, String> VALUES_TO_NAMES = new HashMap<Integer, String>() {{
    put(CONDITIONAL, "CONDITIONAL");
    put(COPY, "COPY");
    put(DDL, "DDL");
    put(MAPRED, "MAPRED");
    put(EXPLAIN, "EXPLAIN");
    put(FETCH, "FETCH");
    put(FUNC, "FUNC");
    put(MAPREDLOCAL, "MAPREDLOCAL");
    put(MOVE, "MOVE");
    put(STATS, "STATS");
  }};
}

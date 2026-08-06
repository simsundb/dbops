package com.example.core;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * 数据源单元测试
 */
public class DataSourceTest {

    /**
     * 测试 Oracle Service Name 格式（斜杠）
     */
    @Test
    public void testDataSourceOracleServiceName() {
        DataSource ds = new DataSource(
                "oracle",
                "ORACLE",
                "localhost",
                1521,
                "ORCL",
                "scott",
                "tiger",
                true   // useServiceName = true
        );
        String expected = "jdbc:oracle:thin:@localhost:1521/ORCL";
        assertEquals(expected, ds.buildUrl());
    }

    /**
     * 测试 Oracle SID 格式（冒号）
     */
    @Test
    public void testDataSourceOracleSid() {
        DataSource ds = new DataSource(
                "oracle",
                "ORACLE",
                "localhost",
                1521,
                "ORCL",
                "scott",
                "tiger",
                false  // useServiceName = false
        );
        String expected = "jdbc:oracle:thin:@localhost:1521:ORCL";
        assertEquals(expected, ds.buildUrl());
    }

    /**
     * 测试 GaussDB（带 schema）
     */
    @Test
    public void testDataSourceGaussDB() {
        DataSource ds = new DataSource(
                "gauss",
                "GAUSSDB",
                "localhost",
                8000,
                "muts",
                "gk_sjdb",
                "testuser",
                "testpwd"
        );
        String expected = "jdbc:gaussdb://localhost:8000/muts?currentSchema=gk_sjdb";
        assertEquals(expected, ds.buildUrl());
    }

    /**
     * 测试 GaussDB（无 schema）
     */
    @Test
    public void testDataSourceGaussDBNoSchema() {
        DataSource ds = new DataSource(
                "gauss",
                "GAUSSDB",
                "localhost",
                8000,
                "muts",
                null,
                "testuser",
                "testpwd"
        );
        String expected = "jdbc:gaussdb://localhost:8000/muts";
        assertEquals(expected, ds.buildUrl());
    }
}
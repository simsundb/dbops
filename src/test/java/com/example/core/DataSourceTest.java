package com.example.core;

import com.example.utils.CryptoUtils;
import org.junit.Test;
import static org.junit.Assert.*;

public class DataSourceTest {

    @Test
    public void testCryptoUtils() {
        String original = "test123";
        String encrypted = CryptoUtils.encrypt(original);
        assertNotNull("Encrypted should not be null", encrypted);
        assertNotEquals("Encrypted should differ from original", original, encrypted);
        String decrypted = CryptoUtils.decrypt(encrypted);
        assertEquals("Decrypted should match original", original, decrypted);
    }

    @Test
    public void testDataSourceOracle() {
        DataSource ds = new DataSource("test-oracle", "ORACLE", "localhost", 1521, "ORCL", "user", "pwd");
        assertNotNull(ds);
        assertEquals("ORACLE", ds.getType());
        assertEquals("localhost", ds.getHost());
        assertEquals(1521, ds.getPort());
        assertEquals("ORCL", ds.getServiceName());
        assertEquals("user", ds.getUser());
        assertEquals("jdbc:oracle:thin:@localhost:1521:ORCL", ds.buildUrl());
    }

    @Test
    public void testDataSourceGaussDB() {
        DataSource ds = new DataSource("test-gauss", "GAUSSDB", "localhost", 5432, "mydb", "public", "user", "pwd");
        assertNotNull(ds);
        assertEquals("GAUSSDB", ds.getType());
        assertEquals("jdbc:gaussdb://localhost:5432/mydb?currentSchema=public", ds.buildUrl());
    }

    @Test
    public void testConnectionManager() {
        ConnectionManager manager = new ConnectionManager();
        assertNotNull("ConnectionManager should not be null", manager);
    }
}

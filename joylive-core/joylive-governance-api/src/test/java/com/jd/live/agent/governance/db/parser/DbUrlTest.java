/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.governance.db.parser;

import com.jd.live.agent.governance.db.DbUrl;
import com.jd.live.agent.governance.db.DbUrlParser;
import com.jd.live.agent.governance.db.parser.as400.As400UrlParser;
import com.jd.live.agent.governance.db.parser.clickhouse.ClickhouselParser;
import com.jd.live.agent.governance.db.parser.db2.Db2UrlParser;
import com.jd.live.agent.governance.db.parser.dm.DMUrlParser;
import com.jd.live.agent.governance.db.parser.gaussdb.GaussDBUrlParser;
import com.jd.live.agent.governance.db.parser.gbase.GBase8aUrlParser;
import com.jd.live.agent.governance.db.parser.gbase.GBase8sUrlParser;
import com.jd.live.agent.governance.db.parser.goldendb.GoldenDBUrlParser;
import com.jd.live.agent.governance.db.parser.h2.H2UrlParser;
import com.jd.live.agent.governance.db.parser.jdts.JtdsUrlParser;
import com.jd.live.agent.governance.db.parser.kingbase.KingBase8UrlParser;
import com.jd.live.agent.governance.db.parser.mariadb.MariadbUrlParser;
import com.jd.live.agent.governance.db.parser.mysql.MysqlUrlParser;
import com.jd.live.agent.governance.db.parser.oceanbase.OceanBaseUrlParser;
import com.jd.live.agent.governance.db.parser.opengauss.OpenGaussUrlParser;
import com.jd.live.agent.governance.db.parser.oracle.OracleUrlParser;
import com.jd.live.agent.governance.db.parser.polardb.PolarDBUrlParser;
import com.jd.live.agent.governance.db.parser.postgresql.PostgresqlUrlParser;
import com.jd.live.agent.governance.db.parser.sqlite.SQLiteUrlParser;
import com.jd.live.agent.governance.db.parser.sqlserver.SqlServerUrlParser;
import com.jd.live.agent.governance.db.parser.sybase.SybaseUrlParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class DbUrlTest {

    private static final Map<String, DbUrlParser> PARSERS = new HashMap<>();

    static {
        PARSERS.put("default", new DefaultUrlParser());
        PARSERS.put("mysql", new MysqlUrlParser());
        PARSERS.put("mariadb", new MariadbUrlParser());
        PARSERS.put("postgresql", new PostgresqlUrlParser());
        PARSERS.put("oracle", new OracleUrlParser());
        PARSERS.put("h2", new H2UrlParser());
        PARSERS.put("sqlserver", new SqlServerUrlParser());
        PARSERS.put("jtds", new JtdsUrlParser());
        PARSERS.put("as400", new As400UrlParser());
        PARSERS.put("db2", new Db2UrlParser());
        PARSERS.put("oceanbase", new OceanBaseUrlParser());
        PARSERS.put("sqlite", new SQLiteUrlParser());
        PARSERS.put("gaussdb", new GaussDBUrlParser());
        PARSERS.put("gbase", new GBase8aUrlParser());
        PARSERS.put("gbasedbt-sqli", new GBase8sUrlParser());
        PARSERS.put("goldendb", new GoldenDBUrlParser());
        PARSERS.put("kingbase8", new KingBase8UrlParser());
        PARSERS.put("opengauss", new OpenGaussUrlParser());
        PARSERS.put("polardb", new PolarDBUrlParser());
        PARSERS.put("sybase", new SybaseUrlParser());
        PARSERS.put("dm", new DMUrlParser());
        PARSERS.put("clickhouse", new ClickhouselParser());
    }

    @Test
    void testMysql() {
        DbUrl url = DbUrlParser.parse("jdbc:mysql://a:b@127.0.0.1:3306/test", PARSERS::get);
        Assertions.assertNotNull(url);
        Assertions.assertEquals("jdbc:mysql", url.getScheme());
        Assertions.assertEquals("mysql", url.getType());
        Assertions.assertEquals("127.0.0.1:3306", url.getAddress());
        Assertions.assertEquals("test", url.getDatabase());
        Assertions.assertEquals("a", url.getUser());
        Assertions.assertEquals("b", url.getPassword());
        Assertions.assertEquals("jdbc:mysql://a:b@127.0.0.1:3306/test", url.toString());

        url = DbUrlParser.parse("jdbc:mysql://127.0.0.1:3306,localhost/test", PARSERS::get);
        Assertions.assertNotNull(url);
        Assertions.assertEquals("jdbc:mysql", url.getScheme());
        Assertions.assertEquals("127.0.0.1:3306,localhost", url.getAddress());
        Assertions.assertEquals(2, url.getNodes().size());
        Assertions.assertEquals("test", url.getDatabase());
        Assertions.assertEquals("jdbc:mysql://127.0.0.1:3306,localhost/test", url.toString());
    }

    @Test
    void testPostgresql() {
        DbUrl url = DbUrlParser.parse("jdbc:postgresql://127.0.0.1:1111/database", PARSERS::get);
        Assertions.assertNotNull(url);
        Assertions.assertEquals("jdbc:postgresql", url.getScheme());
        Assertions.assertEquals("postgresql", url.getType());
        Assertions.assertEquals("127.0.0.1:1111", url.getAddress());
        Assertions.assertEquals("database", url.getDatabase());
        Assertions.assertEquals("jdbc:postgresql://127.0.0.1:1111/database", url.toString());

        url = DbUrlParser.parse("jdbc:postgresql:database", PARSERS::get);
        Assertions.assertNotNull(url);
        Assertions.assertEquals("jdbc:postgresql", url.getScheme());
        Assertions.assertEquals("localhost", url.getAddress());
        Assertions.assertEquals("database", url.getDatabase());
        Assertions.assertEquals("jdbc:postgresql://localhost/database", url.toString());

        url = DbUrlParser.parse("jdbc:postgresql:/", PARSERS::get);
        Assertions.assertNotNull(url);
        Assertions.assertEquals("jdbc:postgresql", url.getScheme());
        Assertions.assertEquals("localhost", url.getAddress());
        Assertions.assertEquals("jdbc:postgresql://localhost/", url.toString());
    }

    @Test
    void testSqlServer() {
        DbUrl url = DbUrlParser.parse("jdbc:jtds:sqlserver://[2001:db8:85a3::8a2e:370:7334]:1433;DatabaseName=database1", PARSERS::get);
        Assertions.assertNotNull(url);
        Assertions.assertEquals("jdbc:jtds:sqlserver", url.getScheme());
        Assertions.assertEquals("[2001:db8:85a3::8a2e:370:7334]:1433", url.getAddress());
        Assertions.assertEquals("database1", url.getDatabase());
        Assertions.assertEquals("jdbc:jtds:sqlserver://[2001:db8:85a3::8a2e:370:7334]:1433;DatabaseName=database1", url.toString());

        url = DbUrlParser.parse("jdbc:sqlserver://localhost;encrypt=true;databaseName=AdventureWorks;integratedSecurity=true", PARSERS::get);
        Assertions.assertNotNull(url);
        Assertions.assertEquals("jdbc:sqlserver", url.getScheme());
        Assertions.assertEquals("localhost", url.getAddress());
        Assertions.assertEquals("AdventureWorks", url.getDatabase());
        Assertions.assertEquals("jdbc:sqlserver://localhost;encrypt=true;databaseName=AdventureWorks;integratedSecurity=true", url.toString());
    }

    @Test
    void testH2() {
        DbUrl url = DbUrlParser.parse("jdbc:h2:mem:testdb", PARSERS::get);
        Assertions.assertNotNull(url);
        Assertions.assertEquals("testdb", url.getDatabase());
        Assertions.assertEquals("jdbc:h2:mem:testdb", url.toString());

        url = DbUrlParser.parse("jdbc:h2:tcp://dbserv:8084/~/sample", PARSERS::get);
        Assertions.assertNotNull(url);
        Assertions.assertEquals("dbserv:8084", url.getAddress());
        Assertions.assertEquals("sample", url.getDatabase());
        Assertions.assertEquals("/~/sample", url.getPath());
        Assertions.assertEquals("jdbc:h2:tcp://dbserv:8084/~/sample", url.toString());

        url = DbUrlParser.parse("jdbc:h2:file:C:/data/sample", PARSERS::get);
        Assertions.assertNotNull(url);
        Assertions.assertNull(url.getAddress());
        Assertions.assertEquals("sample", url.getDatabase());
        Assertions.assertEquals("C:/data/sample", url.getPath());
        Assertions.assertEquals("jdbc:h2:file:C:/data/sample", url.toString());

        url = DbUrlParser.parse("jdbc:h2:~/test", PARSERS::get);
        Assertions.assertNotNull(url);
        Assertions.assertNull(url.getAddress());
        Assertions.assertEquals("test", url.getDatabase());
        Assertions.assertEquals("~/test", url.getPath());
        Assertions.assertEquals("jdbc:h2:~/test", url.toString());

    }

    @Test
    void testDb2() {
        DbUrl url = DbUrlParser.parse("jdbc:db2://localhost:50000/test:a=true", PARSERS::get);
        Assertions.assertNotNull(url);
        Assertions.assertEquals("jdbc:db2", url.getScheme());
        Assertions.assertEquals("localhost:50000", url.getAddress());
        Assertions.assertEquals("test", url.getDatabase());
        Assertions.assertEquals("true", url.getParameter("a"));
        Assertions.assertEquals("jdbc:db2://localhost:50000/test:a=true", url.toString());
    }

    @Test
    void testOracle() {
        DbUrl url = DbUrlParser.parse("jdbc:oracle:thin:@//localhost:8888/a.b.com", PARSERS::get);
        Assertions.assertNotNull(url);
        Assertions.assertEquals("jdbc:oracle:thin", url.getScheme());
        Assertions.assertEquals("localhost:8888", url.getAddress());
        Assertions.assertEquals("a.b.com", url.getDatabase());
        Assertions.assertEquals("jdbc:oracle:thin:@//localhost:8888/a.b.com", url.toString());

        url = DbUrlParser.parse("jdbc:oracle:thin:@//localhost:8888:abc", PARSERS::get);
        Assertions.assertNotNull(url);
        Assertions.assertEquals("jdbc:oracle:thin", url.getScheme());
        Assertions.assertEquals("localhost:8888", url.getAddress());
        Assertions.assertEquals("abc", url.getDatabase());
        Assertions.assertEquals("jdbc:oracle:thin:@//localhost:8888:abc", url.toString());

        url = DbUrlParser.parse("jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=127.0.0.1)(PORT=1521))(FAILOVER=on)(LOAD_BALANCE=off))(CONNECT_DATA= (SERVICE_NAME=orcl)))", PARSERS::get);
        Assertions.assertNull(url.getAddress());
    }

    @Test
    void testOceanBase() {
        DbUrl url = DbUrlParser.parse("jdbc:oceanbase:hamode://127.0.0.1:1001/test", PARSERS::get);
        Assertions.assertNotNull(url);
        Assertions.assertEquals("jdbc:oceanbase:hamode", url.getScheme());
        Assertions.assertEquals("127.0.0.1:1001", url.getAddress());
        Assertions.assertEquals("test", url.getDatabase());
        Assertions.assertEquals("jdbc:oceanbase:hamode://127.0.0.1:1001/test", url.toString());
    }

    @Test
    void testSQLite() {
        DbUrl url = DbUrlParser.parse("jdbc:sqlite:C:/sqlite/db/chinook.db", PARSERS::get);
        Assertions.assertNotNull(url);
        Assertions.assertNull(url.getAddress());
        Assertions.assertEquals("chinook.db", url.getDatabase());
        Assertions.assertEquals("C:/sqlite/db/chinook.db", url.getPath());
        Assertions.assertEquals("jdbc:sqlite:C:/sqlite/db/chinook.db", url.toString());
    }

    @Test
    void testJdts() {
        DbUrl url = DbUrlParser.parse("jdbc:jtds:sqlserver://[2001:db8:85a3::8a2e:370:7334]:1433/database1", PARSERS::get);
        Assertions.assertNotNull(url);
        Assertions.assertEquals("jdbc:jtds:sqlserver", url.getScheme());
        Assertions.assertEquals("sqlserver", url.getType());
        Assertions.assertEquals("[2001:db8:85a3::8a2e:370:7334]:1433", url.getAddress());
        Assertions.assertEquals("database1", url.getDatabase());
        Assertions.assertEquals("jdbc:jtds:sqlserver://[2001:db8:85a3::8a2e:370:7334]:1433/database1", url.toString());

        url = DbUrlParser.parse("jdbc:jtds:sybase://127.0.0.1:1001;databaseName=database1;ssl=request", PARSERS::get);
        Assertions.assertNotNull(url);
        Assertions.assertEquals("jdbc:jtds:sybase", url.getScheme());
        Assertions.assertEquals("sybase", url.getType());
        Assertions.assertEquals("127.0.0.1:1001", url.getAddress());
        Assertions.assertEquals("database1", url.getDatabase());
        Assertions.assertEquals("jdbc:jtds:sybase://127.0.0.1:1001;databaseName=database1;ssl=request", url.toString());
    }

    @Test
    void testSybase() {
        DbUrl url = DbUrlParser.parse("jdbc:sybase:Tds:127.0.0.1:1001/database1?property_name=value", PARSERS::get);
        Assertions.assertNotNull(url);
        Assertions.assertEquals("jdbc:sybase:Tds", url.getScheme());
        Assertions.assertEquals("127.0.0.1:1001", url.getAddress());
        Assertions.assertEquals("database1", url.getDatabase());
        Assertions.assertEquals("jdbc:sybase:Tds:127.0.0.1:1001/database1?property_name=value", url.toString());
    }

    @Test
    void testDm() {
        DbUrl url = DbUrlParser.parse("jdbc:dm://127.0.0.1:1111/database", PARSERS::get);
        Assertions.assertNotNull(url);
        Assertions.assertEquals("jdbc:dm", url.getScheme());
        Assertions.assertEquals("dm", url.getType());
        Assertions.assertEquals("127.0.0.1:1111", url.getAddress());
        Assertions.assertEquals("database", url.getDatabase());
        Assertions.assertEquals("jdbc:dm://127.0.0.1:1111/database", url.toString());

        url = DbUrlParser.parse("jdbc:dm://?host=192.168.0.96&port=5236", PARSERS::get);
        Assertions.assertNotNull(url);
        Assertions.assertEquals("jdbc:dm", url.getScheme());
        Assertions.assertEquals("dm", url.getType());
        Assertions.assertEquals("192.168.0.96:5236", url.getAddress());
        Assertions.assertEquals("jdbc:dm://?host=192.168.0.96&port=5236", url.toString());
        url = url.address("192.168.0.97:5236");
        Assertions.assertEquals("jdbc:dm://?host=192.168.0.97&port=5236", url.toString());

        url = DbUrlParser.parse("jdbc:dm://test?test=(192.168.0.96:5236,192.168.0.96:5237)", PARSERS::get);
        Assertions.assertNotNull(url);
        Assertions.assertEquals("jdbc:dm", url.getScheme());
        Assertions.assertEquals("dm", url.getType());
        Assertions.assertEquals("test", url.getAddress());
        Assertions.assertEquals("jdbc:dm://test?test=(192.168.0.96:5236,192.168.0.96:5237)", url.toString());
        url = url.address("192.168.0.97:5236,192.168.0.97:5237");
        Assertions.assertEquals("jdbc:dm://test?test=(192.168.0.97:5236,192.168.0.97:5237)", url.toString());
        Assertions.assertArrayEquals(new String[]{"192.168.0.97:5236", "192.168.0.97:5237"}, url.getAddresses());
    }
}

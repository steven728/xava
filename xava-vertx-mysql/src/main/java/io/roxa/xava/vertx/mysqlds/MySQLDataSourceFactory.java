/**
 * The MIT License
 * 
 * Copyright (c) 2018-2020 Shell Technologies PTY LTD
 *
 * You may obtain a copy of the License at
 * 
 *       http://mit-license.org/
 *       
 */
package io.roxa.xava.vertx.mysqlds;

import java.util.Map;

import javax.sql.DataSource;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * @author Steven Chen
 *
 */
@interface DataSourceConfig {
	String host() default "mysqld";

	String user() default "root";

	String pass() default "pass4pass";

	String database() default "gophe";

	int leakTraceInterval() default 0;

	boolean prefer() default false;

	int poolsize() default 10;
}

@Component(immediate = true, configurationPid = { "io.roxa.mysql" })
public class MySQLDataSourceFactory {

	private static final Logger logger = LoggerFactory.getLogger(MySQLDataSourceFactory.class);

	private HikariDataSource dikariDataSource;
	private BundleContext bundleContext;
	private ServiceRegistration<DataSource> dataSourceRegistration;
	private DataSourceConfig dataSourceConfig;

	@Activate
	public void start(BundleContext bundleContext, DataSourceConfig dataSourceConfig) {
		this.bundleContext = bundleContext;
		this.dataSourceConfig = dataSourceConfig;
		setupHikariDataSource();

	}

	@Deactivate
	public void stop(BundleContext bundleContext) {
		cleanup();
	}

	/**
	 * 
	 */
	private void cleanup() {
		if (dataSourceRegistration != null) {
			dataSourceRegistration.unregister();
			if (dikariDataSource != null) {
				dikariDataSource.close();
				logger.info("MySQL pool data source closed.");
			}
		}
	}

	/**
	 * 
	 */
	private void setupHikariDataSource() {
		HikariConfig cfg = new HikariConfig();
		int poolsize = Integer.parseInt(mergeConfig("poolsize"));
		String user = mergeConfig("user");
		String jdbcUrl = "jdbc:mysql://" + mergeConfig("host") + "/" + mergeConfig("database");
		cfg.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource");
		cfg.setConnectionTestQuery("select 1");
		cfg.setMaximumPoolSize(poolsize);
		cfg.setInitializationFailTimeout(0);
		cfg.addDataSourceProperty("user", user);
		cfg.addDataSourceProperty("password", mergeConfig("pass"));
		cfg.addDataSourceProperty("url", jdbcUrl);
		cfg.addDataSourceProperty("cachePrepStmts", true);
		cfg.addDataSourceProperty("prepStmtCacheSize", 250);
		cfg.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
		cfg.addDataSourceProperty("useServerPrepStmts", true);
		cfg.addDataSourceProperty("useLocalSessionState", true);
		cfg.addDataSourceProperty("rewriteBatchedStatements", true);
		cfg.addDataSourceProperty("cacheResultSetMetadata", true);
		cfg.addDataSourceProperty("cacheServerConfiguration", true);
		cfg.addDataSourceProperty("elideSetAutoCommits", true);
		cfg.addDataSourceProperty("maintainTimeStats", false);
		int lt = dataSourceConfig.leakTraceInterval();
		if (lt > 5000)
			cfg.setLeakDetectionThreshold(lt);
		dikariDataSource = new HikariDataSource(cfg);
		dataSourceRegistration = bundleContext.registerService(DataSource.class, dikariDataSource, null);
		logger.info(
				"MySQL pool data source registered. config [jdbcUrl:{}, user:{}, poolsize:{}, leakTraceInterval:{}]",
				jdbcUrl, user, poolsize, lt);
	}

	private String mergeConfig(String name) {
		Map<String, String> env = System.getenv();
		switch (name) {
		case "host":
			return preference(env.get("MYSQL_HOST"), dataSourceConfig.host());
		case "user":
			return preference(env.get("MYSQL_USER"), dataSourceConfig.user());
		case "pass":
			return preference(env.get("MYSQL_PASS"), dataSourceConfig.pass());
		case "database":
			return preference(env.get("MYSQL_DATABASE"), dataSourceConfig.database());
		case "poolsize":
			return preference(env.get("MYSQL_POOLSIZE"), String.valueOf(dataSourceConfig.poolsize()));
		default:
			throw new IllegalStateException("Illegal config name:" + name);
		}
	}

	private String preference(String envValue, String configValue) {
		if (dataSourceConfig.prefer())
			return configValue;
		if (envValue == null)
			return configValue;
		return envValue;
	}

}

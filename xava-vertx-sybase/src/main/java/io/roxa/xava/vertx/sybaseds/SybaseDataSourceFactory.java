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
package io.roxa.xava.vertx.sybaseds;

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
	String host() default "sybased";

	String user() default "root";

	int port() default 5200;

	String charset() default "cp936";

	String pass() default "pass4pass";

	String database() default "gophe";

	int poolsize() default 10;

	int leakTraceInterval() default 0;

	boolean prefer() default false;
}

@Component(immediate = true, configurationPid = { "io.roxa.sybase" })
public class SybaseDataSourceFactory {

	private static final Logger logger = LoggerFactory.getLogger(SybaseDataSourceFactory.class);

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
				logger.info("Sybase pool data source closed.");
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
		String host = mergeConfig("host");
		String port = mergeConfig("port");
		String database = mergeConfig("database");
		String charset = mergeConfig("charset");
		cfg.setDataSourceClassName("net.sourceforge.jtds.jdbcx.JtdsDataSource");
		cfg.setConnectionTestQuery("select 1");
		cfg.setMaximumPoolSize(poolsize);
		cfg.setInitializationFailTimeout(0);
		cfg.addDataSourceProperty("user", user);
		cfg.addDataSourceProperty("password", mergeConfig("pass"));
		cfg.addDataSourceProperty("serverName", host);
		cfg.addDataSourceProperty("portNumber", port);
		cfg.addDataSourceProperty("databaseName", database);
		cfg.addDataSourceProperty("serverType", 2);
		cfg.addDataSourceProperty("charset", charset);
		int lt = dataSourceConfig.leakTraceInterval();
		if (lt > 5000)
			cfg.setLeakDetectionThreshold(lt);
		dikariDataSource = new HikariDataSource(cfg);
		dataSourceRegistration = bundleContext.registerService(DataSource.class, dikariDataSource, null);
		logger.info(
				"Sybase pool data source registered. config [host:{}, port:{}, user:{}, database:{}, charset:{}, poolsize:{}, leakTraceInterval:{}, prefer:{}]",
				host, port, user, database, charset, poolsize, lt, dataSourceConfig.prefer());
	}

	private String mergeConfig(String name) {
		Map<String, String> env = System.getenv();
		switch (name) {
		case "host":
			return preference(env.get("SYBASE_HOST"), dataSourceConfig.host());
		case "charset":
			return preference(env.get("SYBASE_CHARSET"), dataSourceConfig.charset());
		case "port":
			return preference(env.get("SYBASE_PORT"), String.valueOf(dataSourceConfig.port()));
		case "user":
			return preference(env.get("SYBASE_USER"), dataSourceConfig.user());
		case "pass":
			return preference(env.get("SYBASE_PASS"), dataSourceConfig.pass());
		case "database":
			return preference(env.get("SYBASE_DATABASE"), dataSourceConfig.database());
		case "poolsize":
			return preference(env.get("SYBASE_POOLSIZE"), String.valueOf(dataSourceConfig.poolsize()));
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

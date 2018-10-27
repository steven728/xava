/**
 * The MIT License
 * 
 * Copyright (c) 2016-2018 Shell Technologies PTY LTD
 *
 * You may obtain a copy of the License at
 * 
 *       http://mit-license.org/
 *       
 */
package io.roxa.xava.vertx.http;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.roxa.xava.AuthorizeRestrictionException;
import io.roxa.xava.GeneralFailureException;
import io.roxa.xava.IllegalParametersException;
import io.roxa.xava.fn.Tuple2;
import io.roxa.xava.http.BadRequestException;
import io.roxa.xava.http.HttpStatusException;
import io.roxa.xava.http.InternalServerErrorException;
import io.roxa.xava.http.ServerSideException;
import io.roxa.xava.http.ServiceUnavailableException;
import io.roxa.xava.http.UnauthorizedException;
import io.roxa.xava.util.Codecs;
import io.roxa.xava.util.Strings;
import io.roxa.xava.vertx.BaseVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.servicediscovery.ServiceDiscovery;

/**
 * 
 * @author Steven Chen
 *
 */
public abstract class AbstractHttpVerticle extends BaseVerticle {

	private static final Logger logger = LoggerFactory.getLogger(AbstractHttpVerticle.class);
	private static final String FILE_UPLOADS_LOCATION = "/var/roxa/file-uploads";
	private static final String STATIC_SERVING_LOCATION = "/assets";
	protected static final String MEDIA_TYPE_APPLICATION_JSON = "application/json";
	protected static final String MEDIA_TYPE_APPLICATION_HTML = "text/html;charset=UTF-8";
	protected int port = 8080;
	protected String contextPath = "/";
	protected String fileUploadsLocation = FILE_UPLOADS_LOCATION;
	protected String staticServingLocation = STATIC_SERVING_LOCATION;

	public AbstractHttpVerticle() {
		super();
	}

	/**
	 * 
	 * @param contextPath
	 */
	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	/**
	 * 
	 * @param port
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @param fileUploadsLocation the fileUploadsLocation to set
	 */
	public void setFileUploadsLocation(String fileUploadsLocation) {
		this.fileUploadsLocation = fileUploadsLocation;
	}

	/**
	 * @param staticServingLocation the staticServingLocation to set
	 */
	public void setStaticServingLocation(String staticServingLocation) {
		this.staticServingLocation = staticServingLocation;
	}

	public void start(Future<Void> fut) throws Exception {
		logger.info("{} prepare to start Http service.", getServerName());
		setupResources().compose(v -> {
			Router router = Router.router(vertx);
			router.route().handler(BodyHandler.create().setUploadsDirectory(fileUploadsLocation));
			router.route(composePath("/*")).handler(ResponseContentTypeHandler.create());
			router.get(composePath("/health")).produces(MEDIA_TYPE_APPLICATION_JSON).handler(this::healthCheck);
			/* Serve the static content - tip: OSGi classloader? */
			StaticHandler staticHandler = StaticHandler.create("assets", this.getClass().getClassLoader());
			router.route(composePath(staticServingLocation + "/*")).handler(staticHandler);
			return Future.succeededFuture(router);
		}).compose(this::setupRouter).compose(r -> {
			Future<Void> httpServerFuture = Future.future();
			vertx.createHttpServer().requestHandler(r::accept).listen(port, ar -> {
				if (ar.succeeded()) {
					logger.info("{} Http service started. Listen on: {}, context path: {}", getServerName(), port,
							contextPath);
					httpServerFuture.complete();
				} else {
					logger.error("Could not start Http service", ar.cause());
					httpServerFuture.fail(ar.cause());
				}
			});
			return httpServerFuture;
		}).compose(this::setupServiceDiscovery).compose(this::setupHttpEndpoint).setHandler(fut.completer());

	}

	protected Future<Void> setupHttpEndpoint(ServiceDiscovery discovery) {
		return Future.succeededFuture();
	}

	protected Future<Void> setupResources() {
		return Future.succeededFuture();
	}

	protected Future<Router> setupRouter(Router router) {
		return Future.succeededFuture(router);

	}

	abstract protected String getServerName();

	protected String requestParam(RoutingContext rc, String name) {
		return rc.request().getParam(name);
	}

	protected <T> Handler<AsyncResult<T>> responseJson(RoutingContext rc) {
		return ar -> {
			if (ar.succeeded()) {
				succeeded(rc, ar.result());
			} else {
				failed(rc, ar.cause());
			}
		};
	}

	protected <T, R> Handler<AsyncResult<T>> responseJson(RoutingContext rc, Function<T, R> converter) {
		return ar -> {
			if (ar.succeeded()) {
				succeeded(rc, converter.apply(ar.result()));
			} else {
				failed(rc, ar.cause());
			}
		};
	}

	protected <T> Handler<AsyncResult<T>> response(RoutingContext rc) {
		return ar -> {
			if (ar.succeeded()) {
				succeeded(rc);
			} else {
				failed(rc, ar.cause());
			}
		};
	}

	protected void succeeded(RoutingContext rc) {
		rc.response().end(buildResponse(200, "OK").encode());
	}

	protected <T> void succeeded(RoutingContext rc, T payload) {
		rc.response().end(buildResponse(200, "OK", payload).encode());
	}

	protected void failed(RoutingContext rc, Throwable t) {
		logFailure(rc).andThen(replyFailure()).apply(t);
	}

	protected String composePath(String path) {
		if (contextPath == null || "/".equals(contextPath)) {
			return path;
		}
		return contextPath + path;
	}

	protected void healthCheck(RoutingContext rc) {
		succeeded(rc, new JsonObject().put("server_name", getServerName()).put("status", "up"));
	}

	protected Function<RoutingContext, Void> errorPage(String baseUrl, String message) {
		return (rc) -> {
			seeOther(rc, urlOfErrorPage(baseUrl, message));
			return (Void) null;
		};
	}

	protected Function<RoutingContext, Void> warnPage(String baseUrl, String message) {
		return (rc) -> {
			seeOther(rc, urlOfWarnPage(baseUrl, message));
			return (Void) null;
		};
	}

	protected Function<RoutingContext, Void> replyFailure() {
		return (rc) -> {
			Tuple2<String, Integer> tupl2 = rc.get("failure.intent");
			rc.response().setStatusCode(200).end(buildResponse(tupl2.$1(), tupl2.$0()).encode());
			return (Void) null;
		};
	}

	protected Function<Throwable, RoutingContext> logFailure(RoutingContext rc) {
		return (e) -> {
			HttpStatusException he = null;
			if (e instanceof HttpStatusException) {
				he = (HttpStatusException) e;
			} else if (e instanceof IllegalParametersException) {
				he = new BadRequestException(e);
			} else if (e instanceof AuthorizeRestrictionException) {
				he = new UnauthorizedException(e);
			} else if (e instanceof GeneralFailureException) {
				he = new ServiceUnavailableException(e);
			} else {
				he = new InternalServerErrorException(e);
			}
			return logHttpStatusException(rc).apply(he);
		};
	}

	protected void seeOther(RoutingContext rc, String url) {
		rc.response().setStatusCode(303).putHeader("Location", url).end();
	}

	protected void found(RoutingContext rc, String url) {
		rc.response().setStatusCode(302).putHeader("Location", url).end();
	}

	protected void movedPermanently(RoutingContext rc, String url) {
		rc.response().setStatusCode(301).putHeader("Location", url).end();
	}

	protected void staticServing(RoutingContext rc, String htmlLocation) {
		rc.reroute("/assets" + htmlLocation);
	}

	protected void logErrorAndStaticServing(RoutingContext rc, Throwable e, String htmlLocation) {
		if (e != null)
			logger.error(e.getMessage(), e);
		else
			logger.error("Unknown!!!");
		staticServing(rc, htmlLocation);
	}

	private Function<HttpStatusException, RoutingContext> logHttpStatusException(RoutingContext rc) {
		return (e) -> {
			Objects.requireNonNull(e);
			String st = nonNullMessage(e.getMessage());
			int sc = e.getStatusCode();
			String serverName = getServerName();
			String emsg = String.format("%s [%s] %s!", serverName == null ? "Server" : serverName, sc, st);
			Throwable cause = e.getCause();
			if (cause != null && cause instanceof RuntimeException) {
				logger.error(emsg, e);
			} else if (e instanceof ServerSideException) {
				logger.error(emsg, e);
			} else {
				logger.warn(emsg);
			}
			rc.put("failure.intent", new Tuple2<String, Integer>(st, sc));
			return rc;
		};
	}

	protected static <T> JsonObject buildResponse(int statusCode, String statusText, T payload) {
		JsonObject resp = new JsonObject().put("sc", statusCode).put("st", statusText);
		if (payload != null)
			resp.put("payload", payload);
		return resp;
	}

	protected static JsonObject buildResponse(int statusCode, String statusText) {
		JsonObject resp = new JsonObject().put("sc", statusCode).put("st", statusText);
		return resp;
	}

	protected static String nonNullMessage(String message) {
		if (message == null)
			return "Unknown";
		String _message = message.trim();
		if (_message.length() == 0)
			return "Unknown";
		return _message;
	}

	protected Future<JsonObject> checkParameters(JsonObject params) {
		if (params == null || params.isEmpty())
			return Future.failedFuture(new IllegalParametersException("Missing all parameter!"));
		return CompositeFuture.all(params.fieldNames().stream().map(k -> {
			Object objValue = params.getValue(k, null);
			if (objValue == null)
				return Future.failedFuture(new IllegalParametersException("Missing " + k + " parameter!"));
			if (objValue instanceof String) {
				String _v = Strings.emptyAsNull((String) objValue);
				if (_v == null)
					return Future.failedFuture(new IllegalParametersException("Missing " + k + " parameter!"));
				return Future.succeededFuture(new Tuple2<String, Object>(k, _v));
			}
			return Future.succeededFuture(new Tuple2<String, Object>(k, objValue));
		}).collect(Collectors.toList())).map(fs -> {
			List<Tuple2<String, Object>> list = fs.list();
			return new JsonObject(list.stream().collect(Collectors.toMap(t -> t.$0(), t -> t.$1())));
		});

	}

	protected static String resolveBaseUrl(RoutingContext rc) {
		String host = rc.request().getHeader("Host");
		String httpSchema = rc.request().getHeader("X-Http-Schema");
		Objects.requireNonNull(host);
		String _httpSchema = "on".equalsIgnoreCase(httpSchema) ? "https" : "http";
		String[] hostAndPort = host.split(":");
		if (hostAndPort.length > 1) {
			String port = hostAndPort[1];
			if (!"80".equals(port) && !"443".equals(port))
				return String.format("%s://%s", _httpSchema, host);
		}
		return String.format("%s://%s", _httpSchema, hostAndPort[0]);

	}

	protected static String urlOfWarnPage(String baseUrl, String causeText) {
		return urlOfFailurePage(baseUrl, "warn", causeText);
	}

	protected static String urlOfErrorPage(String baseUrl, String causeText) {
		return urlOfFailurePage(baseUrl, "error", causeText);
	}

	protected static String urlOfFailurePage(String baseUrl, String level, String causeText) {
		return String.format("%s/%s?cause=%s", baseUrl, "/error.html",
				Codecs.urlEncode(new JsonObject().put("level", level).put("text", causeText).encode()));
	}
}
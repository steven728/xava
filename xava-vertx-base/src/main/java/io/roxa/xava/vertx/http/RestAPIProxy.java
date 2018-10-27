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
package io.roxa.xava.vertx.http;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.roxa.xava.fn.Tuple2;
import io.roxa.xava.util.Strings;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.HttpEndpoint;

/**
 * @author Steven Chen
 *
 */
public abstract class RestAPIProxy {

	private static final Logger logger = LoggerFactory.getLogger(RestAPIProxy.class);

	protected Vertx vertx;
	protected ServiceDiscovery discovery;
	protected String endpointName;
	protected CircuitBreaker circuitBreaker;

	public RestAPIProxy(String endpointName) {
		this.endpointName = endpointName;
	}

	public RestAPIProxy(Vertx vertx, String endpointName) {
		this.vertx = vertx;
		this.endpointName = endpointName;
	}

	protected void setupCircuitBreaker() {
		if (vertx == null)
			return;
		logger.info("Setup the xava.vertx-RestAPIProxyCircuitBreaker");
		circuitBreaker = CircuitBreaker.create("xava.vertx-RestAPIProxyCircuitBreaker", vertx,
				new CircuitBreakerOptions().setMaxFailures(5).setMaxRetries(2).setTimeout(3000).setResetTimeout(10000))
				.openHandler(v -> {
					logger.warn("xava.vertx-RestAPIProxyCircuitBreaker on open state!");
				}).fallback(v -> {
					return new JsonObject().put("sc", "500").put("st", "ciruit opened");
				});
	}

	protected Future<JsonObject> get(String uri) {
		return get(uri, null);
	}

	protected Future<JsonObject> get(String uri, JsonObject queryParams) {
		if (circuitBreaker != null)
			return circuitBreaker.execute(future -> {
				getEndpoint().compose(client -> {
					Future<JsonObject> futureInternal = Future.future();
					HttpRequest<Buffer> request = client.get(uri);
					bindQueryParam(queryParams, request);
					request.as(BodyCodec.jsonObject()).send(responseHandler(futureInternal));
					return futureInternal.map(completeHandler(client));
				}).setHandler(future.completer());
			});
		return getEndpoint().compose(client -> {
			Future<JsonObject> future = Future.future();
			HttpRequest<Buffer> request = client.get(uri);
			bindQueryParam(queryParams, request);
			request.as(BodyCodec.jsonObject()).send(responseHandler(future));
			return future.map(completeHandler(client));
		});

	}

	protected Future<JsonObject> post(String uri, JsonObject payload) {
		return post(uri, null, payload);
	}

	protected Future<JsonObject> post(String uri, JsonObject queryParams, JsonObject payload) {
		if (circuitBreaker != null)
			return circuitBreaker.execute(future -> {
				getEndpoint().compose(client -> {
					Future<JsonObject> futureInternal = Future.future();
					HttpRequest<Buffer> request = client.post(uri);
					bindQueryParam(queryParams, request);
					request.as(BodyCodec.jsonObject()).sendJsonObject(payload, responseHandler(futureInternal));
					return futureInternal.map(completeHandler(client));
				}).setHandler(future.completer());
			});
		return getEndpoint().compose(client -> {
			Future<JsonObject> future = Future.future();
			HttpRequest<Buffer> request = client.post(uri);
			bindQueryParam(queryParams, request);
			request.as(BodyCodec.jsonObject()).sendJsonObject(payload, responseHandler(future));
			return future.map(completeHandler(client));
		});
	}

	protected Future<JsonObject> put(String uri, JsonObject payload) {
		return put(uri, null, payload);
	}

	protected Future<JsonObject> put(String uri, JsonObject queryParams, JsonObject payload) {
		if (circuitBreaker != null)
			return circuitBreaker.execute(future -> {
				getEndpoint().compose(client -> {
					Future<JsonObject> futureInternal = Future.future();
					HttpRequest<Buffer> request = client.put(uri);
					bindQueryParam(queryParams, request);
					request.as(BodyCodec.jsonObject()).sendJsonObject(payload, responseHandler(futureInternal));
					return futureInternal.map(completeHandler(client));
				}).setHandler(future.completer());
			});
		return getEndpoint().compose(client -> {
			Future<JsonObject> future = Future.future();
			HttpRequest<Buffer> request = client.put(uri);
			bindQueryParam(queryParams, request);
			request.as(BodyCodec.jsonObject()).sendJsonObject(payload, responseHandler(future));
			return future.map(completeHandler(client));
		});
	}

	protected Future<JsonObject> delete(String uri) {
		return delete(uri, null, null);
	}

	protected Future<JsonObject> delete(String uri, JsonObject queryParams) {
		return delete(uri, queryParams, null);
	}

	protected Future<JsonObject> delete(String uri, JsonObject queryParams, JsonObject payload) {
		if (circuitBreaker != null)
			return circuitBreaker.execute(future -> {
				getEndpoint().compose(client -> {
					Future<JsonObject> futureInternal = Future.future();
					HttpRequest<Buffer> request = client.delete(uri);
					bindQueryParam(queryParams, request);
					if (payload != null && !payload.isEmpty())
						request.as(BodyCodec.jsonObject()).sendJsonObject(payload, responseHandler(futureInternal));
					else
						request.as(BodyCodec.jsonObject()).send(responseHandler(futureInternal));
					return futureInternal.map(completeHandler(client));
				}).setHandler(future.completer());
			});
		return getEndpoint().compose(client -> {
			Future<JsonObject> future = Future.future();
			HttpRequest<Buffer> request = client.delete(uri);
			bindQueryParam(queryParams, request);
			if (payload != null && !payload.isEmpty())
				request.as(BodyCodec.jsonObject()).sendJsonObject(payload, responseHandler(future));
			else
				request.as(BodyCodec.jsonObject()).send(responseHandler(future));
			return future.map(completeHandler(client));
		});
	}

	/**
	 * @param queryParams
	 * @param request
	 */
	protected void bindQueryParam(JsonObject queryParams, HttpRequest<Buffer> request) {
		if (queryParams != null && !queryParams.isEmpty())
			queryParams.stream().map(e -> {
				String v = String.valueOf(e.getValue());
				v = Strings.emptyAsNull(v);
				if (v != null)
					return new Tuple2<String, String>(e.getKey(), v);
				return null;
			}).filter(i -> i != null).forEach(t -> request.addQueryParam(t.$0(), t.$1()));
	}

	protected Function<JsonObject, JsonObject> completeHandler(WebClient client) {
		return r -> {
			ServiceDiscovery.releaseServiceObject(discovery, client);
			return r;
		};
	}

	protected Future<WebClient> getEndpoint() {
		return setupServiceDiscovery().compose(d -> {
			Future<WebClient> future = Future.future();
			HttpEndpoint.getWebClient(discovery, new JsonObject().put("name", endpointName), future.completer());
			return future;
		});

	}

	protected Future<ServiceDiscovery> setupServiceDiscovery() {
		if (discovery == null)
			discovery = ServiceDiscovery.create(vertx);
		return Future.succeededFuture(discovery);
	}

	protected static Handler<AsyncResult<HttpResponse<JsonObject>>> responseHandler(Future<JsonObject> future) {
		return ar -> {
			if (ar.succeeded()) {
				HttpResponse<JsonObject> response = ar.result();
				int sc = response.statusCode();
				if (sc == 200) {
					future.complete(response.body());
				} else {
					future.fail(response.statusMessage());
				}
			} else {
				logger.error("Could not complete HTTP request!", ar.cause());
				future.fail(ar.cause());
			}
		};
	}

}

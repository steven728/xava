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
package io.roxa.xava.vertx.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.swissbox.extender.BundleManifestScanner;
import org.ops4j.pax.swissbox.extender.ManifestEntry;
import org.ops4j.pax.swissbox.extender.ManifestFilter;
import org.ops4j.pax.swissbox.extender.RegexKeyManifestFilter;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Verticle;

/**
 * 
 * @author Steven Chen
 *
 */
@SuppressWarnings("rawtypes")
public class VerticleObserver {

	private static final Logger logger = LoggerFactory.getLogger(VerticleObserver.class);

	public VerticleExtender createExtender(Bundle bundle) {
		NullArgumentException.validateNotNull(bundle, "Bundle");
		if (bundle.getState() != Bundle.ACTIVE) {
			logger.debug("Bundle {} is not in ACTIVE state, ignore it!", bundle);
			return null;
		}
		// Check compatibility
		Boolean canSeeVerticleClass = canSeeClass(bundle, Verticle.class);
		if (Boolean.FALSE.equals(canSeeVerticleClass)) {
			logger.debug("Ignore bundle {} which is not compatible with this extender", bundle);
			return null;
		}

		BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
		List<BundleCapability> capabilities = bundleWiring.getCapabilities("osgi.wiring.package");
		List<String> packages = capabilities.stream().map(cap -> cap.getAttributes().get("osgi.wiring.package"))
				.map(o -> ((String) o)).distinct()
				.filter(packageName -> Verticle.class.getPackage().getName().equalsIgnoreCase(packageName))
				.collect(Collectors.toList());
		logger.debug("Verticle wiring.package: {}", packages == null ? 0 : packages.size());
		ManifestFilter manifestFilter = new RegexKeyManifestFilter("Provide-Capability");
		BundleManifestScanner manifestScanner = new BundleManifestScanner(manifestFilter);
		List<ManifestEntry> mfEntries = manifestScanner.scan(bundle);

		List<ManifestEntry> collect = mfEntries.stream()
				.filter(entry -> entry.getValue().contains(Verticle.class.getPackage().getName()))
				.collect(Collectors.toList());
		if (!collect.isEmpty()) {
			logger.debug("Ignoring bundle {}, it's already handled by service means", bundle);
			return null;
		}
		Collection<String> classes = bundleWiring.listResources("/", "*.class",
				BundleWiring.LISTRESOURCES_RECURSE | BundleWiring.LISTRESOURCES_LOCAL);
		logger.debug("found {} classes", classes.size());

		List<String> failedClassNames = new ArrayList<>();

		List<Class> verticleAssignables = classes.stream().map(clazzName -> clazzName.replace('/', '.'))
				.map(clazzName -> clazzName.replace(".class", "")).map(clazzName -> {
					try {
						if ("io.vertx.reactivex.core.AbstractVerticle".equals(clazzName))
							return null;
						return bundle.loadClass(clazzName);
					} catch (ClassNotFoundException | NoClassDefFoundError e) {
						failedClassNames.add(clazzName);
						// munch ...
					}
					return null;
				}).filter(clazz -> clazz != null).filter(clazz -> Verticle.class.isAssignableFrom(clazz))
				.collect(Collectors.toList());

		if (verticleAssignables.isEmpty()) {
			logger.debug("Ignoring bundle {}, no Verticle assignable classes found", bundle);
			logger.debug("following list of class names couldn't be loaded from bundle: {}", failedClassNames);
			return null;
		}

		return new VerticleExtender(bundle, verticleAssignables);
	}

	public static Boolean canSeeClass(Bundle bundle, Class<?> clazz) {
		try {
			return bundle.loadClass(clazz.getName()) == clazz;
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

}

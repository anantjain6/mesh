package com.gentics.mesh.cli;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.gentics.mesh.test.AbstractIntegrationTest;

public class MeshIntegerationTest extends AbstractIntegrationTest {

	@Test
	public void testStartup() throws Exception {
		long timeout = DEFAULT_TIMEOUT_SECONDS * 2;

		Mesh.initalize();
		final Mesh mesh = Mesh.mesh();

		final AtomicBoolean customLoaderInvoked = new AtomicBoolean(false);
		final AtomicBoolean meshStarted = new AtomicBoolean(false);
		mesh.setCustomLoader((vertx) -> {
			customLoaderInvoked.set(true);
		});
		final CountDownLatch latch = new CountDownLatch(1);

		new Thread(() -> {
			try {
				mesh.run();
			} catch (Exception e) {
				fail("Error while starting instance: " + e.getMessage());
				e.printStackTrace();
			}
		}).start();
		if (latch.await(timeout, TimeUnit.SECONDS)) {
			assertTrue(meshStarted.get());
		} else {
			fail("Mesh did not startup on time. Timeout {" + timeout + "} seconds reached.");
		}
	}
}

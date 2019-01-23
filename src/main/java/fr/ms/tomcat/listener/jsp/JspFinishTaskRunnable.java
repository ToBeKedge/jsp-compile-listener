package fr.ms.tomcat.listener.jsp;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JspFinishTaskRunnable implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(JspFinishTaskRunnable.class);

	private final ServletContext servletContext;

	private final Set<String> jsps;
	private final List<Future<Boolean>> futures;

	public JspFinishTaskRunnable(final ServletContext servletContext, final Set<String> jsps,
			final List<Future<Boolean>> futures) {
		this.servletContext = servletContext;
		this.jsps = jsps;
		this.futures = futures;
	}

	public void run() {
		if (jsps != null && !jsps.isEmpty() && futures != null && !futures.isEmpty()) {
			final int total = jsps.size();
			int compiles = 0;
			for (final Future<Boolean> future : futures) {
				try {
					final Boolean success = future.get();
					if (success) {
						compiles++;
					}
				} catch (final Exception e) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Failed to get future", e);
					} else {
						LOGGER.error("Unable to get future: {}", e.getMessage());
					}
				}
			}

			final String contextPath = servletContext.getContextPath();

			LOGGER.debug("{} : {} successful compilation on {}", contextPath, compiles, total);
		}
	}
}

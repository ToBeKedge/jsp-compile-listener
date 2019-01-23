package fr.ms.tomcat.listener.jsp;

import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.servlet.ServletContext;

import org.apache.catalina.LifecycleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServletContextDecorator implements InvocationHandler {

	private static final String DEFAULT_ENGINE_NAME = "Catalina";

	private static final Logger LOGGER = LoggerFactory.getLogger(ServletContextDecorator.class);

	private final ServletContext servletContext;
	private final MBeanServer mbs;
	private final ObjectName name;

	private LifecycleState state = LifecycleState.STARTING;

	private ServletContextDecorator(final ServletContext servletContext, MBeanServer mbs, ObjectName name) {
		this.servletContext = servletContext;
		this.mbs = mbs;
		this.name = name;

	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

		final String nameMethod = method.getName();

		if ("getRequestDispatcher".equals(nameMethod)) {
			while (!LifecycleState.STARTED.equals(this.state)) {
				this.state = receiveState();
				Thread.sleep(1000L);
			}
		}

		return method.invoke(servletContext, args);
	}

	private LifecycleState receiveState() {
		try {
			final String attribute = (String) mbs.getAttribute(name, "stateName");
			final LifecycleState state = LifecycleState.valueOf(attribute);
			LOGGER.debug("State: {}", state);
			return state;
		} catch (final Throwable e) {
			// NO-OP
		}

		return null;
	}

	public static ServletContext decorateServletContext(final ServletContext servletContext) {
		final String serverInfo = servletContext.getServerInfo();

		final int start = serverInfo.indexOf('/');
		final int end = serverInfo.indexOf('.', start);

		final String versionString = serverInfo.substring(start + 1, end);

		final Integer version = Integer.valueOf(versionString);

		if (version > 7) {
			try {

				String domain = getCurrentDomain(servletContext);

				MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

				if (!DEFAULT_ENGINE_NAME.equals(domain) && !Arrays.asList(mbs.getDomains()).contains(domain)) {
					domain = DEFAULT_ENGINE_NAME;
				}

				LOGGER.debug("Domain: {}", domain);
				ObjectName name = new ObjectName(domain + ":type=Server");

				final InvocationHandler handler = new ServletContextDecorator(servletContext, mbs, name);

				final ClassLoader classLoader = servletContext.getClass().getClassLoader();
				final Class<?>[] interfaces = new Class<?>[] { ServletContext.class };

				return (ServletContext) Proxy.newProxyInstance(classLoader, interfaces, handler);
			} catch (MalformedObjectNameException e) {
				// NO-OP
			}
		}
		return servletContext;
	}

	private static String getCurrentDomain(final ServletContext servletContext) {
		final String virtualServerName = servletContext.getVirtualServerName();
		int indexOf = virtualServerName.indexOf('/');
		if (indexOf >= 0) {
			return virtualServerName.substring(0, indexOf);
		}
		return DEFAULT_ENGINE_NAME;
	}
}

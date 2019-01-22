/*
 * Copyright 2015 Marco Semiao
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
 *
 */
package fr.ms.tomcat.listener.jsp;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Ce Listener Web permet de compiler toutes les jsp d'une application durant
 * son démarrage.
 * <p>
 * Il crée un thread daemon permettant de compiler en tâche de fond.
 *
 * @see <a href="http://marcosemiao4j.wordpress.com">Marco4J</a>
 * @see <a href="https://github.com/marcosemiao/jsp-compile-listener">GitHub</a>
 *
 * @author Marco Semiao
 *
 */
public class JspCompileListener implements ServletContextListener {

	@Override
	public void contextInitialized(final ServletContextEvent servletContextEvent) {

		ServletContext servletContext = servletContextEvent.getServletContext();

		servletContext = ServletContextDecorator.decorateServletContext(servletContext);

		// Récupere tous les fichiers jsp et jspx présent dans l'application.
		final Set<String> jsps = JspCompileHelper.findFilesInDirectory(servletContext, "/", "jsp", "jspx");

		// Création un executor avec un seul thread daemon et la priorité la
		// plus basse.
		final ThreadFactory threadFactory = new JspCompileThreadFactory(servletContext.getContextPath());
		final ExecutorService executorService = Executors.newSingleThreadExecutor(threadFactory);

		// Création d'un requete et d'une réponse pour la compilation
		final HttpServletRequest request = JspCompileHelper.createHttpServletRequest();
		final HttpServletResponse response = JspCompileHelper.createHttpServletResponse();

		final List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
		for (final String jsp : jsps) {
			final Callable<Boolean> task = new JspCompileCallable(servletContext, jsp, request, response);
			final Future<Boolean> f = executorService.submit(task);
			futures.add(f);
		}
		final Runnable task = new JspFinishTaskRunnable(servletContext, jsps, futures);
		executorService.execute(task);

	}

	@Override
	public void contextDestroyed(final ServletContextEvent sce) {
		// Do nothing here
	}
}

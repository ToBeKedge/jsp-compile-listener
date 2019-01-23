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

import java.util.concurrent.Callable;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Permet d'avoir une instance par ressource pour une invocation de celle-ci.
 *
 * @see <a href="http://marcosemiao4j.wordpress.com">Marco4J</a>
 * @see <a href="https://github.com/marcosemiao/jsp-compile-listener">GitHub</a>
 *
 * @author Marco Semiao
 *
 */
class JspCompileCallable implements Callable<Boolean> {

	private static final Logger LOGGER = LoggerFactory.getLogger(JspCompileCallable.class);

	private final ServletContext servletContext;
	private final String path;

	private final HttpServletRequest request;
	private final HttpServletResponse response;

	/**
	 * Créer une instance de {@link Runnable} permettant de réaliser un include
	 * sur un ressource.
	 *
	 * @param servletContext
	 *            La {@link ServletContext} à utiliser pour la compilation.
	 * @param path
	 *            Le chemin d'accès de la ressource.
	 * @param request
	 *            la {@link HttpServletRequest requete} à utiliser pour la
	 *            compilation.
	 * @param response
	 *            la {@link HttpServletRequest réponse} à utiliser pour la
	 *            compilation.
	 */
	JspCompileCallable(final ServletContext servletContext, final String path, final HttpServletRequest request,
			final HttpServletResponse response) {
		this.servletContext = servletContext;
		this.path = path;
		this.request = request;
		this.response = response;
	}

	public Boolean call() throws Exception {
		final RequestDispatcher requestDispatcher = servletContext.getRequestDispatcher(path);

		if (requestDispatcher == null) {
			return false;
		}

		final String fullPath = servletContext.getContextPath() + path;
		try {

			LOGGER.debug("Compiling : {}", fullPath);
			requestDispatcher.include(request, response);
			return true;
		} catch (final Exception e) {
			LOGGER.error("Not Compiling : {} - {}", fullPath, e.getMessage());
			LOGGER.debug("Not Compiling : {}", fullPath, e);
			return false;
		}
	}
}

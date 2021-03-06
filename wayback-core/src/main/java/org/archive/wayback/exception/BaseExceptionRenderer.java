/*
 *  This file is part of the Wayback archival access software
 *   (http://archive-access.sourceforge.net/projects/wayback/).
 *
 *  Licensed to the Internet Archive (IA) by one or more individual 
 *  contributors. 
 *
 *  The IA licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.archive.wayback.exception;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.archive.wayback.ExceptionRenderer;
import org.archive.wayback.ResultURIConverter;
import org.archive.wayback.core.UIResults;
import org.archive.wayback.core.WaybackRequest;

/**
 * Default implementation responsible for outputting error responses to users
 * for expected failure situations, for both Replay and Query requests.
 * 
 * Has logic to return errors as XML, if in query mode, and if user requested 
 * XML.
 * 
 * Has logic to render errors as CSS, Javascript, and blank images, if the
 * request is Replay mode, embedded, and of an obvious type from the request URL
 *
 * @author brad
 * @version $Date$, $Revision$
 */
public class BaseExceptionRenderer implements ExceptionRenderer {
	private String xmlErrorJsp = "/WEB-INF/exception/XMLError.jsp";
	private String errorJsp = "/WEB-INF/exception/HTMLError.jsp";
	private String imageErrorJsp = "/WEB-INF/exception/HTMLError.jsp";
	private String javascriptErrorJsp = "/WEB-INF/exception/JavaScriptError.jsp";
	private String cssErrorJsp = "/WEB-INF/exception/CSSError.jsp";

	public static final int DEFAULT_MAX_ERR_HEADER_LEN = 300;

	private String errorHeader;
	private int maxErrorHeaderLength = DEFAULT_MAX_ERR_HEADER_LEN;

	protected static final Pattern IMAGE_REGEX = Pattern
			.compile(".*\\.(jpg|jpeg|gif|png|bmp|tiff|tif)$");

	/* ERROR HANDLING RESPONSES: */

	protected boolean requestIsEmbedded(HttpServletRequest httpRequest,
			WaybackRequest wbRequest) {
		// without a wbRequest, assume it is not embedded: send back HTML
		if (wbRequest == null) {
			return false;
		}
		String referer = wbRequest.getRefererUrl();
		return (referer != null && referer.length() > 0);
	}

	protected boolean requestIsImage(HttpServletRequest httpRequest,
			WaybackRequest wbRequest) {
		if (wbRequest == null) {
			return false;
		}
		if(wbRequest.isIMGContext()) {
			return true;
		}
		String requestUrl = wbRequest.getRequestUrl();
		if (requestUrl == null)
			return false;
		Matcher matcher = IMAGE_REGEX.matcher(requestUrl);
		return (matcher != null && matcher.matches());
	}

	protected boolean requestIsJavascript(HttpServletRequest httpRequest,
			WaybackRequest wbRequest) {
		if (wbRequest == null) {
			return false;
		}
		if(wbRequest.isJSContext()) {
			return true;
		}
		String requestUrl = wbRequest.getRequestUrl();
		return (requestUrl != null) && requestUrl.endsWith(".js");
	}

	protected boolean requestIsCSS(HttpServletRequest httpRequest,
			WaybackRequest wbRequest) {
		if (wbRequest == null) {
			return false;
		}
		if(wbRequest.isCSSContext()) {
			return true;
		}
		String requestUrl = wbRequest.getRequestUrl();
		return (requestUrl != null) && requestUrl.endsWith(".css");
	}

	protected String buildErrorDescription(WaybackException e) {
		if (e == null) return null;

		String message = e.toString();
		if (e == null) return null;

		// Get substring from exception name if possible.
		// This handles a common case of error message starting with class
		// name followed by ":". Not sure why e.getClass().getName() does
		// work. Perhaps some error text starts with 'real' error class
		// different from e.getClass()?
		final int classEnd = message.indexOf(':');
		if (classEnd > 0) {
			final int lastPeriod = message.lastIndexOf('.', classEnd);
			if (lastPeriod > 0) {
				message = message.substring(lastPeriod + 1);
			}
		}
		if (message.length() > maxErrorHeaderLength) {
			message = message.substring(0, maxErrorHeaderLength);
		}
		message = message.replace('\n', ' ');

		return message;
	}

	public void renderException(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse, WaybackRequest wbRequest,
			WaybackException exception, ResultURIConverter uriConverter)
		throws ServletException, IOException {

		if (errorHeader != null) {
			final String text = buildErrorDescription(exception);
			if (text != null) {
				httpResponse.setHeader(errorHeader, text);
			}
		}

		httpRequest.setAttribute("exception", exception);
		UIResults uiResults = new UIResults(wbRequest,uriConverter,exception);
		boolean handled = false;
		if((wbRequest != null) && !wbRequest.isReplayRequest()) {

			if(wbRequest.isXMLMode()) {
				uiResults.forward(httpRequest, httpResponse, xmlErrorJsp);
				handled = true;
			}

		} else if (requestIsEmbedded(httpRequest, wbRequest)) {
		
			// try to not cause client errors by sending the HTML response if
			// this request is ebedded, and is obviously one of the special 
			// types:
			handled = true;

			if (requestIsJavascript(httpRequest, wbRequest)) {

				uiResults.forward(httpRequest, httpResponse,
						javascriptErrorJsp);

			} else if (requestIsCSS(httpRequest, wbRequest)) {

				uiResults.forward(httpRequest, httpResponse, cssErrorJsp);

			} else if (requestIsImage(httpRequest, wbRequest)) {

				uiResults.forward(httpRequest, httpResponse, imageErrorJsp);

			} else {
				handled = false;
			}
		}
		if(!handled) {
			uiResults.forward(httpRequest, httpResponse, errorJsp);
		}
	}

	public String getErrorJsp() {
		return errorJsp;
	}

	public void setErrorJsp(String errorJsp) {
		this.errorJsp = errorJsp;
	}

	/**
	 * @return the xmlErrorJsp
	 */
	public String getXmlErrorJsp() {
		return xmlErrorJsp;
	}

	/**
	 * @param xmlErrorJsp the xmlErrorJsp to set
	 */
	public void setXmlErrorJsp(String xmlErrorJsp) {
		this.xmlErrorJsp = xmlErrorJsp;
	}

	public String getImageErrorJsp() {
		return imageErrorJsp;
	}

	public void setImageErrorJsp(String imageErrorJsp) {
		this.imageErrorJsp = imageErrorJsp;
	}

	public String getJavascriptErrorJsp() {
		return javascriptErrorJsp;
	}

	public void setJavascriptErrorJsp(String javascriptErrorJsp) {
		this.javascriptErrorJsp = javascriptErrorJsp;
	}

	public String getCssErrorJsp() {
		return cssErrorJsp;
	}

	public void setCssErrorJsp(String cssErrorJsp) {
		this.cssErrorJsp = cssErrorJsp;
	}

	public String getErrorHeader() {
		return errorHeader;
	}

	/**
	 * Name of the response header field carrying short error description.
	 * <p>
	 * The header field may be captured by front-end HTTP server for
	 * logging purposes.
	 * If {@code null} (default), no header field will be added.
	 * </p>
	 * <p>Compatibility Note: when you use this feature, disable
	 * error header output done in {@link AccessPoint#logError} by
	 * setting its {@code enableErrorMsgHeader} property to {@code false}.
	 * </p>
	 * @param errorHeader response header field name
	 */
	public void setErrorHeader(String errorHeader) {
		this.errorHeader = errorHeader;
	}

	public int getMaxErrorHeaderLength() {
		return maxErrorHeaderLength;
	}

	/**
	 * Maximum length of error message put into the error header.
	 * <p>
	 * Error message text exceeding this length will be truncated.
	 * </p>
	 * @param maxErrorHeaderLength
	 * @see #errorHeader
	 */
	public void setMaxErrorHeaderLength(int maxErrorHeaderLength) {
		this.maxErrorHeaderLength = maxErrorHeaderLength;
	}

}

package org.eclipse.sensinact.gateway.webconsole;

import static org.apache.felix.webconsole.WebConsoleConstants.PLUGIN_CATEGORY;
import static org.apache.felix.webconsole.WebConsoleConstants.PLUGIN_LABEL;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.eclipse.sensinact.gateway.launcher.FeatureLauncher;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Servlet.class, property = { PLUGIN_LABEL + "=" + FeatureLauncherPlugin.LABEL,
		PLUGIN_CATEGORY + "=" + FeatureLauncherPlugin.CATEGORY, })
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 1024 * 1024 * 5, maxRequestSize = 1024 * 1024 * 5 * 5)
public class FeatureLauncherPlugin extends AbstractWebConsolePlugin {

	private static final Logger LOGGER = LoggerFactory.getLogger(FeatureLauncherPlugin.class);
	private static final long serialVersionUID = 1L;

	static final String CATEGORY = "Sensinact";
	static final String LABEL = "Feature";

	@Reference
	FeatureLauncher launcher;

	@Activate
	public void activate() {
		LOGGER.debug("activate Web Console Plugin for feature upload");
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		res.setContentType("text/html");
		Path uploadPath = prepareUploadDir();
		StringBuilder sb = new StringBuilder();
		for (Part part : req.getParts()) {
			String fileName = part.getSubmittedFileName();
			if (!fileName.isBlank()) {
				part.write(uploadPath + File.separator + fileName);
				sb.append(fileName);
				try {
					launcher.addOrUpdate(fileName);
					sb.append(" successfully installed.");
				} catch (ConfigurationException e) {
					sb.append(" installation failed:");
					sb.append(e.getMessage());
				}
			}
		}
		res.sendRedirect(LABEL + "?result=" + sb.toString());
	}

	private Path prepareUploadDir() {
		Path uploadPath = launcher.getFeatureDir();
		File uploadDir = uploadPath.toFile();
		if (!uploadDir.exists()) {
			uploadDir.mkdir();
		}
		return uploadPath;
	}

	@Override
	protected void renderContent(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		res.setContentType("text/html");
		StringBuilder sb = new StringBuilder();
		sb.append("<h2>Upload Feature</h2>");
		sb.append("<form method=\"post\" action=\"\" enctype=\"multipart/form-data\">");
		sb.append("Choose a file: <input type=\"file\" name=\"multiPartServlet\" />");
		sb.append("<input type=\"submit\" value=\"Upload\" /></form>");
		appendInstalledFeatures(sb);
		String result = req.getParameter("result");
		if (result != null && !result.isBlank()) {
			sb.append("<h3>Result:</h3><p></br>").append(result).append("</p>");
		}

		res.getWriter().println(sb.toString());
	}

	private void appendInstalledFeatures(StringBuilder sb) {
		sb.append("<h3>Installed features:</h3><ul>");
		List<String> progress = launcher.getInstalledFeatures();
		progress.forEach(feature -> sb.append("<li>").append(feature).append("</li>"));
		sb.append("</ul>");
	}

	@Override
	public String getLabel() {
		return LABEL;
	}

	@Override
	public String getTitle() {
		return LABEL;
	}

	@Override
	protected String[] getCssReferences() {
		return super.getCssReferences();
	}
}

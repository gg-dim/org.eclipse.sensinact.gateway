package org.eclipse.sensinact.gateway.webconsole;

import static org.apache.felix.webconsole.WebConsoleConstants.PLUGIN_CATEGORY;
import static org.apache.felix.webconsole.WebConsoleConstants.PLUGIN_LABEL;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.eclipse.sensinact.gateway.launcher.FeatureLauncher;
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
	static final String LABEL = "Feature File";

	@Reference
	FeatureLauncher launcher;

	@Activate
	public void activate() {
		LOGGER.debug("activate Web Console Plugin for feature upload");
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		res.setContentType("text/html");
		PrintWriter writer = res.getWriter();
		Path uploadPath = prepareUploadDir();
		for (Part part : req.getParts()) {
			String fileName = part.getSubmittedFileName();
			part.write(uploadPath + File.separator + fileName);
			StringBuilder sb = new StringBuilder();
			sb.append("<h1>Feature Launcher</h1>");
			sb.append(fileName);
			sb.append(" successfully uploaded.");
			writer.println(sb.toString());
			launcher.loadFeature(fileName);
		}
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
		res.getWriter()
				.println("<h1>Upload Feature File</h1>"
						+ "<form method=\"post\" action=\"\" enctype=\"multipart/form-data\">"
						+ "Choose a file: <input type=\"file\" name=\"multiPartServlet\" />"
						+ "<input type=\"submit\" value=\"Upload\" /></form>");
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

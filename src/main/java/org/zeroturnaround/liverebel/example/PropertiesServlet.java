package org.zeroturnaround.liverebel.example;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.liverebel.NoLiveRebelConfigurationResolverImpl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zeroturnaround.liverebel.managedconf.client.LrAppConfiguration;
import com.zeroturnaround.liverebel.managedconf.client.spi.ConfigurationResolver;

public class PropertiesServlet extends HttpServlet {
  private static final Logger LOG = LoggerFactory.getLogger(PropertiesServlet.class);

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    Map<String, Object> model = new HashMap<String, Object>();
    model.put("resolver", getResolverInfo());
    model.put("properties", LrAppConfiguration.getConfig().getLrConfigSnapshot().getProperties());
    model.put("declaredRuntimeProps", LrAppConfiguration.RESOLVER.loadDeclaredRuntimeProps());
    addStaticConfiguration(model);

    Gson gson = new Gson();
    String json = gson.toJson(model);
    final PrintWriter writer = resp.getWriter();
    writer.write(json);
    writer.flush();
    LOG.debug(json);
  }

  private void addStaticConfiguration(Map<String, Object> model) {
    readConfFromPropertyFile(model);
    readConfFromJson(model);
    readConfFromTxt(model);
  }

  private ClassLoader readConfFromPropertyFile(Map<String, Object> model) {
    String confFile = "conf/conf.properties";
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    InputStream is = classLoader.getResourceAsStream(confFile);
    Properties propsFromFile = new Properties();
    try {
      propsFromFile.load(is);
      model.put("propsFromFiles", propsFromFile);
    }
    catch (Exception e) {
      addError(model, e, "Failed to read properties from " + confFile);
    }
    return classLoader;
  }

  private void readConfFromJson(Map<String, Object> model) {
    String confFile = "conf/conf.json";
    InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(confFile);
    InputStreamReader reader = null;
    try {
      reader = new InputStreamReader(is);
      Map<String, String> propsFromJson = new Gson().fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
      model.put("propsFromJsonFile", propsFromJson);
    }
    catch (Exception e) {
      addError(model, e, "Failed to read configuration from " + confFile);
    }
    finally {
      IOUtils.closeQuietly(reader);
    }
  }
  private void readConfFromTxt(Map<String, Object> model) {
    String confFile = "conf/conf.txt";
    InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(confFile);
    try {
      List<String> lines = IOUtils.readLines(is);
      model.put("runtimePropsFile", lines.get(0));
    }
    catch (Exception e) {
      addError(model, e, "Failed to read configuration from " + confFile);
    }
    finally {
      IOUtils.closeQuietly(is);
    }
  }

  static void addError(Map<String, Object> model, String msg) {
    addError(model, null, msg);
  }

  static void addError(Map<String, Object> model, Throwable e, String msg) {
    if (e != null && e.getMessage() != null) {
      LOG.error(msg, e);
      msg += ". Cause: " + e.getMessage();
    }
    else {
      LOG.error(msg);
    }
    String otherError = (String) model.get("error");
    msg = (otherError == null ? "" : otherError + "\n\n\n") + msg;
    model.put("error", msg);
  }

  private Map<String, Object> getResolverInfo() {
    Map<String, Object> resolverModel = new HashMap<String, Object>();
    ConfigurationResolver resolver = LrAppConfiguration.RESOLVER;
    resolverModel.put("class", resolver.getClass().getCanonicalName());
    if (resolver instanceof NoLiveRebelConfigurationResolverImpl) {
      NoLiveRebelConfigurationResolverImpl noLrResolver = (NoLiveRebelConfigurationResolverImpl) resolver;
      File runtimeConfFile = new File(noLrResolver.getRuntimeConfFilePath());
      resolverModel.put("file", runtimeConfFile.getAbsolutePath());
      resolverModel.put("exists", runtimeConfFile.isFile());
    }
    return resolverModel;
  }

}
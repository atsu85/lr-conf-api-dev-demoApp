package org.zeroturnaround.liverebel.example;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.zeroturnaround.liverebel.managedconf.client.LrAppConfiguration;
import com.zeroturnaround.liverebel.managedconf.client.LrConfig;
import com.zeroturnaround.liverebel.managedconf.client.LrConfigSnapshot;
import com.zeroturnaround.liverebel.managedconf.client.listener.ConfigurationEvent;
import com.zeroturnaround.liverebel.managedconf.client.listener.ConfigurationUpdateListener;

/**
 * Test that Application can receive notifications about updated runtime properties through {@link LrAppConfiguration#registerConfUpdateListener(Runnable)}
 */
public class RuntimePropertyUpdateListenerServlet extends HttpServlet {
  /** request parameter name for the property name that should be changed in the middle of the request */
  public static final String PARAM_PROP_NAME = "propName";
  /** the longest period to wait for runtime property to change (successful scenario shouldn't take nearly as much time) */
  private static final int MAW_WAIT_PROPERTY_CHANGE = 60;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    Map<String, Object> model = new HashMap<String, Object>();
    try {
      handleRequest(req, resp, model);
    }
    catch (Exception e) {
      PropertiesServlet.addError(model, e, "Failed to handle the request");
    }
    finally {
      String json = new Gson().toJson(model);
      final PrintWriter writer = resp.getWriter();
      writer.print(json);
    }
  }

  private void handleRequest(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> model) throws IOException {
    LrConfig oldConf = LrAppConfiguration.getConfig();
    String propName = req.getParameter(PARAM_PROP_NAME);
    if(propName == null) {
      PropertiesServlet.addError(model, "expected request parameter '" + PARAM_PROP_NAME + "'");
      return;
    }
    String oldPropValue = oldConf.getProperty(propName);
    boolean listenerCalled = waitForPropertyUpdate(model, oldConf, propName);
    if (!listenerCalled) {
      PropertiesServlet.addError(model, "Update listener was not called in " + MAW_WAIT_PROPERTY_CHANGE + " seconds. ");
    }
    model.put("oldValue", oldPropValue);
  }

  private boolean waitForPropertyUpdate(Map<String, Object> model, LrConfig oldConf, String propName) {
    final CountDownLatch countDownLatch = new CountDownLatch(1);
    ConfigurationUpdateListener listener = new UpdateListener(model, countDownLatch, propName);
    oldConf.registerConfUpdateListener(listener);
    boolean listenerCalled = false;
    try {
      listenerCalled = countDownLatch.await(MAW_WAIT_PROPERTY_CHANGE, TimeUnit.SECONDS);
    }
    catch (InterruptedException e) {
      PropertiesServlet.addError(model, "Listener thread was interrupted: " + e.getMessage());
    }
    finally {
      LrAppConfiguration.unRegisterConfUpdateListener(listener);
    }
    return listenerCalled;
  }

  public static class UpdateListener extends ConfigurationUpdateListener {
    private final String propName;
    private final Map<String, Object> model;
    private final CountDownLatch countDownLatch;

    public UpdateListener(Map<String, Object> model, CountDownLatch countDownLatch, String propName) {
      this.model = model;
      this.countDownLatch = countDownLatch;
      this.propName = propName;
    }

    @Override
    public void configurationUpdated(ConfigurationEvent event) {
      LrConfigSnapshot newProps = event.getNewValues();
      model.put("newValue", newProps.getProperty(propName));
      // writer.print(String.format(TEMPLATE_1_WITH_NEW_VALUE, newProps.getProperty(propName)));
      countDownLatch.countDown();
    }
  }

}
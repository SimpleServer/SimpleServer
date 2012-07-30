/*
 * Copyright (c) 2010 SimpleServer authors (see CONTRIBUTORS)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package simpleserver.minecraft;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipFile;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import simpleserver.Server;
import simpleserver.options.MinecraftOptions;
import simpleserver.options.Options;
import simpleserver.thread.SystemInputQueue;

public class MinecraftWrapper {
  private static final String DOWNLOAD_URL = "http://www.minecraft.net/download/minecraft_server.jar";
  private static final String COMMAND_FORMAT = "java %s -jar %s %s nogui";
  private static final String MEMORY_FORMAT = "-Xmx%sM -Xms%sM";
  private static final String XINCGC_FORMAT = "-Xincgc -Xmx%sM";
  private static final String DEFAULT_ARGUMENTS = "-XX:+AggressiveOpts";
  private static final String BUKKIT_ARGUMENTS = "-nojline";
  private static final String SERVER_JAR = "minecraft_server.jar";
  private static final int MINIMUM_MEMORY = 1024;

  private final MessageHandler messageHandler;
  private final Options options;
  private final MinecraftOptions minecraftOptions;
  private final SystemInputQueue systemInput;

  private Process minecraft;
  private List<Wrapper> wrappers;
  private InputWrapper inputWrapper;
  private boolean active = false;

  public MinecraftWrapper(Server server, Options options,
                          SystemInputQueue systemInput) {
    messageHandler = new MessageHandler(server);
    this.options = options;
    minecraftOptions = new MinecraftOptions(options);
    this.systemInput = systemInput;
  }

  public boolean prepareServerJar() {
    if (verifyMinecraftJar()) {
      return true;
    }

    System.out.println("[SimpleServer] Downloading " + SERVER_JAR
        + ".  Please wait!");

    HttpClient httpclient = new DefaultHttpClient();
    String responseBody;
    try {
      HttpGet httpget = new HttpGet(DOWNLOAD_URL);
      ResponseHandler<String> responseHandler = new BasicResponseHandler();

      try {
        responseBody = httpclient.execute(httpget, responseHandler);
      } catch (ClientProtocolException e) {
        autodownloadError(e, "download");
        return false;
      } catch (IOException e) {
        autodownloadError(e, "download");
        return false;
      }
    } finally {
      httpclient.getConnectionManager().shutdown();
    }

    OutputStream outputFile;
    try {
      outputFile = new FileOutputStream(SERVER_JAR);
    } catch (FileNotFoundException e) {
      autodownloadError(e, "save");
      return false;
    }

    try {
      outputFile.write(responseBody.getBytes("ISO-8859-1"));
    } catch (UnsupportedEncodingException e) {
      autodownloadError(e, "save");
      return false;
    } catch (IOException e) {
      autodownloadError(e, "save");
      return false;
    } finally {
      try {
        outputFile.close();
      } catch (IOException e) {
      }
    }

    if (verifyMinecraftJar()) {
      return true;
    } else {
      System.out.println("[SimpleServer] " + SERVER_JAR + " is corrupt!");
      return false;
    }
  }

  public void start() throws InterruptedException {
    minecraftOptions.save();
    Runtime runtime = Runtime.getRuntime();
    String command = getCommand();

    try {
      minecraft = runtime.exec(command);
    } catch (IOException e) {
      System.out.println("[SimpleServer] " + e);
      System.out.println("[SimpleServer] FATAL ERROR: Could not start minecraft_server.jar!");
      System.exit(-1);
    }

    active = true;
    wrappers = new LinkedList<Wrapper>();
    wrappers.add(new ShutdownHook(this));
    wrappers.add(new ProcessWrapper(minecraft, messageHandler));

    wrappers.add(new OutputWrapper(minecraft.getInputStream(), messageHandler,
                                   "stdout"));
    wrappers.add(new OutputWrapper(minecraft.getErrorStream(), messageHandler,
                                   "stderr"));

    inputWrapper = new InputWrapper(systemInput, minecraft.getOutputStream(),
                                    messageHandler);
    wrappers.add(inputWrapper);

    messageHandler.waitUntilLoaded();
  }

  public void stop() {
    if (!active) {
      return;
    }

    execute("stop", "");
    for (Wrapper wrapper : wrappers) {
      wrapper.stop();
    }

    while (wrappers.size() > 0) {
      Wrapper wrapper = wrappers.get(0);
      try {
        wrapper.join();
        wrappers.remove(wrapper);
      } catch (InterruptedException e) {
      }
    }
    active = false;
  }

  public void execute(String command, String arguments) {
    inputWrapper.injectCommand(command, arguments);
  }

  private String getCommand() {
    int minimumMemory = MINIMUM_MEMORY;
    String arguments = "";

    if (options.contains("javaArguments")) {
      arguments = options.get("javaArguments");
    }

    if (options.getInt("memory") < minimumMemory) {
      minimumMemory = options.getInt("memory");
    }

    if (!options.getBoolean("overwriteArguments")) {
      String memoryArgs;
      if (options.getBoolean("useXincgc")) {
        memoryArgs = String.format(XINCGC_FORMAT, options.get("memory"));
      } else {
        memoryArgs = String.format(MEMORY_FORMAT, minimumMemory, options.get("memory"));
      }
      arguments = String.format("%s %s %s", arguments, memoryArgs, DEFAULT_ARGUMENTS);
    }

    return String.format(COMMAND_FORMAT, arguments, getServerJar(), modArguments());
  }

  private String modArguments() {
    if (getServerJar().contains("ukkit")) {
      return "  " + BUKKIT_ARGUMENTS;
    }
    return "";
  }

  private String getServerJar() {
    if (options.contains("alternateJarFile")) {
      return options.get("alternateJarFile");
    }

    return SERVER_JAR;
  }

  private boolean verifyMinecraftJar() {
    if (getServerJar() != SERVER_JAR) {
      return true;
    }

    boolean valid = false;
    try {
      ZipFile jar = new ZipFile(SERVER_JAR);
      valid = jar.size() > 200;
      jar.close();
    } catch (IOException e) {
    }

    return valid;
  }

  private void autodownloadError(Exception e, String stepName) {
    System.out.println("[SimpleServer] " + e);
    System.out.println("[SimpleServer] Unable to " + stepName + " "
        + SERVER_JAR + "!");
  }
}

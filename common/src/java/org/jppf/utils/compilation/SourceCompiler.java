/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.utils.compilation;

import java.io.*;
import java.net.URL;
import java.util.*;

import javax.tools.*;

/**
 * This class encapsulates a Java platform compiler and provides an API to compile
 * java sources provided as string to either the file system or an in-memory location.
 * @author Laurent Cohen
 */
public class SourceCompiler implements Closeable
{
  /**
   * The java compiler.
   */
  private final JavaCompiler compiler;
  /**
   * The file manager used to store the destination class "files".
   */
  private final StandardJavaFileManager toFileManager;
  /**
   * The file manager used to store the destination class "files".
   */
  private final InMemoryFileManager toMemoryManager;
  /**
   * Reports the errors and warning that occur at compilation.
   */
  private final ErrorReporter errorReporter = new ErrorReporter();
  /**
   * Used to load the compiled classes.
   */
  private CustomClassLoader classloader;

  /**
   * Initialize a <code>SourceCompiler</code>.
   * @throws UnsupportedOperationException if no compiler is available for this platform.
   */
  public SourceCompiler() throws UnsupportedOperationException
  {
    this(SourceCompiler.class.getClassLoader());
  }

  /**
   * Initialize a SourceCompiler with the specified parent class loader.
   * @param parentCL the parent class loader used to build a class loader specific for this source compiler.
   * @throws UnsupportedOperationException if no compiler is available for this platform.
   */
  @SuppressWarnings("unchecked")
  public SourceCompiler(final ClassLoader parentCL) throws UnsupportedOperationException
  {
    if (!isCompilerAvailable()) throw new UnsupportedOperationException("no compiler is available for this platform");
    compiler = ToolProvider.getSystemJavaCompiler();
    this.toFileManager = compiler.getStandardFileManager(null, null, null);
    this.toMemoryManager = new InMemoryFileManager(toFileManager);
    this.classloader = new CustomClassLoader(null, null, parentCL);
  }

  /**
   * Compile the specified sources to the file system.
   * @param sources a mapping of class names to their source code.
   * @param classesDir the root directory where the classes are stored.
   * For instance, if a class <code>mypackage.MyClass</code> is compiled,
   * the resulting class file will be at <code><i>classesDir</i>/mypackage/MyClass.class</code>.
   * @throws Exception if any error occurs.
   */
  public void compileToFile(final Map<String, CharSequence> sources, final File classesDir) throws Exception
  {
    List<File> files = new ArrayList<>();
    files.add(classesDir);
    toFileManager.setLocation(StandardLocation.CLASS_OUTPUT, files);
    Boolean b = compile(sources, toFileManager);
    if (b)
    {
      URL url = classesDir.toURI().toURL();
      if (!classloader.hasURL(url)) classloader.addURL(url);
    }
  }

  /**
   * Compile the specified sources to memory.
   * @param sources a mapping of class names to their source code.
   * @return a mapping of the class names to their generated bytecode.
   * @throws Exception if any error occurs.
   */
  @SuppressWarnings("unchecked")
  public Map<String, byte[]> compileToMemory(final Map<String, CharSequence> sources) throws Exception
  {
    Boolean b = compile(sources, toMemoryManager);
    Map<String, byte[]> result = toMemoryManager.getAllByteCodes();
    if (b) classloader.addClasses(result);
    return result;
  }

  /**
   * Compile the specified sources using the specified file manager.
   * @param sources a mapping of class names to their source code.
   * @param fmgr the file manager to be used by the compiler.
   * @return <code>true</code> if all sources were compiled without error, <code>false</code> otherwise.
   * @throws Exception if any error occurs.
   */
  @SuppressWarnings("unchecked")
  private Boolean compile(final Map<String, CharSequence> sources, final JavaFileManager fmgr) throws Exception
  {
    List<JavaFileObject> sourceObjects = new ArrayList<>();
    for (Map.Entry<String, CharSequence> entry: sources.entrySet())
    {
      sourceObjects.add(new CharSequenceSource(entry.getKey(), entry.getValue()));
    }
    errorReporter.clear();
    return compiler.getTask(null, fmgr, errorReporter, null, null, sourceObjects).call();
  }

  /**
   * Close this source compiler and its associated file manager.
   * @throws IOException if any I/O error occurs.
   */
  @Override
  public void close() throws IOException
  {
    toMemoryManager.close();
    this.classloader = null;
  }

  /**
   * Get the errors and warnings that occured during the last compilation operation.
   * @return a list of {@link Diagnostic} objects.
   */
  @SuppressWarnings("unchecked")
  public List<Diagnostic> getDiagnostics()
  {
    return errorReporter.getErrors();
  }

  /**
   * Get the class loader used to load the classes generated by this <code>SourceCompiler</code>.
   * @return a <code>ClassLoader</code> instance.
   */
  public ClassLoader getClassloader()
  {
    return classloader;
  }

  /**
   * Determines whether a compiler is  available for this platform.
   * @return <code>true</code> if a compiler is available, <code>false</code> otherwise.
   */
  public static boolean isCompilerAvailable()
  {
    return ToolProvider.getSystemJavaCompiler() != null;
  }
}

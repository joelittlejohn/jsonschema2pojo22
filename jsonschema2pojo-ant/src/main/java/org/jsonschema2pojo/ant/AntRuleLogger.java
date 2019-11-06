/*
 * Copyright © 2010-2017 Nokia
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jsonschema2pojo.ant;

import org.apache.tools.ant.Project;
import org.jsonschema2pojo.RuleLogger;

public class AntRuleLogger implements RuleLogger {

  private static final String LEVEL_PREFIX = "[";
  private static final String LEVEL_SUFFIX = "] ";
  private static final String DEBUG_LEVEL_PREFIX = LEVEL_PREFIX + "DEBUG" + LEVEL_SUFFIX;
  private static final String ERROR_LEVEL_PREFIX = LEVEL_PREFIX + "ERROR" + LEVEL_SUFFIX;
  private static final String INFO_LEVEL_PREFIX = LEVEL_PREFIX + "INFO" + LEVEL_SUFFIX;
  private static final String TRACE_LEVEL_PREFIX = LEVEL_PREFIX + "TRACE" + LEVEL_SUFFIX;
  private static final String WARN_LEVEL_PREFIX = LEVEL_PREFIX + "WARN" + LEVEL_SUFFIX;

  private final Jsonschema2PojoTask task;

  public AntRuleLogger(Jsonschema2PojoTask jsonschema2PojoTask) {
    this.task = jsonschema2PojoTask;
  }

  @Override
  public void debug(String msg) {
    log(msg, Project.MSG_DEBUG, DEBUG_LEVEL_PREFIX);
  }

  @Override
  public void error(String msg) {
    log(msg, Project.MSG_ERR, ERROR_LEVEL_PREFIX);
  }

  @Override
  public void info(String msg) {
    log(msg, Project.MSG_INFO, INFO_LEVEL_PREFIX);
  }

  @Override
  public void trace(String msg) {
    log(msg, Project.MSG_VERBOSE, TRACE_LEVEL_PREFIX);
  }

  @Override
  public void warn(String msg) {
    log(msg, Project.MSG_WARN, WARN_LEVEL_PREFIX);
  }

  private void log(String msg, int level, String levelPrefix) {
    if (task != null && task.getProject() != null) {
      task.getProject().log(msg, level);
    } else {
      System.err.println(levelPrefix + msg);
    }
  }
}

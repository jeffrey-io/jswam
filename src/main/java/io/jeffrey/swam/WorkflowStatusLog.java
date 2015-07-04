package io.jeffrey.swam;

/**
 * Defines how we capture status from a workflow; should not be directly parsable, but it is useful for debugging
 * 
 * @author jeffrey
 */
public interface WorkflowStatusLog {

  /**
   * log various apsects
   */
  public void log(String... lineParts);
}

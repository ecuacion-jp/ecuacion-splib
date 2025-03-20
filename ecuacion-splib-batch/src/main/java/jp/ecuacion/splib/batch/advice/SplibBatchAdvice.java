/*
 * Copyright © 2012 ecuacion.jp (info@ecuacion.jp)
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
 */
package jp.ecuacion.splib.batch.advice;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * Provides needed advices for batch.
 */
@Aspect
@Component
public class SplibBatchAdvice {

  private static ThreadLocal<String> currentJob = new ThreadLocal<>();

  private static ThreadLocal<String> currentStep = new ThreadLocal<>();

  /**
   * Keeps tasklet / chunk name for logging.
   */
  private static ThreadLocal<String> currentTaskletOrChunk = new ThreadLocal<>();

  /**
   * Is executed right before {@code Tasklet.execute(..)} is executed.
   * 
   * @param joinPoint joinPoint
   */
  @Before("execution(* org.springframework.batch.core.step.tasklet.Tasklet.execute(..))")
  public void onBeforeTaskletExecute(JoinPoint joinPoint) {
    currentTaskletOrChunk.set(joinPoint.getThis().getClass().getSimpleName());
  }

  /**
   * Is executed right before {@code Chunk.execute(..)} is executed.
   * 
   * <p>The implementation with {@code Chunk} is not concreted. Check when you use this.</p>
   * 
   * @param joinPoint joinPoint
   */
  @Before("execution(* org.springframework.batch.item.Chunk.execute(..))")
  public void onBeforeChunkExecute(JoinPoint joinPoint) {
    currentTaskletOrChunk.set(joinPoint.getThis().getClass().getSimpleName());
  }

  /**
   * Gets current job name.
   */
  public static String getCurrentJob() {
    return currentJob.get();
  }

  /**
   * Sets current job name.
   * 
   * @param currentJob currentJob
   */
  public static void setCurrentJob(String currentJob) {
    SplibBatchAdvice.currentJob.set(currentJob);
  }

  /**
   * Gets current step name.
   */
  public static String getCurrentStep() {
    return currentStep.get();
  }

  /**
   * Sets current step name.
   * 
   * @param currentStep currentStep
   */
  public static void setCurrentStep(String currentStep) {
    SplibBatchAdvice.currentStep.set(currentStep);
  }

  /**
   * Gets current tasklet or chunk name.
   * 
   * @return current tasklet or chunk name
   */
  public static String getCurrentTaskletOrChunk() {
    return currentTaskletOrChunk.get();
  }

  /**
   * Sets current tasklet or chunk name.
   * 
   * @param currentTaskletOrChunk current tasklet or chunk name
   */
  public static void setCurrentTasklet(String currentTaskletOrChunk) {
    SplibBatchAdvice.currentTaskletOrChunk.set(currentTaskletOrChunk);
  }
}

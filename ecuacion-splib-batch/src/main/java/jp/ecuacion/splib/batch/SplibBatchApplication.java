package jp.ecuacion.splib.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Provides default BatchApplication.
 * 
 * <p>We want it to be {@code abstract} to clarify 
 *    that this class is supposed to be extended to use.<br>
 *    But it's not allowed by spring. The following error message obtained.<br>
 *    {@code BeanCreationException: Error creating bean with name 'splibBatchApplication': 
 *    Failed to instantiate [jp.ecuacion.splib.batch.SplibBatchApplication]: 
 *    Is it an abstract class?}</p>
 */
public class SplibBatchApplication {

  /**
   * Is the main method of the spring batch.
   * 
   * <p>java command doesn't seem to start 
   *     by calling the main method in the parent class of the class specified.<br>
   *     So you need to implement main mathod in the BatchApplication class in your each app
   *     and call {@code SplibBatchApplication.main(args)} in it.</p>
   */
  public static void main(String[] args) {
    ConfigurableApplicationContext context =
        SpringApplication.run(SplibBatchApplication.class, args);
    System.exit(SpringApplication.exit(context, () -> 0));
  }
}

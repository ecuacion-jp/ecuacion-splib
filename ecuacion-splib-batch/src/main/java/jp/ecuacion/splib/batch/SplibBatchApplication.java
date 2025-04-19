package jp.ecuacion.splib.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Provides default BatchApplication.
 */
public abstract class SplibBatchApplication {
  public static void main(String[] args) throws Exception {
    ConfigurableApplicationContext context = SpringApplication.run(SplibBatchApplication.class, args);
    System.exit(SpringApplication.exit(context, () -> 0));
  }
}

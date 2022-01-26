package de.samply.dktk.fedsearch.share;

import static java.nio.charset.StandardCharsets.UTF_8;

import de.samply.dktk.fedsearch.share.util.Either;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for working with the classpath.
 */
public interface ClasspathIo {

  Logger logger = LoggerFactory.getLogger(ClasspathIo.class);

  /**
   * Reads the content of resource with {@code name} into a string.
   *
   * @param name the name of the resource to read relative to this class
   * @return either the content of the resource or an error message
   */
  static Either<String, String> slurp(String name) {
    try (InputStream in = Config.class.getResourceAsStream(name)) {
      if (in == null) {
        logger.error("file `{}` not found in classpath", name);
        return Either.left("file `%s` not found in classpath".formatted(name));
      } else {
        logger.info("read file `{}` from classpath", name);
        return Either.right(new String(in.readAllBytes(), UTF_8));
      }
    } catch (IOException e) {
      logger.error("error while reading the file `{}` from classpath", name, e);
      return Either.left("error while reading the file `%s` from classpath".formatted(name));
    }
  }
}

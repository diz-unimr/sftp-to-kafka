/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.config;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

public class JsonUtil {

  private static final Logger log = LoggerFactory.getLogger(JsonUtil.class);

  protected static Configuration suppressExceptionConfiguration =
      Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS);

  /**
   * @param jsonPathExpression filter
   * @param json json as text
   * @return values matched by given json path expression
   */
  public static Object getPropValue(@NonNull String jsonPathExpression, @NonNull String json) {
    if (!StringUtils.hasText(jsonPathExpression)) {
      log.error("input text was empty! ");
      throw new IllegalArgumentException("jsonPathExpression must have a value");
    }
    if (!StringUtils.hasText(json)) {
      return null;
    }
    final ParseContext parseContext = JsonPath.using(suppressExceptionConfiguration);
    return parseContext.parse(json).read(jsonPathExpression);
  }
}

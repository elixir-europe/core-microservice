package eu.crg.ega.microservice.interceptor;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import eu.crg.ega.microservice.constant.CoreConstants;
import eu.crg.ega.microservice.security.RestWebAuthenticationDetails;
import eu.crg.ega.microservice.security.SessionTokenAuthenticationDetails;

public class CustomTokenInterceptor implements ClientHttpRequestInterceptor {

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body,
      ClientHttpRequestExecution execution) throws IOException {

    HttpHeaders headers = request.getHeaders();
    if (headers == null) {
      headers = new HttpHeaders();
    }

    if (!headers.containsKey(CoreConstants.TOKEN_HEADER)) {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null) {
        String sessionToken = null;
        Object details = authentication.getDetails();
        
        if (details instanceof RestWebAuthenticationDetails) {
          RestWebAuthenticationDetails restDetails = (RestWebAuthenticationDetails) details;
          sessionToken = restDetails.getToken();

        } else if (details instanceof SessionTokenAuthenticationDetails) {
          SessionTokenAuthenticationDetails nonWebDetails =
              (SessionTokenAuthenticationDetails) details;
          sessionToken = nonWebDetails.getToken();
        } 
        if (StringUtils.isNotBlank(sessionToken)) {
          headers.add(CoreConstants.TOKEN_HEADER, sessionToken);
        }
      }
    }
    return execution.execute(request, body);
  }

}

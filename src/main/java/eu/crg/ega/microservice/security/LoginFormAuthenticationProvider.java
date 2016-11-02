package eu.crg.ega.microservice.security;

import lombok.extern.log4j.Log4j;
import eu.crg.ega.microservice.constant.ParamName;
import eu.crg.ega.microservice.dto.Base;
import eu.crg.ega.microservice.dto.auth.SessionUser;
import eu.crg.ega.microservice.dto.auth.User;
import eu.crg.ega.microservice.enums.LoginType;
import eu.crg.ega.microservice.enums.ServiceType;
import eu.crg.ega.microservice.rest.RestSender;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Log4j
public class LoginFormAuthenticationProvider implements AuthenticationProvider {

  private static final String AUTHENTICATION_FAILED = "Authentication Failed";
  @Autowired
  private RestSender restSender;

//  private Base<SessionUser> returnedSessionUser;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    LoginTypeUsernamePasswordAuthenticationToken token =
        (LoginTypeUsernamePasswordAuthenticationToken) authentication;

    LoginType
        loginType = token.getLoginType();

    String password = authentication.getCredentials().toString();

    try {
      MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
      params.add(ParamName.USERNAME, authentication.getPrincipal().toString());
      params.add(ParamName.PASSWORD, new String(password));
      params.add(ParamName.LOGIN_TYPE, loginType.getValue());

      final Base<SessionUser>
          response =
          restSender.sendMicroservice(ServiceType.SESSION, ParamName.LOGIN_ENDPOINT,
                                           HttpMethod.POST, params, null, null,
                                           SessionUser.class,
                                           MediaType.APPLICATION_FORM_URLENCODED,
                                           MediaType.APPLICATION_JSON);

      if (response == null
          || response.getHeader() == null
          || !StringUtils.equals(String.valueOf(HttpStatus.OK.value()), response
              .getHeader().getCode()) || response.getResponse() == null
          || response.getResponse().getResult() == null
          || response.getResponse().getResult().size() != 1) {
        log.error(AUTHENTICATION_FAILED + ": Bad Rest call");
        throw new AuthenticationServiceException(AUTHENTICATION_FAILED + ": Bad Rest call");
      }

      final SessionUser sessionUser = response.getResponse().getResult().get(0);
      final User user = sessionUser.getUser();

      if (user == null) {
        log.error(AUTHENTICATION_FAILED + ": Principal from Rest is null");
        throw new AuthenticationServiceException(AUTHENTICATION_FAILED
            + ": Principal from Rest is null");
      }

      // Initialize details with session token
      SessionTokenAuthenticationDetails details =
          new SessionTokenAuthenticationDetails(sessionUser.getSession().getSessionToken());
     
      // Initialize authentication object
      LoginTypeUsernamePasswordAuthenticationToken loginTypeUsernamePasswordAuthenticationToken =
          new LoginTypeUsernamePasswordAuthenticationToken(sessionUser, authentication
              .getCredentials().toString(), loginType, user.getAuthorities(), details);

      return loginTypeUsernamePasswordAuthenticationToken;

    } catch (Exception ex) {
      log.error(AUTHENTICATION_FAILED, ex);
      throw new AuthenticationServiceException(AUTHENTICATION_FAILED, ex);
    }
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return LoginTypeUsernamePasswordAuthenticationToken.class.equals(authentication);
  }
}
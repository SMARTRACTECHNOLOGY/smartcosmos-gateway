package net.smartcosmos.cluster.gateway.filters;

import javax.servlet.http.HttpServletRequest;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;

import org.apache.commons.lang.ArrayUtils;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.security.oauth2.proxy.ProxyAuthenticationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;

import net.smartcosmos.cluster.gateway.AuthenticationClient;

import static ch.qos.logback.classic.Level.WARN;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class PreAuthorizationFilterTest {

    @Mock
    ProxyAuthenticationProperties properties;

    @Mock
    AuthenticationClient authenticationClient;

    @Spy
    @InjectMocks
    PreAuthorizationFilter filter;

    @Mock
    Appender mockAppender;

    @Mock
    HttpServletRequest request;

    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    @Before
    public void setUp() {

        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);

        when(filter.getRequest()).thenReturn(request);
    }

    @After
    public void tearDown() {

        reset(properties, authenticationClient, mockAppender);
    }

    @Test
    public void thatMockingWorks() {

        assertNotNull(properties);
        assertNotNull(authenticationClient);
        assertNotNull(filter);
    }

    // region Logging

    @Test
    public void thatRunLogsWithWarnLevelInCaseOfException() {

        final String username = "someUser";
        final String password = "someArbitraryTestingPassw0rd";

        doReturn(new String[] { username, password }).when(filter)
            .getAuthenticationCredentials();

        when(authenticationClient.getOauthToken(eq(username), eq(password))).thenThrow(new RuntimeException("someException"));

        filter.run();

        verify(mockAppender, times(1)).doAppend(captorLoggingEvent.capture());
        LoggingEvent loggingEvent = captorLoggingEvent.getValue();
        assertEquals(WARN, loggingEvent.getLevel());
    }

    @Test
    public void thatRunLogsWithWarnLevelInCaseOfBadCredentialsException() {

        final String username = "someUser";
        final String password = "someArbitraryTestingPassw0rd";

        doReturn(new String[] { username, password }).when(filter)
            .getAuthenticationCredentials();

        when(authenticationClient.getOauthToken(eq(username), eq(password))).thenThrow(new BadCredentialsException("someException"));
        doNothing().when(filter)
            .setErrorResponse(any(), any());

        filter.run();

        verify(mockAppender, times(1)).doAppend(captorLoggingEvent.capture());
        LoggingEvent loggingEvent = captorLoggingEvent.getValue();
        assertEquals(WARN, loggingEvent.getLevel());
    }

    @Test
    public void thatRunLogsCorrectMessageInCaseOfException() {

        final String username = "someUser";
        final String password = "someArbitraryTestingPassw0rd";

        doReturn(new String[] { username, password }).when(filter)
            .getAuthenticationCredentials();

        when(authenticationClient.getOauthToken(eq(username), eq(password))).thenThrow(new RuntimeException("someException"));

        filter.run();

        verify(mockAppender, times(1)).doAppend(captorLoggingEvent.capture());
        LoggingEvent loggingEvent = captorLoggingEvent.getValue();
        assertNotNull(loggingEvent.getMessage());
        assertTrue(loggingEvent.getMessage()
                       .startsWith("Exception processing authentication request."));
    }

    @Test
    public void thatRunLogsCorrectMessageInCaseOfBadCredentialsException() {

        final String username = "someUser";
        final String password = "someArbitraryTestingPassw0rd";

        doReturn(new String[] { username, password }).when(filter)
            .getAuthenticationCredentials();

        when(authenticationClient.getOauthToken(eq(username), eq(password))).thenThrow(new BadCredentialsException("someException"));
        doNothing().when(filter)
            .setErrorResponse(any(), any());

        filter.run();

        verify(mockAppender, times(1)).doAppend(captorLoggingEvent.capture());
        LoggingEvent loggingEvent = captorLoggingEvent.getValue();
        assertNotNull(loggingEvent.getMessage());
        assertTrue(loggingEvent.getMessage()
                       .startsWith("Authentication request failed."));
    }

    @Test
    public void thatRunLogsNoPasswordInCaseOfException() {

        final String username = "someUser";
        final String password = "someArbitraryTestingPassw0rd";

        doReturn(new String[] { username, password }).when(filter)
            .getAuthenticationCredentials();

        when(authenticationClient.getOauthToken(eq(username), eq(password))).thenThrow(new RuntimeException("someException"));

        filter.run();

        verify(mockAppender, times(1)).doAppend(captorLoggingEvent.capture());
        LoggingEvent loggingEvent = captorLoggingEvent.getValue();
        assertFalse(loggingEvent.getFormattedMessage()
                        .contains(password));
    }

    @Test
    public void thatRunLogsNoPasswordInCaseOfBadCredentialsException() {

        final String username = "someUser";
        final String password = "someArbitraryTestingPassw0rd";

        doReturn(new String[] { username, password }).when(filter)
            .getAuthenticationCredentials();

        when(authenticationClient.getOauthToken(eq(username), eq(password))).thenThrow(new BadCredentialsException("someException"));
        doNothing().when(filter)
            .setErrorResponse(any(), any());

        filter.run();

        verify(mockAppender, times(1)).doAppend(captorLoggingEvent.capture());
        LoggingEvent loggingEvent = captorLoggingEvent.getValue();
        assertFalse(loggingEvent.getFormattedMessage()
                        .contains(password));
    }

    @Test
    public void thatRunLogsEmptyArrayForNullInCaseOfException() {

        final String username = "someUser";
        final String password = "someArbitraryTestingPassw0rd";

        doReturn(null).when(filter)
            .getAuthenticationCredentials();

        when(authenticationClient.getOauthToken(eq(username), eq(password))).thenThrow(new RuntimeException("someException"));

        filter.run();

        verify(mockAppender, times(1)).doAppend(captorLoggingEvent.capture());
        LoggingEvent loggingEvent = captorLoggingEvent.getValue();
        assertTrue(loggingEvent.getFormattedMessage()
                       .contains("'{}'"));
    }

    @Test
    public void thatRunLogsFullArrayForInvalidAuthorizationInCaseOfException() {

        final String username = "someUser";
        final String password = "someArbitraryTestingPassw0rd";
        final String invalid = "somethingInvalid";
        final String[] authArray = new String[] { username, password, invalid };

        doReturn(authArray).when(filter)
            .getAuthenticationCredentials();

        when(authenticationClient.getOauthToken(eq(username), eq(password))).thenThrow(new RuntimeException("someException"));

        filter.run();

        verify(mockAppender, times(1)).doAppend(captorLoggingEvent.capture());
        LoggingEvent loggingEvent = captorLoggingEvent.getValue();
        assertTrue(loggingEvent.getFormattedMessage()
                       .contains(ArrayUtils.toString(authArray)));
    }

    @Test
    public void thatRunLogsException() {

        final String username = "someUser";
        final String password = "someArbitraryTestingPassw0rd";
        final String[] authArray = new String[] { username, password };
        final RuntimeException exception = new RuntimeException("someException");

        doReturn(authArray).when(filter)
            .getAuthenticationCredentials();

        when(authenticationClient.getOauthToken(eq(username), eq(password))).thenThrow(exception);

        filter.run();

        verify(mockAppender, times(1)).doAppend(captorLoggingEvent.capture());
        LoggingEvent loggingEvent = captorLoggingEvent.getValue();
        assertTrue(loggingEvent.getFormattedMessage()
                       .contains(exception.toString()));
    }

    @Test
    public void thatRunCallsSetErrorResponseInCaseOfBadCredentialsException() {

        final String username = "someUser";
        final String password = "someArbitraryTestingPassw0rd";
        final String message = "exception message";

        doReturn(new String[] { username, password }).when(filter)
            .getAuthenticationCredentials();

        when(authenticationClient.getOauthToken(eq(username), eq(password))).thenThrow(new BadCredentialsException(message));
        doNothing().when(filter)
            .setErrorResponse(any(), any());
        when(request.getServletPath()).thenReturn("/test");

        filter.run();

        verify(filter, times(1)).setErrorResponse(eq(UNAUTHORIZED), eq("Access Denied"));
    }

    // endregion

    // region getResponseBody()

    @Test
    public void thatGetResponseBodySetsTimestamp() {

        final HttpStatus status = UNAUTHORIZED;
        final String message = "Some message";
        final String path = "/path";

        final String expectedString = "\"timestamp\"";

        String body = filter.getResponseBody(status, message, path);

        assertTrue(body.contains(expectedString));
    }

    @Test
    public void thatGetResponseBodySetsStatus() {

        final HttpStatus status = UNAUTHORIZED;
        final String message = "Some message";
        final String path = "/path";

        final String expectedString = String.format("\"status\":%s", status.value());

        String body = filter.getResponseBody(status, message, path);

        assertTrue(body.contains(expectedString));
    }

    @Test
    public void thatGetResponseBodySetsError() {

        final HttpStatus status = UNAUTHORIZED;
        final String message = "Some message";
        final String path = "/path";

        final String expectedString = String.format("\"error\":\"%s\"", status.getReasonPhrase());

        String body = filter.getResponseBody(status, message, path);

        assertTrue(body.contains(expectedString));
    }

    @Test
    public void thatGetResponseBodySetsMessage() {

        final HttpStatus status = UNAUTHORIZED;
        final String message = "Some message";
        final String path = "/path";

        final String expectedString = String.format("\"message\":\"%s\"", message);

        String body = filter.getResponseBody(status, message, path);

        assertTrue(body.contains(expectedString));
    }

    @Test
    public void thatGetResponseBodySetsPath() {

        final HttpStatus status = UNAUTHORIZED;
        final String message = "Some message";
        final String path = "/path";

        final String expectedString = String.format("\"path\":\"%s\"", path);

        String body = filter.getResponseBody(status, message, path);

        assertTrue(body.contains(expectedString));
    }

    // endregion

}

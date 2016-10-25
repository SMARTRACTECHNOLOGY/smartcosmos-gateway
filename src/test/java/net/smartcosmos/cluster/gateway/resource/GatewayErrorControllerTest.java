package net.smartcosmos.cluster.gateway.resource;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.netflix.client.ClientException;
import com.netflix.zuul.context.RequestContext;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import net.smartcosmos.cluster.gateway.domain.ErrorResponse;

import static ch.qos.logback.classic.Level.DEBUG;
import static ch.qos.logback.classic.Level.TRACE;
import static ch.qos.logback.classic.Level.WARN;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.I_AM_A_TEAPOT;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

import static net.smartcosmos.cluster.gateway.resource.GatewayErrorController.ATTR_ERROR_EXCEPTION;
import static net.smartcosmos.cluster.gateway.resource.GatewayErrorController.ATTR_ERROR_MESSAGE;
import static net.smartcosmos.cluster.gateway.resource.GatewayErrorController.ATTR_ERROR_STATUS_CODE;
import static net.smartcosmos.cluster.gateway.resource.GatewayErrorController.ATTR_PROXY;
import static net.smartcosmos.cluster.gateway.resource.GatewayErrorController.ATTR_SERVICE_ID;
import static net.smartcosmos.cluster.gateway.resource.GatewayErrorController.ERROR_MESSAGE_TIMEOUT;
import static net.smartcosmos.cluster.gateway.resource.GatewayErrorController.ZUUL_REQUEST_URI;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class GatewayErrorControllerTest {

    @Mock
    RequestContext requestContext;

    @Spy
    GatewayErrorController errorController = new GatewayErrorController();

    @Mock
    Appender mockAppender;

    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    private static Logger logger = LoggerFactory.getLogger(GatewayErrorController.class);

    @Before
    public void setUp() {

        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);
        root.setLevel(TRACE);

        when(errorController.getCurrentContext()).thenReturn(requestContext);

        when(requestContext.containsKey(anyString())).thenReturn(false);
    }

    @After
    public void tearDown() {

        reset(errorController, mockAppender, requestContext);
    }

    @Test
    public void thatMockingWorks() {

        assertNotNull(errorController);
        assertNotNull(mockAppender);
        assertNotNull(requestContext);
        assertNotNull(logger);
    }

    // region error()

    // region empty context

    @Test
    public void thatErrorCallsErrorResponseForEmptyContext() {

        final HttpStatus expectedStatus = INTERNAL_SERVER_ERROR;
        final String expectedMessage = "No context information available. A reason for this can be that no configured route matched the request.";
        final String expectedPath = null;

        when(requestContext.isEmpty()).thenReturn(true);

        errorController.error();

        verify(errorController, times(1)).errorResponse(eq(expectedStatus), eq(expectedMessage), eq(expectedPath));
    }

    @Test
    public void thatErrorLogsInformationAtWarnLevelForEmptyContext() {

        when(requestContext.isEmpty()).thenReturn(true);

        errorController.error();

        verify(mockAppender, times(1)).doAppend(captorLoggingEvent.capture());
        LoggingEvent loggingEvent = captorLoggingEvent.getValue();
        assertEquals(WARN, loggingEvent.getLevel());
    }

    @Test
    public void thatErrorLogsMessageForEmptyContext() {

        final String expectedLogMessage = "No context information available. A reason for this can be that no configured route matched the request.";

        when(requestContext.isEmpty()).thenReturn(true);

        errorController.error();

        verify(mockAppender, times(1)).doAppend(captorLoggingEvent.capture());
        LoggingEvent loggingEvent = captorLoggingEvent.getValue();
        assertEquals(expectedLogMessage, loggingEvent.getFormattedMessage());
    }

    @Test
    public void thatThatErrorReturnsExpectedResponseEntityForEmptyContext() {

        final HttpStatus expectedStatus = INTERNAL_SERVER_ERROR;
        final String expectedMessage = "No context information available. A reason for this can be that no configured route matched the request.";
        final String expectedPath = null;

        when(requestContext.isEmpty()).thenReturn(true);

        ResponseEntity responseEntity = errorController.error();
        ErrorResponse responseBody = (ErrorResponse) responseEntity.getBody();

        assertEquals(expectedStatus, responseEntity.getStatusCode());
        assertEquals(expectedMessage, responseBody.getMessage());
        assertEquals(expectedPath, responseBody.getPath());
    }

    // endregion

    // region context information available

    @Test
    public void thatErrorReadsContextInformationIs() {

        when(requestContext.isEmpty()).thenReturn(false);

        errorController.error();

        verify(errorController, times(1)).getRequestUriFromRequestContext(eq(requestContext));
        verify(errorController, times(1)).getHttpStatusFromRequestContext(eq(requestContext));
        verify(errorController, times(1)).getRouteFromRequestContext(eq(requestContext));
        verify(errorController, times(1)).getServiceIdFromRequestContext(eq(requestContext));
        verify(errorController, times(1)).getErrorMessageFromRequestContext(eq(requestContext));
        verify(errorController, times(1)).getExceptionFromRequestContext(eq(requestContext));
    }

    @Test
    public void thatErrorLogsAtWarnLevel() {

        when(requestContext.isEmpty()).thenReturn(false);

        errorController.error();

        verify(mockAppender, times(2)).doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> loggingEvents = captorLoggingEvent.getAllValues();

        Boolean loggedAtWarn = false;
        for (LoggingEvent loggingEvent : loggingEvents) {
            if (WARN.equals(loggingEvent.getLevel())) {
                loggedAtWarn = true;
                break;
            }
        }
        assertTrue(loggedAtWarn);
    }

    @Test
    public void thatErrorLogsAtDebugLevel() {

        when(requestContext.isEmpty()).thenReturn(false);

        errorController.error();

        verify(mockAppender, times(2)).doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> loggingEvents = captorLoggingEvent.getAllValues();

        Boolean loggedAtDebug = false;
        for (LoggingEvent loggingEvent : loggingEvents) {
            if (DEBUG.equals(loggingEvent.getLevel())) {
                loggedAtDebug = true;
                break;
            }
        }
        assertTrue(loggedAtDebug);
    }

    @Test
    public void thatErrorLogsRoute() {

        final String expectedRoute = "some-route";
        final String expectedLogOutput = String.format("Using route '%s'", expectedRoute);

        when(requestContext.isEmpty()).thenReturn(false);
        doReturn(expectedRoute).when(errorController)
            .getRouteFromRequestContext(eq(requestContext));

        errorController.error();

        verify(mockAppender, times(2)).doAppend(captorLoggingEvent.capture());
        LoggingEvent loggingEvent = captorLoggingEvent.getAllValues()
            .get(0);
        String logMessage = loggingEvent.getFormattedMessage();

        assertTrue(logMessage.contains(expectedLogOutput));
    }

    @Test
    public void thatErrorLogsService() {

        final String expectedService = "some-service";
        final String expectedLogOutput = String.format("to service '%s'", expectedService);

        when(requestContext.isEmpty()).thenReturn(false);
        doReturn(expectedService).when(errorController)
            .getServiceIdFromRequestContext(eq(requestContext));

        errorController.error();

        verify(mockAppender, times(2)).doAppend(captorLoggingEvent.capture());
        LoggingEvent loggingEvent = captorLoggingEvent.getAllValues()
            .get(0);
        String logMessage = loggingEvent.getFormattedMessage();

        assertTrue(logMessage.contains(expectedLogOutput));
    }

    @Test
    public void thatErrorLogsRequest() {

        final String expectedRequest = "/path";
        final String expectedLogOutput = String.format("for request '%s'", expectedRequest);

        when(requestContext.isEmpty()).thenReturn(false);
        doReturn(expectedRequest).when(errorController)
            .getRequestUriFromRequestContext(eq(requestContext));

        errorController.error();

        verify(mockAppender, times(2)).doAppend(captorLoggingEvent.capture());
        LoggingEvent loggingEvent = captorLoggingEvent.getAllValues()
            .get(0);
        String logMessage = loggingEvent.getFormattedMessage();

        assertTrue(logMessage.contains(expectedLogOutput));
    }

    @Test
    public void thatErrorLogsStatusCode() {

        final HttpStatus expectedHttpStatus = I_AM_A_TEAPOT;
        final String expectedLogOutput = String.format("Status code: '%s'", expectedHttpStatus.value());

        when(requestContext.isEmpty()).thenReturn(false);
        doReturn(expectedHttpStatus).when(errorController)
            .getHttpStatusFromRequestContext(eq(requestContext));

        errorController.error();

        verify(mockAppender, times(2)).doAppend(captorLoggingEvent.capture());
        LoggingEvent loggingEvent = captorLoggingEvent.getAllValues()
            .get(0);
        String logMessage = loggingEvent.getFormattedMessage();

        assertTrue(logMessage.contains(expectedLogOutput));
    }

    @Test
    public void thatErrorLogsErrorMessage() {

        final String expectedErrorMessage = "some error";
        final String expectedLogOutput = String.format("Error: '%s'", expectedErrorMessage);

        when(requestContext.isEmpty()).thenReturn(false);
        doReturn(expectedErrorMessage).when(errorController)
            .getErrorMessageFromRequestContext(eq(requestContext));

        errorController.error();

        verify(mockAppender, times(2)).doAppend(captorLoggingEvent.capture());
        LoggingEvent loggingEvent = captorLoggingEvent.getAllValues()
            .get(0);
        String logMessage = loggingEvent.getFormattedMessage();

        assertTrue(logMessage.contains(expectedLogOutput));
    }

    @Test
    public void thatErrorLogsMessageForMissingCause() {

        final Exception expectedException = null;
        final String expectedExceptionString = "No exception available in context";
        final String expectedLogOutput = String.format("Cause: %s", expectedExceptionString);

        when(requestContext.isEmpty()).thenReturn(false);
        doReturn(expectedException).when(errorController)
            .getExceptionFromRequestContext(eq(requestContext));

        errorController.error();

        verify(mockAppender, times(2)).doAppend(captorLoggingEvent.capture());
        LoggingEvent loggingEvent = captorLoggingEvent.getAllValues()
            .get(0);
        String logMessage = loggingEvent.getFormattedMessage();

        assertTrue(logMessage.contains(expectedLogOutput));
    }

    @Test
    public void thatErrorLogsCause() {

        final Exception expectedException = new RuntimeException("some exception");
        final String expectedExceptionString = expectedException.toString();
        final String expectedLogOutput = String.format("Cause: %s", expectedExceptionString);

        when(requestContext.isEmpty()).thenReturn(false);
        doReturn(expectedException).when(errorController)
            .getExceptionFromRequestContext(eq(requestContext));

        errorController.error();

        verify(mockAppender, times(2)).doAppend(captorLoggingEvent.capture());
        LoggingEvent loggingEvent = captorLoggingEvent.getAllValues()
            .get(0);
        String logMessage = loggingEvent.getFormattedMessage();

        assertTrue(logMessage.contains(expectedLogOutput));
    }

    @Test
    public void thatErrorLogsMessageForMissingRootCause() {

        final Exception expectedException = new Exception("some exception");
        final String expectedRootCauseString = "N/A";
        final String expectedLogOutput = String.format("Root cause: %s", expectedRootCauseString);

        when(requestContext.isEmpty()).thenReturn(false);
        doReturn(expectedException).when(errorController)
            .getExceptionFromRequestContext(eq(requestContext));

        errorController.error();

        verify(mockAppender, times(2)).doAppend(captorLoggingEvent.capture());
        LoggingEvent loggingEvent = captorLoggingEvent.getAllValues()
            .get(0);
        String logMessage = loggingEvent.getFormattedMessage();

        assertTrue(logMessage.contains(expectedLogOutput));
    }

    @Test
    public void thatErrorLogsRootCause() {

        final Exception rootCause = new RuntimeException("some root cause");
        final Exception expectedException = new Exception("some exception", rootCause);
        final String expectedRootCauseString = rootCause.toString();
        final String expectedLogOutput = String.format("Root cause: %s", expectedRootCauseString);

        when(requestContext.isEmpty()).thenReturn(false);
        doReturn(expectedException).when(errorController)
            .getExceptionFromRequestContext(eq(requestContext));

        errorController.error();

        verify(mockAppender, times(2)).doAppend(captorLoggingEvent.capture());
        LoggingEvent loggingEvent = captorLoggingEvent.getAllValues()
            .get(0);
        String logMessage = loggingEvent.getFormattedMessage();

        assertTrue(logMessage.contains(expectedLogOutput));
    }

    @Test
    public void thatErrorLogsCompleteMessageCorrectly() {

        final String expectedRoute = "some-route";
        final String expectedServiceId = "some-service";
        final String expectedRequestUri = "/some/dummy/path";
        final HttpStatus expectedHttpStatus = I_AM_A_TEAPOT;
        final String expectedErrorMessage = "some error";
        final Exception expectedRootCause = new RuntimeException("some root cause");
        final Exception expectedException = new Exception("some exception", expectedRootCause);

        final String expectedLogOutput = String.format("Using route '%s' to service '%s' for request '%s' failed.\n"
                                                       + "Status code: '%s', Error: '%s'\n"
                                                       + "Cause: %s\n"
                                                       + "Root cause: %s",
                                                       expectedRoute,
                                                       expectedServiceId,
                                                       expectedRequestUri,
                                                       expectedHttpStatus,
                                                       expectedErrorMessage,
                                                       expectedException.toString(),
                                                       expectedRootCause.toString());

        when(requestContext.isEmpty()).thenReturn(false);

        doReturn(expectedRoute).when(errorController)
            .getRouteFromRequestContext(eq(requestContext));
        doReturn(expectedServiceId).when(errorController)
            .getServiceIdFromRequestContext(eq(requestContext));
        doReturn(expectedRequestUri).when(errorController)
            .getRequestUriFromRequestContext(eq(requestContext));
        doReturn(expectedHttpStatus).when(errorController)
            .getHttpStatusFromRequestContext(eq(requestContext));
        doReturn(expectedErrorMessage).when(errorController)
            .getErrorMessageFromRequestContext(eq(requestContext));
        doReturn(expectedException).when(errorController)
            .getExceptionFromRequestContext(eq(requestContext));

        errorController.error();

        verify(mockAppender, times(2)).doAppend(captorLoggingEvent.capture());
        LoggingEvent loggingEvent = captorLoggingEvent.getAllValues()
            .get(0);
        String logMessage = loggingEvent.getFormattedMessage();

        assertTrue(logMessage.contains(expectedLogOutput));
    }

    @Test
    public void thatErrorReturnsCorrectResponseBodyWithRootCause() {

        final String expectedRoute = "some-route";
        final String expectedPath = "/some/dummy/path";
        final Exception expectedRootCause = new RuntimeException("some root cause");
        final Exception expectedException = new Exception("some exception", expectedRootCause);

        final HttpStatus expectedStatus = INTERNAL_SERVER_ERROR;
        final String expectedExceptionMessage = String.format("%s: %s", expectedException.getMessage(), expectedRootCause.getMessage());
        final String expectedMessage = String.format("Gateway error: '%s' failed: %s", expectedRoute, expectedExceptionMessage);

        when(requestContext.isEmpty()).thenReturn(false);

        doReturn(expectedRoute).when(errorController)
            .getRouteFromRequestContext(eq(requestContext));
        doReturn(expectedPath).when(errorController)
            .getRequestUriFromRequestContext(eq(requestContext));
        doReturn(expectedException).when(errorController)
            .getExceptionFromRequestContext(eq(requestContext));

        ResponseEntity responseEntity = errorController.error();
        ErrorResponse responseBody = (ErrorResponse) responseEntity.getBody();

        assertEquals(expectedStatus, responseEntity.getStatusCode());
        assertEquals(expectedMessage, responseBody.getMessage());
        assertEquals(expectedPath, responseBody.getPath());
    }

    @Test
    public void thatErrorReturnsCorrectResponseBodyWithoutRootCause() {

        final String expectedRoute = "some-route";
        final String expectedPath = "/some/dummy/path";
        final Exception expectedException = new Exception("some exception");

        final String expectedExceptionMessage = String.format("%s", expectedException.getMessage());
        final String expectedMessage = String.format("Gateway error: '%s' failed: %s", expectedRoute, expectedExceptionMessage);

        when(requestContext.isEmpty()).thenReturn(false);

        doReturn(expectedRoute).when(errorController)
            .getRouteFromRequestContext(eq(requestContext));
        doReturn(expectedPath).when(errorController)
            .getRequestUriFromRequestContext(eq(requestContext));
        doReturn(expectedException).when(errorController)
            .getExceptionFromRequestContext(eq(requestContext));

        ResponseEntity responseEntity = errorController.error();
        ErrorResponse responseBody = (ErrorResponse) responseEntity.getBody();

        assertEquals(expectedMessage, responseBody.getMessage());
        assertEquals(expectedPath, responseBody.getPath());
    }

    // endregion

    // region timeout

    @Test
    public void thatThatErrorReturnsGatewayTimeout() {

        final HttpStatus expectedStatus = GATEWAY_TIMEOUT;

        when(requestContext.isEmpty()).thenReturn(false);
        when(errorController.isGatewayTimeout(any())).thenReturn(true);
        when(errorController.isServiceUnavailable(any())).thenReturn(false);

        ResponseEntity responseEntity = errorController.error();

        assertEquals(expectedStatus, responseEntity.getStatusCode());
    }

    @Test
    public void thatThatErrorReturnsGatewayTimeoutInCaseOfTimeoutErrorMessage() {

        final HttpStatus expectedStatus = GATEWAY_TIMEOUT;

        when(requestContext.isEmpty()).thenReturn(false);
        when(errorController.getErrorMessageFromRequestContext(eq(requestContext))).thenReturn(ERROR_MESSAGE_TIMEOUT);
        when(errorController.isGatewayTimeout(any())).thenReturn(false);
        when(errorController.isServiceUnavailable(any())).thenReturn(false);

        ResponseEntity responseEntity = errorController.error();

        assertEquals(expectedStatus, responseEntity.getStatusCode());
    }

    // endregion

    // region service unavailable

    @Test
    public void thatThatErrorReturnsServiceUnavailable() {

        final HttpStatus expectedStatus = SERVICE_UNAVAILABLE;

        when(requestContext.isEmpty()).thenReturn(false);
        when(errorController.isGatewayTimeout(any())).thenReturn(false);
        when(errorController.isServiceUnavailable(any())).thenReturn(true);

        ResponseEntity responseEntity = errorController.error();

        assertEquals(expectedStatus, responseEntity.getStatusCode());
    }

    // endregion

    // region other response

    @Test
    public void thatThatErrorReturnsInternalServerErrorAsDefault() {

        final HttpStatus expectedStatus = INTERNAL_SERVER_ERROR;

        when(requestContext.isEmpty()).thenReturn(false);
        when(errorController.isGatewayTimeout(any())).thenReturn(false);
        when(errorController.isServiceUnavailable(any())).thenReturn(false);

        ResponseEntity responseEntity = errorController.error();

        assertEquals(expectedStatus, responseEntity.getStatusCode());
    }

    // endregion

    // endregion

    // region errorResponse()

    @Test
    public void thatErrorResponseReturnsResponseEntity() {

        final HttpStatus expectedStatus = INTERNAL_SERVER_ERROR;
        final String expectedMessage = "someMessage";
        final String expectedPath = "/path";

        ResponseEntity responseEntity = errorController.errorResponse(expectedStatus, expectedMessage, expectedPath);

        assertNotNull(responseEntity);
    }

    @Test
    public void thatErrorResponseReturnsExpectedHttpStatus() {

        final HttpStatus expectedStatus = INTERNAL_SERVER_ERROR;
        final String expectedMessage = "someMessage";
        final String expectedPath = "/path";

        ResponseEntity responseEntity = errorController.errorResponse(expectedStatus, expectedMessage, expectedPath);

        assertEquals(INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
    }

    @Test
    public void thatErrorResponseReturnsErrorResponseBody() {

        final HttpStatus expectedStatus = INTERNAL_SERVER_ERROR;
        final String expectedMessage = "someMessage";
        final String expectedPath = "/path";

        ResponseEntity responseEntity = errorController.errorResponse(expectedStatus, expectedMessage, expectedPath);

        assertTrue(responseEntity.hasBody());
        assertTrue(responseEntity.getBody() instanceof ErrorResponse);
        assertNotNull(responseEntity.getBody());
    }

    @Test
    public void thatErrorResponseIncludesHttpStatusInBody() {

        final HttpStatus expectedStatus = INTERNAL_SERVER_ERROR;
        final String expectedMessage = "someMessage";
        final String expectedPath = "/path";

        ResponseEntity responseEntity = errorController.errorResponse(expectedStatus, expectedMessage, expectedPath);
        ErrorResponse errorResponse = (ErrorResponse) responseEntity.getBody();

        assertNotNull(errorResponse.getStatus());
        assertEquals((Integer) expectedStatus.value(), errorResponse.getStatus());

        assertNotNull(errorResponse.getError());
        assertEquals(expectedStatus.getReasonPhrase(), errorResponse.getError());
    }

    @Test
    public void thatErrorResponseIncludesTimestampInBody() {

        final HttpStatus expectedStatus = INTERNAL_SERVER_ERROR;
        final String expectedMessage = "someMessage";
        final String expectedPath = "/path";

        ResponseEntity responseEntity = errorController.errorResponse(expectedStatus, expectedMessage, expectedPath);
        ErrorResponse errorResponse = (ErrorResponse) responseEntity.getBody();

        assertNotNull(errorResponse.getTimestamp());
        assertTrue(errorResponse.getTimestamp() instanceof Long);
    }

    @Test
    public void thatErrorResponseIncludesMessageInBody() {

        final HttpStatus expectedStatus = INTERNAL_SERVER_ERROR;
        final String expectedMessage = "someMessage";
        final String expectedPath = "/path";

        ResponseEntity responseEntity = errorController.errorResponse(expectedStatus, expectedMessage, expectedPath);
        ErrorResponse errorResponse = (ErrorResponse) responseEntity.getBody();

        assertNotNull(errorResponse.getMessage());
        assertEquals(expectedMessage, errorResponse.getMessage());
    }

    @Test
    public void thatErrorResponseIncludesPathInBody() {

        final HttpStatus expectedStatus = INTERNAL_SERVER_ERROR;
        final String expectedMessage = "someMessage";
        final String expectedPath = "/path";

        ResponseEntity responseEntity = errorController.errorResponse(expectedStatus, expectedMessage, expectedPath);
        ErrorResponse errorResponse = (ErrorResponse) responseEntity.getBody();

        assertNotNull(errorResponse.getMessage());
        assertEquals(expectedPath, errorResponse.getPath());
    }

    // endregion

    // region isServiceUnavailable()

    @Test
    public void thatIsServiceUnavailableReturnsTrueForClientException() {

        Exception exception = mock(ClientException.class);
        assertTrue(errorController.isServiceUnavailable(exception));
    }

    @Test
    public void thatIsServiceUnavailableReturnsFalseForArbitraryException() {

        Exception exception = mock(Exception.class);
        assertFalse(errorController.isServiceUnavailable(exception));
    }

    // endregion

    // region isGatewayTimeout()

    @Test
    public void thatIsGatewayTimeoutReturnsTrueForSocketTimeoutException() {

        Exception exception = mock(SocketTimeoutException.class);
        assertTrue(errorController.isGatewayTimeout(exception));
    }

    @Test
    public void thatIsGatewayTimeoutReturnsTrueForTimeoutException() {

        Exception exception = mock(TimeoutException.class);
        assertTrue(errorController.isGatewayTimeout(exception));
    }

    @Test
    public void thatIsGatewayTimeoutReturnsFalseForArbitraryException() {

        Exception exception = mock(Exception.class);
        assertFalse(errorController.isGatewayTimeout(exception));
    }

    // endregion

    // region getRequestUriFromRequestContext()

    @Test
    public void thatGetRequestUriFromRequestContextDefaultsToUnknown() {

        final String expectedUri = "unknown-uri";

        when(requestContext.containsKey(eq(ZUUL_REQUEST_URI))).thenReturn(false);

        String uri = errorController.getRequestUriFromRequestContext(requestContext);

        assertEquals(expectedUri, uri);
    }

    @Test
    public void thatGetRequestUriFromRequestContextSucceeds() {

        final String expectedUri = "/some/request/uri";

        when(requestContext.containsKey(eq(ZUUL_REQUEST_URI))).thenReturn(true);
        when(requestContext.get(eq(ZUUL_REQUEST_URI))).thenReturn(expectedUri);

        String uri = errorController.getRequestUriFromRequestContext(requestContext);

        assertEquals(expectedUri, uri);
    }

    // endregion

    // region getHttpStatusFromRequestContext()

    @Test
    public void thatGetHttpStatusFromRequestContextDefaultsToNull() {

        final HttpStatus expectedHttpStatus = null;

        when(requestContext.containsKey(eq(ATTR_ERROR_STATUS_CODE))).thenReturn(false);

        HttpStatus httpStatus = errorController.getHttpStatusFromRequestContext(requestContext);

        assertEquals(expectedHttpStatus, httpStatus);
    }

    @Test
    public void thatGetHttpStatusFromRequestContextSucceeds() {

        final HttpStatus expectedHttpStatus = I_AM_A_TEAPOT;

        when(requestContext.containsKey(eq(ATTR_ERROR_STATUS_CODE))).thenReturn(true);
        when(requestContext.get(eq(ATTR_ERROR_STATUS_CODE))).thenReturn(expectedHttpStatus.value());

        HttpStatus httpStatus = errorController.getHttpStatusFromRequestContext(requestContext);

        assertEquals(expectedHttpStatus, httpStatus);
    }

    // endregion

    // region getRouteFromRequestContext()

    @Test
    public void thatGetRouteFromRequestContextDefaultsToUnknown() {

        final String expectedRoute = "unknown-route";

        when(requestContext.containsKey(eq(ATTR_PROXY))).thenReturn(false);

        String route = errorController.getRouteFromRequestContext(requestContext);

        assertEquals(expectedRoute, route);
    }

    @Test
    public void thatGetRouteFromRequestContextSucceeds() {

        final String expectedRoute = "some-route";

        when(requestContext.containsKey(eq(ATTR_PROXY))).thenReturn(true);
        when(requestContext.get(eq(ATTR_PROXY))).thenReturn(expectedRoute);

        String route = errorController.getRouteFromRequestContext(requestContext);

        assertEquals(expectedRoute, route);
    }

    // endregion

    // region getServiceIdFromRequestContext()

    @Test
    public void thatGetServiceIdFromRequestContextDefaultsToUnknown() {

        final String expectedServiceId = "unknown-service";

        when(requestContext.containsKey(eq(ATTR_SERVICE_ID))).thenReturn(false);

        String serviceId = errorController.getServiceIdFromRequestContext(requestContext);

        assertEquals(expectedServiceId, serviceId);
    }

    @Test
    public void thatGetServiceIdFromRequestContextSucceeds() {

        final String expectedServiceId = "some-service";

        when(requestContext.containsKey(eq(ATTR_SERVICE_ID))).thenReturn(true);
        when(requestContext.get(eq(ATTR_SERVICE_ID))).thenReturn(expectedServiceId);

        String serviceId = errorController.getServiceIdFromRequestContext(requestContext);

        assertEquals(expectedServiceId, serviceId);
    }

    // endregion

    // region getExceptionFromRequestContext()

    @Test
    public void thatGetExceptionFromRequestContextDefaultsToNull() {

        final Exception expectedException = null;

        when(requestContext.containsKey(eq(ATTR_ERROR_EXCEPTION))).thenReturn(false);

        Exception exception = errorController.getExceptionFromRequestContext(requestContext);

        assertEquals(expectedException, exception);
    }

    @Test
    public void thatGetExceptionFromRequestContextSucceeds() {

        final Exception expectedException = mock(Exception.class);

        when(requestContext.containsKey(eq(ATTR_ERROR_EXCEPTION))).thenReturn(true);
        when(requestContext.get(eq(ATTR_ERROR_EXCEPTION))).thenReturn(expectedException);

        Exception exception = errorController.getExceptionFromRequestContext(requestContext);

        assertEquals(expectedException, exception);
    }

    // endregion

    // region getErrorMessageFromRequestContext()

    @Test
    public void thatGetErrorMessageFromRequestContextDefaultsToUnknown() {

        final String expectedErrorMessage = "No message available";

        when(requestContext.containsKey(eq(ATTR_ERROR_MESSAGE))).thenReturn(false);

        String serviceId = errorController.getErrorMessageFromRequestContext(requestContext);

        assertEquals(expectedErrorMessage, serviceId);
    }

    @Test
    public void thatGetErrorMessageFromRequestContextSucceeds() {

        final String expectedErrorMessage = "Some error message";

        when(requestContext.containsKey(eq(ATTR_ERROR_MESSAGE))).thenReturn(true);
        when(requestContext.get(eq(ATTR_ERROR_MESSAGE))).thenReturn(expectedErrorMessage);

        String serviceId = errorController.getErrorMessageFromRequestContext(requestContext);

        assertEquals(expectedErrorMessage, serviceId);
    }

    // endregion
}

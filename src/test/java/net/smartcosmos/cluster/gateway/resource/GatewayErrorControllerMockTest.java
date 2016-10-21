package net.smartcosmos.cluster.gateway.resource;

import java.net.SocketTimeoutException;

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

import static ch.qos.logback.classic.Level.WARN;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.I_AM_A_TEAPOT;

import static net.smartcosmos.cluster.gateway.resource.GatewayErrorController.ATTR_ERROR_EXCEPTION;
import static net.smartcosmos.cluster.gateway.resource.GatewayErrorController.ATTR_ERROR_MESSAGE;
import static net.smartcosmos.cluster.gateway.resource.GatewayErrorController.ATTR_ERROR_STATUS_CODE;
import static net.smartcosmos.cluster.gateway.resource.GatewayErrorController.ATTR_PROXY;
import static net.smartcosmos.cluster.gateway.resource.GatewayErrorController.ATTR_SERVICE_ID;
import static net.smartcosmos.cluster.gateway.resource.GatewayErrorController.ZUUL_REQUEST_URI;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class GatewayErrorControllerMockTest {

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

        when(errorController.getCurrentContext()).thenReturn(requestContext);
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

        when(requestContext.size()).thenReturn(0);

        errorController.error();

        verify(errorController, times(1)).errorResponse(eq(expectedStatus), eq(expectedMessage), eq(expectedPath));
    }

    @Test
    public void thatErrorLogsInformationAtWarnLevelForEmptyContext() {

        when(requestContext.size()).thenReturn(0);

        errorController.error();

        verify(mockAppender, times(1)).doAppend(captorLoggingEvent.capture());
        LoggingEvent loggingEvent = captorLoggingEvent.getValue();
        assertEquals(WARN, loggingEvent.getLevel());
    }

    @Test
    public void thatErrorLogsMessageForEmptyContext() {

        final String expectedLogMessage = "A request failed without any available context information";

        when(requestContext.size()).thenReturn(0);

        errorController.error();

        verify(mockAppender, times(1)).doAppend(captorLoggingEvent.capture());
        LoggingEvent loggingEvent = captorLoggingEvent.getValue();
        assertEquals(expectedLogMessage, loggingEvent.getFormattedMessage());
    }

    @Test
    public void thatThatErrorReturnsExpectedResponseEntity() {

        final HttpStatus expectedStatus = INTERNAL_SERVER_ERROR;
        final String expectedMessage = "No context information available. A reason for this can be that no configured route matched the request.";
        final String expectedPath = null;

        when(requestContext.size()).thenReturn(0);

        ResponseEntity responseEntity = errorController.error();
        ErrorResponse responseBody = (ErrorResponse) responseEntity.getBody();

        assertEquals(expectedStatus, responseEntity.getStatusCode());
        assertEquals(expectedMessage, responseBody.getMessage());
        assertEquals(expectedPath, responseBody.getPath());
    }

    // endregion

    // region timeout

    // endregion

    // region service unavailable

    // endregion

    // region other response

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

        String uri = GatewayErrorController.getRequestUriFromRequestContext(requestContext);

        assertEquals(expectedUri, uri);
    }

    @Test
    public void thatGetRequestUriFromRequestContextSucceeds() {

        final String expectedUri = "/some/request/uri";

        when(requestContext.containsKey(eq(ZUUL_REQUEST_URI))).thenReturn(true);
        when(requestContext.get(eq(ZUUL_REQUEST_URI))).thenReturn(expectedUri);

        String uri = GatewayErrorController.getRequestUriFromRequestContext(requestContext);

        assertEquals(expectedUri, uri);
    }

    // endregion

    // region getHttpStatusFromRequestContext()

    @Test
    public void thatGetHttpStatusFromRequestContextDefaultsToNull() {

        final HttpStatus expectedHttpStatus = null;

        when(requestContext.containsKey(eq(ATTR_ERROR_STATUS_CODE))).thenReturn(false);

        HttpStatus httpStatus = GatewayErrorController.getHttpStatusFromRequestContext(requestContext);

        assertEquals(expectedHttpStatus, httpStatus);
    }

    @Test
    public void thatGetHttpStatusFromRequestContextSucceeds() {

        final HttpStatus expectedHttpStatus = I_AM_A_TEAPOT;

        when(requestContext.containsKey(eq(ATTR_ERROR_STATUS_CODE))).thenReturn(true);
        when(requestContext.get(eq(ATTR_ERROR_STATUS_CODE))).thenReturn(expectedHttpStatus.value());

        HttpStatus httpStatus = GatewayErrorController.getHttpStatusFromRequestContext(requestContext);

        assertEquals(expectedHttpStatus, httpStatus);
    }

    // endregion

    // region getRouteFromRequestContext()

    @Test
    public void thatGetRouteFromRequestContextDefaultsToUnknown() {

        final String expectedRoute = "unknown-route";

        when(requestContext.containsKey(eq(ATTR_PROXY))).thenReturn(false);

        String route = GatewayErrorController.getRouteFromRequestContext(requestContext);

        assertEquals(expectedRoute, route);
    }

    @Test
    public void thatGetRouteFromRequestContextSucceeds() {

        final String expectedRoute = "some-route";

        when(requestContext.containsKey(eq(ATTR_PROXY))).thenReturn(true);
        when(requestContext.get(eq(ATTR_PROXY))).thenReturn(expectedRoute);

        String route = GatewayErrorController.getRouteFromRequestContext(requestContext);

        assertEquals(expectedRoute, route);
    }

    // endregion

    // region getServiceIdFromRequestContext()

    @Test
    public void thatGetServiceIdFromRequestContextDefaultsToUnknown() {

        final String expectedServiceId = "unknown-service";

        when(requestContext.containsKey(eq(ATTR_SERVICE_ID))).thenReturn(false);

        String serviceId = GatewayErrorController.getServiceIdFromRequestContext(requestContext);

        assertEquals(expectedServiceId, serviceId);
    }

    @Test
    public void thatGetServiceIdFromRequestContextSucceeds() {

        final String expectedServiceId = "some-service";

        when(requestContext.containsKey(eq(ATTR_SERVICE_ID))).thenReturn(true);
        when(requestContext.get(eq(ATTR_SERVICE_ID))).thenReturn(expectedServiceId);

        String serviceId = GatewayErrorController.getServiceIdFromRequestContext(requestContext);

        assertEquals(expectedServiceId, serviceId);
    }

    // endregion

    // region getExceptionFromRequestContext()

    @Test
    public void thatGetExceptionFromRequestContextDefaultsToNull() {

        final Exception expectedException = null;

        when(requestContext.containsKey(eq(ATTR_ERROR_EXCEPTION))).thenReturn(false);

        Exception exception = GatewayErrorController.getExceptionFromRequestContext(requestContext);

        assertEquals(expectedException, exception);
    }

    @Test
    public void thatGetExceptionFromRequestContextSucceeds() {

        final Exception expectedException = mock(Exception.class);

        when(requestContext.containsKey(eq(ATTR_ERROR_EXCEPTION))).thenReturn(true);
        when(requestContext.get(eq(ATTR_ERROR_EXCEPTION))).thenReturn(expectedException);

        Exception exception = GatewayErrorController.getExceptionFromRequestContext(requestContext);

        assertEquals(expectedException, exception);
    }

    // endregion

    // region getErrorMessageFromRequestContext()

    @Test
    public void thatGetErrorMessageFromRequestContextDefaultsToUnknown() {

        final String expectedErrorMessage = "No message available";

        when(requestContext.containsKey(eq(ATTR_ERROR_MESSAGE))).thenReturn(false);

        String serviceId = GatewayErrorController.getErrorMessageFromRequestContext(requestContext);

        assertEquals(expectedErrorMessage, serviceId);
    }

    @Test
    public void thatGetErrorMessageFromRequestContextSucceeds() {

        final String expectedErrorMessage = "Some error message";

        when(requestContext.containsKey(eq(ATTR_ERROR_MESSAGE))).thenReturn(true);
        when(requestContext.get(eq(ATTR_ERROR_MESSAGE))).thenReturn(expectedErrorMessage);

        String serviceId = GatewayErrorController.getErrorMessageFromRequestContext(requestContext);

        assertEquals(expectedErrorMessage, serviceId);
    }

    // endregion
}

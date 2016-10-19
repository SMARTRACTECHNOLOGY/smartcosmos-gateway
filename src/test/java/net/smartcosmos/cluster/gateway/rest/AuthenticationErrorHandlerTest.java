package net.smartcosmos.cluster.gateway.rest;

import java.io.IOException;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClientException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthenticationErrorHandlerTest {

    @Spy
    private final AuthenticationErrorHandler errorHandler = new AuthenticationErrorHandler();

    @Mock
    ClientHttpResponse response;

    @Before
    public void setUp() throws IOException {

        when(response.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(response.getStatusText()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        when(response.getHeaders()).thenReturn(mock(HttpHeaders.class));
    }

    @After
    public void tearDown() {

        reset(response, errorHandler);
    }

    // region hasError()

    @Test
    public void thatHasErrorReturnsTrueInCaseOfBadRequest() throws Exception {

        when(response.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);

        assertTrue(errorHandler.hasError(response));
    }

    @Test
    public void thatHasErrorReturnsFalseInCaseOfOk() throws Exception {

        when(response.getStatusCode()).thenReturn(HttpStatus.OK);

        assertFalse(errorHandler.hasError(response));
    }

    @Test
    public void thatHasErrorRespondsLikeBaseClass() throws Exception {

        final ResponseErrorHandler baseErrorHandler = new DefaultResponseErrorHandler();

        Boolean expectedResult = baseErrorHandler.hasError(response);
        Boolean result = errorHandler.hasError(response);

        assertNotNull(expectedResult);
        assertNotNull(result);
        assertEquals(expectedResult, result);
    }

    // endregion

    // region handleError()

    @Test(expected = BadCredentialsException.class)
    public void thatHandleErrorThrowsBadCredentialsExceptionInCaseOfBadRequest() throws Exception {

        when(response.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);

        errorHandler.handleError(response);
    }

    @Test
    public void thatHandleErrorThrowsExceptionLikeBaseClass() throws Exception {

        final ResponseErrorHandler baseErrorHandler = new DefaultResponseErrorHandler();

        Exception expectedException = null;
        Exception exception = null;

        try {
            baseErrorHandler.handleError(response);
        } catch (RestClientException e) {
            expectedException = e;
        }

        try {
            errorHandler.handleError(response);
        } catch (RestClientException e) {
            exception = e;
        }

        assertNotNull(expectedException);
        assertNotNull(exception);
        assertEquals(expectedException.toString(), exception.toString());
    }

    // endregion

    // region getErrorDescriptionFromBody()

    @Test
    public void thatGetErrorDescriptionFromBodySucceeds() throws Exception {

        final String field = "error_description";
        final String expectedDescription = "some arbitrary error description";
        final String responseBody = String.format("{\"key1\":\"value1\",\"%s\":\"%s\",\"key3\":\"value3\"}", field, expectedDescription);

        String description = errorHandler.getErrorDescriptionFromBody(responseBody);

        assertEquals(expectedDescription, description);
    }

    @Test
    public void thatGetErrorDescriptionFromBodyReturnsFallbackMessageForNotFoundField() throws Exception {

        final String field = "someOtherField";
        final String expectedDescription = "Invalid username or password";
        final String responseBody = String.format("{\"%s\":\"%s\"}", field, "some arbitrary error description");

        String description = errorHandler.getErrorDescriptionFromBody(responseBody);

        assertEquals(expectedDescription, description);
    }

    @Test
    public void thatGetErrorDescriptionFromBodyReturnsFallbackMessageForEmptyBody() throws Exception {

        final String expectedDescription = "Invalid username or password";
        final String responseBody = "";

        String description = errorHandler.getErrorDescriptionFromBody(responseBody);

        assertEquals(expectedDescription, description);
    }

    @Test
    public void thatGetErrorDescriptionFromBodyReturnsFallbackMessageForNonJsonBody() throws Exception {

        final String expectedDescription = "Invalid username or password";
        final String responseBody = "Non-JSON response body";

        String description = errorHandler.getErrorDescriptionFromBody(responseBody);

        assertEquals(expectedDescription, description);
    }

    // endregion

}

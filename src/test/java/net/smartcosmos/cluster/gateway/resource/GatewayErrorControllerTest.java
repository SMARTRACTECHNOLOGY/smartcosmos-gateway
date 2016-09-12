package net.smartcosmos.cluster.gateway.resource;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.netflix.zuul.context.RequestContext;

import org.hamcrest.CoreMatchers;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import net.smartcosmos.cluster.gateway.GatewayApplication;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static net.smartcosmos.cluster.gateway.resource.GatewayErrorController.ATTR_ERROR_EXCEPTION;
import static net.smartcosmos.cluster.gateway.resource.GatewayErrorController.ATTR_ERROR_MESSAGE;
import static net.smartcosmos.cluster.gateway.resource.GatewayErrorController.ATTR_ERROR_STATUS_CODE;

/**
 * Test the error controller.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = { GatewayApplication.class, GatewayErrorControllerTest.TestConfiguration.class })
@WebAppConfiguration
@ActiveProfiles("test")
@IntegrationTest({ "spring.cloud.config.enabled=false", "eureka.client.enabled:false" })
public class GatewayErrorControllerTest {

    public static final String URL_INTERNAL_SERVER_ERROR = "/internalServerError";
    public static final int EXPECTED_HTTP_STATUS_CODE = HttpStatus.INTERNAL_SERVER_ERROR.value();
    MockMvc mockMvc;

    @Autowired
    WebApplicationContext webApplicationContext;

    @Captor
    ArgumentCaptor argumentCaptor;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);

        this.mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .build();
    }

    /**
     * Test that logging happens.  A merging of the ideas from
     * - https://dzone.com/articles/unit-testing-asserting-line
     * - http://bloodredsun.com/2010/12/09/checking-logging-in-unit-tests/
     *
     * @throws Exception
     */
    @Test
    public void thatErrorHandlesNoExceptionInTheContext() throws Exception {

        int EXPECTED_HTTP_STATUS_CODE = HttpStatus.INTERNAL_SERVER_ERROR.value();
        String EXPECTED_MSG_TESTING_EXCEPTION_FOUND = "Testing exception found!";

        //Zuul RequestContext
        RequestContext.getCurrentContext().set(ATTR_ERROR_STATUS_CODE, HttpStatus.INTERNAL_SERVER_ERROR.value());
        RequestContext.getCurrentContext().set(ATTR_ERROR_MESSAGE, EXPECTED_MSG_TESTING_EXCEPTION_FOUND);

        Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);

        MvcResult response = mockMvc.perform(MockMvcRequestBuilders.get(GatewayErrorController.ERROR_PATH))
            .andReturn();

        verify(mockAppender).doAppend(argumentCaptor.capture());
        LoggingEvent loggingEvent = (LoggingEvent) argumentCaptor.getValue();
        assertThat(loggingEvent.getLevel(), CoreMatchers.is(Level.WARN));
        assertThat(loggingEvent.getMessage(), containsString("exception was encountered processing"));
        assertThat(loggingEvent.getMessage(), containsString("cause: 'No exception in context'"));
        assertThat(loggingEvent.getMessage(), containsString("statusCode: '" + EXPECTED_HTTP_STATUS_CODE + "'"));
        assertThat(loggingEvent.getMessage(), containsString("message: '" + EXPECTED_MSG_TESTING_EXCEPTION_FOUND + "'"));
    }

    @Test
    public void thatErrorHandlesNoMessageInTheContext() throws Exception {

    }

    /**
     * Test that logging happens.  A merging of the ideas from
     * - https://dzone.com/articles/unit-testing-asserting-line
     * - http://bloodredsun.com/2010/12/09/checking-logging-in-unit-tests/
     *
     * @throws Exception
     */
    @Test
    public void thatErrorIsHandledWithMessageAndException() throws Exception {

        int EXPECTED_HTTP_STATUS_CODE = HttpStatus.INTERNAL_SERVER_ERROR.value();
        String EXPECTED_MSG_TESTING_EXCEPTION_FOUND = "Testing exception found!";
        Exception EXPECTED_EXCEPTION = new InternalServerErrorException(EXPECTED_MSG_TESTING_EXCEPTION_FOUND);

        //Zuul RequestContext
        RequestContext.getCurrentContext().set(ATTR_ERROR_STATUS_CODE, HttpStatus.INTERNAL_SERVER_ERROR.value());
        RequestContext.getCurrentContext().set(ATTR_ERROR_EXCEPTION, EXPECTED_EXCEPTION);
        RequestContext.getCurrentContext().set(ATTR_ERROR_MESSAGE, EXPECTED_MSG_TESTING_EXCEPTION_FOUND);

        Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);

        MvcResult response = mockMvc.perform(MockMvcRequestBuilders.get(GatewayErrorController.ERROR_PATH))
            .andReturn();

        verify(mockAppender).doAppend(argumentCaptor.capture());
        LoggingEvent loggingEvent = (LoggingEvent) argumentCaptor.getValue();
        assertThat(loggingEvent.getLevel(), CoreMatchers.is(Level.WARN));
        assertThat(loggingEvent.getMessage(), containsString("exception was encountered processing"));
        assertThat(loggingEvent.getMessage(), containsString("message: '" + EXPECTED_MSG_TESTING_EXCEPTION_FOUND + "'"));
        assertThat(loggingEvent.getMessage(), containsString("statusCode: '" + EXPECTED_HTTP_STATUS_CODE + "'"));
        assertThat(loggingEvent.getMessage(),
                   containsString("cause: '" + InternalServerErrorException.class.getName() + ": " + EXPECTED_MSG_TESTING_EXCEPTION_FOUND + "'"));
    }

    @Test
    public void getErrorPath() throws Exception {

    }

    private class ErrorDispatcher implements RequestBuilder {

        private MvcResult result;

        private String path;

        ErrorDispatcher(MvcResult result, String path) {

            this.result = result;
            this.path = path;
        }

        @Override
        public MockHttpServletRequest buildRequest(ServletContext servletContext) {

            MockHttpServletRequest request = this.result.getRequest();
            request.setDispatcherType(DispatcherType.ERROR);
            request.setRequestURI(this.path);
            return request;
        }
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    private static class InternalServerErrorException extends RuntimeException {

        InternalServerErrorException(String string) {

            super(string);
        }

    }

    @Configuration
    public static class TestConfiguration {

        @RestController
        protected static class Errors {

            @RequestMapping(URL_INTERNAL_SERVER_ERROR)
            public String bang() {

                throw new InternalServerErrorException("Expected");
            }
        }
    }
}

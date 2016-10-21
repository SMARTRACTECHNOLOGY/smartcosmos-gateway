package net.smartcosmos.cluster.gateway.util;

import org.junit.*;
import org.junit.runner.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import net.smartcosmos.cluster.gateway.config.ErrorMessageTestConfiguration;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ErrorMessageTestConfiguration.class })
public class MessageServiceTest {

    private static final String GATEWAY_WARNING_MESSAGE_000001 = "gateway.W000001";
    private static final String INVALID_MESSAGE_CODE = "an.invalid.message";
    private static final String GATEWAY_ERROR_MESSAGE_0000001 = "gateway.E000001";
    private static final String PREFIX_GATEWAY = "gateway";
    private static final String ERROR_CODE_000003 = "E000003";
    private static final String INFO_MESSAGE_000001 = "I000001";
    private static final String INFO_MESSAGE_000002 = "I000002";
    private static final String GATEWAY_INFO_MESSAGE_000002 = String.join(".", PREFIX_GATEWAY, INFO_MESSAGE_000002);

    @Autowired
    private MessageService messageService;

    @Test
    public void thatTestsWillWork() {

        assertNotNull(messageService);
    }

    @Test
    public void thatGetMessageReturnsCorrectDefaultMessage() throws Exception {

        String expectedMessage = "This is a default message";
        assertEquals(expectedMessage, messageService.getMessage(INVALID_MESSAGE_CODE, null, expectedMessage));
    }

    @Test
    public void thatGetMessageWithDefaultProperlyReplacesArguments() throws Exception {

        String anArgument = "anArgument";
        String anotherArgument = "anotherArgument";
        String[] args = { anArgument, anotherArgument };

        String expectedMessage = String.format("test warning message 1, param 1: %s, param 2: %s", anArgument, anotherArgument);
        String defaultMessage = "This should  not happen!";
        assertEquals(expectedMessage, messageService.getMessage(GATEWAY_WARNING_MESSAGE_000001, args, defaultMessage));
    }

    @Test
    public void thatGetMessageWorks() throws Exception {

        assertEquals("Test error message 1", messageService.getMessage(GATEWAY_ERROR_MESSAGE_0000001));
    }

    @Test
    public void thatGetMessageReturnsMessageIdWhenNotFound() throws Exception {

        assertEquals(INVALID_MESSAGE_CODE, messageService.getMessage(INVALID_MESSAGE_CODE));
    }

    @Test
    public void thatGetMessageWithPrefixWorks() throws Exception {

        String expectedMessage = "test error message 3";
        assertEquals(expectedMessage, messageService.getMessage(PREFIX_GATEWAY, ERROR_CODE_000003));
    }

    @Test
    public void thatGetMessageWithPrefixAndArgumentsWorks() throws Exception {

        String anArgument = "anArgument";
        String anotherArgument = "anotherArgument";
        String[] args = { anArgument, anotherArgument };

        String expectedMessage = String.format("Test informational message 1, param 1: %s, param 2: %s", anArgument, anotherArgument);
        String defaultMessage = "This should  not happen!";
        assertEquals(expectedMessage, messageService.getMessage(PREFIX_GATEWAY, INFO_MESSAGE_000001, args));
    }

    @Test
    public void thatGetMessageWithPrefixAndArgsWorks() throws Exception {

        String anArgument = "wonky";
        String anotherArgument = "goonies";
        String[] args = { anArgument, anotherArgument };

        String expectedMessage = String.format("This is another %s test message for %s", anArgument, anotherArgument);
        String defaultMessage = "This should  not happen!";
        assertEquals(expectedMessage, messageService.getMessage(PREFIX_GATEWAY, INFO_MESSAGE_000002, args));
    }

    @Test
    public void thatGetMessageWithArgsWorks() throws Exception {

        String anArgument = "wonky";
        String anotherArgument = "goonies";
        String[] args = { anArgument, anotherArgument };

        String expectedMessage = String.format("This is another %s test message for %s", anArgument, anotherArgument);
        String defaultMessage = "This should  not happen!";
        assertEquals(expectedMessage, messageService.getMessage(GATEWAY_INFO_MESSAGE_000002, args));

    }

    @Test
    public void thatGetMessageWithPrefixArgumentsAndDefaultMessageWorks() throws Exception {

        String anArgument = "anArgument";
        String anotherArgument = "anotherArgument";
        String[] args = { anArgument, anotherArgument };

        String expectedMessage = String.format("Test informational message 1, param 1: %s, param 2: %s", anArgument, anotherArgument);
        String defaultMessage = "This should  not happen!";
        assertEquals(expectedMessage, messageService.getMessage(PREFIX_GATEWAY, INFO_MESSAGE_000001, args, defaultMessage));
    }

}

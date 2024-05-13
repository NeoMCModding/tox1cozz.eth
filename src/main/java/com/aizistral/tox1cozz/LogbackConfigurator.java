package com.aizistral.tox1cozz;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.FilterReply;

public class LogbackConfigurator extends ContextAwareBase implements Configurator {

    public LogbackConfigurator() {
        // NO-OP
    }

    @Override
    public ExecutionStatus configure(LoggerContext context) {
        this.addInfo("Setting up custom configuration.");

        LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
        encoder.setContext(context);

        PatternLayout layout = new PatternLayout();
        layout.setPattern("[%d{HH:mm:ss.SSS}] [T:%thread] [%level] [%logger{16}]: %msg%n");
        layout.setContext(context);
        layout.start();

        encoder.setLayout(layout);

        ConsoleAppender<ILoggingEvent> console = new ConsoleAppender<>();
        console.setContext(context);
        console.setName("console");
        console.addFilter(new Filter<ILoggingEvent>() {
            @Override
            public FilterReply decide(ILoggingEvent event) {
                return event.getLevel().isGreaterOrEqual(Level.INFO) ? FilterReply.ACCEPT : FilterReply.DENY;
            }
        });
        console.setEncoder(encoder);
        console.start();

        FileAppender<ILoggingEvent> fileStd = new FileAppender<>();
        fileStd.setContext(context);
        fileStd.setName("stdlog");
        fileStd.setFile("machine.log");
        fileStd.addFilter(new Filter<ILoggingEvent>() {
            @Override
            public FilterReply decide(ILoggingEvent event) {
                return event.getLevel().isGreaterOrEqual(Level.INFO) ? FilterReply.ACCEPT : FilterReply.DENY;
            }
        });
        fileStd.setEncoder(encoder);
        fileStd.start();

        FileAppender<ILoggingEvent> fileErr = new FileAppender<>();
        fileErr.setContext(context);
        fileErr.setName("errlog");
        fileErr.setFile("machine.error.log");
        fileErr.addFilter(new Filter<ILoggingEvent>() {
            @Override
            public FilterReply decide(ILoggingEvent event) {
                return event.getLevel().isGreaterOrEqual(Level.ERROR) ? FilterReply.ACCEPT : FilterReply.DENY;
            }
        });
        fileErr.setEncoder(encoder);
        fileErr.start();

        FileAppender<ILoggingEvent> fileDebug = new FileAppender<>();
        fileDebug.setContext(context);
        fileDebug.setName("dbglog");
        fileDebug.setFile("machine.debug.log");
        fileDebug.addFilter(new Filter<ILoggingEvent>() {
            @Override
            public FilterReply decide(ILoggingEvent event) {
                return FilterReply.ACCEPT;
            }
        });
        fileDebug.setEncoder(encoder);
        fileDebug.start();

        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(console);
        rootLogger.addAppender(fileStd);
        rootLogger.addAppender(fileErr);
        rootLogger.addAppender(fileDebug);

        return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY;
    }

}


package ru.practicum.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import ru.practicum.analyzer.domain.processor.SimilarityProcessor;
import ru.practicum.analyzer.domain.processor.UserActionProcessor;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Analyzer {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Analyzer.class, args);

        UserActionProcessor userActionProcessor = context.getBean(UserActionProcessor.class);
        SimilarityProcessor similarityProcessor = context.getBean(SimilarityProcessor.class);

        startUserActionsThread(userActionProcessor);
        similarityProcessor.start();
    }

    private static void startUserActionsThread(UserActionProcessor userActionProcessor) {
        Thread userActionsThread = new Thread(userActionProcessor);
        userActionsThread.setName("UserActionHandlerThread");
        userActionsThread.start();
    }
}
